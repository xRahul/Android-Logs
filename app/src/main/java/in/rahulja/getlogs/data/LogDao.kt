package `in`.rahulja.getlogs.data

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import `in`.rahulja.getlogs.model.LogEntity

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity): Long

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC, id DESC")
    fun getAllLogsPaging(): PagingSource<Int, LogEntity>

    @Query(
        """
        SELECT event_logs.* FROM event_logs
        JOIN event_logs_fts ON event_logs.id = event_logs_fts.docid
        WHERE event_logs_fts MATCH :query
        ORDER BY event_logs.timestamp DESC, event_logs.id DESC
        """
    )
    fun searchLogsPaging(query: String): PagingSource<Int, LogEntity>

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
