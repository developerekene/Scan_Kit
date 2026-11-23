package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM qr_history ORDER BY isFavorite DESC, timestamp DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity): Long

    @Query("UPDATE qr_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM qr_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM qr_history")
    suspend fun clearAll()
}
