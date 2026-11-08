package com.javimetallab.deadmanshot.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.javimetallab.deadmanshot.R
import com.javimetallab.deadmanshot.core.Engine
import com.javimetallab.deadmanshot.core.Store
import kotlinx.coroutines.delay

enum class Destination(val titleRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Filled.Home),
    EMAIL(R.string.tab_email, Icons.Filled.Email),
    PULSE(R.string.tab_pulse, Icons.Filled.Favorite),
    PC(R.string.tab_pc, Icons.Filled.Computer),
}

@Composable
fun MainScreen(startTab: Int = 0) {
    val context = LocalContext.current
    val snap by Store.state.collectAsStateWithLifecycle()
    val s = snap ?: return

    var tick by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            tick++
        }
    }
    var permissionsTick by rememberSaveable { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        Engine.tick(context)
        permissionsTick++
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) context.startActivity(notificationSettings(context))
        permissionsTick++
    }
    val askNotifications = {
        if (!notificationsAllowed(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(notificationSettings(context))
            }
        }
    }

    var destination by rememberSaveable { mutableIntStateOf(startTab.coerceIn(0, Destination.entries.size - 1)) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Destination.entries.forEachIndexed { index, d ->
                    NavigationBarItem(
                        selected = destination == index,
                        onClick = { destination = index },
                        icon = { Icon(d.icon, contentDescription = null) },
                        label = { Text(stringResource(d.titleRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = RunningColor,
                            indicatorColor = CheckInColor,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.fillMaxSize().padding(padding)
        when (Destination.entries[destination]) {
            Destination.HOME -> HomeTab(s, tick, permissionsTick, askNotifications, modifier = mod)
            Destination.EMAIL -> EmailTab(s, modifier = mod)
            Destination.PULSE -> PulseTab(s, modifier = mod)
            Destination.PC -> PcTab(s, rememberCoroutineScope(), modifier = mod)
        }
    }
}

internal fun notificationsAllowed(context: Context) =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

internal fun notificationSettings(context: Context) =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
