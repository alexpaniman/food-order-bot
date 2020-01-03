package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Producers
import org.order.logic.corpus.Text

class Producer(id: EntityID<Int>): Role(id) {
    companion object: RoleClass<Producer>(Producers) {
        @JvmStatic override val roleName
            get() = Text["producer"]
        @JvmStatic override val userLink
            get() = Producers.user
    }

    var user by User referencedOn Producers.user

    override val description
        get() = Text["producer-description"]
}