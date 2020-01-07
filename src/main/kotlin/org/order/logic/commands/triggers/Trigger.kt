package org.order.logic.commands.triggers

import org.telegram.telegrambots.meta.api.objects.Update
import org.order.data.entities.User

interface Trigger {
    fun test(user: User, update: Update): Boolean
}