package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Rights: IntIdTable() {
    val name = varchar("name", 15)
}