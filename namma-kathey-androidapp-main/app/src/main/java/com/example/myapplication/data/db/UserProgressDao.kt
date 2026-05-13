package com.example.myapplication.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE ownerUid = :ownerUid ORDER BY heroId ASC")
    fun observeForOwner(ownerUid: String): Flow<List<UserProgressEntity>>

    @Query(
        """
        SELECT * FROM user_progress
        WHERE ownerUid = :ownerUid AND heroId = :heroId
        LIMIT 1
        """,
    )
    suspend fun getForOwnerAndHero(ownerUid: String, heroId: Int): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserProgressEntity)
}
