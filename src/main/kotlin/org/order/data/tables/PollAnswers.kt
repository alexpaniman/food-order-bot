package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.and

object PollAnswers: IntIdTable("POLL_ANSWERS") {
    val order = reference("order_id", Orders)
    val dish = reference("dish_id", Dishes)
    val rate = integer("rate").check {
        (it greaterEq 1) and (it lessEq  5)
    }
}