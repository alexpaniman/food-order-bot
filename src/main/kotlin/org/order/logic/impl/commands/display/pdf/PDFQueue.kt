package org.order.logic.impl.commands.display.pdf

import java.util.*
import kotlin.concurrent.thread

object PDFQueue {
    private val queue = LinkedList<() -> Unit>()

    init {
        thread(start = true) {
            while (true) synchronized(queue) {
                if (queue.isNotEmpty())
                    queue.pop()!!()
            }
        }
    }

    fun schedule(builder: () -> Unit) {
        synchronized(queue) {
            queue.push(builder)
        }
    }
}