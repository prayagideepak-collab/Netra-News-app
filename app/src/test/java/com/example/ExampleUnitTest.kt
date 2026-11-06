package com.example

import com.example.domain.ArticleRecord
import com.example.domain.CorrectionEntry
import com.example.domain.FeedEnvelope
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
 * Unit Tests for Netra News Contract (Sections 2, 3, 4.2, 6 & Ultra-Low Power 24/7 Rules):
 * - Schema parsing & incompatible schema rejection
 * - TEST fixture (id=test-001) rejection in production feeds
 * - Missing, invalid, and future timestamp rejection
 * - Stale envelope & article expiry logic
 * - Source allowlist & HTTPS URI validation + HTML/script sanitization
 * - Duplicate canonical link consolidation preserving source attribution
 * - Rights-based content presentation (LINK_ONLY vs ORIGINAL/LICENSED vs BLOCKED)
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
        rightsStatus: RightsStatus = RightsStatus.ORIGINAL,
        summary: String? = "आधिकारिक संक्षिप्त विवरण",
        expiryIso: String? = null
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
        verifiedAtIso = "2026-11-06T05:05:00+05:30",
        verificationStatus = verificationStatus,
        rightsStatus = rightsStatus,
        permittedSummary = summary,
        expiryIso = expiryIso
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
    fun `enforces rights status presentation and suppresses LINK_ONLY summaries`() {
        val linkOnlyArticle = buildValidSampleArticle(
            id = "link-only-1",
            rightsStatus = RightsStatus.LINK_ONLY,
            summary = "Scraped text that should be suppressed"
        )
        val originalArticle = buildValidSampleArticle(
            id = "orig-1",
            rightsStatus = RightsStatus.ORIGINAL,
            summary = "Permitted official release summary"
        )
        assertNull(ValidationAndRightsEngine.getRightsPermittedSummary(linkOnlyArticle))
        assertEquals(
            "Permitted official release summary",
            ValidationAndRightsEngine.getRightsPermittedSummary(originalArticle)
        )
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
