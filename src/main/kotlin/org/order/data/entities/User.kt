package org.order.data.entities

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.order.data.tables.Orders
import org.order.data.tables.Users

class User(id: EntityID<Int>): IntEntity(id) {
    companion object : EntityClass<Int, User>(Users)

    var telegramId by Users.telegramId

    var name by Users.name
    var phone by Users.phone
    var right by Right referencedOn Users.right
    var grade by Grade referencedOn Users.grade

    val orders by Order referrersOn Orders.user
}