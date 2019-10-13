package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update

object CallbackUserLoader: TelegramUserLoader {
    override fun loadUser(update: Update) = update.callbackQuery?.from
}