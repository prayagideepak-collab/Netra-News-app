package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.ArticleRecord
import com.example.domain.CorrectionEntry
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.VerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Room Local Persistence Layer (Section 3.1, 3.5 & Efficiency Rules):
 * - Caches ONLY rights-permitted metadata/summaries locally; never downloads full copyrighted prose.
 * - Batches database writes via @Insert(onConflict = REPLACE) on list collections.
 * - Keeps bookmarks 100% local on device (no account/cloud sync).
 * - Maintains a bounded, rotated local health log (max 100 rows).
 */

@Entity(tableName = "cached_articles")
data class CachedArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val language: String,
    val category: String,
    val scopeWire: String,
    val countryCode: String?,
    val stateCode: String?,
    val cityId: String?,
    val publisherName: String,
    val canonicalSourceUrl: String,
    val publishedAtIso: String?,
    val fetchedAtEpochMs: Long,
    val verifiedAtIso: String?,
    val verificationStatusWire: String,
    val rightsStatusWire: String,
    val permittedSummary: String?, // Null if LINK_ONLY
    val correctionsPipeDelimited: String, // "iso::reason||iso::reason"
    val expiryIso: String?,
    val isBreakingOrLive: Boolean,
    val consolidatedSourcesCsv: String
) {
    fun toDomain(): ArticleRecord {
        val parsedCorrections = if (correctionsPipeDelimited.isBlank()) {
            emptyList()
        } else {
            correctionsPipeDelimited.split("||").mapNotNull { token ->
                val parts = token.split("::", limit = 2)
                if (parts.size == 2) CorrectionEntry(parts[0], parts[1]) else null
            }
        }
        val parsedSources = consolidatedSourcesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(publisherName) }

        return ArticleRecord(
            id = id,
            title = title,
            language = language,
            category = category,
            scope = NewsScope.fromWire(scopeWire) ?: NewsScope.NATIONAL,
            countryCode = countryCode,
            stateCode = stateCode,
            cityId = cityId,
            publisherName = publisherName,
            canonicalSourceUrl = canonicalSourceUrl,
            publishedAtIso = publishedAtIso,
            fetchedAtEpochMs = fetchedAtEpochMs,
            verifiedAtIso = verifiedAtIso,
            verificationStatus = VerificationStatus.fromWire(verificationStatusWire),
            rightsStatus = RightsStatus.fromWire(rightsStatusWire),
            permittedSummary = if (RightsStatus.fromWire(rightsStatusWire) == RightsStatus.LINK_ONLY) null else permittedSummary,
            corrections = parsedCorrections,
            expiryIso = expiryIso,
            isBreakingOrLive = isBreakingOrLive,
            consolidatedSources = parsedSources
        )
    }

    companion object {
        fun fromDomain(record: ArticleRecord): CachedArticleEntity {
            val serializedCorrections = record.corrections.joinToString("||") {
                "${it.correctedAtIso}::${it.reason}"
            }
            // Enforce Section 3.1: Do not cache unpermitted summaries for LINK_ONLY items
            val safeSummary = if (record.rightsStatus == RightsStatus.LINK_ONLY ||
                record.rightsStatus == RightsStatus.BLOCKED
            ) {
                null
            } else {
                record.permittedSummary
            }
            return CachedArticleEntity(
                id = record.id,
                title = record.title,
                language = record.language,
                category = record.category,
                scopeWire = record.scope.wireKey,
                countryCode = record.countryCode,
                stateCode = record.stateCode,
                cityId = record.cityId,
                publisherName = record.publisherName,
                canonicalSourceUrl = record.canonicalSourceUrl,
                publishedAtIso = record.publishedAtIso,
                fetchedAtEpochMs = record.fetchedAtEpochMs,
                verifiedAtIso = record.verifiedAtIso,
                verificationStatusWire = record.verificationStatus.wireKey,
                rightsStatusWire = record.rightsStatus.wireKey,
                permittedSummary = safeSummary,
                correctionsPipeDelimited = serializedCorrections,
                expiryIso = record.expiryIso,
                isBreakingOrLive = record.isBreakingOrLive,
                consolidatedSourcesCsv = record.consolidatedSources.joinToString(",")
            )
        }
    }
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val articleId: String,
    val title: String,
    val language: String,
    val category: String,
    val scopeWire: String,
    val cityId: String?,
    val stateCode: String?,
    val publisherName: String,
    val canonicalSourceUrl: String,
    val publishedAtIso: String?,
    val savedAtEpochMs: Long,
    val rightsStatusWire: String,
    val permittedSummary: String?
)

@Entity(tableName = "health_logs")
data class HealthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long,
    val powerTier: String,
    val eventType: String, // "SYNC_OK", "BACKOFF_RETRY", "CIRCUIT_OPEN", "SCHEMA_REJECT", "LOCATION_COARSE"
    val message: String
)

@Dao
interface NetraDao {

    @Query("SELECT * FROM cached_articles ORDER BY isBreakingOrLive DESC, publishedAtIso DESC")
    fun observeCachedArticles(): Flow<List<CachedArticleEntity>>

    @Query("SELECT * FROM cached_articles ORDER BY isBreakingOrLive DESC, publishedAtIso DESC")
    suspend fun getCachedArticlesOnce(): List<CachedArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedArticlesBatch(items: List<CachedArticleEntity>)

    @Query("DELETE FROM cached_articles")
    suspend fun clearCachedArticles()

    @Query("SELECT * FROM bookmarks ORDER BY savedAtEpochMs DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE articleId = :articleId")
    suspend fun deleteBookmarkById(articleId: String)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAllBookmarks()

    @Query("SELECT * FROM health_logs ORDER BY timestampEpochMs DESC LIMIT 100")
    fun observeBoundedHealthLogs(): Flow<List<HealthLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthLog(log: HealthLogEntity)

    @Query("DELETE FROM health_logs WHERE id NOT IN (SELECT id FROM health_logs ORDER BY timestampEpochMs DESC LIMIT 100)")
    suspend fun pruneHealthLogsToMax100()

    @Query("DELETE FROM health_logs")
    suspend fun clearHealthLogs()
}

@Database(
    entities = [CachedArticleEntity::class, BookmarkEntity::class, HealthLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NetraLocalDatabase : RoomDatabase() {
    abstract fun netraDao(): NetraDao

    companion object {
        @Volatile
        private var INSTANCE: NetraLocalDatabase? = null

        fun getInstance(context: Context): NetraLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetraLocalDatabase::class.java,
                    "netra_news_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
