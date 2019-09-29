package org.order.data.entities

import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.order.data.tables.Orders
import org.order.data.tables.Users

class User(id: EntityID<Int>): IntEntity(id) {
    companion object : EntityClass<Int, User>(Users)

    var chat by Users.chat

    var name by Users.name
    var phone by Users.phone
    var right by Users.right
    var grade by Grade optionalReferencedOn Users.grade

    var state by Users.state

    var firstName by Users.firstName

    var lastName by Users.lastName
    var username by Users.username

    val orders by Order referrersOn Orders.user
}