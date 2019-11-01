package org.order.logic.impl.commands.registration.parent

import org.order.bot.send.Sender
import org.order.data.entities.Parent
import org.order.data.entities.State
import org.order.data.entities.Teacher
import org.order.data.entities.User
import org.order.data.tables.Parents
import org.order.logic.commands.Command
import org.order.logic.commands.CommandScope
import org.order.logic.impl.commands.registration.user.readName
import org.telegram.telegrambots.meta.api.objects.Update

val REGISTER_NEW = object : Command {
    override fun Sender.process(user: User, update: Update): Boolean {
        val linkedParent = Parent
                .find { Parents.user eq user.id }
                .singleOrNull() ?: return false
        if (user.state != State.CHOOSES_ROLES)
            return false

        val children = linkedParent.children.toList()

        when {
            children.isEmpty()                -> readName          (user)
            children.any { it.grade == null } -> readChildGrade    (user)
            else                              -> suggestAddingChild(user)
        }
        return true
    }
}

val PARENT_REGISTRATION = CommandScope(READ_CHILD_NAME, READ_CHILD_LINK, READ_CHILD_GRADE, REGISTER_NEW) {
    !it.valid
    &&
    !Parent.find { Parents.user eq it.id }.empty() // User has parent role
}