package org.order.logic.commands.triggers

import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

infix fun Trigger.and(other: Trigger) = object : Trigger {
    override fun test(user: User, update: Update) =
            this@and.test(user, update) && other.test(user, update)
}

infix fun Trigger.or(other: Trigger) = object : Trigger {
    override fun test(user: User, update: Update) =
            this@or.test(user, update) || other.test(user, update)
}