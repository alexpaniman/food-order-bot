package org.order.logic.impl.commands.modules

import org.order.bot.send.SenderContext
import org.order.bot.send.button
import org.order.bot.send.inline
import org.order.data.entities.Client
import org.order.data.entities.State.COMMAND
import org.order.data.entities.State.READ_SEARCH_STRING
import org.order.data.entities.User
import org.order.logic.commands.questions.Question
import org.order.logic.commands.questions.QuestionSet
import org.order.logic.commands.triggers.NegativeTrigger
import org.order.logic.commands.window.Window
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.getTempProperty
import org.order.logic.impl.utils.grade
import org.order.logic.impl.utils.removeTempProperty
import org.order.logic.impl.utils.setTempProperty
import org.telegram.telegrambots.meta.api.objects.Update
import kotlin.math.max

// Modules are never trigger by themselves
// So all of them use NegativeTrigger
// And also because of that they don't need any default arguments
val USER_SEARCHER = QuestionSet(ReadSearchString, trigger = NegativeTrigger())

private fun longestCommonSubstring(s1: String, s2: String): Int {
    val dynamic = Array(s1.length + 1) {
        IntArray(s2.length + 1) { 0 }
    }

    for (i in 1..s1.length)
        for (j in 1..s2.length) {
            dynamic[i][j] = max(dynamic[i - 1][j], dynamic[i][j - 1])
            if (s1[i - 1] == s2[j - 1])
                dynamic[i][j] += 1
        }

    return dynamic.last().last()
}

object ReadSearchString : Question(READ_SEARCH_STRING) {
    override fun SenderContext.ask(user: User) = user.send(Text["user-searcher:title"])

    override fun SenderContext.receive(user: User, update: Update): Boolean {
        val searchString = update.message?.text

        if (searchString == null) {
            user.send(Text["user-searcher:wrong-action"])
            return false
        }

        val (_, clients) = Client.all()
                .groupBy { longestCommonSubstring(it.user.name!!, searchString) }
                .maxBy { (similarity, _) -> similarity }
                ?: return false // Means empty clients list (which is very unlikely)

        // Check number of results

        val acceptCallback = user.getTempProperty("user-searcher:accept-callback")

        user.send(Text["user-searcher:select-user"]) {
            inline {
                for (client in clients) {
                    // Fill callback with search result
                    val result = (listOf(client) + clients
                            .filter { it != client }
                            .take(5)) // Drop results if there's too many of them TODO Check if this is a good idea
                            .joinToString(",") { "${it.id}" }
                    val callback = acceptCallback.replace("{}", result)

                    // FIXME Probably replace with string from text corpus
                    button(client.user.name + ", " + client.user.grade, callback)
                }
            }
        }

        user.removeTempProperty("user-searcher:accept-callback")
        user.state = COMMAND
        return true
    }
}

fun SenderContext.searchUsers(user: User, acceptCallback: String) {
    user.setTempProperty("user-searcher:accept-callback", acceptCallback)

    user.state = READ_SEARCH_STRING
    user.send(Text["user-searcher:title"])
}


// Window version of user searcher
private const val WINDOW_MARKER = "user-searcher"
val USER_SEARCHER_WINDOW = Window(WINDOW_MARKER, trigger = NegativeTrigger()) { user, _ ->
    user.state = READ_SEARCH_STRING
    show(Text["user-searcher:title"])
}

// This module prompts user for string to search for.
// After that it replaces "{}" in callback with client ids separated with ","
// And makes user call it after right user selection
fun replaceWithUsersSearch(user: User, acceptCallback: String): String {
    user.setTempProperty("user-searcher:accept-callback", acceptCallback)
    return "$WINDOW_MARKER:"
}

fun clearUsersSearch(user: User) =
    user.removeTempProperty("user-searcher:accept-callback")