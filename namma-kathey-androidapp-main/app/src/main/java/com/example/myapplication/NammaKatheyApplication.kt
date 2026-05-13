package com.example.myapplication

import android.app.Application
import android.content.Context
import com.example.myapplication.auth.UserSessionStore
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.DatabaseInitializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class NammaKatheyApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userSessionStore: UserSessionStore
        private set

    override fun onCreate() {
        super.onCreate()
        userSessionStore = UserSessionStore(this)
        database = AppDatabase.create(this)
        runBlocking(Dispatchers.IO) {
            DatabaseInitializer.initializeIfEmpty(database, this@NammaKatheyApplication)
        }
    }

    companion object {
        fun database(app: Application): AppDatabase = (app as NammaKatheyApplication).database

        fun userSession(context: Context): UserSessionStore =
            (context.applicationContext as NammaKatheyApplication).userSessionStore
    }
}
