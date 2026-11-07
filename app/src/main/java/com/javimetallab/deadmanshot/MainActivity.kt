package com.javimetallab.deadmanshot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.javimetallab.deadmanshot.core.Store
import com.javimetallab.deadmanshot.ui.DeadManShotTheme
import com.javimetallab.deadmanshot.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Debug builds only: `am start … --ez demo true --ei tab N` fills in invented data
        // for screenshots. While it is in place, nothing is ever sent or run.
        var startTab = 0
        if (BuildConfig.DEBUG && savedInstanceState == null) {
            if (intent.getBooleanExtra("demo", false)) Store.seedDemo(System.currentTimeMillis())
            startTab = intent.getIntExtra("tab", 0)
        }
        enableEdgeToEdge()
        setContent {
            DeadManShotTheme {
                MainScreen(startTab)
            }
        }
    }
}
