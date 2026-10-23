package io.github.dangtruong96.renewalradar.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dangtruong96.renewalradar.data.AppDatabase
import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.data.SubscriptionRepository
import io.github.dangtruong96.renewalradar.domain.BillingCycle
import io.github.dangtruong96.renewalradar.domain.RenewalCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class RenewalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SubscriptionRepository(
        AppDatabase.getInstance(application).subscriptionDao()
    )

    val subscriptions: StateFlow<List<Subscription>> =
        repository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Dashboard totals: monthly-equivalent spend grouped by currency code,
     * e.g. {"USD": 4599, "JPY": 1500}. Currencies are never summed together —
     * there is no honest way to add dollars to yen without an exchange
     * rate, and this app has no network to fetch one with.
     */
    val monthlyTotalsByCurrency: StateFlow<Map<String, Long>> =
        repository.all.map { subs ->
            // Pending trials are not charging yet, so they stay out of
            // the totals; trials whose end date has passed have converted
            // to paid plans and DO count.
            subs.filter { !RenewalCalculator.isTrialPending(it, LocalDate.now()) }
                .groupBy { it.currency }
                .mapValues { (_, items) ->
                    items.sumOf {
                        RenewalCalculator.monthlyEquivalentMinor(
                            it.amountMinor,
                            BillingCycle.fromName(it.cycle)
                        )
                    }
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun add(subscription: Subscription) {
        viewModelScope.launch { repository.add(subscription) }
    }

    fun update(subscription: Subscription) {
        viewModelScope.launch { repository.update(subscription) }
    }

    fun delete(subscription: Subscription) {
        viewModelScope.launch { repository.delete(subscription) }
    }

    // --- JSON export / import (org.json ships with Android) ---

    fun exportJson(): String {
        val array = JSONArray()
        subscriptions.value.forEach { sub ->
            array.put(
                JSONObject()
                    .put("name", sub.name)
                    .put("amountMinor", sub.amountMinor)
                    .put("currency", sub.currency)
                    .put("cycle", sub.cycle)
                    .put("anchorDate", sub.anchorDate)
                    .put("isTrial", sub.isTrial)
                    .put("trialEndDate", sub.trialEndDate ?: JSONObject.NULL)
                    .put("remindDaysBefore", sub.remindDaysBefore)
                    .put("category", sub.category)
                    .put("notes", sub.notes)
                    .put("cancelUrl", sub.cancelUrl)
            )
        }
        return JSONObject().put("version", 1).put("subscriptions", array).toString(2)
    }

    /** Import a document produced by [exportJson]. Returns false on bad input. */
    fun importJson(json: String): Boolean {
        val items = try {
            val array = JSONObject(json).getJSONArray("subscriptions")
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Subscription(
                    name = obj.getString("name"),
                    amountMinor = obj.getLong("amountMinor"),
                    currency = obj.optString("currency", "USD"),
                    cycle = obj.optString("cycle", "MONTHLY"),
                    anchorDate = obj.getString("anchorDate"),
                    isTrial = obj.optBoolean("isTrial", false),
                    trialEndDate = if (obj.isNull("trialEndDate")) null
                    else obj.optString("trialEndDate", ""),
                    remindDaysBefore = obj.optInt("remindDaysBefore", 3),
                    category = obj.optString("category", ""),
                    notes = obj.optString("notes", ""),
                    cancelUrl = obj.optString("cancelUrl", "")
                )
            }
        } catch (e: Exception) {
            return false
        }
        viewModelScope.launch { repository.replaceAll(items) }
        return true
    }
}
