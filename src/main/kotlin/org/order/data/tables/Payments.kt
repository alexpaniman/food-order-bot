package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Payments: IntIdTable() {
    val date = date("date")
    val order = reference("order_id", Orders)
}