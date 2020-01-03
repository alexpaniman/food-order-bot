package org.order.data

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity

abstract class Role(id: EntityID<Int>): IntEntity(id) {
    abstract val description: String
}