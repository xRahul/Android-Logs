package `in`.rahulja.getlogs.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val action: String,
    val dataPayload: String,
    val logType: LogType,
    val formattedText: String
)
