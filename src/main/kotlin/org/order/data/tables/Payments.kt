package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Payments: IntIdTable() {
    val madeBy = reference("made_by_id", Users  )
    val client = reference("client_id" , Clients).nullable()

    val amount = float("amount").nullable()

    val registered = datetime("registered").nullable()

    val telegramId = varchar("telegram_id", 255).nullable()
    val providerId = varchar("provider_id", 255).nullable()
}