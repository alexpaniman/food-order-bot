package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User as TUser

interface TelegramUserLoader {
    fun loadUser(update: Update): TUser?
}