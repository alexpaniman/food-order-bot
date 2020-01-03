package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Coordinators
import org.order.logic.corpus.Text

class Coordinator(id: EntityID<Int>): Role(id) {
    companion object: RoleClass<Coordinator>(Coordinators) {
        @JvmStatic override val roleName
            get() = Text["coordinator"]
        @JvmStatic override val userLink
            get() = Coordinators.user
    }

    var user  by User  referencedOn Coordinators. user
    var grade by Grade referencedOn Coordinators.grade

    override val description: String
        get() = Text.get("coordinator-description") {
            it["grade"] = grade.name
        }
}