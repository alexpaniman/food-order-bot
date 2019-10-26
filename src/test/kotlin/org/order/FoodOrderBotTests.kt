package org.order

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.order.FoodOrderBotTests.Keyboard.*
import org.order.bot.send.Sender
import org.order.data.entities.Grade
import org.order.data.entities.User
import org.order.data.tables.*
import org.order.logic.corpus.Text
import org.order.logic.impl.bot.FoodOrderBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.Message as TMessage
import kotlin.test.assertTrue

@Suppress("unused", "SameParameterValue")

class FoodOrderBotTests {
    private companion object {
        @BeforeClass
        @JvmStatic
        fun connectToDB() {
            Database.connect(
                    url = System.getenv("JDBC_DATABASE_URL"),
                    driver = System.getenv("DATABASE_DRIVER")
            )
            transaction {
                SchemaUtils.create(
                        Admins,
                        Clients,
                        Dishes,
                        Grades,
                        Menus,
                        Orders,
                        Parents,
                        Payments,
                        Producers,
                        Relations,
                        Users
                )
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

    private val sender: Sender = mockk {
        every { any<User>().send(any(), any(), any()) } answers {

            val user: User = arg(0)
            val text: String = arg(1)
            val mark: Boolean = arg(2)
            val init: SendMessage.() -> Unit = arg(3)

            val send = SendMessage().apply(init).apply {
                this.chatId = user.chat.toString()
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

        every { any<TMessage>().safeEdit(any(), any(), any()) } answers {
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

    private val bot = FoodOrderBot(sender, "", "")

    private fun sendText(text: String, id: Int = 0) {
        val update = mockk<Update>(relaxed = true) {
            every { message.text } returns text
            every { message.from.id } returns id
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

    private fun ReplyButton.answer() = sendText(text, message.chatId)

    private val Message.inline get() = (this.keyboard as Inline).buttons
    private val Message.reply get() = (this.keyboard as Reply).buttons

    private fun chat(num: Int) = chats.computeIfAbsent(num) { QueueList() }

    private fun mockText(vararg links: Pair<String, String>) { // TODO?
        mockkObject(Text)

        for ((key, value) in links)
            every { Text.get(key, any()) } returns value
    }

    @Before
    fun unMock() = unmockkAll()

    @Test
    fun `test bot greeting when new user is come`() {
        val chat = chat(0)
        sendText("/start")

        assertTrue { chat[-2].text == Text["greeting"] }
        assertTrue { chat.last().text == Text["register-name"] }

        sendText("Invalid")

        assertTrue { chat.last().text == Text["wrong-name"] }

        sendText("И Ф")

        assertTrue { chat.last().text == Text["wrong-name"] }

        sendText("Имя Фамилия")

        assertTrue { chat.last().text == Text["register-phone"] }

        sendText("000")

        assertTrue { chat.last().text == Text["wrong-phone"] }

        sendText("+0000000000")

        assertTrue { chat.last().text == Text["register-state"] }
        assertTrue { chat.last().reply.flatten().size == 5 }
        assertTrue { chat.last().reply[0][0].text == Text["student"] }

        transaction {
            Grade.new {
                this.name = "10"
            }
        }

        chat.last().reply[0][0].answer()

        assertTrue { chat.last().text == Text["register-grade"] }

        chat.last().reply[0][0].answer()
    }
}