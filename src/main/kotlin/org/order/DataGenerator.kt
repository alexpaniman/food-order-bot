package org.order

import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.order.data.entities.*
import org.order.logic.impl.utils.Schedule

private fun generateName() =
        ('А'..'Я').random() + (0..9).joinToString("") { ('а'..'я').random().toString() }

private fun createUser(chatId: Int) = User.new {
    this.chat = chatId
    this.name = generateName() + " " + generateName()
    this.phone = "+380669360000"
    this.state = State.COMMAND
    this.valid = true
}

private fun createClient(user: User) = Client.new {
    this.user = user
    this.balance = 0f
}

private fun createMenu() = Menu.new {
    this.schedule = Schedule.parse("2020-01-15:1")
    this.name = (0..1000).random().toString()
    this.cost = (0..1000).random().toFloat()
}

private fun createOrder(menu: Menu, client: Client, orderDate: LocalDate) = Order.new {
    this.client = client
    this.madeBy = client.user
    this.registered = DateTime.now()
    this.orderDate = orderDate
    this.menu = menu
}

fun createData() {
    val users = (0..100).map { createUser(it) }
    val menus = (0..20).map { createMenu() }
    val clients = users.map { createClient(it) }
    (0..500).map {
        createOrder(
                menus.random(),
                clients.random(),
                LocalDate.now().plusDays((0..10).random()))
    }
}