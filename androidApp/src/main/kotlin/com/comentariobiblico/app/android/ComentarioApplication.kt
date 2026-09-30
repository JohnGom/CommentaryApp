package com.comentariobiblico.app.android

import android.app.Application
import com.comentariobiblico.app.di.appModule
import com.comentariobiblico.app.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ComentarioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ComentarioApplication)
            modules(appModule, platformModule())
        }
    }
}
