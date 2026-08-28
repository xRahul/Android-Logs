package `in`.rahulja.getlogs.model

import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = LogEntity::class)
@Entity(tableName = "event_logs_fts")
data class LogFtsEntity(
    val action: String,
    val dataPayload: String,
    val formattedText: String
)
