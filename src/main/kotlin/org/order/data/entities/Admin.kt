package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Admins

class Admin(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Admin>(Admins)

    val user by User referencedOn Admins.user
}