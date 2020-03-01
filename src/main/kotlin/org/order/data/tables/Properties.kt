package org.order.data.tables

import org.jetbrains.exposed.sql.Table

object Properties: Table() {
    val key = varchar("key", 255)
    val value = varchar("value", 255)
}