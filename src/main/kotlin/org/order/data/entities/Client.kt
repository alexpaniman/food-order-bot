package org.order.data.entities

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.order.data.tables.Clients

class Client(id: EntityID<Int>): IntEntity(id) {
    companion object: IntEntityClass<Client>(Clients)

    val user    by User  referencedOn Clients.user
    var balance by                    Clients.balance
}