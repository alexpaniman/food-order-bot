package org.order.logic.impl.utils

import org.order.logic.impl.commands.COMMISSION
import kotlin.math.ceil

val Float.withCommission
    get() = ceil(this / (1 - COMMISSION) * 100f) / 100f