package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Clients: IntIdTable() {
    val user    = reference("user_id" , Users )
    val balance = float("balance").default(0f)
}