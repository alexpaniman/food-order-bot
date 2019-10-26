package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Teachers

class Teacher(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Teacher>(Teachers)

    var user by User referencedOn Teachers.user
}