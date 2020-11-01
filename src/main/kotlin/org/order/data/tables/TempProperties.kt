package org.order.data.tables

import org.jetbrains.exposed.sql.Table

// This has no entity, properties are acquired in UserUtils
object TempProperties: Table() {
    // This properties are user-scoped
    val user = reference("user_id", Users)

    // Properties are represented as (key, value) pairs
    val key = varchar("key", 255)
    val value = varchar("key", 255)
}