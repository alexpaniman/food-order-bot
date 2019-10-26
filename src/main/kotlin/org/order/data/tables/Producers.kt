package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Producers: IntIdTable() {
    val user = reference("user_id", Users)
}