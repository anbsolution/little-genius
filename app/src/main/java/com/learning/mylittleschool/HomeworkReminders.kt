package com.learning.mylittleschool

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class HomeworkReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE)?.takeIf(String::isNotBlank) ?: return Result.failure()
        val subject = inputData.getString(KEY_SUBJECT).orEmpty()
        val profileId = inputData.getString(KEY_PROFILE_ID).orEmpty()
        val taskId = inputData.getString(KEY_TASK_ID).orEmpty()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }
        val notifications = NotificationManagerCompat.from(applicationContext)
        if (!notifications.areNotificationsEnabled()) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Homework reminders / गृहकार्य अनुस्मारक",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            applicationContext.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Homework reminder / गृहकार्य याद दिलाना")
            .setContentText(if (subject.isBlank()) title else "$subject · $title")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n$subject"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        return runCatching {
            notifications.notify("$profileId:$taskId", taskId.hashCode(), notification)
            Result.success()
        }.getOrElse {
            if (it is SecurityException) Result.success() else Result.retry()
        }
    }

    companion object {
        private const val KEY_TITLE = "title"
        private const val KEY_SUBJECT = "subject"
        private const val KEY_PROFILE_ID = "profile_id"
        private const val KEY_TASK_ID = "task_id"
        private const val CHANNEL_ID = "homework_reminders"

        fun schedule(context: Context, task: HomeworkEntity) {
            val reminderAt = task.reminderAt ?: return
            if (task.isCompleted) return
            val request = OneTimeWorkRequestBuilder<HomeworkReminderWorker>()
                .setInputData(
                    workDataOf(
                        KEY_TITLE to task.title,
                        KEY_SUBJECT to task.subject,
                        KEY_PROFILE_ID to task.profileId,
                        KEY_TASK_ID to task.taskId
                    )
                )
                .setInitialDelay((reminderAt - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                uniqueWorkName(task.profileId, task.taskId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, profileId: String, taskId: String) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(uniqueWorkName(profileId, taskId))
        }

        private fun uniqueWorkName(profileId: String, taskId: String) =
            "homework-reminder:$profileId:$taskId"
    }
}
