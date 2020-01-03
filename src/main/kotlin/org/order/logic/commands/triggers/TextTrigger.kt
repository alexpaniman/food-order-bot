package org.order.logic.commands.triggers

import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

class TextTrigger(val text: String): Trigger {
    override fun test(user: User, update: Update) =
            update.message?.text == text
}