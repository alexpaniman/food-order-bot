package org.order.data.entities

import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.table
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.entities.State.IMAGINE
import org.order.data.tables.*

class User(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<User>(Users)

    var chat by Users.chat
    var name by Users.name
    var phone by Users.phone
    var state by Users.state
    var valid by Users.valid

    val orders by Order referrersOn Orders.madeBy
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

    fun buildDescription(vararg roles: RoleClass<*>) = table {
        cellStyle {
            border = true
            alignment = TextAlignment.MiddleCenter
            paddingRight = 1; paddingLeft = 1
        }

        if (name != null)
            row(name)

        if (phone != null)
            row(phone)

        for (role in roles)
            if (hasLinked(role))
                row(linked(role).description)
    }.toString()
            .replace('─', '-')
            .replace('│', '|')
            .replace("[┼┐┌└┘┤├]".toRegex(), " ")
            .lines().joinToString("\n") { "`$it`" }

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