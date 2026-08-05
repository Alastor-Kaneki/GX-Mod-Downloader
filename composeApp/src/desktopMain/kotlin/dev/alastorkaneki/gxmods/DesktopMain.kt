package dev.alastorkaneki.gxmods

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "GX Mod Downloader",
        state = WindowState(width = 1280.dp, height = 820.dp),
    ) {
        App(DesktopPlatformActions())
    }
}
