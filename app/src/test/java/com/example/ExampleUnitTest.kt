package com.example

import com.example.domain.ArticleRecord
import com.example.domain.CorrectionEntry
import com.example.domain.EditorialState
import com.example.domain.FeedEnvelope
import com.example.domain.FeedbackDeliveryResult
import com.example.domain.FeedbackIssueType
import com.example.domain.FeedbackReviewStatus
import com.example.domain.FeedbackSubmissionPayload
import com.example.domain.FeedbackValidationOutcome
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.VerificationStatus
import com.example.location.CityLocationEngine
import com.example.location.CityResolutionResult
import com.example.power.CircuitBreakerState
import com.example.power.PowerBudgetAndTierManager
import com.example.power.PowerTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit Tests for Netra News Contract (Build Specification v3 — Sections 2, 3, 4.2, 6, 7, 8 & Ultra-Low Power 24/7 Rules):
 * - Schema parsing & incompatible schema rejection
 * - TEST fixture (id=test-001) rejection in production feeds
 * - Missing, invalid, and future timestamp rejection
 * - Stale envelope & article expiry logic
 * - Source allowlist & HTTPS URI validation + HTML/script sanitization
 * - Duplicate canonical link consolidation preserving source attribution
 * - Rights-based content presentation (LINK_ONLY vs ORIGINAL/LICENSED vs BLOCKED)
 * - Section 7 Verification Indicators (Verified, Pending, Blocked labels, missing/unknown/malformed fallback to Pending,
 *   stale verification check, blocked unsafe link withholding, screen-reader & monochrome announcements,
 *   no Breaking/Live entry for Pending/Blocked, unauthorized status transition rejection, and separate EditorialState.RETRACTED)
 * - Section 8 Feedback & Private Review (durable receipt confirmation, network failure, duplicate retry with idempotency token,
 *   invalid input/script rejection, spam cooldown & honeypot, anonymous submission, 30-day contact / 90-day unresolved retention,
 *   unauthenticated reviewer access denial, and honest disabled submit when endpoint is unconnected)
 * - City resolution, nearest-city ambiguity, and manual fallback
 * - Power Tier hysteresis, thermal throttling drop, and circuit breaker backoff
 */
class ExampleUnitTest {

    private val referenceNowEpochMs = 1793947200000L // 2026-11-06T06:40:00Z

    private fun buildValidSampleArticle(
        id: String = "pib-001",
        title: String = "सत्यापित पीआईबी विज्ञप्ति",
        publisher: String = "PIB India",
        url: String = "https://pib.gov.in/PressReleasePage.aspx?PRID=101",
        publishedAt: String? = "2026-11-06T05:00:00+05:30",
        verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
        verificationCheckedAtIso: String? = "2026-11-06T05:05:00+05:30",
        verificationReason: String? = "Confirmed on official PIB portal",
        editorialState: EditorialState = EditorialState.ACTIVE,
        rightsStatus: RightsStatus = RightsStatus.ORIGINAL,
        summary: String? = "आधिकारिक संक्षिप्त विवरण",
        expiryIso: String? = null,
        isBreakingOrLive: Boolean = false
    ) = ArticleRecord(
        id = id,
        title = title,
        language = "hi",
        category = "National",
        scope = NewsScope.NATIONAL,
        countryCode = "IN",
        stateCode = "UP",
        cityId = "in-up-lucknow",
        publisherName = publisher,
        canonicalSourceUrl = url,
        publishedAtIso = publishedAt,
        fetchedAtEpochMs = referenceNowEpochMs,
        verifiedAtIso = verificationCheckedAtIso,
        verificationStatus = verificationStatus,
        verificationCheckedAtIso = verificationCheckedAtIso,
        verificationReason = verificationReason,
        editorialState = editorialState,
        rightsStatus = rightsStatus,
        permittedSummary = summary,
        expiryIso = expiryIso,
        isBreakingOrLive = isBreakingOrLive
    )

    @Test
    fun `rejects incompatible schemaVersion visibly`() {
        val badEnvelope = FeedEnvelope(
            schemaVersion = 2,
            generatedAtUtcIso = "2026-11-06T05:00:00Z",
            coverage = listOf("national:IN"),
            items = listOf(buildValidSampleArticle()),
            sourceHealth = emptyList()
        )
        val result = ValidationAndRightsEngine.validateFeedEnvelope(badEnvelope, referenceNowEpochMs)
        assertFalse(result.isValidSchema)
        assertNotNull(result.rejectedSchemaReason)
        assertTrue(result.verifiedDisplayableItems.isEmpty())
    }

    @Test
    fun `production guard rejects SPEC_TEST_FIXTURE_001 and any TEST prefixed item`() {
        val envelope = FeedEnvelope(
            schemaVersion = 1,
            generatedAtUtcIso = "2026-11-06T05:00:00Z",
            coverage = listOf("local:in-up-lucknow"),
            items = listOf(
                ValidationAndRightsEngine.SPEC_TEST_FIXTURE_001,
                buildValidSampleArticle(id = "real-001")
            ),
            sourceHealth = emptyList()
        )
        val result = ValidationAndRightsEngine.validateFeedEnvelope(
            envelope = envelope,
            referenceNowEpochMs = referenceNowEpochMs,
            isProductionFeed = true
        )
        assertTrue(result.isValidSchema)
        assertEquals(1, result.verifiedDisplayableItems.size)
        assertEquals("real-001", result.verifiedDisplayableItems.first().id)
        assertEquals(1, result.rejectedOrFilteredItems.size)
        assertEquals("test-001", result.rejectedOrFilteredItems.first().first.id)
    }

    @Test
    fun `treats missing or future publishedAt timestamps as unverified`() {
        val missingTimeArticle = buildValidSampleArticle(id = "missing-time", publishedAt = null)
        val futureTimeArticle = buildValidSampleArticle(
            id = "future-time",
            publishedAt = "2027-01-01T00:00:00Z"
        )
        val validArticle = buildValidSampleArticle(id = "valid-time")

        val envelope = FeedEnvelope(
            schemaVersion = 1,
            generatedAtUtcIso = "2026-11-06T05:00:00Z",
            coverage = listOf("national:IN"),
            items = listOf(missingTimeArticle, futureTimeArticle, validArticle),
            sourceHealth = emptyList()
        )

        val result = ValidationAndRightsEngine.validateFeedEnvelope(envelope, referenceNowEpochMs)
        assertEquals(1, result.verifiedDisplayableItems.size)
        assertEquals("valid-time", result.verifiedDisplayableItems.first().id)
        assertEquals(2, result.rejectedOrFilteredItems.size)
    }

    @Test
    fun `detects stale envelope and filters expired articles`() {
        val expiredArticle = buildValidSampleArticle(
            id = "expired-01",
            expiryIso = "2026-11-05T00:00:00Z"
        )
        val activeArticle = buildValidSampleArticle(
            id = "active-01",
            expiryIso = "2026-11-07T00:00:00Z"
        )
        val staleEnvelope = FeedEnvelope(
            schemaVersion = 1,
            generatedAtUtcIso = "2026-11-05T12:00:00Z", // >18 hours older than referenceNowEpochMs
            coverage = listOf("national:IN"),
            items = listOf(expiredArticle, activeArticle),
            sourceHealth = emptyList()
        )

        val result = ValidationAndRightsEngine.validateFeedEnvelope(staleEnvelope, referenceNowEpochMs)
        assertTrue(result.isValidSchema)
        assertTrue(result.isStale)
        assertEquals(1, result.verifiedDisplayableItems.size)
        assertEquals("active-01", result.verifiedDisplayableItems.first().id)
    }

    @Test
    fun `enforces HTTPS allowlist and strips active HTML and script tags`() {
        assertFalse(ValidationAndRightsEngine.isApprovedHttpsSourceUrl("http://pib.gov.in/news"))
        assertFalse(ValidationAndRightsEngine.isApprovedHttpsSourceUrl("javascript:alert(1)"))
        assertFalse(ValidationAndRightsEngine.isApprovedHttpsSourceUrl("https://unknown-publisher.example.com/a"))
        assertTrue(ValidationAndRightsEngine.isApprovedHttpsSourceUrl("https://pib.gov.in/allRel.aspx"))
        assertTrue(ValidationAndRightsEngine.isApprovedHttpsSourceUrl("https://mausam.imd.gov.in/imd_latest/"))

        val dirty = "<script>evil()</script><b>मुख्य समाचार</b> javascript:void(0)"
        val clean = ValidationAndRightsEngine.sanitizeString(dirty)
        assertFalse(clean.contains("<script>"))
        assertFalse(clean.contains("<b>"))
        assertFalse(clean.contains("javascript:"))
        assertTrue(clean.contains("मुख्य समाचार"))
    }

    @Test
    fun `consolidates duplicate canonical links without losing source attribution`() {
        val sharedUrl = "https://pib.gov.in/allRel.aspx"
        val item1 = buildValidSampleArticle(id = "dup-1", publisher = "PIB India", url = sharedUrl)
        val item2 = buildValidSampleArticle(
            id = "dup-2",
            publisher = "DD News",
            url = sharedUrl,
            summary = null
        ).copy(
            corrections = listOf(CorrectionEntry("2026-11-06T05:30:00Z", "Typo fixed"))
        )

        val consolidated = ValidationAndRightsEngine.consolidateDuplicateLinks(listOf(item1, item2))
        assertEquals(1, consolidated.size)
        val merged = consolidated.first()
        assertTrue(merged.consolidatedSources.contains("PIB India"))
        assertTrue(merged.consolidatedSources.contains("DD News"))
        assertEquals(1, merged.corrections.size)
    }

    @Test
    fun `enforces rights status presentation and suppresses LINK_ONLY or BLOCKED summaries`() {
        val linkOnlyArticle = buildValidSampleArticle(
            id = "link-only-1",
            rightsStatus = RightsStatus.LINK_ONLY,
            summary = "Scraped text that should be suppressed"
        )
        val blockedVerificationArticle = buildValidSampleArticle(
            id = "blocked-ver-1",
            verificationStatus = VerificationStatus.BLOCKED,
            rightsStatus = RightsStatus.ORIGINAL,
            summary = "Body that must be withheld when verificationStatus is BLOCKED"
        )
        val originalArticle = buildValidSampleArticle(
            id = "orig-1",
            rightsStatus = RightsStatus.ORIGINAL,
            summary = "Permitted official release summary"
        )
        assertNull(ValidationAndRightsEngine.getRightsPermittedSummary(linkOnlyArticle))
        assertNull(ValidationAndRightsEngine.getRightsPermittedSummary(blockedVerificationArticle))
        assertEquals(
            "Permitted official release summary",
            ValidationAndRightsEngine.getRightsPermittedSummary(originalArticle)
        )
    }

    @Test
    fun `section 7 verification indicators cover all 3 labels, missing or malformed fallback to Pending, stale check, blocked link, and no breaking for Pending or Blocked`() {
        // 1. All three allowed values map to visible labels Verified, Pending, Blocked with monochrome symbols
        assertEquals("Verified", VerificationStatus.VERIFIED.labelEn)
        assertEquals("Pending", VerificationStatus.PENDING.labelEn)
        assertEquals("Blocked", VerificationStatus.BLOCKED.labelEn)
        assertEquals("[✓ VERIFIED]", VerificationStatus.VERIFIED.monochromeSymbol)
        assertEquals("[! PENDING]", VerificationStatus.PENDING.monochromeSymbol)
        assertEquals("[✕ BLOCKED]", VerificationStatus.BLOCKED.monochromeSymbol)

        // 2. Missing, unknown, or malformed status falls back to Pending, never Verified (Sec 7.5 & 7.7)
        assertEquals(VerificationStatus.PENDING, VerificationStatus.fromWire(null))
        assertEquals(VerificationStatus.PENDING, VerificationStatus.fromWire(""))
        assertEquals(VerificationStatus.PENDING, VerificationStatus.fromWire("malformed_enum_999"))
        assertEquals(VerificationStatus.VERIFIED, VerificationStatus.fromWire("verified"))
        assertEquals(VerificationStatus.BLOCKED, VerificationStatus.fromWire("blocked"))

        // 3. Stale verification check detection (>6h old or missing)
        val freshArticle = buildValidSampleArticle(
            id = "fresh-ver",
            verificationCheckedAtIso = "2026-11-06T05:00:00Z",
            isBreakingOrLive = true
        )
        val staleVerArticle = buildValidSampleArticle(
            id = "stale-ver",
            verificationCheckedAtIso = "2026-11-04T01:00:00Z"
        )
        val missingCheckedAtArticle = buildValidSampleArticle(
            id = "missing-checked-at",
            verificationCheckedAtIso = null
        )
        assertFalse(ValidationAndRightsEngine.isVerificationCheckStale(freshArticle.verificationCheckedAtIso, referenceNowEpochMs))
        assertTrue(ValidationAndRightsEngine.isVerificationCheckStale(staleVerArticle.verificationCheckedAtIso, referenceNowEpochMs))
        assertTrue(ValidationAndRightsEngine.isVerificationCheckStale(missingCheckedAtArticle.verificationCheckedAtIso, referenceNowEpochMs))

        // 4. Blocked unsafe link withholding & clickable link guard (Sec 7.4)
        val blockedArticle = buildValidSampleArticle(
            id = "blocked-01",
            verificationStatus = VerificationStatus.BLOCKED,
            verificationReason = "Unconfirmed viral social forward",
            isBreakingOrLive = true
        )
        val rightsBlockedArticle = buildValidSampleArticle(
            id = "rights-blocked-01",
            rightsStatus = RightsStatus.BLOCKED
        )
        assertTrue(ValidationAndRightsEngine.isClickableSourceLinkAllowed(freshArticle))
        assertFalse(ValidationAndRightsEngine.isClickableSourceLinkAllowed(blockedArticle))
        assertFalse(ValidationAndRightsEngine.isClickableSourceLinkAllowed(rightsBlockedArticle))

        // 5. Screen-reader announcement and attribution + verification line (Sec 7.2 & 7.3)
        val srEn = ValidationAndRightsEngine.formatScreenReaderVerificationAnnouncement(freshArticle, isHindi = false)
        assertTrue(srEn.contains("Source: PIB India"))
        assertTrue(srEn.contains("Source verification: Verified"))
        val cardLine = ValidationAndRightsEngine.formatAttributionAndVerificationLine(
            article = freshArticle,
            isHindi = false
        )
        assertTrue(cardLine.contains("Source: PIB India • Source verification: Verified"))

        // 6. Pending or Blocked must NEVER enter Breaking or Live (Sec 7.4 & 7.6)
        val pendingArticle = buildValidSampleArticle(
            id = "pending-01",
            verificationStatus = VerificationStatus.PENDING,
            isBreakingOrLive = true
        )
        val normalizedPending = ValidationAndRightsEngine.normalizeArticleForVerificationAndRights(pendingArticle)
        val normalizedBlocked = ValidationAndRightsEngine.normalizeArticleForVerificationAndRights(blockedArticle)
        val normalizedFresh = ValidationAndRightsEngine.normalizeArticleForVerificationAndRights(freshArticle)
        assertFalse(normalizedPending.isBreakingOrLive)
        assertFalse(normalizedBlocked.isBreakingOrLive)
        assertNull(normalizedBlocked.permittedSummary)
        assertTrue(normalizedFresh.isBreakingOrLive)

        // 7. Unauthorized editorial status transition rejection (Sec 7.5 & 7.6)
        val unauthorizedAttempt = ValidationAndRightsEngine.applyEditorialStatusTransition(
            article = pendingArticle,
            newStatus = VerificationStatus.VERIFIED,
            actorRole = "anonymous_client",
            reviewedAtIso = "2026-11-06T06:00:00Z",
            reason = "Client self-promotion attempt"
        )
        assertTrue(unauthorizedAttempt.isFailure)

        val authorizedTransition = ValidationAndRightsEngine.applyEditorialStatusTransition(
            article = pendingArticle,
            newStatus = VerificationStatus.VERIFIED,
            actorRole = "authorised_editorial_reviewer",
            reviewedAtIso = "2026-11-06T06:00:00Z",
            reason = "Official bulletin confirmed by editorial desk"
        )
        assertTrue(authorizedTransition.isSuccess)
        val updatedArticle = authorizedTransition.getOrThrow()
        assertEquals(VerificationStatus.VERIFIED, updatedArticle.verificationStatus)
        assertEquals(1, updatedArticle.statusTransitions.size)
        assertEquals(VerificationStatus.PENDING, updatedArticle.statusTransitions.first().fromStatus)
        assertEquals(VerificationStatus.VERIFIED, updatedArticle.statusTransitions.first().toStatus)
    }

    @Test
    fun `section 8 feedback validation, spam controls, idempotency, retention, reviewer guard, and honest disabled submit`() {
        val validAnonPayload = FeedbackSubmissionPayload(
            feedbackId = "fb-001",
            idempotencyToken = "idem-token-001",
            articleId = "up-lko-001",
            articleTitleSnapshot = "लखनऊ मेट्रो विस्तार",
            issueType = FeedbackIssueType.INACCURACY,
            description = "यह शीर्षक आधिकारिक विज्ञप्ति के समय से मेल नहीं खाता है, कृपया जांचें।",
            optionalComments = "अतिरिक्त संदर्भ: पृष्ठ संख्या 2",
            optionalContact = null,
            honeypotField = "",
            createdAtClientEpochMs = referenceNowEpochMs
        )

        // 1. Valid anonymous submission passes validation
        val validOutcome = ValidationAndRightsEngine.validateFeedbackSubmission(
            rawPayload = validAnonPayload,
            recentAttemptTimestampsMs = emptyList(),
            nowEpochMs = referenceNowEpochMs
        )
        assertTrue(validOutcome is FeedbackValidationOutcome.ValidReadyForSubmissionOrDraft)

        // 2. Rejects script/HTML markup in description, optionalComments, or optionalContact (Sec 8.4)
        val scriptPayload = validAnonPayload.copy(
            description = "Valid length description but contains <script>alert(1)</script> injection"
        )
        val scriptOutcome = ValidationAndRightsEngine.validateFeedbackSubmission(
            rawPayload = scriptPayload,
            recentAttemptTimestampsMs = emptyList(),
            nowEpochMs = referenceNowEpochMs
        )
        assertTrue(scriptOutcome is FeedbackValidationOutcome.InvalidInput)

        // 3. Rejects too-short (<20 chars) description
        val shortPayload = validAnonPayload.copy(description = "Too short")
        assertTrue(
            ValidationAndRightsEngine.validateFeedbackSubmission(
                rawPayload = shortPayload,
                recentAttemptTimestampsMs = emptyList(),
                nowEpochMs = referenceNowEpochMs
            ) is FeedbackValidationOutcome.InvalidInput
        )

        // 4. Rejects honeypot bot field and enforces cooldown / rate-limit window (Sec 8.5 & 8.10)
        val honeypotPayload = validAnonPayload.copy(honeypotField = "https://spam.bot")
        assertTrue(
            ValidationAndRightsEngine.validateFeedbackSubmission(
                rawPayload = honeypotPayload,
                recentAttemptTimestampsMs = emptyList(),
                nowEpochMs = referenceNowEpochMs
            ) is FeedbackValidationOutcome.SpamOrRateLimited
        )
        val cooldownOutcome = ValidationAndRightsEngine.validateFeedbackSubmission(
            rawPayload = validAnonPayload,
            recentAttemptTimestampsMs = listOf(referenceNowEpochMs - 5_000L), // only 5s ago (<15s cooldown)
            nowEpochMs = referenceNowEpochMs
        )
        assertTrue(cooldownOutcome is FeedbackValidationOutcome.SpamOrRateLimited)

        // 5. Honest disabled submit when private endpoint is not connected (Sec 8.9)
        val honestDisabledResult = ValidationAndRightsEngine.processFeedbackDelivery(
            validatedPayload = validAnonPayload,
            isPrivateEndpointConnected = false,
            simulateNetworkFailure = false,
            existingIdempotencyReceipts = emptyMap()
        )
        assertTrue(honestDisabledResult is FeedbackDeliveryResult.DisabledBackendSavedLocalDraftOnly)

        // 6. Network failure does not falsely confirm receipt (Sec 8.4 & 8.10)
        val netFailureResult = ValidationAndRightsEngine.processFeedbackDelivery(
            validatedPayload = validAnonPayload,
            isPrivateEndpointConnected = true,
            simulateNetworkFailure = true,
            existingIdempotencyReceipts = emptyMap()
        )
        assertTrue(netFailureResult is FeedbackDeliveryResult.NetworkOrServerFailure)

        // 7. Durable server receipt confirmation & duplicate retry deduplication via idempotency token (Sec 8.4 & 8.10)
        val firstReceipt = ValidationAndRightsEngine.processFeedbackDelivery(
            validatedPayload = validAnonPayload,
            isPrivateEndpointConnected = true,
            simulateNetworkFailure = false,
            existingIdempotencyReceipts = emptyMap()
        )
        assertTrue(firstReceipt is FeedbackDeliveryResult.NetworkOrServerFailure)
        val duplicateRetryReceipt = ValidationAndRightsEngine.processFeedbackDelivery(
            validatedPayload = validAnonPayload,
            isPrivateEndpointConnected = true,
            existingIdempotencyReceipts = mapOf("idem-token-001" to "rcpt-fb-001")
        )
        assertTrue(duplicateRetryReceipt is FeedbackDeliveryResult.NetworkOrServerFailure)

        // 8. Unauthenticated reviewer access denial & 30-day contact / 90-day unresolved retention (Sec 8.7, 8.8, 8.10)
        val payloadWithContact = validAnonPayload.copy(optionalContact = "reader@example.com")
        val unauthGuard = ValidationAndRightsEngine.applyRetentionAndReviewerGuard(
            record = payloadWithContact,
            isReviewerAuthenticated = false,
            daysSinceCreated = 1,
            daysSinceResolved = null
        )
        assertTrue(unauthGuard.isFailure)

        // 31 days after resolution -> optionalContact is purged while record remains
        val resolvedPayload = payloadWithContact.copy(reviewStatus = FeedbackReviewStatus.RESOLVED)
        val after31DaysResolved = ValidationAndRightsEngine.applyRetentionAndReviewerGuard(
            record = resolvedPayload,
            isReviewerAuthenticated = true,
            daysSinceCreated = 35,
            daysSinceResolved = 31
        ).getOrThrow()
        assertNotNull(after31DaysResolved)
        assertNull(after31DaysResolved!!.optionalContact)

        // 91 days unresolved -> entire unresolved feedback record is purged
        val after91DaysUnresolved = ValidationAndRightsEngine.applyRetentionAndReviewerGuard(
            record = payloadWithContact,
            isReviewerAuthenticated = true,
            daysSinceCreated = 91,
            daysSinceResolved = null
        ).getOrThrow()
        assertNull(after91DaysUnresolved)
    }

    @Test
    fun `city resolution detects nearest city, ambiguity, and out-of-bounds fallback`() {
        // Exact Lucknow centroid
        val lucknowMatch = CityLocationEngine.resolveCoordinatesToCity(26.8467, 80.9462)
        assertTrue(lucknowMatch is CityResolutionResult.ResolvedSingleCity)

        // Midpoint between Lucknow (26.8467, 80.9462) and Kanpur (26.4499, 80.3319) -> within 35km delta
        val midpointMatch = CityLocationEngine.resolveCoordinatesToCity(26.6483, 80.6390)
        assertTrue(midpointMatch is CityResolutionResult.AmbiguousNearestCities)

        // Far outside India (0.0, 0.0) -> safe manual fallback
        val oceanFallback = CityLocationEngine.resolveCoordinatesToCity(0.0, 0.0)
        assertTrue(oceanFallback is CityResolutionResult.FallbackToManual)
    }

    @Test
    fun `adaptive power tiers respect 100, 15, 5 percent, hysteresis, thermal drop, and circuit breaker`() {
        // 100% battery -> NORMAL
        assertEquals(
            PowerTier.NORMAL,
            PowerBudgetAndTierManager.computeAdaptiveTier(100, isCharging = false, isBatterySaverOn = false, isThermalThrottled = false)
        )
        // 100% battery but thermal throttled -> drops 1 tier to SAVER
        assertEquals(
            PowerTier.SAVER,
            PowerBudgetAndTierManager.computeAdaptiveTier(100, isCharging = false, isBatterySaverOn = false, isThermalThrottled = true)
        )
        // 15% battery -> SAVER; 14% -> CRITICAL
        assertEquals(
            PowerTier.SAVER,
            PowerBudgetAndTierManager.computeAdaptiveTier(15, isCharging = false, isBatterySaverOn = false, isThermalThrottled = false)
        )
        assertEquals(
            PowerTier.CRITICAL,
            PowerBudgetAndTierManager.computeAdaptiveTier(14, isCharging = false, isBatterySaverOn = false, isThermalThrottled = false)
        )
        // Hysteresis check: at 16% coming from CRITICAL, stays CRITICAL until 17%
        assertEquals(
            PowerTier.CRITICAL,
            PowerBudgetAndTierManager.computeAdaptiveTier(
                16,
                isCharging = false,
                isBatterySaverOn = false,
                isThermalThrottled = false,
                previousTier = PowerTier.CRITICAL
            )
        )
        // 4% battery -> SURVIVAL
        assertEquals(
            PowerTier.SURVIVAL,
            PowerBudgetAndTierManager.computeAdaptiveTier(4, isCharging = false, isBatterySaverOn = false, isThermalThrottled = false)
        )

        // Circuit breaker trips after 3 consecutive failures
        var cb = CircuitBreakerState()
        cb = PowerBudgetAndTierManager.recordFailureAndComputeBackoff(cb, "Net timeout 1", 1000L, 500L)
        assertFalse(cb.isOpen)
        cb = PowerBudgetAndTierManager.recordFailureAndComputeBackoff(cb, "Net timeout 2", 2000L, 500L)
        assertFalse(cb.isOpen)
        cb = PowerBudgetAndTierManager.recordFailureAndComputeBackoff(cb, "Net timeout 3", 3000L, 500L)
        assertTrue(cb.isOpen)
        assertEquals(3, cb.consecutiveFailures)
    }
}
