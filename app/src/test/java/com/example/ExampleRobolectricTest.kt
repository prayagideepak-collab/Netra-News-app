package com.example

import com.example.data.local.CachedArticleEntity
import com.example.data.local.FeedbackDraftEntity
import com.example.domain.CorrectionEntry
import com.example.domain.EditorialState
import com.example.domain.RightsStatus
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.VerificationStatus
import com.example.location.CityLocationEngine
import com.example.location.CitySelectionMode
import com.example.location.LocationPreference
import com.example.settings.GovernanceLedgerData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract & Persistence Mapping Tests (CUJ Verification on Local JVM):
 * Verifies Room entity rights stripping, verificationStatus/editorialState round-trip,
 * local feedback draft entity mapping, city preference structure, and v3 governance matrix.
 */
class ExampleRobolectricTest {

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
}
