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
import com.example.domain.EditorialState
import com.example.domain.FeedbackIssueType
import com.example.domain.FeedbackReviewStatus
import com.example.domain.FeedbackSubmissionPayload
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.VerificationStatus
import kotlinx.coroutines.flow.Flow

/**
 * Room Local Persistence Layer (Build Specification v3, Sections 3.1, 3.5, 7, 8):
 * - Caches ONLY rights-permitted metadata/summaries locally; never downloads full copyrighted prose.
 * - Preserves verificationStatus (Verified/Pending/Blocked) separate from editorialState (Active/Corrected/Retracted).
 * - Stores local-only bookmarks and optional local feedback drafts on the user's device.
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
    val verificationCheckedAtIso: String?,
    val verificationReason: String?,
    val verificationStatusWire: String,
    val rightsStatusWire: String,
    val editorialStateWire: String,
    val permittedSummary: String?, // Null if LINK_ONLY or BLOCKED
    val correctionsPipeDelimited: String,
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

        val vStatus = VerificationStatus.fromWire(verificationStatusWire)
        val rStatus = RightsStatus.fromWire(rightsStatusWire)
        val eState = EditorialState.fromWire(editorialStateWire)

        val safeSummary = if (rStatus == RightsStatus.LINK_ONLY ||
            rStatus == RightsStatus.BLOCKED ||
            vStatus == VerificationStatus.BLOCKED ||
            eState == EditorialState.RETRACTED
        ) {
            null
        } else {
            permittedSummary
        }

        // Section 7.4: Pending or Blocked content must never appear in Breaking or Live
        val safeBreaking = if (vStatus != VerificationStatus.VERIFIED ||
            rStatus == RightsStatus.BLOCKED ||
            eState == EditorialState.RETRACTED
        ) {
            false
        } else {
            isBreakingOrLive
        }

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
            verificationCheckedAtIso = verificationCheckedAtIso,
            verificationReason = verificationReason,
            verificationStatus = vStatus,
            rightsStatus = rStatus,
            editorialState = eState,
            permittedSummary = safeSummary,
            corrections = parsedCorrections,
            expiryIso = expiryIso,
            isBreakingOrLive = safeBreaking,
            consolidatedSources = parsedSources
        )
    }

    companion object {
        fun fromDomain(record: ArticleRecord): CachedArticleEntity {
            val serializedCorrections = record.corrections.joinToString("||") {
                "${it.correctedAtIso}::${it.reason}"
            }
            val safeSummary = if (record.rightsStatus == RightsStatus.LINK_ONLY ||
                record.rightsStatus == RightsStatus.BLOCKED ||
                record.verificationStatus == VerificationStatus.BLOCKED ||
                record.editorialState == EditorialState.RETRACTED
            ) {
                null
            } else {
                record.permittedSummary
            }
            val safeBreaking = if (record.verificationStatus != VerificationStatus.VERIFIED ||
                record.rightsStatus == RightsStatus.BLOCKED ||
                record.editorialState == EditorialState.RETRACTED
            ) {
                false
            } else {
                record.isBreakingOrLive
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
                verificationCheckedAtIso = record.verificationCheckedAtIso,
                verificationReason = record.verificationReason,
                verificationStatusWire = record.verificationStatus.wireKey,
                rightsStatusWire = record.rightsStatus.wireKey,
                editorialStateWire = record.editorialState.wireKey,
                permittedSummary = safeSummary,
                correctionsPipeDelimited = serializedCorrections,
                expiryIso = record.expiryIso,
                isBreakingOrLive = safeBreaking,
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
    val verificationStatusWire: String = VerificationStatus.VERIFIED.wireKey,
    val rightsStatusWire: String,
    val permittedSummary: String?
)

/**
 * Local Feedback Draft Entity per Section 8.9:
 * "If private storage or reviewer access is not ready, disable submit with an honest unavailable
 * message and preserve only an optional local draft on the user device."
 */
@Entity(tableName = "feedback_drafts")
data class FeedbackDraftEntity(
    @PrimaryKey val feedbackId: String,
    val idempotencyToken: String,
    val articleId: String?,
    val articleTitleSnapshot: String?,
    val issueTypeWire: String,
    val description: String,
    val optionalComments: String?,
    val optionalContact: String?,
    val createdAtEpochMs: Long,
    val reviewStatusWire: String = FeedbackReviewStatus.NEW.wireKey
) {
    fun toDomain(): FeedbackSubmissionPayload = FeedbackSubmissionPayload(
        feedbackId = feedbackId,
        idempotencyToken = idempotencyToken,
        articleId = articleId,
        articleTitleSnapshot = articleTitleSnapshot,
        issueType = FeedbackIssueType.fromWire(issueTypeWire) ?: FeedbackIssueType.GENERAL,
        description = description,
        optionalComments = optionalComments,
        optionalContact = optionalContact,
        createdAtClientEpochMs = createdAtEpochMs
    )

    companion object {
        fun fromDomain(payload: FeedbackSubmissionPayload): FeedbackDraftEntity = FeedbackDraftEntity(
            feedbackId = payload.feedbackId,
            idempotencyToken = payload.idempotencyToken,
            articleId = payload.articleId,
            articleTitleSnapshot = payload.articleTitleSnapshot,
            issueTypeWire = payload.issueType.wireKey,
            description = payload.description,
            optionalComments = payload.optionalComments,
            optionalContact = payload.optionalContact,
            createdAtEpochMs = payload.createdAtClientEpochMs,
            reviewStatusWire = payload.reviewStatus.wireKey
        )
    }
}

@Entity(tableName = "health_logs")
data class HealthLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampEpochMs: Long,
    val powerTier: String,
    val eventType: String,
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

    @Query("SELECT * FROM feedback_drafts ORDER BY createdAtEpochMs DESC")
    fun observeFeedbackDrafts(): Flow<List<FeedbackDraftEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbackDraft(draft: FeedbackDraftEntity)

    @Query("UPDATE feedback_drafts SET optionalContact = NULL WHERE feedbackId = :feedbackId")
    suspend fun removeContactFromDraft(feedbackId: String)

    @Query("DELETE FROM feedback_drafts WHERE feedbackId = :feedbackId")
    suspend fun deleteFeedbackDraftById(feedbackId: String)

    @Query("DELETE FROM feedback_drafts")
    suspend fun clearAllFeedbackDrafts()

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
    entities = [
        CachedArticleEntity::class,
        BookmarkEntity::class,
        FeedbackDraftEntity::class,
        HealthLogEntity::class
    ],
    version = 2,
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
