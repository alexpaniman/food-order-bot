package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Parents
import org.order.data.tables.Relations

class Parent(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Parent>(Parents)

    var user     by User    referencedOn Parents.user
    val children by Student via          Relations
}