package org.order.logic.impl.commands.tools

import org.order.data.entities.State.BANNED
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.StateTrigger

val BAN_FILTER = TriggerCommand(StateTrigger(BANNED)) { _, _ -> }