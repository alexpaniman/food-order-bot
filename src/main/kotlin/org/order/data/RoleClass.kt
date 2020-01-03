package org.order.data

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Column

abstract class RoleClass<T: Role>(table: IntIdTable): IntEntityClass<T>(table) {
    abstract val roleName: String
    abstract val userLink: Column<EntityID<Int>>
}