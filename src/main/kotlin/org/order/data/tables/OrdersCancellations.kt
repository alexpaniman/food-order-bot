package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object OrdersCancellations: IntIdTable("order_cancellations") {
    val order = reference("order_id", Orders)

    val canceled = datetime("canceled")
    val canceledBy = reference("canceled_by_id", Users)
}