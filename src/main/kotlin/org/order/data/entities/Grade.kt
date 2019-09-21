package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Grades
import org.order.data.tables.Users

class Grade(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Grade>(Grades)

    var name by Grades.name
}