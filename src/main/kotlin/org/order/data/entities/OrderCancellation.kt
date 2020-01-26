package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.OrdersCancellations

class OrderCancellation(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<OrderCancellation>(OrdersCancellations)

    var order by Order referencedOn OrdersCancellations.order

    var canceled by OrdersCancellations.canceled
    var canceledBy by User referencedOn OrdersCancellations.canceledBy
}