package com.calisbloomprints.pos

import android.app.Application

class BloomprintsPosApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
