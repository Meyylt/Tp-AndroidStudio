package com.example.runcorse

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as RunApplication
            val dao = app.database.runDao()

            val pendingRuns = dao.getUnsyncedRuns()
            if (pendingRuns.isEmpty()) return Result.success()

            val idsToMark = mutableListOf<Long>()

            withContext(Dispatchers.IO) {
                for (run in pendingRuns) {
                    val url = URL("https://httpbin.org/post")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.outputStream.write("{\"text\":\"${run.text}\"}".toByteArray())

                    if (conn.responseCode == 200) {
                        idsToMark.add(run.id)
                    }
                    conn.disconnect()
                }
            }

            if (idsToMark.isNotEmpty()) {
                dao.markSynced(idsToMark)
            }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }
}