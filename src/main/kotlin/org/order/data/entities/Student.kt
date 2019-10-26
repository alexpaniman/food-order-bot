package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Relations
import org.order.data.tables.Students

class Student(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Student>(Students)

    var user  by User  referencedOn         Students.user
    var grade by Grade optionalReferencedOn Students.grade

    val parents by Parent via Relations
}