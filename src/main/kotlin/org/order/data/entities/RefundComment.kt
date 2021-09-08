package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.RefundComments

class RefundComment(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<RefundComment>(RefundComments)

    var payment by Payment referencedOn RefundComments.payment
    var comment by RefundComments.comment
}