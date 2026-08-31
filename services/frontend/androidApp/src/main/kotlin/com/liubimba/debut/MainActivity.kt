package com.liubimba.debut

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.liubimba.debut.ui.AppContainer

class MainActivity : ComponentActivity() {
    private val container: AppContainer by lazy {
        AppContainer(path = filesDir.path)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val microphone = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> container.microphoneGranted.value = granted }

            val lifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    container.microphoneGranted.value = isMicrophoneGranted()
                }
            }

            App(
                container = container,
                onRequestMicrophone = {
                    if (isMicrophoneGranted()) {
                        container.microphoneGranted.value = true
                    } else {
                        microphone.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
        }
    }

    private fun isMicrophoneGranted(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        if (isFinishing) {
            container.close()
        }
        super.onDestroy()
    }
}
