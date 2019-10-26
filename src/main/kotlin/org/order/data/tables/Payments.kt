package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Payments: IntIdTable() {
    val madeBy = reference("made_by_id", Users  )
    val client = reference("client_id" , Clients)

    val amount = integer("amount")

    val registered = datetime("registered")

    val telegramId = varchar("telegram_id", 255)
    val providerId = varchar("provider_id", 255)
}