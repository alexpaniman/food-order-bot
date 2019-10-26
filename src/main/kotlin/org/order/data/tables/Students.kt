package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Students: IntIdTable() {
    val user  = reference("user_id" ,  Users)
    val grade = reference("grade_id", Grades).nullable()
}