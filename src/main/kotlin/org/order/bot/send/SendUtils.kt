package org.order.bot.send

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

fun inline(init: InlineKeyboardMarkup.() -> Unit) = InlineKeyboardMarkup().apply(init)
fun reply(init: ReplyKeyboardMarkup.() -> Unit) = ReplyKeyboardMarkup().apply {
        this.resizeKeyboard = true
    }.apply(init)

fun SendMessage.inline(init: InlineKeyboardMarkup.() -> Unit) = keyboard(org.order.bot.send.inline(init))
fun SendMessage.reply(init: ReplyKeyboardMarkup.() -> Unit) = keyboard(org.order.bot.send.reply(init))

fun SendMessage.keyboard(markup: ReplyKeyboard) {
    replyMarkup = markup
}

fun InlineKeyboardMarkup.row(init: MutableList<InlineKeyboardButton>.() -> Unit) {
    if (keyboard == null)
        keyboard = mutableListOf()

    keyboard.add(mutableListOf())
    keyboard.last().apply(init)
}
fun ReplyKeyboardMarkup.row(init: KeyboardRow.() -> Unit) {
    if (keyboard == null)
        keyboard = mutableListOf()

    keyboard.add(KeyboardRow())
    keyboard.last().apply(init)
}

fun MutableList<InlineKeyboardButton>.button(text: String, callback: String = ":", init: InlineKeyboardButton.() -> Unit = {}) {
    this += InlineKeyboardButton(text).apply {
        this.callbackData = callback
    }.apply(init)
}
fun KeyboardRow.button(text: String, init: KeyboardButton.() -> Unit = {}) {
    this += KeyboardButton(text).apply(init)
}

fun ReplyKeyboardMarkup.button(text: String, init: KeyboardButton.() -> Unit = {}) =
        row { button(text, init) }
fun InlineKeyboardMarkup.button(text: String, callback: String = ":", init: InlineKeyboardButton.() -> Unit = {}) =
        row { button(text, callback, init) }

fun <T: Any> InlineKeyboardMarkup.show(elements: List<T>, length: Int, callback: (T) -> String = { it.toString() }) {
    check(length > 0) { "length must be greater then 0" }
    if (keyboard == null)
        keyboard = mutableListOf()


    for (element in elements) {
        if (keyboard.size == 0 || keyboard.last().size == length)
            keyboard.add(mutableListOf())

        keyboard.last() += InlineKeyboardButton(element.toString()).setCallbackData(callback(element))
    }
}
fun <T: Any> ReplyKeyboardMarkup.show(elements: List<T>, length: Int) {
    check(length > 0) { "length must be greater then 0" }
    if (keyboard == null)
        keyboard = mutableListOf()


    for (element in elements) {
        if (keyboard.size == 0 || keyboard.last().size == length)
            keyboard.add(KeyboardRow())

        keyboard.last() += KeyboardButton(element.toString())
    }
}

fun String.bold() = "***$this***"
fun String.code() = "`$this`"