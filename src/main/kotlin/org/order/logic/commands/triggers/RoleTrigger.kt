package org.order.logic.commands.triggers

import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.entities.User
import org.telegram.telegrambots.meta.api.objects.Update

class RoleTrigger<T: Role>(private val role: RoleClass<T>): Trigger {
    override fun test(user: User, update: Update) = user.hasLinked(role)
}