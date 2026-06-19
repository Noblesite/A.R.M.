package net.noblesite.arm

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ArmDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: ResourceSnapshotEntity)

    @Query("UPDATE sessions SET endedAtMillis = :endedAtMillis WHERE id = :sessionId")
    suspend fun markSessionEnded(sessionId: Long, endedAtMillis: Long)

    @Transaction
    @Query("SELECT * FROM sessions WHERE endedAtMillis IS NULL ORDER BY startedAtMillis DESC LIMIT 1")
    suspend fun getActiveSession(): SessionWithSnapshots?

    @Transaction
    @Query("SELECT * FROM sessions WHERE endedAtMillis IS NOT NULL ORDER BY endedAtMillis DESC LIMIT 1")
    suspend fun getLastCompletedSession(): SessionWithSnapshots?
}
