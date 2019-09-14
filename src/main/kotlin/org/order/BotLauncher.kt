package org.order

import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi

fun main() {
    ApiContextInitializer.init()
    TelegramBotsApi().registerBot(FoodOrderBot("", ""))
}