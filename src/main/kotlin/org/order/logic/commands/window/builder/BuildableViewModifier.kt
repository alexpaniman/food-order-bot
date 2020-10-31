package org.order.logic.commands.window.builder

fun interface BuildableViewModifier {
    fun BuildableWindowView.modify(): BuildableWindowView
}