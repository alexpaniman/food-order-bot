package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Orders

class Order(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Order>(Orders)

    var registered by Orders.registered
    var onDate by Orders.onDate

    var user by User referencedOn Orders.user
    var menu by Menu referencedOn Orders.menu
}