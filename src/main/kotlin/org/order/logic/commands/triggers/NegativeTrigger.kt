package org.order.logic.commands.triggers

import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

class NegativeTrigger: Trigger {
    override fun test(user: User, update: Update) = false
}