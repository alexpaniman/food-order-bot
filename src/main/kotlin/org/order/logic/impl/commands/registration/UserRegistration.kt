package org.order.logic.impl.commands.registration

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.telegram.telegrambots.meta.api.objects.Update

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.reply

import org.order.data.entities.State.READ_NAME
import org.order.data.entities.State.READ_PHONE
import org.order.data.entities.State.NEW

import org.order.data.entities.User

import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.StateTrigger

import org.order.logic.corpus.Text

private const val NAME_VALIDATOR = "^[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)? [А-ЯЁ][а-яё]+$"

private object NameQuestion : Question(READ_NAME) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["register-name"])

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val inputName = update.message?.text
        val isValid = inputName?.matches(NAME_VALIDATOR.toRegex()) ?: false

        if (isValid)
            user.name = inputName
        else
            user.send(Text["wrong-name"])

        return isValid
    }
}

private object PhoneQuestion : Question(READ_PHONE) {
    override fun SenderContext.ask(user: User) =
            user.send(Text["register-phone"]) {
                reply {
                    button(Text["send-my-phone-button"]) { requestContact = true }
                }
            }

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val inputPhone = update.message?.text ?: "+" + update.message?.contact?.phoneNumber

        val phoneUtils = PhoneNumberUtil.getInstance()
        val phoneNumber =
                try {
                    phoneUtils.parse(inputPhone, "UKR")
                } catch (exc: NumberParseException) {
                    null
                }

        val isValid = phoneNumber != null && phoneUtils.isValidNumber(phoneNumber)

        if (isValid)
            user.phone = phoneUtils.format(
                    phoneNumber,
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
            )
        else
            user.send(Text["wrong-phone"])

        return isValid
    }
}

val USER_REGISTRATION = QuestionSet(
        NameQuestion, PhoneQuestion,
        beginning = {
            it.send(Text["greeting"])
        },

        conclusion = {
            it.state = NEW
        },

        trigger = StateTrigger(NEW)
)