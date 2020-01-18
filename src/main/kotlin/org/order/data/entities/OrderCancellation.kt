package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.OrderCancellations

class OrderCancellation(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<OrderCancellation>(OrderCancellations)

    var order by Order referencedOn OrderCancellations.order

    var canceled by OrderCancellations.canceled
    var canceledBy by User referencedOn OrderCancellations.canceledBy
}