package org.order.logic.impl.bot

import org.order.bot.CommandsBot
import org.order.bot.send.Sender
import org.order.logic.impl.commands.Registration
import org.order.logic.loaders.CallbackUserLoader
import org.order.logic.loaders.MessageUserLoader
import org.order.logic.loaders.PreCheckoutQueryUserLoader

class FoodOrderBot(sender: Sender, username: String, token: String): CommandsBot(sender, username, token) {
    init {
        // Setup user loaders
        this += MessageUserLoader
        this += CallbackUserLoader
        this += PreCheckoutQueryUserLoader

        // Setup command scopes
        this += Registration
    }
}