package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Menus: IntIdTable() {
    val name = varchar("name", 255)
    val cost = float("cost")
    val active = bool("is_active")
    val schedule = varchar("schedule", 255)
}