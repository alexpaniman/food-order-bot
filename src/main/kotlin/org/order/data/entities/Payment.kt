package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Payments

class Payment(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Payment>(Payments)

    var madeBy by User referencedOn Payments.madeBy
    var client by Client referencedOn Payments.client
    var amount by Payments.amount
    var registered by Payments.registered
    var telegramId by Payments.telegramId
    var providerId by Payments.providerId
}