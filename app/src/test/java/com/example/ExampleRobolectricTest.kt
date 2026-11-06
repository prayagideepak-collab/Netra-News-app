package com.example

import com.example.data.local.CachedArticleEntity
import com.example.domain.CorrectionEntry
import com.example.domain.NewsScope
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
 * Verifies Room entity rights stripping, city preference structure, and Stage 1-6 governance matrix.
 */
class ExampleRobolectricTest {

    @Test
    fun `cached article entity strips summary when rights status is LINK_ONLY`() {
        val linkOnlyRecord = ValidationAndRightsEngine.SPEC_TEST_FIXTURE_001.copy(
            id = "dd-news-101",
            title = "डीडी न्यूज़ राज्य बुलेटिन",
            publisherName = "DD News",
            canonicalSourceUrl = "https://ddnews.gov.in/",
            publishedAtIso = "2026-11-06T05:00:00+05:30",
            verificationStatus = VerificationStatus.VERIFIED,
            rightsStatus = RightsStatus.LINK_ONLY,
            permittedSummary = "Unlicensed summary text that must not be cached",
            corrections = listOf(CorrectionEntry("2026-11-06T05:30:00+05:30", "Updated link"))
        )

        val entity = CachedArticleEntity.fromDomain(linkOnlyRecord)
        assertNull(entity.permittedSummary)

        val roundTrip = entity.toDomain()
        assertNull(roundTrip.permittedSummary)
        assertEquals(1, roundTrip.corrections.size)
        assertEquals("Updated link", roundTrip.corrections.first().reason)
    }

    @Test
    fun `location preference and feature matrix meet stage 1 specification`() {
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
        assertTrue(GovernanceLedgerData.FEATURE_MATRIX.isNotEmpty())
        assertTrue(GovernanceLedgerData.OPEN_DECISIONS.size >= 5)
    }
}
