package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Admins
import org.order.logic.corpus.Text

class Admin(id: EntityID<Int>): Role(id) {
    companion object: RoleClass<Admin>(Admins) {
        @JvmStatic override val roleName
            get() = Text["admin"]
        @JvmStatic override val userLink
            get() = Admins.user
    }

    var user by User referencedOn Admins.user

    override val description
        get() = Text["admin-description"]
}