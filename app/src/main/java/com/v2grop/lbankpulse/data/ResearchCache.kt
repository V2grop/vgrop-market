package com.v2grop.lbankpulse.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executors

/** Display/history cache only. No cached row is an implicit inference input. */
@Entity(tableName = "research_cache", primaryKeys = ["instrumentId", "timeframe", "category"])
data class CacheEntry(
    val instrumentId: String,
    val timeframe: Int,
    val category: String,
    val source: String,
    val marketKind: String,
    val sourceTimestamp: Long,
    val retrievedAt: Long,
    val expiresAt: Long,
    val payload: String
) {
    fun isFresh(now: Long): Boolean = now >= retrievedAt && now <= expiresAt && sourceTimestamp <= now
}
@Dao
interface CacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun put(row: CacheEntry)
    @Query("SELECT * FROM research_cache WHERE instrumentId=:id AND timeframe=:timeframe AND category=:category")
    fun get(id: String, timeframe: Int, category: String): CacheEntry?
    @Query("SELECT * FROM research_cache ORDER BY retrievedAt DESC LIMIT 100") fun observe(): Flow<List<CacheEntry>>
    @Query("DELETE FROM research_cache WHERE retrievedAt < :before") fun prune(before: Long)
}
@Database(entities = [CacheEntry::class], version = 1, exportSchema = true)
abstract class ResearchDatabase : RoomDatabase() { abstract fun entries(): CacheDao }

class ResearchCacheRepository(private val dao: CacheDao) {
    fun history(): Flow<List<CacheEntry>> = dao.observe()
    fun getFresh(id: String, timeframe: Int, category: String, now: Long): CacheEntry? =
        dao.get(id, timeframe, category)?.takeIf { it.isFresh(now) }
    fun record(row: CacheEntry) { dao.put(row); dao.prune(row.retrievedAt - 30L * 86400000) }
}
/** Single app-process writer. Errors cannot prevent live analysis or destroy legacy preferences. */
object CacheStore {
    private val writer = Executors.newSingleThreadExecutor()
    @Volatile private var db: ResearchDatabase? = null
    @Synchronized private fun database(context: Context): ResearchDatabase = db ?: Room.databaseBuilder(
        context.applicationContext, ResearchDatabase::class.java, "vgrop-research.db"
    ).build().also { db = it }
    @JvmStatic fun record(context: Context, id: String, timeframe: Int, category: String,
        source: String, kind: String, sourceTime: Long, retrievedAt: Long, ttl: Long, payload: String) {
        val app = context.applicationContext
        writer.execute { try {
            ResearchCacheRepository(database(app).entries()).record(CacheEntry(id, timeframe, category,
                source, kind, sourceTime, retrievedAt, retrievedAt + ttl, payload))
        } catch (e: Exception) { android.util.Log.w("VGropCache", "Cache write unavailable") } }
    }
}
