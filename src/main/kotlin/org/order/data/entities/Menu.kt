package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Ingredients
import org.order.data.tables.Menus

class Menu(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Menu>(Menus)

    var name by Menus.name
    var active by Menus.active
    var amount by Menus.amount

    val ingredients by Ingredient referrersOn Ingredients.menu
}