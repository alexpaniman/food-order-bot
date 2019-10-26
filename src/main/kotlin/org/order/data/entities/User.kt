package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Admins
import org.order.data.tables.Users

class User(id: EntityID<Int>): IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var chat  by Users.chat

    var name  by Users.name
    var phone by Users.phone

    var state by Users.state

    var valid by Users.valid
}