package org.order.logic.commands.triggers

import org.order.data.entities.State
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

class StateTrigger(private val state: State): Trigger {
    override fun test(user: User, update: Update) = state == user.state
}