package org.order.data.tables

import org.jetbrains.exposed.sql.Table

object Relations: Table() {
    val parent = reference("parent_id",  Parents)
    val child  = reference("child_id" , Students)
}