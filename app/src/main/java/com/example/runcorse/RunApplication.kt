package com.example.runcorse

import android.app.Application
import androidx.room.Room

class RunApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(this, RunDatabase::class.java, "run-corse-db").build()
    }
    val repository by lazy { RunRepository(database.runDao(), this) }
}