package org.order.data.tables

import org.jetbrains.exposed.dao.IntIdTable

object Users: IntIdTable() {
    val telegramId = integer("telegram_id")
    val name = varchar("name", 255)
    val phone = varchar("phone", 15)

    val grade = reference("grade_id", Grades)
    val right = reference("right_id", Rights)
}