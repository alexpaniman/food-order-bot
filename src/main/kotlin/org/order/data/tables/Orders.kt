package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Orders: IntIdTable() {
    val registered = datetime("registered")
    val orderDate = varchar("orderDate", 10) //yyyy-MM-dd

    val user = reference("user_id", Users)
    val menu = reference("menu_id", Menus)
}