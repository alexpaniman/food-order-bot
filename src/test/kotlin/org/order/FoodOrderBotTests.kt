package org.order

import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.BeforeClass
import org.junit.Test
import org.order.FoodOrderBotTests.Keyboard.*
import org.order.bot.send.SenderContext
import org.order.data.entities.User
import org.order.data.tables.*
import org.order.logic.corpus.Text
import org.order.logic.impl.FoodOrderBot
import org.order.logic.impl.commands.DATABASE_DRIVER
import org.order.logic.impl.commands.JDBC_DATABASE_URL
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import kotlin.test.assertTrue
import org.telegram.telegrambots.meta.api.objects.Message as TMessage

@Suppress("unused", "SameParameterValue")

class FoodOrderBotTests {
    private companion object {
        @BeforeClass
        @JvmStatic
        fun connectToDB() {
            Database.connect(url = JDBC_DATABASE_URL, driver = DATABASE_DRIVER)

            transaction {
                SchemaUtils.create(Admins, Clients, Dishes,
                        Grades, Menus, Orders, Parents,
                        Payments, Producers, Relations, Users
                )

                Grades.batchInsert(listOf("7", "8", "9", "10", "11", "12")) { // Create grades
                    this[Grades.name] = it
                }
            }
        }
    }

    private sealed class Keyboard {
        data class InlineButton(val text: String, val callback: String) {
            lateinit var message: Message
        }

        data class ReplyButton(val text: String) {
            lateinit var message: Message
        }

        data class Inline(val buttons: List<List<InlineButton>>) : Keyboard()
        data class Reply(val buttons: List<List<ReplyButton>>) : Keyboard()
        object Remove : Keyboard()
        object None : Keyboard()
    }

    private data class Message(val chatId: Int, val text: String, val keyboard: Keyboard) {
        init {
            when (keyboard) {
                is Inline -> keyboard.buttons.forEach { row ->
                    row.forEach {
                        it.message = this
                    }
                }
                is Reply -> keyboard.buttons.forEach { row ->
                    row.forEach {
                        it.message = this
                    }
                }
            }
        }
    }

    private val chats: MutableMap<Int, MutableList<Message>> = mutableMapOf()

    private class QueueList<T> : ArrayList<T>() {
        override fun get(index: Int): T =
                if (index >= 0) super.get(index)
                else super.get(lastIndex + index + 1)
    }

    private fun ReplyKeyboard?.toKeyboard() = when (this) {
        is InlineKeyboardMarkup -> Inline(
                this.keyboard.map { row ->
                    row.map {
                        InlineButton(it.text, it.callbackData)
                    }
                }
        )
        is ReplyKeyboardMarkup -> Reply(
                this.keyboard.map { row ->
                    row.map {
                        ReplyButton(it.text)
                    }
                }
        )
        is ReplyKeyboardRemove -> Remove
        else -> None
    }

    private val senderContext: SenderContext = mockk {
        every { any<User>().send(any(), any(), any()) } answers {

            val user: User = arg(0)
            val text: String = arg(1)
            val mark: Boolean = arg(2)
            val init: SendMessage.() -> Unit = arg(3)

            val send = SendMessage().apply(init).apply {
                this.chatId = user.chat!!.toString()
                this.text = text

                this.enableMarkdown(mark)
            }

            if (send.text != null)
                chats.computeIfAbsent(send.chatId.toInt()) {
                    QueueList()
                } += Message(
                        send.chatId.toIntOrNull() ?: error("Illegal chat!"),
                        send.text,
                        send.replyMarkup.toKeyboard()
                )

            mockk()
        }

        every { any<TMessage>().edit(any(), any(), any()) } answers {
            val message: TMessage = arg(0)
            val text: String = arg(1)
            val mark: Boolean = arg(2)
            val init: EditMessageText.() -> Unit = arg(3)

            val edit = EditMessageText().apply(init).apply {
                this.text = text
                this.chatId = message.chatId.toString()
                this.messageId = message.messageId

                this.enableMarkdown(mark)
            }

            val chatId = edit.chatId.toInt()
            val keyboard = edit.replyMarkup.toKeyboard()

            chats[chatId]!![edit.messageId] = Message(
                    chatId,
                    edit.text,
                    keyboard
            )

            mockk()
        }
    }

    private val bot = FoodOrderBot(senderContext, "", "")

    private fun Int.sendText(text: String) {
        val update = mockk<Update>(relaxed = true) {
            every { message.text } returns text
            every { message.from.id } returns this@sendText
        }

        bot.onUpdateReceived(update)
    }

    private fun InlineButton.answer() {
        val update = mockk<Update>(relaxed = true) {
            every { callbackQuery.data } returns callback
            every { callbackQuery.from.id } returns this@answer.message.chatId

            every { callbackQuery.message } returns mockk {
                every { chatId } returns this@answer.message.chatId.toLong()
                every { messageId } returns chats[this@answer.message.chatId]!!
                        .indexOf(this@answer.message)
            }
        }

        bot.onUpdateReceived(update)
    }

    private fun ReplyButton.answer() = message.chatId.sendText(text)

    private val Message.inline get() = (this.keyboard as Inline).buttons
    private val Message.reply get() = (this.keyboard as Reply).buttons

    private val Int.chat get() = chats.computeIfAbsent(this) { QueueList() }

    private fun chat(num: Int, run: Int.() -> Unit) = num.run(run)

    @Test
    fun `a new student has come`() = chat(0) {
        sendText("/start")

        assertTrue { chat[-2].text    == Text["greeting"] }
        assertTrue { chat.last().text == Text["register-name"] }

        sendText("three words")

        assertTrue { chat.last().text == Text["wrong-name"] }

        sendText("WRONG WRONG")

        assertTrue { chat.last().text == Text["wrong-name"] }

        sendText("Имя Фамилия")

        assertTrue { chat.last().text == Text["register-phone"] }

        sendText("+000000000000")

        assertTrue { chat.last().text == Text["wrong-phone"] }

        sendText("+380500000000")

        assertTrue { chat.last().text == Text["register-role"] }
        assertTrue { chat.last().reply.size == 5 }
        assertTrue { chat.last().reply[0][0].text == Text["student"] }

        chat.last().reply[0][0].answer()

        assertTrue { chat.last().text == Text["register-grade"] }

        chat.last().reply[0][0].answer()
    }

    @Test
    fun `test bot when new teacher is come`() = chat(1) {
        sendText("some initial text")
        sendText("***REMOVED*** ***REMOVED***")
        sendText("+000000000000")

        chat.last().reply[2][0].answer()

        assertTrue { chat.last().text == Text["register-child-name"] }
    }
}