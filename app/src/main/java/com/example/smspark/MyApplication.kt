package com.example.smspark

import android.app.Application
import com.example.smspark.dimodules.webserviceModule
import com.example.smspark.dimodules.zoneModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        //Start Koin
        startKoin{
            androidLogger()
            androidContext(this@MyApplication)
            modules(listOf(webserviceModule, zoneModule))
        }
    }
}