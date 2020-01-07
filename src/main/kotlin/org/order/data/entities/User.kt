package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Payments
import org.order.data.tables.Users
import org.order.logic.corpus.Text

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var chat by Users.chat
    var name by Users.name
    var phone by Users.phone
    var state by Users.state

    val payments by Payment referrersOn Payments.madeBy

    fun <T : Role> linked(roleClass: RoleClass<T>) = roleClass
            .find { roleClass.userLink eq id }
            .first()

    fun <T : Role> linkedOrNull(roleClass: RoleClass<T>) = roleClass
            .find { roleClass.userLink eq id }
            .firstOrNull()

    fun hasLinked(roleClass: RoleClass<*>) = !roleClass
            .find { roleClass.userLink eq id }
            .empty()

    fun buildDescription(vararg roles: RoleClass<*>) = buildString {
        appendln(Text.get("user-description") {
            it["name"] = name!!
            it["phone"] = phone!!
        })

        for (role in roles)
            if (hasLinked(role))
                appendln(linked(role).description)
    }

    private fun clearRoles(vararg roles: RoleClass<*>) {
        for (role in roles)
            role.find { role.userLink eq id }
                    .forEach { it.delete() }
    }

    fun clear() {
        clearRoles(Admin, Client, Coordinator, Parent, Producer, Student, Teacher)
        name  = null
        phone = null
    }
}