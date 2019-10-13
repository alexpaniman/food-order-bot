package org.order

import io.mockk.every
import io.mockk.mockk
import org.telegram.telegrambots.meta.api.objects.Update

fun text(str: String) {
    val update = mockk<Update>(relaxed = true)
    every { update.message.text } returns str
    every { update.message.from.id } returns 0
}