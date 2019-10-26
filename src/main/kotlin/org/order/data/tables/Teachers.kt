package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Teachers: IntIdTable() {
    val user = reference("user_id", Users)
}