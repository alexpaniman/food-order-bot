package org.order.logic.impl.commands.display

import org.order.data.entities.*
import org.order.data.entities.State.COMMAND
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.corpus.Text

private val HELP_TRIGGER = CommandTrigger(Text["help-command"]) and StateTrigger(COMMAND)
val HELP = TriggerCommand(HELP_TRIGGER) { user, _ ->
    when {
        user.hasLinked(Student) ->
            user.send(Text["student-help"])

        user.hasLinked(Producer) ->
            user.send(Text["producer-help"])

        user.hasLinked(Parent) && user.hasLinked(Teacher) ->
            user.send(Text["parent-teacher-help"])

        user.hasLinked(Parent) ->
            user.send(Text["parent-help"])

        user.hasLinked(Teacher) ->
            user.send(Text["teacher-help"])
    }

}