package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Producers

class Producer(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Producer>(Producers)

    var user by User referencedOn Producers.user
}