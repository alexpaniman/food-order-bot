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
                            commands.filterKeys {
                                if (user.name == null) it.isEmpty()
                                else message.text in it
                            }
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
        /*transaction {
            val ph10 = Grade.new {
                name = "10-Ф"
            }
            val math10 = Grade.new {
                name = "10-М"
            }
            val ph11 = Grade.new {
                name = "11-Ф"
            }

            fun menu(name: String, schedule: Int) = Menu.new {
                this.name = name
                this.active = true
                this.cost = 10
                this.schedule = schedule
            }

            fun dish(menu: Menu, name: String) = Dish.new {
                this.name = name
                this.menu = menu
            }

            fun dishes(vararg names: String, menu: Menu) = names.forEach { dish(menu, it) }

            fun user(name: String = "Александр Паниман", chat: Int = 505120843) = User.new {
                this.chat = chat
                this.name = name
                phone = "nothing"
                grade = listOf(math10, ph10, ph11).random()

                right = ADMINISTRATOR
                state = COMMAND
            }

            fun order(date: String, user: User, menu: Menu) = Order.new {
                this.orderDate = LocalDate.parse(date)
                this.menu = menu
                this.user = user

                this.registered = DateTime.now()
            }

            user()

            val menus = listOf(
                    menu("1", 1),
                    menu("2", 2),
                    menu("3", 3),
                    menu("4", 4),
                    menu("5", 5),

                    menu("6", 1),
                    menu("7", 2),
                    menu("8", 3),
                    menu("9", 4),
                    menu("10", 5)
            )

            val dishes = arrayOf("картошка", "котлета", "сок")

            repeat(16) { dishes(*dishes, menu = menus.random()) }

            repeat(500) {
                val user = user(chat = 100939823, name = "Имя Фамилия" + (10000..99999).random().toString())
                order(LocalDate.now().plusDays((0..3).random()).toString(), user, menus.random())
            }
        }*/

        val locale = Locale("ru")

        val customer = reply {
            row {
                button("заказать обед")
                button("отменить заказ")
            }
            row {
                button("список моих заказов")
            }
            row {
                button("список всех заказов")
            }
            row {
                button("сводка всех заказов")
            }
        }

        fun User.getMainKeyboard() = when (right) {
            ADMINISTRATOR, PRODUCER, CUSTOMER -> customer
        }

        command { user ->
            if (user.name != null && user.phone != null && user.grade != null)
                return@command
            user.send("Вас приветствует бот для заказа еды в буфете Ришельевского лицея! " +
                    "Для начала работы нам понадобиться некоторая информация о вас. " +
                    "Введите свое имя и фамилию через пробел:")
            user.state = NAME
        }

        reader(NAME) { user, message ->
            val text = message.text ?: return@reader
            val isCorrectName = text matches "^[А-ЯҐІЄЇ][а-яґієї]+ [А-ЯҐІЄЇ][а-яґієї]+(-[А-ЯҐІЄЇ][а-яґієї]+)?$".toRegex()
            if (isCorrectName) user.apply {
                name = text
                send("Ваше имя было успешно установленно, как '${name!!.code()}'. Теперь введите свой номер телефона или нажмите на соответствующую кнопку:") {
                    reply {
                        row {
                            button("отправить мой номер") {
                                requestContact = true
                            }
                        }
                    }
                }
                state = PHONE
            } else user.send("Неверный ввод имени! " +
                    "Пожалуйста вводите имя и фамилию кириллицей с большой буквы через пробел. " +
                    "Повторите попытку:")
        }

        reader(PHONE) { user, message ->
            val telephone = message.contact?.phoneNumber ?: message.text ?: return@reader
            val isCorrectPhone = telephone matches "^(\\+)?([- _(]?[0-9]){10,14}$".toRegex()

            if (isCorrectPhone) user.apply {
                phone = telephone
                send("Ваш телефонный номер был успешно установлен, как ${phone!!.code()}. Теперь выберите свой класс:") {
                    val grades = Grade.all().map { it.name }
                    inline {
                        show(grades, length = 4) { "set-grade:$it" }
                    }
                }
                user.state = GRADE
            } else user.send("Неверный ввод телефона! Повторите попытку:") {
                reply {
                    row {
                        button("отправить мой номер") {
                            requestContact = true
                        }
                    }
                }
            }
        }

        callback("set-grade") { user, src, (name) ->
            val grade = Grade.find { Grades.name eq name }.singleOrNull() ?: return@callback
            user.grade = grade
            src.safeEdit("Установлен ${grade.name} класс!")
            user.state = COMMAND
            user.send("Теперь вы зарегистрированы! Можете пользоваться ботом. Для этого импользуйте кнопки снизу:") {
                keyboard(user.getMainKeyboard())
            }
        }

        reader(GRADE) { user, _ ->
            user.send("Сначала выберите класс!")
        }

        fun LocalDate.toLimitDateTime() = toDateTime(
                LocalTime.parse(
                        System.getenv("LAST_ORDER_TIME") // hh:mm
                )
        )

        command("заказать обед") { user ->
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
                    .map { it.toLimitDateTime() } // Map to last order time in each day
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

            if (activeDaysOfWeek.isEmpty()) {
                user.send("На данный момент нет доступных для заказа меню!".bold())
                return@command
            }

            user.send("Выберите дату заказа:") {
                inline {
                    row {
                        for (dayOfWeek in 1..5) {
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
                src.safeEdit("Ошибка! В этот день уже нет активных меню!".bold())
                return@callback
            }

            val id = identifier.toIntOrNull() ?: menus.first().id.value
            val current = menus.find { it.id.value == id } ?: menus.first()
            val index = menus.indexOf(current)

            val message = "Выберите меню из предложенных:"

            src.safeEdit(message.bold() +
                    System.lineSeparator().repeat(2) +
                    current.displayMessage(index + 1, menus.size, message.length)) {
                row {
                    val displayedDate = orderDate.toString("yyyy-MM-dd")!!
                    val menuId = menus[index].id.value

                    if (index - 1 in menus.indices)
                        button("◀", "order-menu:$displayedDate:${menus[index - 1].id.value}")
                    else
                        button("∅")

                    button("Выбрать", "choose-menu:$displayedDate:$menuId")

                    if (index + 1 in menus.indices)
                        button("▶", "order-menu:$displayedDate:${menus[index + 1].id.value}")
                    else
                        button("∅")
                }
            }
        }

        fun LocalDate.checkOrderTime() = !toLimitDateTime().isBeforeNow

        callback("choose-menu") { _, src, (date, identifier) ->
            val orderDate = LocalDate.parse(date)

            val menu = Menu.findById(identifier.toInt())
            if (menu == null || !menu.active) {
                src.safeEdit("Ошибка! Это меню уже не активно!".bold())
                return@callback
            }
            if (!orderDate.checkOrderTime()) {
                src.safeEdit("Ошибка! Уже слишком поздно, вы не можете сделать этот заказ сейчас.".bold())
                return@callback
            }

            src.safeEdit("Вы уверены, что хотите заказать:".bold()
                    + System.lineSeparator().repeat(2) + menu.displayMessage()) {
                row {
                    button("Подтвердить!", "confirm-order:$date:$identifier")
                }
            }
        }

        callback("confirm-order") { user, src, (date, identifier) ->
            val orderDate = LocalDate.parse(date)

            val menu = Menu.findById(identifier.toInt())
            if (menu == null || !menu.active) {
                src.safeEdit("Ошибка! Это меню уже не активно!".bold())
                return@callback
            }
            if (!orderDate.checkOrderTime()) {
                src.safeEdit("Ошибка! Уже слишком поздно, вы не можете сделать заказ сейчас.".bold())
                return@callback
            }

            val registered = DateTime.now()
            src.safeEdit("Заказ был успешно оформлен!".bold() +
                    System.lineSeparator().repeat(2) + menu.displayMessage() +
                    System.lineSeparator().repeat(2) +
                    "Зарегистрирован: ".code() +
                    registered.toString("hh:mm:ss"))

            Order.new {
                this.orderDate = orderDate
                this.registered = registered

                this.user = user
                this.menu = menu
            }
        }

        command("список всех заказов") { called ->
            val now = LocalDate.now()

            val orders = User.all().asSequence()
                    .map { it.orders }
                    .flatten()
                    .filter {
                        !it.orderDate.isBefore(now)
                    }
                    .toList()

            if (orders.none()) {
                called.send("Заказов ещё нет!".bold())
                return@command
            }

            val message = buildString {
                appendln("Все заказы [${orders.size}]:\n".bold())

                var linesCounter = 0
                val groupedByDate = orders
                        .groupBy { it.orderDate }
                        .toSortedMap()
                for ((date, byDate) in groupedByDate) {
                    val dayOfWeek = date.dayOfWeek().getAsText(locale).toUpperCase()

                    appendln("$dayOfWeek [${byDate.size}]:".bold())

                    val groupedByGrade = byDate
                            .groupBy { it.user.grade }
                            .toSortedMap(compareBy { it!!.name })
                    for ((grade, byGrade) in groupedByGrade) {
                        appendln("     ${grade!!.name} [${byGrade.size}]:".bold())
                        append("```")

                        val groupedByUser = byGrade
                                .groupBy { it.user }
                                .toSortedMap(compareBy { it.name })
                        for ((user, byUser) in groupedByUser) {
                            val joined = byUser.joinToString(", ") { it.menu.name }
                            appendln("     ${user.name}: $joined")
                        }

                        append("```")
                    }
                    linesCounter += byDate.size
                    if (linesCounter >= 100) {
                        append("|")
                        linesCounter = 0
                    }
                }
            }
            message.split('|').forEach {
                if (it.isNotBlank()) {
                    called.send(it)
                }
            }
        }

        command("сводка всех заказов") { user ->
            val now = LocalDate.now()

            val orders = User.all().asSequence()
                    .map { it.orders }
                    .flatten()
                    .filter {
                        !it.orderDate.isBefore(now)
                    }
                    .toList()

            if (orders.none()) {
                user.send("Заказов ещё нет!".bold())
                return@command
            }

            val message = buildString {
                appendln("Всего заказов: ".bold() + orders.size.toString().code())

                appendln()

                appendln("По меню: ".bold())
                val groupedByMenu = orders.groupBy { it.menu }
                for ((menu, byMenu) in groupedByMenu)
                    appendln(" - Меню ${menu.name}: ${byMenu.size}".code())

                appendln()

                appendln("По классу: ".bold())
                val groupedByGrade = orders.groupBy { it.user.grade }
                for ((grade, byGrade) in groupedByGrade) {
                    appendln(" - ${grade!!.name}: ${byGrade.size}".code())
                    val groupedByMenuInGrade = byGrade.groupBy { it.menu }
                    for ((menu, byMenu) in groupedByMenuInGrade)
                        appendln("    - Меню ${menu.name}: ${byMenu.size}".code())
                }
            }
            user.send(message)
        }

        command("список моих заказов") { user ->
            val orders = user.orders
            if (orders.none()) {
                user.send("Вы ещё не делали заказы!".bold())
                return@command
            }
            val message = buildString {
                appendln("Список ваших заказов:".bold())

                val groupedByDate = user.orders.groupBy { it.orderDate }
                for ((date, userOrders) in groupedByDate) {
                    val joined = userOrders.joinToString(", ") { it.menu.name }
                    appendln("     ${date.toString("yyyy-MM-dd")}: $joined")
                }
            }
            user.send(message)
        }

        command("отменить заказ") { user ->
            val orders = user.orders
            if (orders.none()) {
                user.send("Вы ещё не делали заказы!".bold())
                return@command
            }
            user.send("Выберите заказ, который хотите отменить:") {
                inline {
                    for (order in orders) {
                        val dateDisplay = order.orderDate.toString("yyyy-MM-dd")
                        val menuName = order.menu.name
                        row {
                            button("     $dateDisplay: $menuName", "cancel-order:${order.id.value}")
                        }
                    }
                }
            }
        }

        callback("cancel-order") { user, src, (identifier) ->
            val id = identifier.toInt()
            val order = user.orders.find { it.id.value == id }
            if (order == null) {
                src.safeEdit("Этот заказ уже был отменён!")
                return@callback
            }
            order.delete()
            src.safeEdit("Вы успешно отменили заказ!")
        }
    }
}