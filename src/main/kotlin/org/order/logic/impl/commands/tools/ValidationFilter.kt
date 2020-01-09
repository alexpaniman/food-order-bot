package org.order.logic.impl.commands.tools

import org.order.data.entities.State.VALIDATION
import org.order.logic.commands.TriggerCommand
import org.order.logic.commands.triggers.StateTrigger

val VALIDATION_FILTER = TriggerCommand(StateTrigger(VALIDATION)) { _, _ ->  }