package org.order.logic.commands.window.builder

fun createWindow(marker: String, initialView: String) =
        BuildableWindow(marker, initialView)

fun BuildableWindow.scrollable(scrollLimit: Int, scrollOverflow: Int, ): BuildableWindow {

}
