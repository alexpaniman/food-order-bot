package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Rights
import org.order.data.tables.Users

class Right(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Right>(Rights)

    var name by Rights.name
    val users by User referrersOn Users.right
}