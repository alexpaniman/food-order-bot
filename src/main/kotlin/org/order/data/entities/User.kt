package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Payments
import org.order.data.tables.Users
import org.order.logic.corpus.Text
import org.order.data.entities.State.IMAGINE
import org.order.data.tables.Relations

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var chat by Users.chat
    var name by Users.name
    var phone by Users.phone
    var state by Users.state
    var valid by Users.valid

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

    private fun unlinkRoles(vararg roles: RoleClass<*>) {
        for (role in roles)
            role.find { role.userLink eq id }
                    .forEach { it.delete() }
    }

    private fun unlinkParent() {
        val parent = linkedOrNull(Parent) ?: return
        for (children in parent.children)
            if (children.user.state == IMAGINE) {
                val links = Relations.select {
                    Relations.child eq children.id
                }.count()

                if (links == 1) { // This user was created by this parent
                    Relations.deleteWhere { // Unlink this child
                        Relations.child eq children.id
                    }
                    children.delete()
                }
            }

        Relations.deleteWhere { // Unlink parent from all children he linked with
            Relations.parent eq parent.id
        }
        parent.delete()
    }

    private fun unlinkStudent() {
        val student = linkedOrNull(Student) ?: return
        Relations.deleteWhere {
            Relations.child eq student.id
        }
        student.delete()
    }

    private fun unlinkAll() {
        unlinkRoles(Admin, Client, Coordinator, Producer, Student, Teacher)
        unlinkStudent() // Unlink parents and then delete student
        unlinkParent() // Unlink children and then delete parent

    }

    fun clear() {
        unlinkAll()
        name  = null
        phone = null
    }

    fun safeDelete() {
        unlinkAll()
        delete()
    }
}