package com.example.audiobook

import android.app.Application
import android.content.Context
import androidx.multidex.MultiDex

class BaseApplication : Application(){
    companion object {
        private lateinit var mutableApp: BaseApplication
        val app: BaseApplication by lazy { mutableApp }
    }

    override fun onCreate() {
        super.onCreate()
        mutableApp = this
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }
}