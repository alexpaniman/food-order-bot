package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Teachers
import org.order.logic.corpus.Text

class Teacher(id: EntityID<Int>): Role(id) {
    companion object: RoleClass<Teacher>(Teachers) {
        @JvmStatic override val roleName
            get() = Text["teacher"]
        @JvmStatic override val userLink
            get() = Teachers.user
    }

    var user by User referencedOn Teachers.user

    override val description
        get() = Text["teacher-description"]
}