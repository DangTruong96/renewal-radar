package io.github.dangtruong96.renewalradar.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.dangtruong96.renewalradar.data.AppDatabase
import io.github.dangtruong96.renewalradar.domain.Money
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Runs once a day, fully offline. For every subscription it computes the
 * effective date (trial end, or next renewal) and posts a notification when:
 *
 *  - a renewal is exactly `remindDaysBefore` days away, or
 *  - a free trial ends in 3, 1, or 0 days (trials always get the countdown,
 *    because "cancel before it converts" is the whole point).
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
            val date = RenewalCalculator.effectiveDate(sub, today) ?: continue
            val days = RenewalCalculator.daysUntil(date, today)
            val price = Money.format(sub.amountMinor, sub.currency)
            // A trial whose end date has passed has converted to paid;
            // it gets renewal reminders like any other subscription.
            val trialPending = RenewalCalculator.isTrialPending(sub, today)
            when {
                trialPending && days in TRIAL_REMINDER_DAYS -> {
                    NotificationHelper.notify(
                        applicationContext,
                        sub.id.toInt(),
                        "Free trial ending: ${sub.name}",
                        when (days) {
                            0L -> "${sub.name} converts to a paid plan TODAY at $price. Cancel now if you don't want it."
                            1L -> "${sub.name} converts to a paid plan TOMORROW at $price."
                            else -> "${sub.name} converts to a paid plan in $days days at $price."
                        }
                    )
                }
                !trialPending && days == sub.remindDaysBefore.toLong() -> {
                    NotificationHelper.notify(
                        applicationContext,
                        sub.id.toInt(),
                        "Renewal coming up: ${sub.name}",
                        "${sub.name} renews in $days day(s) at $price. Still using it?"
                    )
                }
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-renewal-check"
        private val TRIAL_REMINDER_DAYS = setOf(3L, 1L, 0L)

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
