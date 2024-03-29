package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.tables.Parents
import org.order.data.tables.Relations
import org.order.logic.corpus.Text
import org.order.logic.impl.utils.stringifyTable

class Parent(id: EntityID<Int>): Role(id) {
    companion object: RoleClass<Parent>(Parents) {
        @JvmStatic override val roleName
            get() = Text["parent"]
        @JvmStatic override val userLink
            get() = Parents.user
    }

    var user     by User    referencedOn Parents.user
    var children by Student via          Relations

    override val description
        get() = Text.get("parent-description") {
            it["children"] = children.joinToString("\n") { child ->
                child.user.buildDescription(Student).stringifyTable()
            }
        }
}