package org.order.logic.impl.commands.tools

import org.order.logic.commands.processors.CallbackProcessor

val MESSAGE_REMOVER = CallbackProcessor("remove-message") { _, src, _ -> src.delete() }