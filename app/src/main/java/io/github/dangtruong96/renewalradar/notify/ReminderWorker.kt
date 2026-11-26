package io.github.dangtruong96.renewalradar.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.dangtruong96.renewalradar.data.AppDatabase
import io.github.dangtruong96.renewalradar.domain.ReminderMessage
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Runs once a day, fully offline. For every subscription it asks
 * [RenewalCalculator.reminderDue] whether a notification is due today
 * and posts it — the date math (trial countdown plus the chosen lead
 * time, renewal lead clamped to the closest day the cycle can reach)
 * lives in that pure function, so the worker only renders the message.
 *
 * Known limitation: the exact-day rule means a renewal reminder is
 * skipped (not delivered late) if the device is off on that one day —
 * see "Known limitations" in the README for why the fix needs a
 * persisted already-reminded marker rather than a wider window.
 *
 * WorkManager persists the schedule across reboots by itself, so no
 * RECEIVE_BOOT_COMPLETED receiver is needed.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        val dao = AppDatabase.getInstance(applicationContext).subscriptionDao()
        val subscriptions = dao.getAllOnce()

        for (sub in subscriptions) {
            val reminder = RenewalCalculator.reminderDue(sub, today) ?: continue
            // The exact title/body live in ReminderMessage (pure, tested);
            // the worker only hands them to the OS.
            val (title, text) = ReminderMessage.render(sub, reminder)
            NotificationHelper.notify(applicationContext, sub.id.toInt(), title, text)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-renewal-check"

        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
