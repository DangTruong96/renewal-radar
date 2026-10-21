package io.github.dangtruong96.renewalradar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY name ASC")
    fun getAll(): Flow<List<Subscription>>

    /** One-shot snapshot used by the reminder worker. */
    @Query("SELECT * FROM subscriptions")
    suspend fun getAllOnce(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: Subscription): Long

    @Update
    suspend fun update(subscription: Subscription)

    @Delete
    suspend fun delete(subscription: Subscription)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAll()
}
