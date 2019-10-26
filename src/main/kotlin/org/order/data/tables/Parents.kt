package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Parents: IntIdTable() {
    val user = reference("user_id", Users)
}