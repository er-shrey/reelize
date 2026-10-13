package com.shrey.reelize

import android.app.Application
import com.shrey.reelize.data.Stats

class ReelizeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Stats.init(this)
    }
}
