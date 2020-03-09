package org.order.logic.impl.commands.display

import org.order.data.entities.State.COMMAND
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.CommandTrigger
import org.order.logic.commands.triggers.StateTrigger
import org.order.logic.commands.triggers.and
import org.order.logic.commands.triggers.or
import org.order.logic.corpus.Text

private val HELP_TRIGGER = CommandTrigger(Text["help-command"]) and StateTrigger(COMMAND)

val HELP = TriggerCommand(HELP_TRIGGER) { user, _ ->
    user.send(Text["help"])
}