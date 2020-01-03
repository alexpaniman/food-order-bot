package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable
import org.order.data.entities.State

object Users: IntIdTable() {
    val chat = integer("chat").nullable()

    val name  = varchar("name", 255).nullable()
    val phone = varchar("phone", 255).nullable()

    val state = enumeration("state_id", State::class)
}