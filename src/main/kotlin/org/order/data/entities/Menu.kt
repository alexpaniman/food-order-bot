package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Dishes
import org.order.data.tables.Menus

class Menu(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Menu>(Menus)

    var name by Menus.name
    var amount by Menus.amount

    var active by Menus.active
    var schedule by Menus.schedule

    val dishes by Dish referrersOn Dishes.menu
}