package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object PollComments: IntIdTable("POLL_COMMENTS") {
    val order = reference("order_id", Orders)
    val text = varchar("text", 2048).nullable()
}