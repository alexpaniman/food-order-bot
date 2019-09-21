package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable
import org.order.data.entities.Right
import org.order.data.entities.State

object Users: IntIdTable() {
    val chat = integer("chat")
    val name = varchar("name", 255).nullable()
    val phone = varchar("phone", 15).nullable()

    val grade = reference("grade_id", Grades).nullable()

    val right = enumeration("right", Right::class)
    val state = enumeration("state", State::class)
}