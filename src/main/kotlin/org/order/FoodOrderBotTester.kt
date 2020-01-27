package org.order

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import org.order.FoodOrderBotTester.Keyboard.*
import org.order.bot.send.SenderContext
import org.order.data.entities.*
import org.order.data.tables.*
import org.order.logic.impl.FoodOrderBot
import org.order.logic.impl.commands.DATABASE_DRIVER
import org.order.logic.impl.commands.JDBC_DATABASE_URL
import org.order.logic.impl.utils.Schedule
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import java.io.File
import java.lang.Thread.sleep
import org.telegram.telegrambots.meta.api.objects.Message as TMessage

@Suppress("unused", "SameParameterValue")

class FoodOrderBotTester {
    private sealed class Keyboard {
        data class InlineButton(val text: String, val callback: String) {
            lateinit var message: Message
        }
        data class ReplyButton (val text: String) {
            lateinit var message: Message
        }

        data class Inline(val buttons: List<List<InlineButton>>) : Keyboard()
        data class Reply (val buttons: List<List<ReplyButton>> ) : Keyboard()
        object Remove : Keyboard()
        object None   : Keyboard()

        fun asArray() = when(this) {
            is Inline -> this.buttons.map { row ->
                row.map { "${it.text} : ${it.callback}" }
                        .toTypedArray()
            }.toTypedArray()

            is Reply  -> this.buttons.map { row ->
                row.map { it.text }
                        .toTypedArray()
            }.toTypedArray()

            is Remove -> arrayOf(arrayOf("REMOVE"))
            is None   -> arrayOf()
        }
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

    private fun String.cut(len: Int) = substring(len, length - len)

    private fun String.ansi(num: Int) = "\u001B[${num}m$this\u001B[0m"

    private fun String.bold() = ansi(1)
    private fun String.code() = ansi(93)
    private fun String.italic() = ansi(3)

    private fun Message.asString(): String {
        val table = table {
            cellStyle {
                border = true
                alignment = TextAlignment.MiddleCenter
            }

            val asArray = keyboard.asArray()
            val maxLength = asArray
                    .map { it.size }
                    .max() ?: 1

            row {
                val coloredText = text.replace("([*_]|```|`).*?\\1".toRegex()) {
                    it.value.run {
                        when {
                            startsWith("```") -> cut(3).code()
                            startsWith('`')    -> cut(1).code()
                            startsWith('*')  -> cut(1).bold()
                            startsWith('_')  -> cut(1).italic()

                            else -> error("")
                        }
                    }
                }

                cell(coloredText) {
                    border = false
                    columnSpan = maxLength
                }
            }

            for (row in asArray) {
                val perCell = maxLength / row.size
                val withoutLast = row.dropLast(1)
                row {
                    for (content in withoutLast)
                        cell(content) {
                            columnSpan = perCell
                        }

                    cell(row.last()) {
                        columnSpan = maxLength - perCell * withoutLast.size
                    }
                }
            }
        }.toString()

        val lines = table.lines()
        val edge = "-".repeat(lines[lines.lastIndex - 1].length)
        return edge + "\n" + table + edge
    }

    private var displayMessages = false
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

            val chatId = send.chatId.toInt()
            val keyboard = send.replyMarkup.toKeyboard()

            val message = Message(chatId, text, keyboard)

            chats.computeIfAbsent(chatId) {
                QueueList()
            } += message

            val messageId = chats[chatId]!!.size - 1

            if (displayMessages && active == chatId)
                println("\nMessage [$messageId] in active chat [$active]:\n" + message.asString())

            mockk()
        }

        every { any<TMessage>().edit(any(), any(), any()) } answers {
            val message: TMessage = arg(0)
            val text: String = arg(1)
            val mark: Boolean = arg(2)
            val init: InlineKeyboardMarkup.() -> Unit = arg(3)

            val edit = EditMessageText().apply {
                this.text = text
                this.chatId = message.chatId.toString()
                this.messageId = message.messageId

                this.replyMarkup = InlineKeyboardMarkup().apply(init)

                this.enableMarkdown(mark)
            }

            val chatId = edit.chatId.toInt()
            val messageId = edit.messageId

            val keyboard = edit.replyMarkup.toKeyboard()

            chats[chatId]!![messageId] = Message(
                    chatId,
                    edit.text,
                    keyboard
            ).apply {
                if (displayMessages && active?.toLong() == message.chatId)
                    println("\nMessage [$messageId] in active chat [$active]:\n" + this.asString())
            }

            mockk()
        }

        every { any<TMessage>().delete() } answers {
            val message: TMessage = arg(0)

            val chatId = message.chatId.toInt()
            val messageId = message.messageId.toInt()

            chats[chatId]!!.removeAt(messageId)

            if (displayMessages && active == chatId)
                print("Message [$messageId] in chat [$chatId] was deleted!")

            true
        }
    }

    private val bot = FoodOrderBot(senderContext, "", "")

    private fun Int.sendText(text: String) {
        val update = mockk<Update> {
            every { message.text } returns text
            every { message.from.id } returns this@sendText

            every { message.successfulPayment } returns null
            every { callbackQuery } returns null
            every { preCheckoutQuery } returns null
        }

        bot.onUpdateReceived(update)
    }

    private fun InlineButton.answer() {
        val update = mockk<Update> {
            every { callbackQuery.data } returns callback
            every { callbackQuery.from.id } returns this@answer.message.chatId

            every { callbackQuery.message } returns mockk {
                every { chatId } returns this@answer.message.chatId.toLong()
                every { messageId } returns chats[this@answer.message.chatId]!!
                        .indexOf(this@answer.message)
            }

            every { message } returns null
            every { preCheckoutQuery } returns null
        }

        bot.onUpdateReceived(update)
    }

    private fun ReplyButton.answer() = message.chatId.sendText(text)

    private val Message.inline get() = (this.keyboard as Inline).buttons
    private val Message.reply get() = (this.keyboard as Reply).buttons

    private fun chat(index: Int) = chats.computeIfAbsent(index) { QueueList() }

    private fun activeChat(execute: (List<Message>) -> String) = when (val chat = chats[active]) {
        null -> "There's no active chat!"
        else -> execute(chat)
    }

    private var active: Int? = null
    private fun executeCommand(name: String, args: List<String>): String = when (name) {
        "list" -> chats.keys.joinToString(" ")
        "mkchat" -> when (val chatId = args[0].toIntOrNull()) {
            null -> "ChatId isn't specified!"
            in chats -> "Chat [$chatId] already exists!"
            else -> {
                chats[chatId] = QueueList()
                active = chatId
                "Chat [$chatId] was created and set as active."
            }
        }
        "rmchat" -> {
            val chatId = args[0].toIntOrNull()
            if (chatId !in chats)
                "Chat [$chatId] already doesn't exist!"
            else {
                chats.remove(chatId)
                "Chat [$chatId] was deleted."
            }
        }
        "chchat" -> {
            val chatId = args[0].toIntOrNull()
            if (chatId !in chats)
                "Chat [$chatId] doesn't exist!"
            else {
                active = chatId
                "Chat [$chatId] was set as active."
            }
        }
        "last" -> activeChat { chat ->
            val messageId = -(args.getOrNull(0)?.toInt() ?: 1)
            "Message [${chat.lastIndex + messageId + 1}] in active chat [$active]:\n" +
                    chat[messageId].asString()
        }
        "display" -> activeChat { chat ->
            buildString {
                for (messageId in chat.indices) {
                    appendln("Message [$messageId] in active chat [$active]:")
                    appendln(chat[messageId].asString())
                    appendln()
                }
            }
        }
        "reply" -> activeChat { chat ->
            val pos = Pair(
                    args.getOrNull(0)?.toIntOrNull() ?: 0,
                    args.getOrNull(1)?.toIntOrNull() ?: 0
            )

            val message = chat.last { it.keyboard is Reply }
            val keyboard = message.keyboard as Reply

            keyboard.buttons[pos.first][pos.second].answer()
            "Button at [row = ${pos.first}, column = ${pos.second}] was clicked."
        }
        "inline" -> activeChat { chat ->
            val pos = Pair(
                    args.getOrNull(0)?.toIntOrNull() ?: 0,
                    args.getOrNull(1)?.toIntOrNull() ?: 0
            )

            val messages = chat
                    .filter { it.keyboard is Inline }
                    .map { it.keyboard as Inline }
                    .map { it.buttons }
                    .asReversed()

            val messageId = args.getOrNull(2)?.toIntOrNull() ?: 0
            messages[messageId][pos.first][pos.second].answer()
            "Button at [row = ${pos.first}, column = ${pos.second}] in $messageId last message with inline was clicked."
        }
        "sendText" -> activeChat {
            if (args.isNotEmpty()) {
                active!!.sendText(args.joinToString(" "))
                "Message sent to active chat [$active]."
            }
            else {
                val builder = StringBuilder()
                while (true) {
                    print("... ")
                    builder.append(readLine()!!)
                    if (builder.last() == '\\') {
                        builder.deleteCharAt(builder.lastIndex)
                        builder.appendln()
                    } else break
                }
                val text = builder.trim().toString()
                active!!.sendText(text)
                "Message sent to active chat [$active]."
            }
        }
        "setTime" -> {
            if (args.isEmpty()) {
                unmockkStatic(LocalDate::class, DateTime::class, LocalTime::class)
                "Time line restored!"
            }
            else {
                val time = DateTime.parse(
                        args.joinToString(" "),
                        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm")
                )
                val millis = System.currentTimeMillis()

                fun newTime() = time.plusMillis((System.currentTimeMillis() - millis).toInt())

                mockkStatic(LocalDate::class, DateTime::class, LocalTime::class)
                every { DateTime.now() } answers { newTime() }
                every { LocalDate.now() } answers { newTime().toLocalDate() }
                every { LocalTime.now() } answers { newTime().toLocalTime() }
                "Current time was shifted to ${newTime()}"
            }

        }
        else -> {
            val builder = StringBuilder((listOf(name) + args).joinToString(" "))
            while (true) {
                if (builder.last() == '\\') {
                    builder.deleteCharAt(builder.lastIndex)
                    builder.appendln()
                } else break
                print("... ")
                builder.append(readLine()!!)
            }
            val text = builder.trim().toString()
            active!!.sendText(text)
            "Message sent to active chat [$active]."
        }
    }

    private fun String.eval(): String {
        val tokens = split(' ')
        val name = tokens.getOrElse(0) { "" }
        val args = tokens.drop(1)
        return executeCommand(name, args)
    }

    fun String.runScript(display: Boolean) {
        if (this.isBlank())
            return

        displayMessages = display

        for (line in lines()) try {
            when {
                line.startsWith("echo#") -> println(line.removePrefix("echo#").eval())
                !line.startsWith('#') -> line.eval()
            }
        } catch (exc: Exception) {
            println("Script error occurs:")
            exc.printStackTrace()
        }
    }

    fun startREPL() {
        displayMessages = true
        while (true) try {
            sleep(20)
            print("[time : ${DateTime.now().toString("yyyy-MM-dd HH:mm:ss")}, chat : ${active?.toString() ?: "-"}] $ ")
            val read = readLine()!!
            if (read.isBlank())
                continue
            val output = read.eval()
            println(output)
        } catch (exc: Exception) {
            println("Exception occurs:")
            exc.printStackTrace()
        }
    }
}

const val PRE_TEST_SCRIPT = "src/main/resources/pre-test.bt"

private fun createMenu(name: String, cost: Float, schedule: String, vararg dishes: String) {
    val menu = Menu.new {
        this.name = name
        this.cost = cost
        this.schedule = Schedule.parse(schedule)
    }

    for (dish in dishes)
        Dish.new {
            this.menu = menu
            this.name = dish
        }
}

private fun createGrades(vararg grades: String) = grades.map { grade ->
    Grade.new {
        this.name = grade
    }
}

private fun createAdmin(chat: Int, name: String, phone: String) {
    Admin.new {
        this.user = User.new {
            this.name = name
            this.phone = phone
            this.state = State.COMMAND
            this.valid = true
            this.chat = chat
        }
    }
}

private fun createCoordinator(grade: Grade, chat: Int, name: String, phone: String) {
    Coordinator.new {
        this.user = User.new {
            this.name = name
            this.phone = phone
            this.state = State.COMMAND
            this.valid = true
            this.chat = chat
        }

        this.grade = grade
    }
}

fun main() {
    Database.connect(url = JDBC_DATABASE_URL, driver = DATABASE_DRIVER)

    transaction {
        SchemaUtils.create(
                Teachers, Admins, Clients, Dishes, Grades,
                Menus, Orders, Parents, Payments, Producers,
                Relations, Coordinators, Users,
                OrdersCancellations
        )

        createAdmin(0, "Аоооо Аооо", "+380669360000")

        val grades = createGrades("9-Ф", "10-Ф", "11-ИКТ", "8-Б")
        createCoordinator(grades[0], 1, "Абв Авб", "+380669363333")
        createCoordinator(grades[1], 2, "Абгв Авб", "+380669364444")
        createCoordinator(grades[2], 3, "Абв Агвб", "+380669365555")
        createCoordinator(grades[3], 4, "Абв Авбг", "+380669366666")

        createMenu("1", 55.0f, "2020-01-27:14", "Меню 1:1:1", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-01-28:14", "Меню 1:1:2", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-01-29:14", "Меню 1:1:3", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-01-30:14", "Меню 1:1:4", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-01-31:14", "Меню 1:1:5", "блюдо 1", "блюдо 2")

        createMenu("1", 55.0f, "2020-02-03:14", "Меню 1:2:1", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-02-04:14", "Меню 1:2:2", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-02-05:14", "Меню 1:2:3", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-02-06:14", "Меню 1:2:4", "блюдо 1", "блюдо 2")
        createMenu("1", 55.0f, "2020-02-07:14", "Меню 1:2:5", "блюдо 1", "блюдо 2")

        createMenu("2", 42.0f, "2020-01-27:7", "Меню 2:1", "блюдо 1")
        createMenu("2", 42.0f, "2020-01-28:7", "Меню 2:2", "блюдо 1")
        createMenu("2", 41.0f, "2020-01-29:7", "Меню 2:3", "блюдо 1")
        createMenu("2", 43.0f, "2020-01-30:7", "Меню 2:4", "блюдо 1")
        createMenu("2", 43.0f, "2020-01-31:7", "Меню 2:5", "блюдо 1")
    }

    FoodOrderBotTester().apply {
        File(PRE_TEST_SCRIPT)
                .readText()
                .runScript(display = true)

        startREPL()
    }
}