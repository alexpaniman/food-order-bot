package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Ingredients: IntIdTable() {
    val menu = reference("menu", Menus)
    val name = varchar("name", 255)
}