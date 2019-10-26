package org.order.logic.impl.bot

import org.order.bot.CommandsBot
import org.order.bot.send.Sender
import org.order.logic.impl.commands.registration.state.STATE_REGISTRATION
import org.order.logic.impl.commands.registration.student.STUDENT_REGISTRATION
import org.order.logic.impl.commands.registration.user.USER_REGISTRATION

class FoodOrderBot(sender: Sender, username: String, token: String): CommandsBot(sender, username, token) {
    init {
        this += USER_REGISTRATION
        this += STATE_REGISTRATION
        this += STUDENT_REGISTRATION
    }
}