package `in`.rahulja.getlogs.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogType

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity): Long

    @Query(
        """
        SELECT * FROM event_logs
        WHERE (:logType IS NULL OR logType = :logType)
        ORDER BY timestamp DESC, id DESC
        """
    )
    fun getAllLogsPaging(logType: LogType? = null): PagingSource<Int, LogEntity>

    @Query(
        """
        SELECT event_logs.* FROM event_logs
        JOIN event_logs_fts ON event_logs.id = event_logs_fts.docid
        WHERE event_logs_fts MATCH :query
          AND (:logType IS NULL OR event_logs.logType = :logType)
        ORDER BY event_logs.timestamp DESC, event_logs.id DESC
        """
    )
    fun searchLogsPaging(query: String, logType: LogType? = null): PagingSource<Int, LogEntity>

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC, id DESC")
    suspend fun getAllLogsList(): List<LogEntity>

    @Query(
        """
        SELECT event_logs.* FROM event_logs
        JOIN event_logs_fts ON event_logs.id = event_logs_fts.docid
        WHERE event_logs_fts MATCH :query
        ORDER BY event_logs.timestamp DESC, event_logs.id DESC
        """
    )
    suspend fun searchLogsList(query: String): List<LogEntity>

    @Query("SELECT * FROM event_logs WHERE id = :id")
    suspend fun getLogById(id: Long): LogEntity?

    @Query("DELETE FROM event_logs")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM event_logs")
    suspend fun count(): Int
}
