package com.example.data

import com.example.data.local.AppDatabase
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.CachedArticleEntity
import com.example.data.local.FeedbackDraftEntity
import com.example.data.local.HealthLogEntity
import com.example.data.local.NetraDao
import com.example.data.remote.FeedbackRequestDto
import com.example.data.remote.NetraNetworkClient
import com.example.data.remote.NetraNewsApiService
import com.example.domain.Article
import com.example.domain.ArticleRecord
import com.example.domain.EditorialState
import com.example.domain.FeedEnvelope
import com.example.domain.FeedbackDeliveryResult
import com.example.domain.FeedbackSubmissionPayload
import com.example.domain.NewsScope
import com.example.domain.OfflineSyncStatus
import com.example.domain.RightsStatus
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.VerificationStatus
import com.example.power.PowerTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Result of reconciling saved offline bookmarks against a newly fetched feed envelope.
 */
data class BookmarkReconciliationSummary(
    val updatedCount: Int = 0,
    val retractedOrBlockedCount: Int = 0,
    val removedFromLiveFeedCount: Int = 0
)

/**
 * Unified Data Repository (`com.example.data.DataRepository`) mediating between the local
 * Room database ([AppDatabase] -> [ArticleDao], [NetraDao]) and external Retrofit/Moshi
 * network data sources ([NetraNewsApiService]).
 *
 * Responsibilities:
 * 1. Network <-> Room Cache Mediation: Fetches remote [FeedEnvelope] when a verified HTTPS
 *    endpoint is configured, validates schema/rights via [ValidationAndRightsEngine], updates
 *    Room cache, and falls back gracefully to cached Room data when offline.
 * 2. Offline Reading Mode (Saved Bookmarks): Exposes reactive flows of [ArticleEntity] and
 *    [Article], performs bookmark CRUD, marks items clearly as offline content, and gracefully
 *    handles upstream updates, retractions/blocks, or feed removals without crashing or deleting
 *    user bookmarks unexpectedly.
 * 3. User Feedback Mediation: Logs validated feedback reports locally in Room (`feedback_drafts`)
 *    or dispatches them via Retrofit to a designated private review endpoint when connected.
 */
class DataRepository(
    private val articleDao: ArticleDao,
    private val netraDao: NetraDao,
    private val apiService: NetraNewsApiService? = null,
    private val isRemoteFeedEndpointConfigured: Boolean = false,
    private val isPrivateFeedbackEndpointConnected: Boolean = false
) {
    constructor(
        database: AppDatabase,
        apiService: NetraNewsApiService? = null,
        isRemoteFeedEndpointConfigured: Boolean = false,
        isPrivateFeedbackEndpointConnected: Boolean = false
    ) : this(
        articleDao = database.articleDao(),
        netraDao = database.netraDao(),
        apiService = apiService,
        isRemoteFeedEndpointConfigured = isRemoteFeedEndpointConfigured,
        isPrivateFeedbackEndpointConnected = isPrivateFeedbackEndpointConnected
    )

    // =========================================================================
    // Reactive Flows (Room Single Source of Truth)
    // =========================================================================

    val offlineBookmarkEntitiesFlow: Flow<List<ArticleEntity>> = articleDao.getAllBookmarks()

    val offlineBookmarkedArticlesFlow: Flow<List<Article>> =
        articleDao.getAllBookmarks().map { entities -> entities.map { it.toDomain() } }

    val cachedArticlesFlow: Flow<List<ArticleRecord>> =
        netraDao.observeCachedArticles().map { list -> list.map { it.toDomain() } }

    val feedbackLogsFlow: Flow<List<FeedbackDraftEntity>> = netraDao.observeFeedbackDrafts()

    val healthLogsFlow: Flow<List<HealthLogEntity>> = netraDao.observeBoundedHealthLogs()

    fun observeIsBookmarkedOffline(articleId: String): Flow<Boolean> =
        articleDao.isBookmarked(articleId)

    // =========================================================================
    // Network <-> Room Feed Mediation
    // =========================================================================

    /**
     * Attempts to fetch a [FeedEnvelope] from the Retrofit/Moshi [NetraNewsApiService] when
     * configured and online; otherwise delegates to [fallbackEnvelopeProvider] or Room cache.
     * Upon receiving fresh validated articles, reconciles all saved offline bookmarks gracefully.
     */
    suspend fun fetchFeedWithRoomFallback(
        scope: NewsScope,
        cityId: String?,
        isOffline: Boolean,
        etag: String? = null,
        nowEpochMs: Long = System.currentTimeMillis(),
        fallbackEnvelopeProvider: (() -> FeedEnvelope)? = null
    ): Result<FeedEnvelope> = withContext(Dispatchers.IO) {
        if (isOffline) {
            val cached = netraDao.getCachedArticlesOnce().map { it.toDomain() }
            return@withContext Result.success(
                FeedEnvelope(
                    schemaVersion = ValidationAndRightsEngine.SUPPORTED_SCHEMA_VERSION,
                    generatedAtUtcIso = "2026-11-18T00:00:00Z",
                    coverage = listOf("${scope.wireKey}:${cityId ?: "IN"}"),
                    items = cached,
                    sourceHealth = emptyList()
                )
            )
        }

        if (isRemoteFeedEndpointConfigured && apiService != null) {
            try {
                val response = apiService.fetchFeedEnvelope(
                    scope = scope.wireKey,
                    cityId = cityId,
                    ifNoneMatch = etag
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val envelope = body.toDomain(fetchedAtMs = nowEpochMs)
                        val validation = ValidationAndRightsEngine.validateFeedEnvelope(
                            envelope = envelope,
                            referenceNowEpochMs = nowEpochMs,
                            isProductionFeed = true
                        )
                        if (validation.isValidSchema) {
                            persistFeedToRoomAndReconcileBookmarks(
                                activeArticles = validation.verifiedDisplayableItems,
                                retractedOrBlocked = validation.retractedNoticeItems +
                                    validation.warningStatusRecords.filter {
                                        it.verificationStatus == VerificationStatus.BLOCKED ||
                                            it.rightsStatus == RightsStatus.BLOCKED
                                    },
                                syncedAtEpochMs = nowEpochMs
                            )
                        }
                        return@withContext Result.success(envelope)
                    }
                }
            } catch (e: Exception) {
                // Fall through cleanly to local fallback / Room cache if remote fetch fails
            }
        }

        val fallback = fallbackEnvelopeProvider?.invoke()
        if (fallback != null) {
            return@withContext Result.success(fallback)
        }

        val cached = netraDao.getCachedArticlesOnce().map { it.toDomain() }
        Result.success(
            FeedEnvelope(
                schemaVersion = ValidationAndRightsEngine.SUPPORTED_SCHEMA_VERSION,
                generatedAtUtcIso = "2026-11-18T00:00:00Z",
                coverage = listOf("${scope.wireKey}:${cityId ?: "IN"}"),
                items = cached,
                sourceHealth = emptyList()
            )
        )
    }

    suspend fun persistFeedToRoomAndReconcileBookmarks(
        activeArticles: List<ArticleRecord>,
        retractedOrBlocked: List<ArticleRecord> = emptyList(),
        syncedAtEpochMs: Long = System.currentTimeMillis()
    ): BookmarkReconciliationSummary = withContext(Dispatchers.IO) {
        if (activeArticles.isNotEmpty()) {
            val cachedEntities = activeArticles.map { CachedArticleEntity.fromDomain(it) }
            netraDao.clearCachedArticles()
            netraDao.insertCachedArticlesBatch(cachedEntities)
        }
        reconcileSavedBookmarksWithFeed(
            latestActiveArticles = activeArticles,
            retractedOrBlockedArticles = retractedOrBlocked,
            syncedAtEpochMs = syncedAtEpochMs
        )
    }

    // =========================================================================
    // Offline Reading Mode: Saved Bookmarks CRUD & Graceful Update/Removal
    // =========================================================================

    suspend fun saveOfflineBookmark(
        article: Article,
        savedAtEpochMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        articleDao.saveBookmark(article, savedAtEpochMs)
        val entity = ArticleEntity.fromDomain(article, savedAtEpochMs = savedAtEpochMs, isBookmarked = true)
        netraDao.insertBookmark(entity.toBookmarkEntity())
    }

    suspend fun saveOfflineBookmark(
        record: ArticleRecord,
        savedAtEpochMs: Long = System.currentTimeMillis()
    ) = saveOfflineBookmark(Article.fromArticleRecord(record), savedAtEpochMs)

    suspend fun toggleOfflineBookmark(
        record: ArticleRecord,
        currentlySavedIds: Set<String>,
        savedAtEpochMs: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        if (currentlySavedIds.contains(record.id) || articleDao.isBookmarkedOnce(record.id)) {
            removeOfflineBookmark(record.id)
            false
        } else {
            saveOfflineBookmark(record, savedAtEpochMs)
            true
        }
    }

    suspend fun updateOfflineBookmark(
        article: Article,
        syncStatus: OfflineSyncStatus = OfflineSyncStatus.UPDATED_FROM_FEED,
        syncedAtEpochMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        articleDao.updateSavedBookmark(article, syncStatus, syncedAtEpochMs)
        val updatedEntity = articleDao.getArticleById(article.id)
        if (updatedEntity != null) {
            netraDao.insertBookmark(updatedEntity.toBookmarkEntity())
        }
    }

    suspend fun removeOfflineBookmark(articleId: String) = withContext(Dispatchers.IO) {
        articleDao.removeBookmark(articleId)
        netraDao.deleteBookmarkById(articleId)
    }

    suspend fun clearAllOfflineBookmarks() = withContext(Dispatchers.IO) {
        articleDao.clearAllBookmarks()
        netraDao.clearAllBookmarks()
    }

    suspend fun getOfflineBookmarkById(articleId: String): ArticleEntity? =
        withContext(Dispatchers.IO) { articleDao.getArticleById(articleId) }

    suspend fun getAllOfflineBookmarksOnce(): List<ArticleEntity> =
        withContext(Dispatchers.IO) { articleDao.getAllBookmarksOnce() }

    /**
     * Gracefully reconciles saved offline bookmarks when a feed refresh arrives:
     * 1. If a saved article was **retracted** or **blocked**, strips its summary/media, updates
     *    its editorial/verification status, and marks it [OfflineSyncStatus.RETRACTED_OR_BLOCKED].
     * 2. If a saved article was **updated** (e.g., corrected title, summary, or status), updates the
     *    saved Room row and marks it [OfflineSyncStatus.UPDATED_FROM_FEED].
     * 3. If a saved article is **no longer present** in the active feed, preserves the user's offline
     *    copy rather than silently deleting it and marks it [OfflineSyncStatus.REMOVED_FROM_LIVE_FEED].
     */
    suspend fun reconcileSavedBookmarksWithFeed(
        latestActiveArticles: List<ArticleRecord>,
        retractedOrBlockedArticles: List<ArticleRecord> = emptyList(),
        syncedAtEpochMs: Long = System.currentTimeMillis(),
        markMissingAsRemovedFromFeed: Boolean = false
    ): BookmarkReconciliationSummary = withContext(Dispatchers.IO) {
        val savedBookmarks = articleDao.getAllBookmarksOnce()
        if (savedBookmarks.isEmpty()) {
            return@withContext BookmarkReconciliationSummary()
        }

        val activeById = latestActiveArticles.associateBy { it.id }
        val withheldById = retractedOrBlockedArticles.associateBy { it.id }

        var updatedCount = 0
        var retractedCount = 0
        var removedCount = 0

        for (saved in savedBookmarks) {
            val withheld = withheldById[saved.id]
            if (withheld != null) {
                articleDao.markBookmarkRetractedOrBlocked(
                    articleId = saved.id,
                    editorialState = withheld.editorialState.wireKey,
                    verificationStatus = withheld.verificationStatus.wireKey,
                    reason = withheld.verificationReason
                        ?: withheld.corrections.firstOrNull()?.reason
                        ?: "Content retracted or blocked by source editorial check.",
                    syncedAtEpochMs = syncedAtEpochMs
                )
                val updated = articleDao.getArticleById(saved.id)
                if (updated != null) {
                    netraDao.insertBookmark(updated.toBookmarkEntity())
                }
                retractedCount++
                continue
            }

            val fresh = activeById[saved.id]
            if (fresh != null) {
                val freshArticle = Article.fromArticleRecord(fresh)
                val contentChanged = freshArticle.title != saved.title ||
                    freshArticle.safePermittedSummary != saved.permittedSummary ||
                    freshArticle.verificationStatus.wireKey != saved.verificationStatus ||
                    freshArticle.rightsStatus.wireKey != saved.rightsStatus ||
                    freshArticle.editorialState.wireKey != saved.editorialState
                if (contentChanged) {
                    articleDao.updateSavedBookmark(
                        article = freshArticle,
                        syncStatus = OfflineSyncStatus.UPDATED_FROM_FEED,
                        syncedAtEpochMs = syncedAtEpochMs
                    )
                    val updated = articleDao.getArticleById(saved.id)
                    if (updated != null) {
                        netraDao.insertBookmark(updated.toBookmarkEntity())
                    }
                    updatedCount++
                }
            } else if (markMissingAsRemovedFromFeed &&
                saved.offlineSyncEnum != OfflineSyncStatus.RETRACTED_OR_BLOCKED &&
                saved.offlineSyncEnum != OfflineSyncStatus.REMOVED_FROM_LIVE_FEED
            ) {
                articleDao.updateOfflineSyncStatus(
                    articleId = saved.id,
                    syncStatus = OfflineSyncStatus.REMOVED_FROM_LIVE_FEED.wireKey,
                    syncedAtEpochMs = syncedAtEpochMs
                )
                removedCount++
            }
        }

        BookmarkReconciliationSummary(
            updatedCount = updatedCount,
            retractedOrBlockedCount = retractedCount,
            removedFromLiveFeedCount = removedCount
        )
    }

    // =========================================================================
    // User Feedback Mediation (Local Room Logging + Optional Remote Endpoint)
    // =========================================================================

    /**
     * Logs a validated feedback item in Room (`feedback_drafts`) and, when a private review
     * endpoint is configured and reachable, attempts delivery via Retrofit [NetraNewsApiService].
     * Per Section 8.9, if the remote endpoint is not connected, keeps remote submit honestly
     * disabled (never simulating success) while retaining the local feedback log entry.
     */
    suspend fun submitOrLogFeedback(
        payload: FeedbackSubmissionPayload,
        simulateNetworkFailure: Boolean = false
    ): FeedbackDeliveryResult = withContext(Dispatchers.IO) {
        if (!isPrivateFeedbackEndpointConnected || apiService == null) {
            netraDao.insertFeedbackDraft(
                FeedbackDraftEntity.fromDomain(
                    payload = payload,
                    deliveryStatus = "LOGGED_LOCALLY_ENDPOINT_DISABLED",
                    receiptId = null
                )
            )
            return@withContext ValidationAndRightsEngine.processFeedbackDelivery(
                validatedPayload = payload,
                isPrivateEndpointConnected = false
            )
        }

        if (simulateNetworkFailure) {
            netraDao.insertFeedbackDraft(
                FeedbackDraftEntity.fromDomain(
                    payload = payload,
                    deliveryStatus = "LOGGED_LOCALLY_PENDING_RETRY",
                    receiptId = null
                )
            )
            return@withContext ValidationAndRightsEngine.processFeedbackDelivery(
                validatedPayload = payload,
                isPrivateEndpointConnected = true,
                simulateNetworkFailure = true
            )
        }

        return@withContext try {
            val response = apiService.submitFeedbackReport(
                idempotencyToken = payload.idempotencyToken,
                request = FeedbackRequestDto.fromDomain(payload)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                netraDao.insertFeedbackDraft(
                    FeedbackDraftEntity.fromDomain(
                        payload = payload,
                        deliveryStatus = "SENT_TO_REVIEW_ENDPOINT",
                        receiptId = body.receiptId
                    )
                )
                FeedbackDeliveryResult.DurableServerReceipt(
                    receiptId = body.receiptId,
                    serverTimestampIso = body.serverTimestamp,
                    deduplicatedRetry = body.deduplicated
                )
            } else {
                netraDao.insertFeedbackDraft(
                    FeedbackDraftEntity.fromDomain(
                        payload = payload,
                        deliveryStatus = "LOGGED_LOCALLY_PENDING_RETRY",
                        receiptId = null
                    )
                )
                FeedbackDeliveryResult.NetworkOrServerFailure(
                    errorMessageHi = "समीक्षा एंडपॉइंट त्रुटि (HTTP ${response.code()})। प्रतिक्रिया स्थानीय रूप से सहेजी गई है।",
                    errorMessageEn = "Review endpoint returned HTTP ${response.code()}. Feedback is safely logged locally for retry.",
                    canRetryWithSameIdempotencyToken = true
                )
            }
        } catch (e: Exception) {
            netraDao.insertFeedbackDraft(
                FeedbackDraftEntity.fromDomain(
                    payload = payload,
                    deliveryStatus = "LOGGED_LOCALLY_OFFLINE_QUEUE",
                    receiptId = null
                )
            )
            FeedbackDeliveryResult.NetworkOrServerFailure(
                errorMessageHi = "नेटवर्क अनुपलब्ध: प्रतिक्रिया स्थानीय रूप से लॉग की गई है (${payload.feedbackId})।",
                errorMessageEn = "Network unreachable: Feedback logged locally (${payload.feedbackId}) and ready for retry.",
                canRetryWithSameIdempotencyToken = true
            )
        }
    }

    suspend fun removeContactFromFeedbackLog(feedbackId: String) = withContext(Dispatchers.IO) {
        netraDao.removeContactFromDraft(feedbackId)
    }

    suspend fun deleteFeedbackLogEntry(feedbackId: String) = withContext(Dispatchers.IO) {
        netraDao.deleteFeedbackDraftById(feedbackId)
    }

    suspend fun logPowerHealthEvent(
        powerTier: PowerTier,
        eventType: String,
        message: String
    ) = withContext(Dispatchers.IO) {
        netraDao.insertHealthLog(
            HealthLogEntity(
                timestampEpochMs = System.currentTimeMillis(),
                powerTier = powerTier.name,
                eventType = eventType,
                message = message.take(240)
            )
        )
        netraDao.pruneHealthLogsToMax100()
    }

    companion object {
        fun createDefault(database: AppDatabase): DataRepository {
            val placeholderApi = NetraNetworkClient.createApiService(
                NetraNetworkClient.STAGE1_PLACEHOLDER_BASE_URL
            )
            return DataRepository(
                database = database,
                apiService = placeholderApi,
                isRemoteFeedEndpointConfigured = false,
                isPrivateFeedbackEndpointConnected = false
            )
        }
    }
}
