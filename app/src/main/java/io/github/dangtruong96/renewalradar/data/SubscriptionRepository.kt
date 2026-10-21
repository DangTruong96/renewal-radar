package io.github.dangtruong96.renewalradar.data

import kotlinx.coroutines.flow.Flow

/** Thin wrapper over [SubscriptionDao]; the UI and worker talk to this. */
class SubscriptionRepository(private val dao: SubscriptionDao) {

    val all: Flow<List<Subscription>> = dao.getAll()

    suspend fun getAllOnce(): List<Subscription> = dao.getAllOnce()

    suspend fun getById(id: Long): Subscription? = dao.getById(id)

    suspend fun add(subscription: Subscription): Long = dao.insert(subscription)

    suspend fun update(subscription: Subscription) = dao.update(subscription)

    suspend fun delete(subscription: Subscription) = dao.delete(subscription)

    /** Replace the whole library (used by JSON import). */
    suspend fun replaceAll(items: List<Subscription>) {
        dao.deleteAll()
        items.forEach { dao.insert(it.copy(id = 0)) }
    }
}
