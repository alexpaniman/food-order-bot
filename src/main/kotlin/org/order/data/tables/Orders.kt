package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Orders: IntIdTable() {
    val canceled = bool("is_canceled").default(false)

    val registered = datetime("registered")
    val orderDate = varchar("order_date", 255)

    val client = reference("client_id", Clients)
    val madeBy = reference("made_by_id", Users)

    val menu = reference("menu_id", Menus)
}