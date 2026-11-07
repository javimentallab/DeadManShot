package com.javimetallab.deadmanshot

import android.app.Application
import com.javimetallab.deadmanshot.core.Notifications
import com.javimetallab.deadmanshot.core.Store

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        Notifications.createChannels(this)
    }
}
