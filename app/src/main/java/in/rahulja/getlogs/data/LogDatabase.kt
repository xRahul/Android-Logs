package `in`.rahulja.getlogs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import `in`.rahulja.getlogs.model.LogEntity
import `in`.rahulja.getlogs.model.LogFtsEntity

@Database(
    entities = [LogEntity::class, LogFtsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LogDatabase : RoomDatabase() {

    abstract fun logDao(): LogDao

    companion object {
        const val DATABASE_NAME = "logs.db"

        @Volatile
        private var instance: LogDatabase? = null

        fun getInstance(context: Context): LogDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    DATABASE_NAME
                ).build().also { instance = it }
            }
        }
    }
}
