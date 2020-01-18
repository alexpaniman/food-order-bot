package org.order.logic.impl.utils

import org.order.data.entities.Client
import org.order.data.entities.User

fun User.clients(): List<Client> {
    val children = linkedOrNull(org.order.data.entities.Parent)
            ?.children
            ?.map { it.user.linked(Client) } ?: listOf()

    val selfClient = linkedOrNull(Client)

    val unsortedClients = children +
            if (selfClient != null)
                listOf(selfClient)
            else listOf()

    // Sorting to guarantee same clients order
    return unsortedClients.sortedBy { it.id }
}