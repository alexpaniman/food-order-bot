package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update

object CallbackIdLoader: UserIdLoader {
    override fun loadId(update: Update): Int? = update.callbackQuery?.from?.id
}