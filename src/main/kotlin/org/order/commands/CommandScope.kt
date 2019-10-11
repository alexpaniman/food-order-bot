package org.order.commands

import org.order.commands.annotations.global.Register
import org.order.commands.annotations.keyboard.Callback
import org.order.commands.annotations.messages.Command
import org.order.commands.annotations.messages.Reader
import org.order.commands.annotations.payments.OnPreCheckoutQuery
import org.order.commands.annotations.payments.OnSuccessfulPayment
import org.order.data.entities.Right
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.reflect.KClass
import kotlin.reflect.full.functions

abstract class CommandScope(vararg val right: Right) {
    // Find annotated methods and return map with this annotation as key and method as value
    private fun <T : Annotation> findMethods(annotation: KClass<T>) = this::class.functions
            .filter { method ->
                annotation in method.annotations.map { it::class } // Find methods
            }.map { method ->
                @Suppress("UNCHECKED_CAST")
                method.annotations.find {
                    it::class == annotation
                }!! as T to method // Create map
            }.toMap()

    // Callback processors
    private val callbacks = findMethods(Callback::class)
            .mapKeys { it.key.marker } // Fetch callback marker information from annotation

    // Text commands processors
    private val commands = findMethods(Command::class)
            .flatMap { (key, value) ->
                key.commands.map { it to value } // Each command can content many different command names
            }.toMap()

    // Readers which gets all messages from users when user state is equal to state defined by annotation
    private val readers = findMethods(Reader::class)
            .mapKeys { it.key.state } // Fetch state information from annotation

    // When new user sends message to bot at first this annotation will be called
    private val newUser = findMethods(Register::class).values
            .singleOrNull() // Only one new user processor can be defined

    // Pre checkout query will be processed by this handler
    private val preCheckoutQuery = findMethods(OnPreCheckoutQuery::class)
            .values // Annotation don't have any information. It's just mark

    // Processor for message with successful payment in it
    private val successfulPayments = findMethods(OnSuccessfulPayment::class)
            .values // Annotation don't have any information. It's just mark


    // This method will be called when message will come
    fun onMessage(user: User, message: Message): Boolean {
        val reader = readers[user.state]
        if (reader != null) {
            reader.call(user, message)
            return true
        }

        val command = commands[message.text]
        if (command != null) {
            command.call(user, message.text)
            return true
        }

        return false
    }

    fun onCallback(user: User, marker: String, data: Array<String>, src: Message): Boolean {
        val handler = callbacks[marker] // All callbacks processors is linked to marker from data in callback query
        if (handler != null) {
            handler.call(user, handler)
            return true
        }

        return false
    }

    fun onCallback(user: User, callback: CallbackQuery): Boolean {
        val handler = callbacks[callback.data ?: return false] // All callbacks processors is linked to data in callback query
        if (handler != null) {
            handler.call(user, handler)
            return true
        }

        return false
    }

    // It is global method that represents command scope work. it will be called when new update will come
    fun onUpdate(user: User, update: Update): Boolean {
        if (update.message != null)
            if (onMessage(user, update.message)) // Process this message
                return true

        if (update.callbackQuery?.data != null)
            if (callbacks[update.callbackQuery.data!!]?.call(user, update.callbackQuery!!) != null) // Process this callback
                return true

        return false
    }
}