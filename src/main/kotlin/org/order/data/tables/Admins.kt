package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Admins: IntIdTable() {
    val user = reference("user_id", Users)
}