package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Menus: IntIdTable() {
    val name   = varchar("name", 255).nullable()
    val amount = integer("amount")

    val active   = bool("active")
    val schedule = integer("day")
}