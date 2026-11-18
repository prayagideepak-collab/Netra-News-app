package com.example

import com.example.data.DataRepository
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.CachedArticleEntity
import com.example.data.local.FeedbackDraftEntity
import com.example.data.local.HealthLogEntity
import com.example.data.local.NetraDao
import com.example.data.remote.ArticleDto
import com.example.data.remote.FeedEnvelopeDto
import com.example.data.remote.FeedbackReceiptDto
import com.example.data.remote.FeedbackRequestDto
import com.example.data.remote.NetraNewsApiService
import com.example.domain.Article
import com.example.domain.CorrectionEntry
import com.example.domain.EditorialState
import com.example.domain.FeedbackDeliveryResult
import com.example.domain.FeedbackIssueType
import com.example.domain.FeedbackSubmissionPayload
import com.example.domain.NewsScope
import com.example.domain.OfflineSyncStatus
import com.example.domain.RightsStatus
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.VerificationStatus
import com.example.location.CityLocationEngine
import com.example.location.CitySelectionMode
import com.example.location.LocationPreference
import com.example.settings.GovernanceLedgerData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

/**
 * Contract & Persistence Mapping Tests (CUJ Verification on Local JVM):
 * Verifies domain Article model, ArticleEntity & CachedArticleEntity rights stripping,
 * verificationStatus/editorialState round-trip, local feedback draft entity mapping,
 * city preference structure, and v3 governance matrix.
 */
class ExampleRobolectricTest {

    @Test
    fun `domain Article and Room ArticleEntity map all required bookmark fields and enforce rights status`() {
        val domainArticle = Article(
            id = "up-lko-001",
            title = "लखनऊ मेट्रो कॉरिडोर विस्तार की आधिकारिक समीक्षा बैठक संपन्न",
            language = "hi",
            category = "Civic Infrastructure",
            scope = NewsScope.LOCAL,
            cityId = "in-up-lucknow",
            publisherName = "सूचना एवं जनसंपर्क विभाग, उत्तर प्रदेश",
            sourceUrl = "https://up.gov.in/en/page/press-release",
            publishedAt = "2026-11-06T08:15:00+05:30",
            fetchedAt = 1793947200000L,
            verificationStatus = VerificationStatus.VERIFIED,
            rightsStatus = RightsStatus.ORIGINAL,
            stateCode = "UP",
            permittedSummary = "लखनऊ में सार्वजनिक परिवहन विस्तार हेतु समीक्षा बैठक का आधिकारिक सारांश।"
        )

        val entity = ArticleEntity.fromDomain(
            article = domainArticle,
            savedAtEpochMs = 1793947250000L,
            isBookmarked = true
        )
        assertEquals("up-lko-001", entity.id)
        assertEquals("local", entity.scope)
        assertEquals("in-up-lucknow", entity.cityId)
        assertEquals("https://up.gov.in/en/page/press-release", entity.sourceUrl)
        assertEquals("verified", entity.verificationStatus)
        assertEquals("original", entity.rightsStatus)
        assertTrue(entity.isBookmarked)

        val roundTrip = entity.toDomain()
        assertEquals(domainArticle.id, roundTrip.id)
        assertEquals(domainArticle.title, roundTrip.title)
        assertEquals(domainArticle.scope, roundTrip.scope)
        assertEquals(domainArticle.cityId, roundTrip.cityId)
        assertEquals(domainArticle.publisherName, roundTrip.publisherName)
        assertEquals(domainArticle.sourceUrl, roundTrip.sourceUrl)
        assertEquals(domainArticle.publishedAt, roundTrip.publishedAt)
        assertEquals(domainArticle.fetchedAt, roundTrip.fetchedAt)
        assertEquals(VerificationStatus.VERIFIED, roundTrip.verificationStatus)
        assertEquals(RightsStatus.ORIGINAL, roundTrip.rightsStatus)
        assertEquals(domainArticle.permittedSummary, roundTrip.safePermittedSummary)

        // Verify LINK_ONLY and BLOCKED articles strip unlicensed summaries in ArticleEntity
        val linkOnlyArticle = domainArticle.copy(
            id = "dd-lko-002",
            rightsStatus = RightsStatus.LINK_ONLY,
            permittedSummary = "Unlicensed summary that must be stripped"
        )
        val linkOnlyEntity = ArticleEntity.fromDomain(linkOnlyArticle)
        assertNull(linkOnlyEntity.permittedSummary)
        assertNull(linkOnlyEntity.toDomain().safePermittedSummary)

        val blockedArticle = domainArticle.copy(
            id = "blocked-lko-003",
            verificationStatus = VerificationStatus.BLOCKED,
            permittedSummary = "Blocked summary that must be withheld"
        )
        val blockedEntity = ArticleEntity.fromDomain(blockedArticle)
        assertNull(blockedEntity.permittedSummary)
        assertEquals(VerificationStatus.BLOCKED, blockedEntity.toDomain().verificationStatus)
    }

    @Test
    fun `cached article entity strips summary when rights status is LINK_ONLY or verificationStatus is BLOCKED`() {
        val linkOnlyRecord = ValidationAndRightsEngine.SPEC_TEST_FIXTURE_001.copy(
            id = "dd-news-101",
            title = "डीडी न्यूज़ राज्य बुलेटिन",
            publisherName = "DD News",
            canonicalSourceUrl = "https://ddnews.gov.in/",
            publishedAtIso = "2026-11-06T05:00:00+05:30",
            verificationStatus = VerificationStatus.VERIFIED,
            verificationCheckedAtIso = "2026-11-06T05:10:00+05:30",
            verificationReason = "Confirmed on DD News portal",
            editorialState = EditorialState.CORRECTED,
            rightsStatus = RightsStatus.LINK_ONLY,
            permittedSummary = "Unlicensed summary text that must not be cached",
            corrections = listOf(CorrectionEntry("2026-11-06T05:30:00+05:30", "Updated link"))
        )

        val entity = CachedArticleEntity.fromDomain(linkOnlyRecord)
        assertNull(entity.permittedSummary)

        val roundTrip = entity.toDomain()
        assertNull(roundTrip.permittedSummary)
        assertEquals(VerificationStatus.VERIFIED, roundTrip.verificationStatus)
        assertEquals("2026-11-06T05:10:00+05:30", roundTrip.verificationCheckedAtIso)
        assertEquals("Confirmed on DD News portal", roundTrip.verificationReason)
        assertEquals(EditorialState.CORRECTED, roundTrip.editorialState)
        assertEquals(1, roundTrip.corrections.size)
        assertEquals("Updated link", roundTrip.corrections.first().reason)

        // Blocked verification record also withholds cached summary
        val blockedRecord = linkOnlyRecord.copy(
            id = "blocked-202",
            verificationStatus = VerificationStatus.BLOCKED,
            rightsStatus = RightsStatus.ORIGINAL,
            permittedSummary = "Unsafe unverified body that must be withheld"
        )
        val blockedEntity = CachedArticleEntity.fromDomain(blockedRecord)
        assertNull(blockedEntity.permittedSummary)
        assertEquals(VerificationStatus.BLOCKED, blockedEntity.toDomain().verificationStatus)
    }

    @Test
    fun `feedback draft entity and location preference and v3 governance matrix meet specification`() {
        val city = CityLocationEngine.findByCityId("in-up-varanasi")
        assertEquals("UP", city.stateCode)
        assertEquals("IN", city.countryCode)

        val pref = LocationPreference(
            countryCode = city.countryCode,
            stateCode = city.stateCode,
            cityId = city.cityId,
            cityLabelHi = city.fullLabelHi,
            cityLabelEn = city.fullLabelEn,
            selectionMode = CitySelectionMode.MANUAL,
            updatedAtEpochMs = 1793947200000L,
            enabled = true
        )
        assertEquals("in-up-varanasi", pref.cityId)

        val draft = FeedbackDraftEntity(
            feedbackId = "draft-test-01",
            idempotencyToken = "idem-01",
            articleId = "up-lko-001",
            articleTitleSnapshot = "लखनऊ मेट्रो विस्तार",
            issueTypeWire = "outdated",
            description = "यह विवरण पुरानी तिथि दिखाता है, कृपया नवीनतम पीआईबी विज्ञप्ति देखें।",
            optionalComments = "अतिरिक्त टिप्पणी: कोई व्यक्तिगत डेटा नहीं",
            optionalContact = null,
            createdAtEpochMs = 1793947200000L
        )
        assertEquals("outdated", draft.issueTypeWire)
        assertNull(draft.optionalContact)
        assertEquals("outdated", draft.toDomain().issueType.wireKey)

        assertTrue(GovernanceLedgerData.WEB_REFERENCE_REPO.contains("Netra-News-Web"))
        assertTrue(GovernanceLedgerData.FEATURE_MATRIX.size >= 10)
        assertTrue(GovernanceLedgerData.OPEN_DECISIONS.size >= 6)
    }

    @Test
    fun `DataRepository manages offline bookmarks, graceful updates, retractions, and feed removals`() = runBlocking {
        val articleDao = FakeArticleDao()
        val netraDao = FakeNetraDao()
        val repo = DataRepository(
            articleDao = articleDao,
            netraDao = netraDao,
            apiService = null,
            isRemoteFeedEndpointConfigured = false,
            isPrivateFeedbackEndpointConnected = false
        )

        val originalRecord = ValidationAndRightsEngine.SPEC_TEST_FIXTURE_001.copy(
            id = "up-lko-101",
            title = "लखनऊ नगर निगम बजट मसौदा जारी",
            publisherName = "PIB India",
            canonicalSourceUrl = "https://pib.gov.in/PressReleasePage.aspx?PRID=101",
            publishedAtIso = "2026-11-06T05:00:00+05:30",
            verificationStatus = VerificationStatus.VERIFIED,
            rightsStatus = RightsStatus.ORIGINAL,
            editorialState = EditorialState.ACTIVE,
            permittedSummary = "प्रारंभिक सारांश पाठ।"
        )
        val retractedTargetRecord = originalRecord.copy(
            id = "up-lko-102",
            title = "अपुष्ट दावे वाली विज्ञप्ति",
            canonicalSourceUrl = "https://pib.gov.in/PressReleasePage.aspx?PRID=102",
            permittedSummary = "यह पाठ बाद में हटाया जाएगा।"
        )
        val removedFromFeedRecord = originalRecord.copy(
            id = "up-lko-103",
            title = "पुराना शहर बुलेटिन",
            canonicalSourceUrl = "https://pib.gov.in/PressReleasePage.aspx?PRID=103"
        )

        repo.saveOfflineBookmark(originalRecord, savedAtEpochMs = 1793947000000L)
        repo.saveOfflineBookmark(retractedTargetRecord, savedAtEpochMs = 1793947000000L)
        repo.saveOfflineBookmark(removedFromFeedRecord, savedAtEpochMs = 1793947000000L)
        assertEquals(3, repo.getAllOfflineBookmarksOnce().size)

        // Upstream feed arrives: 101 is updated, 102 is retracted, 103 is absent from live feed
        val updated101 = originalRecord.copy(
            title = "लखनऊ नगर निगम बजट मसौदा (संशोधित)",
            permittedSummary = "अद्यतन सारांश पाठ।",
            editorialState = EditorialState.CORRECTED
        )
        val retracted102 = retractedTargetRecord.copy(
            editorialState = EditorialState.RETRACTED,
            verificationStatus = VerificationStatus.BLOCKED,
            verificationReason = "Withdrawn by source editorial desk"
        )

        val summary = repo.reconcileSavedBookmarksWithFeed(
            latestActiveArticles = listOf(updated101),
            retractedOrBlockedArticles = listOf(retracted102),
            syncedAtEpochMs = 1793948000000L,
            markMissingAsRemovedFromFeed = true
        )

        assertEquals(1, summary.updatedCount)
        assertEquals(1, summary.retractedOrBlockedCount)
        assertEquals(1, summary.removedFromLiveFeedCount)

        val saved101 = repo.getOfflineBookmarkById("up-lko-101")!!
        assertEquals(OfflineSyncStatus.UPDATED_FROM_FEED, saved101.offlineSyncEnum)
        assertEquals("अद्यतन सारांश पाठ।", saved101.permittedSummary)

        val saved102 = repo.getOfflineBookmarkById("up-lko-102")!!
        assertEquals(OfflineSyncStatus.RETRACTED_OR_BLOCKED, saved102.offlineSyncEnum)
        assertNull(saved102.permittedSummary)
        assertEquals("blocked", saved102.verificationStatus)

        val saved103 = repo.getOfflineBookmarkById("up-lko-103")!!
        assertEquals(OfflineSyncStatus.REMOVED_FROM_LIVE_FEED, saved103.offlineSyncEnum)
        assertEquals("पुराना शहर बुलेटिन", saved103.title)

        // Toggle removes an existing bookmark cleanly
        val toggledState = repo.toggleOfflineBookmark(originalRecord, setOf("up-lko-101"))
        assertFalse(toggledState)
        assertNull(repo.getOfflineBookmarkById("up-lko-101"))
    }

    @Test
    fun `DataRepository mediates Retrofit feed fallback and feedback logging vs connected endpoint`() = runBlocking {
        val articleDao = FakeArticleDao()
        val netraDao = FakeNetraDao()
        val fakeApi = object : NetraNewsApiService {
            override suspend fun fetchFeedEnvelope(
                scope: String,
                cityId: String?,
                ifNoneMatch: String?
            ): Response<FeedEnvelopeDto> {
                return Response.success(
                    FeedEnvelopeDto(
                        schemaVersion = 1,
                        generatedAt = "2026-11-06T06:30:00Z",
                        coverage = listOf("$scope:${cityId ?: "IN"}"),
                        items = listOf(
                            ArticleDto(
                                id = "nat-01",
                                title = "पीआईबी आधिकारिक सूचना",
                                language = "hi",
                                category = "National",
                                scope = "national",
                                publisherName = "PIB India",
                                sourceUrl = "https://pib.gov.in/PressReleasePage.aspx?PRID=500",
                                publishedAt = "2026-11-06T06:00:00Z",
                                verificationStatus = "verified",
                                rightsStatus = "original",
                                editorialState = "active",
                                permittedSummary = "नेटवर्क से प्राप्त आधिकारिक सारांश।"
                            )
                        )
                    )
                )
            }

            override suspend fun submitFeedbackReport(
                idempotencyToken: String,
                request: FeedbackRequestDto
            ): Response<FeedbackReceiptDto> {
                return Response.success(
                    FeedbackReceiptDto(
                        receiptId = "rcpt-${request.feedbackId}",
                        serverTimestamp = "2026-11-06T06:45:00Z",
                        deduplicated = false
                    )
                )
            }
        }

        // 1. Offline mode falls back to Room cache rather than calling network
        val offlineRepo = DataRepository(
            articleDao = articleDao,
            netraDao = netraDao,
            apiService = fakeApi,
            isRemoteFeedEndpointConfigured = true,
            isPrivateFeedbackEndpointConnected = false
        )
        val onlineEnvelope = offlineRepo.fetchFeedWithRoomFallback(
            scope = NewsScope.NATIONAL,
            cityId = "in-up-lucknow",
            isOffline = false,
            nowEpochMs = 1793947200000L
        ).getOrThrow()
        assertEquals(1, onlineEnvelope.items.size)
        assertEquals(1, netraDao.getCachedArticlesOnce().size)

        val offlineFallback = offlineRepo.fetchFeedWithRoomFallback(
            scope = NewsScope.NATIONAL,
            cityId = "in-up-lucknow",
            isOffline = true,
            nowEpochMs = 1793947200000L
        ).getOrThrow()
        assertEquals(1, offlineFallback.items.size)
        assertEquals("nat-01", offlineFallback.items.first().id)

        // 2. Stage-1 disconnected feedback endpoint logs locally without simulating server receipt
        val biasPayload = FeedbackSubmissionPayload(
            feedbackId = "fb-101",
            idempotencyToken = "idem-101",
            articleId = "nat-01",
            articleTitleSnapshot = "पीआईबी आधिकारिक सूचना",
            issueType = FeedbackIssueType.BIASED_REPORTING,
            description = "यह समाचार केवल एक पक्ष दिखाता है, कृपया संतुलित विवरण जोड़ें।",
            optionalComments = null,
            optionalContact = null,
            createdAtClientEpochMs = 1793947200000L
        )
        val localResult = offlineRepo.submitOrLogFeedback(biasPayload)
        assertTrue(localResult is FeedbackDeliveryResult.DisabledBackendSavedLocalDraftOnly)
        assertEquals("LOGGED_LOCALLY_ENDPOINT_DISABLED", netraDao.drafts.last().deliveryStatus)

        // 3. Connected feedback endpoint dispatches via Retrofit and stores receiptId in Room
        val connectedRepo = DataRepository(
            articleDao = articleDao,
            netraDao = netraDao,
            apiService = fakeApi,
            isRemoteFeedEndpointConfigured = true,
            isPrivateFeedbackEndpointConnected = true
        )
        val remoteResult = connectedRepo.submitOrLogFeedback(biasPayload.copy(feedbackId = "fb-102"))
        assertTrue(remoteResult is FeedbackDeliveryResult.DurableServerReceipt)
        assertEquals("rcpt-fb-102", (remoteResult as FeedbackDeliveryResult.DurableServerReceipt).receiptId)
        assertEquals("SENT_TO_REVIEW_ENDPOINT", netraDao.drafts.last().deliveryStatus)
    }
}

private class FakeArticleDao : ArticleDao {
    private val store = linkedMapOf<String, ArticleEntity>()
    private val flow = MutableStateFlow<List<ArticleEntity>>(emptyList())

    private fun emit() {
        flow.value = store.values.sortedByDescending { it.savedAtEpochMs }
    }

    override fun getAllBookmarks(): Flow<List<ArticleEntity>> =
        flow.map { list -> list.filter { it.isBookmarked } }

    override fun getAllArticles(): Flow<List<ArticleEntity>> = flow

    override suspend fun getAllBookmarksOnce(): List<ArticleEntity> =
        store.values.filter { it.isBookmarked }.sortedByDescending { it.savedAtEpochMs }

    override suspend fun getArticleById(id: String): ArticleEntity? = store[id]

    override fun observeArticleById(id: String): Flow<ArticleEntity?> =
        flow.map { list -> list.firstOrNull { it.id == id } }

    override fun isBookmarked(id: String): Flow<Boolean> =
        flow.map { list -> list.any { it.id == id && it.isBookmarked } }

    override suspend fun isBookmarkedOnce(id: String): Boolean =
        store[id]?.isBookmarked == true

    override suspend fun insertArticle(article: ArticleEntity) {
        store[article.id] = article
        emit()
    }

    override suspend fun insertBookmark(article: ArticleEntity) {
        store[article.id] = article
        emit()
    }

    override suspend fun insertBookmarks(articles: List<ArticleEntity>) {
        articles.forEach { store[it.id] = it }
        emit()
    }

    override suspend fun updateArticle(article: ArticleEntity) {
        store[article.id] = article
        emit()
    }

    override suspend fun updateBookmark(article: ArticleEntity) {
        store[article.id] = article
        emit()
    }

    override suspend fun deleteArticle(article: ArticleEntity) {
        store.remove(article.id)
        emit()
    }

    override suspend fun deleteBookmark(article: ArticleEntity) {
        store.remove(article.id)
        emit()
    }

    override suspend fun deleteBookmarkById(id: String) {
        store.remove(id)
        emit()
    }

    override suspend fun clearAllBookmarks() {
        store.entries.removeIf { it.value.isBookmarked }
        emit()
    }

    override suspend fun updateOfflineSyncStatus(articleId: String, syncStatus: String, syncedAtEpochMs: Long) {
        val current = store[articleId] ?: return
        if (current.isBookmarked) {
            store[articleId] = current.copy(offlineSyncStatus = syncStatus, lastSyncedEpochMs = syncedAtEpochMs)
            emit()
        }
    }

    override suspend fun markBookmarkRetractedOrBlocked(
        articleId: String,
        editorialState: String,
        verificationStatus: String,
        reason: String?,
        syncedAtEpochMs: Long
    ) {
        val current = store[articleId] ?: return
        if (current.isBookmarked) {
            store[articleId] = current.copy(
                offlineSyncStatus = OfflineSyncStatus.RETRACTED_OR_BLOCKED.wireKey,
                editorialState = editorialState,
                verificationStatus = verificationStatus,
                verificationReason = reason,
                permittedSummary = null,
                lastSyncedEpochMs = syncedAtEpochMs
            )
            emit()
        }
    }
}

private class FakeNetraDao : NetraDao {
    private val cached = mutableListOf<CachedArticleEntity>()
    private val bookmarks = linkedMapOf<String, BookmarkEntity>()
    val drafts = mutableListOf<FeedbackDraftEntity>()
    private val healthLogs = mutableListOf<HealthLogEntity>()

    private val cachedFlow = MutableStateFlow<List<CachedArticleEntity>>(emptyList())
    private val bookmarksFlow = MutableStateFlow<List<BookmarkEntity>>(emptyList())
    private val draftsFlow = MutableStateFlow<List<FeedbackDraftEntity>>(emptyList())
    private val healthFlow = MutableStateFlow<List<HealthLogEntity>>(emptyList())

    override fun observeCachedArticles(): Flow<List<CachedArticleEntity>> = cachedFlow
    override suspend fun getCachedArticlesOnce(): List<CachedArticleEntity> = cached.toList()
    override suspend fun insertCachedArticlesBatch(items: List<CachedArticleEntity>) {
        cached.addAll(items)
        cachedFlow.value = cached.toList()
    }

    override suspend fun clearCachedArticles() {
        cached.clear()
        cachedFlow.value = emptyList()
    }

    override fun observeBookmarks(): Flow<List<BookmarkEntity>> = bookmarksFlow
    override suspend fun insertBookmark(bookmark: BookmarkEntity) {
        bookmarks[bookmark.articleId] = bookmark
        bookmarksFlow.value = bookmarks.values.toList()
    }

    override suspend fun deleteBookmarkById(articleId: String) {
        bookmarks.remove(articleId)
        bookmarksFlow.value = bookmarks.values.toList()
    }

    override suspend fun clearAllBookmarks() {
        bookmarks.clear()
        bookmarksFlow.value = emptyList()
    }

    override fun observeFeedbackDrafts(): Flow<List<FeedbackDraftEntity>> = draftsFlow
    override suspend fun insertFeedbackDraft(draft: FeedbackDraftEntity) {
        drafts.removeAll { it.feedbackId == draft.feedbackId }
        drafts.add(draft)
        draftsFlow.value = drafts.toList()
    }

    override suspend fun removeContactFromDraft(feedbackId: String) {
        val idx = drafts.indexOfFirst { it.feedbackId == feedbackId }
        if (idx >= 0) {
            drafts[idx] = drafts[idx].copy(optionalContact = null)
            draftsFlow.value = drafts.toList()
        }
    }

    override suspend fun deleteFeedbackDraftById(feedbackId: String) {
        drafts.removeAll { it.feedbackId == feedbackId }
        draftsFlow.value = drafts.toList()
    }

    override suspend fun clearAllFeedbackDrafts() {
        drafts.clear()
        draftsFlow.value = emptyList()
    }

    override fun observeBoundedHealthLogs(): Flow<List<HealthLogEntity>> = healthFlow
    override suspend fun insertHealthLog(log: HealthLogEntity) {
        healthLogs.add(log)
        healthFlow.value = healthLogs.toList()
    }

    override suspend fun pruneHealthLogsToMax100() {
        while (healthLogs.size > 100) healthLogs.removeAt(0)
        healthFlow.value = healthLogs.toList()
    }

    override suspend fun clearHealthLogs() {
        healthLogs.clear()
        healthFlow.value = emptyList()
    }
}
