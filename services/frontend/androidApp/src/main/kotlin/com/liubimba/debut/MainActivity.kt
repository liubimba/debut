package com.liubimba.debut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liubimba.debut.ui.AppContainer

class MainActivity : ComponentActivity() {
    private val container: AppContainer by lazy {
        AppContainer(path = filesDir.path)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(container)
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            container.close()
        }
        super.onDestroy()
    }
}
