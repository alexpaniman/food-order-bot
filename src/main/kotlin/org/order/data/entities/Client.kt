package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.order.data.Role
import org.order.data.RoleClass
import org.order.data.entities.Client.Companion.referrersOn
import org.order.data.tables.Clients
import org.order.data.tables.OrderCancellations
import org.order.data.tables.Orders
import org.order.data.tables.Payments
import org.order.logic.corpus.Text

class Client(id: EntityID<Int>) : Role(id) {
    companion object : RoleClass<Client>(Clients) {
        @JvmStatic
        override val roleName
            get() = Text["client"]
        @JvmStatic
        override val userLink
            get() = Clients.user
    }

    var user by User referencedOn Clients.user
    var balance by Clients.balance

    val orders by Order referrersOn Orders.client
    val payments by Payment referrersOn Payments.client

    override val description
        get() = Text.get("client-description") {
            it["balance"] = balance.toString()
        }
}