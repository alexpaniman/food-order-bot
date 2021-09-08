package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object RefundComments: IntIdTable("refund_comments") {
    val payment = reference("refund_payment_id", Payments)
    val comment = varchar("comment", 2048)
}