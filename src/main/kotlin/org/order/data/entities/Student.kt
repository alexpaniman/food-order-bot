package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Coordinators
import org.order.data.tables.Relations
import org.order.data.tables.Students
import org.order.logic.corpus.Text

class Student(id: EntityID<Int>): Role(id) {
    companion object: RoleClass<Student>(Students) {
        @JvmStatic override val roleName
            get() = Text["student"]
        @JvmStatic override val userLink
            get() = Students.user
    }

    var user  by User  referencedOn         Students.user
    var grade by Grade optionalReferencedOn Students.grade

    val parents by Parent via Relations

    override val description
        get() = Text.get("student-description") {
            it["grade"] = grade!!.name
        }

    val coordinators
        get() = Coordinator.find {
            Coordinators.grade eq grade!!.id
        }
}