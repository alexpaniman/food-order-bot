package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Ingredients

class Ingredient(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Ingredient>(Ingredients)

    var name by Ingredients.name
    var menu by Menu referencedOn Ingredients.menu
}