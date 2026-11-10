package com.example.runcorse

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow

class RunRepository(private val runDao: RunDao, private val context: Context) {

    val allRuns: Flow<List<RunEntity>> = runDao.allRuns()

    suspend fun insertRun(text: String, photo: String? = null) {
        val newRun = RunEntity(
            text = text,
            photo = photo,
            timestamp = System.currentTimeMillis(),
            synced = false
        )
        runDao.upsert(newRun)
        enqueueSync()
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "sync_runs",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}