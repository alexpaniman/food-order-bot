package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update

object PreCheckoutQueryIdLoader: UserIdLoader {
    override fun loadId(update: Update): Int? = update.preCheckoutQuery?.from?.id
}