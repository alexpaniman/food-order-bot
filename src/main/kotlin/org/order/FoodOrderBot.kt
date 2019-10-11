package org.order

import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.order.data.entities.*
import org.order.data.entities.Right.*
import org.order.data.entities.State.*
import org.order.data.tables.Menus
import org.order.data.tables.Users
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice
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

    private var newUser: Sender.(User) -> Unit = { }

    private fun callback(marker: String, run: Sender.(User, Message, List<String>) -> Unit) {
        callbacks[marker] = run
    }

    private fun command(vararg names: String, run: Sender.(User) -> Unit) {
        commands[names.toList()] = run
    }

    private fun reader(state: State, run: Sender.(User, Message) -> Unit) {
        readers[state] = run
    }

    private fun newUser(run: Sender.(User) -> Unit) {
        this.newUser = run
    }

    private fun fetchUser(telegramUser: org.telegram.telegrambots.meta.api.objects.User) =
            User.find { Users.chat eq telegramUser.id }.firstOrNull()

    private fun createUser(telegramUser: org.telegram.telegrambots.meta.api.objects.User) = User.new {
        chat = telegramUser.id

        firstName = telegramUser.firstName
        lastName = telegramUser.lastName
        username = telegramUser.userName

        name = null
        phone = null
        grade = null

        right = CUSTOMER
        state = COMMAND
    }

    override fun onUpdateReceived(update: Update) = transaction {
        addLogger(StdOutSqlLogger)

        val telegramUser = update.message?.from ?: update.callbackQuery?.from ?: return@transaction

        val fetchUser = fetchUser(telegramUser)
        val isNewUser = fetchUser == null
        val user = fetchUser ?: createUser(telegramUser)

        if (isNewUser) {
            sender.newUser(user)
            return@transaction
        }

        update.message.successfulPayment.currency
        when {
            update.message != null -> if (update.message.isUserMessage) readers
                    .filterKeys { it == user.state }
                    .map { (_, run) -> run }
                    .singleOrNull()
                    ?.invoke(sender, user, update.message) ?: if (update.message.hasText())
                commands.filterKeys { update.message.text in it }
                        .map { (_, run) -> run }
                        .singleOrNull()
                        ?.invoke(sender, user)
            else return@transaction

            update.hasCallbackQuery() -> {
                val callback = update.callbackQuery!!

                val message = callback.message

                val parts = callback.data.split(':')
                val marker = parts[0]
                val data = parts.drop(1)

                if (message.isUserMessage) callbacks
                        .filterKeys { it == marker }
                        .map { (_, run) -> run }
                        .singleOrNull()
                        ?.invoke(sender, user, callback.message, data)
            }
        }
    }

    private var summaryList: List<String>? = null
    private var ordersList: List<String>? = null

    init {
        /*
        transaction {
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
                this.lastName = ""
                this.firstName = ""
                this.username = ""

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

//            user()

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

            repeat(100) {
                val user = user(chat = 100939823, name = "Имя Фамилия" + (10000..99999).random().toString())
                order(LocalDate.now().plusDays((0..3).random()).toString(), user, menus.random())
            }
        }*/

        DateTimeZone.setDefault(DateTimeZone.forID("Europe/Kiev"))
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
            row {
                button("изменить имя")
                button("изменить телефон")
                button("изменить класс")
            }
        }

        fun User.actionsKeyboard() = when (right) {
            ADMINISTRATOR, PRODUCER, CUSTOMER -> customer
        }

        fun <Q, T> Q.validate(transform: (Q) -> T, validate: (T) -> Boolean, valid: (T) -> Unit, invalid: (T) -> Unit) {
            val transformed = transform(this)
            if (validate(transformed))
                valid(transformed)
            else
                invalid(transformed)
        }

        fun User.readName() = sender.run {
            val isModification = name != null

            send("Введите, пожалуйста, свою фамилию и затем имя через пробел:") {
                if (isModification)
                    reply {
                        row {
                            button("отмена")
                        }
                    }
            }
            state = NAME
        }

        fun User.readPhone() = sender.run {
            val isModification = phone != null

            send("Введите пожалуста свой номер телефона или нажмите на кнопку снизу:") {
                reply {
                    row {
                        button("отправить мой номер") {
                            requestContact = true
                        }
                    }
                    if (isModification)
                        row {
                            button("отмена")
                        }
                }
            }
            state = PHONE
        }

        fun User.readGrade() = sender.run {
            val isModification = grade != null

            send("Выберите свой класс:") {
                reply {
                    show(Grade.all().map { it.name }, 5)
                    if (isModification)
                        row {
                            button("отмена")
                        }
                }
            }
            state = GRADE
        }

        fun User.checkRegistration() = sender.run {
            when {
                name == null -> readName()
                phone == null -> readPhone()
                grade == null -> readGrade()
                else -> return@run true
            }
            false
        }

        fun User.sendMainKeyboard() = sender.run {
            send("Пользуйтесь ботом, используя кнопки снизу:") {
                keyboard(actionsKeyboard())
            }
        }

        fun User.registrationStep() {
            if (checkRegistration()) {
                sendMainKeyboard()
                state = COMMAND
            }
        }

        newUser { user ->
            val send = SendInvoice(user.chat,
                    "pay",
                    "description",
                    "payload",
                    System.getenv("PAYMENTS_TOKEN"),
                    "pay-parameter",
                    "EUR",
                    listOf(LabeledPrice("price", 100))
            )
            sender.execute(send)
            if (user.name != null && user.phone != null && user.grade != null)
                return@newUser
            user.send("Вас приветствует бот для заказа еды в буфете Ришельевского лицея! " +
                    "Для начала работы нам понадобится некоторая информация о Вас.")
            user.registrationStep()
        }

        reader(NAME) { user, message ->
            if (message.text == "отмена" && user.name != null) {
                user.send("Успешно отменено!") {
                    keyboard(user.actionsKeyboard())
                }
                user.state = COMMAND
                return@reader
            }
            message.validate({ it.text }, { it != null &&
                    it matches "^[А-ЯҐІЄЇ][а-яґієї]+ [А-ЯҐІЄЇ][а-яґієї]+(-[А-ЯҐІЄЇ][а-яґієї]+)?\$".toRegex()
            }, {
                user.run {
                    name = it
                    send("Ваше имя успешно установлено как '$it'")
                    registrationStep()
                }
            }, {
                user.send("Неверный ввод! Пожалуйста, введите фамилию и затем имя с большой буквы через пробел:")
            })
        }

        reader(PHONE) { user, message ->
            if (message.text == "отмена" && user.phone != null) {
                user.send("Успешно отменено!") {
                    keyboard(user.actionsKeyboard())
                }
                user.state = COMMAND
                return@reader
            }
            message.validate({ it.contact?.phoneNumber ?: it.text }, { it != null &&
                        it matches "^(\\+)?([- _(]?[0-9]){10,14}$".toRegex()
            }, {
                user.run {
                    phone = it
                    send("Ваш телефонный номер был успешно установлен как '$it'")
                    registrationStep()
                }
            }, {
                user.send("Неверный ввод телефона! Повторите попытку:")
            })
        }

        reader(GRADE) { user, message ->
            if (message.text == "отмена" && user.grade != null){
                user.send("Успешно отменено!") {
                    keyboard(user.actionsKeyboard())
                }
                user.state = COMMAND
                return@reader
            }
            val grades = Grade.all().map { it.name to it }.toMap()
            message.validate({ it.text }, { it != null && it in grades }, {
                user.run {
                    grade = grades[it]
                    send("Ваш класс был успешно установлен как '$it'")
                    registrationStep()
                }
            }, {
                user.send("Вы ввели неверный класс! Пожалуйста, используйте кнопки снизу. Повторите попытку:")
            })
        }

        command("изменить имя") { user ->
            user.readName()
        }

        command("изменить телефон") { user ->
            user.readPhone()
        }

        command("изменить класс") { user ->
            user.readGrade()
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
                    "Зарегистрирован: ".code() + System.lineSeparator() +
                    registered.toString("yyyy-MM-dd HH:mm").code())

            Order.new {
                this.orderDate = orderDate
                this.registered = registered

                this.user = user
                this.menu = menu
            }

            summaryList = null
            ordersList = null
        }

        fun LocalDate.dayOfWeekAsText(): String {
            val text = dayOfWeek().getAsText(locale)
            return text[0].toUpperCase() + text.drop(1)
        }

        command("список всех заказов") { called ->
            if (ordersList != null) {
                ordersList!!.forEach { called.send(it) }
                return@command
            }

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
                    appendln("${date.dayOfWeekAsText()} [${byDate.size}]:".bold())

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
                            appendln("     ${user.name}: Меню $joined")
                        }
                        append("```")
                        appendln()
                        linesCounter += byGrade.size
                        if (linesCounter >= 70) {
                            append("|")
                            linesCounter = 0
                        }
                    }
                    appendln()
                    appendln()
                }
            }
            val list = message.split('|').filter { it.isNotBlank() }
            ordersList = list
            list.forEach { called.send(it) }
        }

        command("сводка всех заказов") { user ->
            if (summaryList != null) {
                summaryList!!.forEach { user.send(it) }
                return@command
            }

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
                appendln("Всего заказов: ${orders.size}".bold())
                appendln()

                var linesCounter = 0
                val groupedByDate = orders.groupBy { it.orderDate }
                for ((date, byDate) in groupedByDate) {
                    appendln("${date.dayOfWeekAsText()}: ${byDate.size}".bold())

                    appendln("      По меню: ".bold())
                    append("```")

                    val groupedByMenu = byDate.groupBy { it.menu }
                    for ((menu, byMenu) in groupedByMenu)
                        appendln("    Меню ${menu.name}: ${byMenu.size}")

                    append("```")
                    appendln()

                    appendln("      По классу: ".bold())

                    val groupedByGrade = byDate.groupBy { it.user.grade }
                    for ((grade, byGrade) in groupedByGrade) {
                        appendln("            ${grade!!.name}: ${byGrade.size}".bold())
                        val groupedByMenuInGrade = byGrade.groupBy { it.menu }
                        append("```")
                        for ((menu, byMenu) in groupedByMenuInGrade)
                            appendln("        Меню ${menu.name}: ${byMenu.size}")
                        append("```")
                        appendln()
                        linesCounter += groupedByMenuInGrade.size
                        if (linesCounter >= 70) {
                            append("|")
                            linesCounter = 0
                        }
                    }
                    appendln()
                    appendln()
                }
            }
            val list = message.split('|').filter { it.isNotBlank() }
            summaryList = list
            list.forEach { user.send(it) }
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
                    appendln("     ${date.toString("yyyy-MM-dd")}: Меню $joined")
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
                            button("     $dateDisplay: Меню $menuName", "cancel-order:${order.id.value}")
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

            summaryList = null
            ordersList = null
        }
    }
}