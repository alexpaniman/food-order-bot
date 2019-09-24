package org.order

import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.data.entities.*
import org.order.data.entities.Right.*

import org.order.data.tables.Users

import org.order.data.entities.State.*
import org.order.data.tables.Grades
import org.order.data.tables.Menus

import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.util.WebhookUtils
import java.util.*

class FoodOrderBot(
        private val sender: Sender,

        private val username: String,
        private val token: String
) : LongPollingBot {
    override fun getOptions() = sender.options!!
    override fun clearWebhook() = WebhookUtils.clearWebhook(sender)

    override fun getBotUsername() = username
    override fun getBotToken() = token

    private val callbacks = mutableMapOf<String, Sender.(User, Message, List<String>) -> Unit>()

    private val commands = mutableMapOf<List<String>, Sender.(User) -> Unit>()
    private val readers = mutableMapOf<State, Sender.(User, Message) -> Unit>()

    private fun callback(marker: String, run: Sender.(User, Message, List<String>) -> Unit) {
        callbacks[marker] = run
    }

    private fun command(vararg names: String, run: Sender.(User) -> Unit) {
        commands[names.toList()] = run
    }

    private fun reader(state: State, run: Sender.(User, Message) -> Unit) {
        readers[state] = run
    }

    private fun fetchOrCreateUser(telegramUser: org.telegram.telegrambots.meta.api.objects.User) =
            User.find { Users.chat eq telegramUser.id }.firstOrNull() ?: User.new {
                chat = telegramUser.id

                name = null
                phone = null
                grade = null

                right = CUSTOMER
                state = COMMAND
            }

    override fun onUpdateReceived(update: Update) = transaction {
        when {
            update.hasMessage() -> {
                val message = update.message
                val user = fetchOrCreateUser(message.from)
                if (message.isUserMessage) {
                    val reader = readers
                            .filterKeys { it == user.state }
                            .map { (_, run) -> run }
                            .singleOrNull()
                    if (reader == null) {
                        if (message.hasText())
                            commands.filterKeys { it.isEmpty() || message.text in it }
                                    .forEach { (_, run) ->
                                        sender.run(user)
                                    }
                    } else sender.reader(user, message)
                }
            }
            update.hasCallbackQuery() -> {
                val callback = update.callbackQuery
                val message = callback.message
                val parts = callback.data.split(':')
                val marker = parts[0]
                val data = parts.drop(1)

                val user = fetchOrCreateUser(callback.from)
                if (message.isUserMessage) {
                    val handler = callbacks
                            .filterKeys { it == marker }
                            .map { (_, run) -> run }
                            .singleOrNull() ?: return@transaction

                    handler(sender, user, callback.message, data)
                }
            }
        }
    }

    init {
        transaction {
            val ph = Grade.new {
                name = "10-Ф"
            }
            Grade.new {
                name = "10-М"
            }
            Grade.new {
                name = "11-ХБ"
            }
            Grade.new {
                name = "7-V"
            }
            Grade.new {
                name = "5"
            }
            Grade.new {
                name = "9-Ф"
            }

            val mon = Menu.new {
                name = "223"
                active = true
                cost = 10
                schedule = 3
            }

            val mon2 = Menu.new {
                name = "1"
                active = true
                cost = 10
                schedule = 3
            }

            Dish.new {
                name = "картошка"
                menu = mon2
            }

            Dish.new {
                name = "котлета"
                menu = mon2
            }

            Dish.new {
                name = "картошка"
                menu = mon
            }

            Dish.new {
                name = "котлета"
                menu = mon
            }

            Dish.new {
                name = "салат"
                menu = mon
            }

            User.new {
                chat = 505120843
                name = "Александр Паниман"
                phone = "+380669362726"
                grade = ph

                right = ADMINISTRATOR
                state = COMMAND
            }
        }

        val locale = Locale("ru")

        val customer = reply {
            row {
                button("заказать обед")
                button("отменить заказ")
            }
            row {
                button("список заказов")
            }
            row {
                button("изменить имя")
                button("изменить класс")
                button("изменить телефон")
            }
        }

        val producer = reply {
            row {
                button("создать меню")
                button("изменить меню")
                button("удалить меню")
            }
            row {
                button("сводка всез заказов")
            }
            row {
                button("список всех заказов")
            }
        }

        val admin = reply {
            row {
                button("заказать обед")
                button("отменить заказ")
            }
            row {
                button("список заказов")
            }
            row {
                button("изменить имя")
                button("изменить класс")
                button("изменить телефон")
            }
            row {
                button("создать меню")
                button("изменить меню")
                button("удалить меню")
            }
            row {
                button("сводка всез заказов")
            }
            row {
                button("список всех заказов")
            }
            row {
                button("создать пользователя")
                button("изменить пользователя")
                button("удалить пользователя")
            }
        }

        fun User.getMainKeyboard() = when (right) {
            ADMINISTRATOR -> admin
            PRODUCER -> producer
            CUSTOMER -> customer
        }

        command { user ->
            when {
                user.name == null -> {
                    user.send("Вас приветствует бот для заказа еды в буфете Ришельевского лицея! " +
                            "Для начала работы нам понадобиться некоторая информация о вас. " +
                            "Введите свое имя и фамилию через пробел:")
                    user.state = NAME
                }
                user.phone == null -> {
                    user.send("Нам не хватает информации о вас для продолжения работы! " +
                            "Пожалуйста введите свой номер телефона или нажмите на соответствующую кнопку:") {
                        reply {
                            row {
                                button("отправить мой номер") {
                                    requestContact = true
                                }
                            }
                        }
                    }
                    user.state = PHONE
                }
                user.grade == null -> {
                    user.send(
                            "Нам не хватает информации о вас для продолжения работы!" +
                                    "Пожалуйста выберите свой класс:") {
                        val grades = Grade.all().map { it.name }
                        inline {
                            show(grades, length = 4) { "set-grade:$it" }
                        }
                    }
                }
            }
        }

        reader(NAME) { user, message ->
            val text = message.text ?: return@reader

            val isModification = user.name != null
            val isNextEmpty = user.phone == null

            val isCorrectName = text matches "^[А-ЯҐІЄЇ][а-яґієї]+ [А-ЯҐІЄЇ][а-яґієї]+(-[А-ЯҐІЄЇ][а-яґієї]+)?$".toRegex()
            if (isCorrectName) {
                user.name = text
                if (isModification) user.apply {
                    send("Имя было успешно изменено на `$name`!") {
                        keyboard(user.getMainKeyboard())
                    }
                    state = COMMAND
                } else user.apply {
                    send("Ваше имя было успешно установленно, как `$name`!")

                    if (isNextEmpty)
                        send("Теперь пожалуйста введите свой номер телефона или нажмите на соответствующую кнопку:") {
                            reply {
                                row {
                                    button("отправить мой номер") {
                                        requestContact = true
                                    }
                                }
                            }
                        }
                    state = PHONE
                }
            } else user.send("Неверный ввод имени! " +
                    "Пожалуйста вводите имя и фамилию кириллицей с большой буквы через пробел. " +
                    "Повторите попытку:")
        }

        reader(PHONE) { user, message ->
            val telephone = message.contact?.phoneNumber ?: message.text ?: return@reader

            val isRegistered = user.name != null && user.phone != null && user.grade != null
            if (isRegistered && telephone == "отмена") {
                user.state = COMMAND
                user.send("Успешно отменено!")
            }

            val isModification = user.phone != null
            val isNextEmpty = user.grade == null

            val isCorrectPhone = telephone matches "^(\\+)?([- _(]?[0-9]){10,14}$".toRegex()

            if (isCorrectPhone) {
                user.phone = telephone
                if (isModification)
                    user.send("Телефонный номер был успешно изменен на `${user.phone}`!") {
                        keyboard(user.getMainKeyboard())
                    }
                else user.apply {
                    send("Ваш телефонный номер был успешно установлен, как `$phone`!")

                    if (isNextEmpty)
                        send("Теперь выберите свой класс:") {
                            val grades = Grade.all().map { it.name }
                            inline {
                                show(grades, length = 4) { "set-grade:$it" }
                            }
                        }
                }
                user.state = COMMAND
            } else user.send("Неверный ввод телефона! Повторите попытку:") {
                reply {
                    row {
                        button("отправить мой номер") {
                            requestContact = true
                        }
                        if (isModification)
                            button("отмена")
                    }
                }
            }
        }

        callback("set-grade") { user, src, (name) ->
            val grade = Grade.find { Grades.name eq name }.singleOrNull() ?: return@callback
            user.grade = grade

            src.edit("Установлен ${grade.name} класс!") {}
            user.send("Вы успешно зарегистрированы!") {
                keyboard(user.getMainKeyboard())
            }
        }

        command("изменить имя") { user ->
            user.send("Введите изменённое имя и фамилию через пробел:") {
                inline {
                    row {
                        button("отмена", "cancellation:")
                    }
                }
            }
            user.state = NAME
        }

        command("изменить телефон") { user ->
            user.send("Введите новый номер телефона или нажмите на соответствующую кнопку:") {
                reply {
                    row {
                        button("отправить мой номер") {
                            requestContact = true
                        }
                        button("отмена")
                    }
                }
            }
            user.state = PHONE
        }

        command("изменить класс") { user ->
            user.send("Выберите другой класс:") {
                val grades = Grade.all().map { it.name }
                inline {
                    show(grades, length = 4) { "set-grade:$it" }
                }
            }
            user.state = PHONE
        }

        callback("cancellation") { user, src, _ ->
            val isRegistered = user.name != null && user.phone != null && user.grade != null
            if (isRegistered) {
                user.state = COMMAND
                src.safeEdit("Успешно отменено!")
            }
        }

        fun LocalDate.checkOrderTime() = !toDateTime(
                LocalTime.parse(
                        System.getenv("LAST_ORDER_TIME") //"HH:mm"
                )
        ).isBeforeNow

        command("заказать обед") { user ->
            val lastOrderTime = LocalTime.parse(
                    System.getenv("LAST_ORDER_TIME") //"HH:mm"
            )

            val now = LocalDate.now()

            var day = now
            // Move day to nearest next monday or friday
            while (day.dayOfWeek != 1 && day.dayOfWeek != 5)
                day = day.plusDays(1)
            // If day is friday then move day to nearest previous monday
            while (day.dayOfWeek != 1)
                day = day.minusDays(1)

            val datesFromWeekStart = (0..4).map { day.plusDays(it) }
            val datesWithActiveMenu = datesFromWeekStart
                    .map { it.toDateTime(lastOrderTime) } // Map to last order time in each day
                    .filter { !it.isBeforeNow } // Filter previous days
                    .filter {
                        !Menu.find {
                            (Menus.schedule eq it.dayOfWeek) and
                                    (Menus.active eq true) // Filter days with at least one active menu
                        }.empty()
                    }
            val activeDaysOfWeek = datesWithActiveMenu
                    .map { it.dayOfWeek to it }
                    .toMap()

            user.send("Выберите дату заказа:") {
                inline {
                    row {
                        for (dayOfWeek in 1..7) {
                            val date = activeDaysOfWeek[dayOfWeek]
                            val displayedName = date?.dayOfWeek()?.getAsShortText(locale)
                            if (displayedName != null)
                                button(displayedName, "order-menu:${date.toString("yyyy-MM-dd")}:?")
                            else
                                button("✘")
                        }

                    }
                }
            }
        }

        callback("order-menu") { _, src, (date, identifier) ->
            val orderDate = DateTime.parse(date)

            // List of menus on selected day
            val menus = Menu.find {
                (Menus.schedule eq orderDate.dayOfWeek) and
                        (Menus.active eq true)
            }.toList()

            if (menus.isEmpty()) {
                src.safeEdit("Ошибка! В этот день уже нет активных меню!")
                return@callback
            }

            val id = identifier.toIntOrNull() ?: menus.first().id.value
            val current = menus.find { it.id.value == id } ?: menus.first()
            val index = menus.indexOf(current)

            src.safeEdit("Выберите меню:\n\n".bold() +
                    current.displayMessage(id + 1, menus.size)) {
                row {
                    val displayedDate = orderDate.toString("yyyy-MM-dd")!!
                    val menuId = menus[index].id.value
                    val callback = "$displayedDate:$menuId"

                    if (index - 1 in menus.indices)
                        button("◀", "order-menu:$callback")
                    else
                        button("∅")

                    button("Выбрать", "choose-menu:$callback")

                    if (index + 1 in menus.indices)
                        button("▶", "order-menu:$callback")
                    else
                        button("∅")
                }
            }
        }

        callback("choose-menu") { _, src, (date, identifier) ->
            val orderDate = LocalDate.parse(date)

            val menu = Menu.findById(identifier.toInt())
            if (menu == null) {
                src.safeEdit("Ошибка! Это меню уже не существует!")
                return@callback
            }
            if (!orderDate.checkOrderTime()) {
                src.safeEdit("Ошибка! Уже слишком поздно, вы не можете сделать заказ сейчас.")
                return@callback
            }

            src.safeEdit("**Вы уверены, что хотите заказать:**\n\n${menu.displayMessage()}") {
                row {
                    button("Подтвердить!", "confirm-order:$date:$identifier")
                }
            }
        }

        callback("confirm-order") { user, src, (date, identifier) ->
            val orderDate = LocalDate.parse(date)

            val menu = Menu.findById(identifier.toInt())
            if (menu == null) {
                src.safeEdit("Ошибка! Это меню уже не существует!")
                return@callback
            }
            if (!orderDate.checkOrderTime()) {
                src.safeEdit("Ошибка! Уже слишком поздно, вы не можете сделать заказ сейчас.")
                return@callback
            }

            val registered = DateTime.now()
            src.safeEdit("""
                |Заказ был успешно оформлен!
                |
                |${menu.displayMessage()}
                |
                |Время регистрации: `${registered.toString("hh:mm:ss")}`""".trimMargin())

            Order.new {
                this.orderDate = orderDate
                this.registered = registered

                this.user = user
                this.menu = menu
            }
        }

        command("посмотреть список всех заказов") { called ->
            val now = LocalDate.now()

            val orders = User.all().asSequence()
                    .map { it.orders }
                    .flatten()
                    .filter {
                        !it.orderDate.isBefore(now)
                    }

            val message = buildString {
                appendln("Все активные заказы:\n".bold())

                val groupedByDate = orders
                        .groupBy { it.orderDate }
                        .toSortedMap()
                for ((date, byDate) in groupedByDate) {
                    val dateDisplay = date.toString("yyyy-MM-dd")
                    val dayOfWeek = date.dayOfWeek().getAsShortText(locale)

                    appendln("$dayOfWeek($dateDisplay) [${byDate.size}]:".bold())

                    val groupedByGrade = byDate
                            .groupBy { it.user.grade }
                            .toSortedMap(compareBy { it!!.name })
                    for ((grade, byGrade) in groupedByGrade) {
                        appendln("\t${grade!!.name} [${byGrade.size}]:".bold())

                        val groupedByUser = byGrade
                                .groupBy { it.user }
                                .toSortedMap(compareBy { it.name })
                        for ((user, byUser) in groupedByUser) {
                            val pad = " ".repeat(5)
                            val name = user.name!!.code()
                            val joined = byUser.joinToString(", ") { it.menu.name.code() }
                            appendln("$pad$name: $joined")
                        }
                    }
                }

            }
            called.send(message)
        }

        command("отменить заказ") { user ->
            user.send("")
        }
//
//        command("посмотреть список заказов") { user ->
//
//        }
    }
}