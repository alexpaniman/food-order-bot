package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Payments

class Payment(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Payment>(Payments)

    var date by Payments.date
    var order by Order referencedOn Payments.order
}