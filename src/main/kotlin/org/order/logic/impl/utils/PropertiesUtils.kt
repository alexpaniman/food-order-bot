package org.order.logic.impl.utils

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.order.data.tables.Properties

fun getproperty(key: String) = transaction {
    Properties
            .select { Properties.key eq key }
            .first()[Properties.value]
}

fun setproperty(key: String, value: String) {
    transaction {
        Properties
                .update({ Properties.key eq key }) {
                    it[Properties.value] = value
                }
    }
}

fun newproperty(key: String, value: String) {
    transaction {
        Properties
                .insert {
                    it[Properties.key] = key
                    it[Properties.value] = value
                }
    }
}