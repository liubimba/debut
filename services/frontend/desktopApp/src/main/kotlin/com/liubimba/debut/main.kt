package com.liubimba.debut

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.liubimba.debut.ui.AppContainer

fun main() {
    val container = AppContainer(
        path = System.getProperty("user.home") + "/.debut",
    )

    application {
        Window(
            onCloseRequest = {
                container.close()
                exitApplication()
            },
            title = "Debut",
        ) {
            App(container)
        }
    }
}
