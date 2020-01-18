package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.PollAnswers

class PollAnswer(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<PollAnswer>(PollAnswers)

    var order by Order referencedOn PollAnswers.order
    var dish by Dish referencedOn PollAnswers.dish
    var rate by PollAnswers.rate
}