package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Menus: IntIdTable() {
    val name = varchar("name", 255)
    val active = bool("active")
    val amount = integer("amount")
}