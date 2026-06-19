package net.noblesite.arm

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long?
)

@Entity(
    tableName = "resource_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["sessionId", "stage"], unique = true)
    ]
)
data class ResourceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sessionId: Long,
    val stage: SnapshotStage,
    val capturedAtMillis: Long,
    val availableMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val lowMemory: Boolean,
    val batteryPercent: Int?,
    val batteryCharging: Boolean?,
    val totalRxBytes: Long?,
    val totalTxBytes: Long?
)

enum class SnapshotStage {
    START,
    STOP
}

data class SessionWithSnapshots(
    @Embedded val session: SessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val snapshots: List<ResourceSnapshotEntity>
)
