package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Dishes: IntIdTable() {
    val menu = reference("menu_id", Menus)
    val name =   varchar("name", 255)
}