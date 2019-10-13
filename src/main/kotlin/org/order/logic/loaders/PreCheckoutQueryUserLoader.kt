package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update

object PreCheckoutQueryUserLoader: TelegramUserLoader {
    override fun loadUser(update: Update) = update.preCheckoutQuery?.from
}