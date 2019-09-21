package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Dishes

class Dish(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Dish>(Dishes)

    var name by Dishes.name
    var menu by Menu referencedOn Dishes.menu
}