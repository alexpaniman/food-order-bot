package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.PollComments

class PollComment(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PollComment>(PollComments)

    var order by Order referencedOn PollComments.order
    var text by PollComments.text
}