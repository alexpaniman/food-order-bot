package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Payments

class Payment(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Payment>(Payments)

    val madeBy     by User   referencedOn Payments.madeBy
    val client     by Client referencedOn Payments.client

    val amount     by                     Payments.amount

    val registered by                     Payments.registered

    val telegramId by                     Payments.telegramId
    val providerId by                     Payments.providerId
}