package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update

object MessageUserLoader: TelegramUserLoader {
    override fun loadUser(update: Update) = update.message?.from
}