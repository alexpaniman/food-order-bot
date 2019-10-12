package org.order.logic.loaders

import org.telegram.telegrambots.meta.api.objects.Update

interface UserIdLoader {
    fun loadId(update: Update): Int?
}