package com.example.domain

/**
 * Shared Content, Evidence & Rights Contract for Netra News (Stage 1 Specification, 6 Nov 2026).
 * Unknown fields remain null, never invented.
 */

enum class AppLanguage(val code: String, val labelHi: String, val labelEn: String) {
    HINDI("hi", "हिंदी (Hindi-First)", "Hindi (Primary)"),
    ENGLISH("en", "अंग्रेज़ी (English)", "English")
}

enum class NewsScope(
    val wireKey: String,
    val labelHi: String,
    val labelEn: String,
    val isStage1Enabled: Boolean,
    val stageNoteHi: String,
    val stageNoteEn: String
) {
    LOCAL(
        wireKey = "local",
        labelHi = "स्थानीय (Local)",
        labelEn = "Local",
        isStage1Enabled = true,
        stageNoteHi = "चरण-1 सक्रिय: चयनित शहर व जनपद",
        stageNoteEn = "Stage-1 Active: Selected City & District"
    ),
    STATE(
        wireKey = "state",
        labelHi = "राज्य (State)",
        labelEn = "State",
        isStage1Enabled = true,
        stageNoteHi = "चरण-1 सक्रिय: राज्य स्तरीय आधिकारिक स्रोत",
        stageNoteEn = "Stage-1 Active: State-level verified feeds"
    ),
    NATIONAL(
        wireKey = "national",
        labelHi = "राष्ट्रीय (National)",
        labelEn = "National",
        isStage1Enabled = true,
        stageNoteHi = "चरण-1 सत्यापित फ़ॉलबैक (PIB / DD / AIR)",
        stageNoteEn = "Stage-1 Verified Fallback (PIB / DD / AIR)"
    ),
    WORLD(
        wireKey = "world",
        labelHi = "विश्व (World • चरण-6)",
        labelEn = "World (Stage-6)",
        isStage1Enabled = false,
        stageNoteHi = "चरण-6 के लिए प्रस्तावित: अंतरराष्ट्रीय प्रकाशक अधिकार व स्रोत जांच पूर्ण होने तक अक्षम।",
        stageNoteEn = "Planned for Stage-6: Disabled until international publisher rights & feed audits complete."
    );

    companion object {
        fun fromWire(value: String?): NewsScope? =
            entries.firstOrNull { it.wireKey.equals(value?.trim(), ignoreCase = true) }
    }
}

enum class VerificationStatus(val wireKey: String, val labelHi: String, val labelEn: String) {
    VERIFIED("verified", "सत्यापित स्रोत", "Verified Source"),
    PENDING("pending", "असत्यापित / लंबित", "Pending Verification"),
    RETRACTED("retracted", "वापस लिया गया (Retracted)", "Retracted");

    companion object {
        fun fromWire(value: String?): VerificationStatus =
            entries.firstOrNull { it.wireKey.equals(value?.trim(), ignoreCase = true) } ?: PENDING
    }
}

enum class RightsStatus(val wireKey: String, val labelHi: String, val labelEn: String) {
    LINK_ONLY(
        "linkOnly",
        "केवल स्रोत लिंक (Link-Out Only)",
        "Source-Labelled Link-Out Only"
    ),
    LICENSED(
        "licensed",
        "लाइसेंस प्राप्त सारांश",
        "Licensed Summary Permitted"
    ),
    ORIGINAL(
        "original",
        "सार्वजनिक/मूल विज्ञप्ति",
        "Public Domain / Original Bulletin"
    ),
    BLOCKED(
        "blocked",
        "अधिकार अवरुद्ध (Blocked)",
        "Rights Blocked"
    );

    companion object {
        fun fromWire(value: String?): RightsStatus =
            entries.firstOrNull { it.wireKey.equals(value?.trim(), ignoreCase = true) } ?: BLOCKED
    }
}

data class CorrectionEntry(
    val correctedAtIso: String,
    val reason: String
)

/**
 * Shared Article Record per Section 2.1 & 6.2:
 * Unknown fields remain null, never invented.
 */
data class ArticleRecord(
    val id: String,
    val title: String,
    val language: String, // "hi" or "en"
    val category: String,
    val scope: NewsScope,
    val countryCode: String?,
    val stateCode: String?,
    val cityId: String?,
    val publisherName: String,
    val canonicalSourceUrl: String,
    val publishedAtIso: String?, // With timezone, null if missing
    val fetchedAtEpochMs: Long,
    val verifiedAtIso: String?,
    val verificationStatus: VerificationStatus,
    val rightsStatus: RightsStatus,
    val permittedSummary: String?, // Only rendered if rightsStatus is LICENSED or ORIGINAL
    val corrections: List<CorrectionEntry> = emptyList(),
    val expiryIso: String? = null,
    val licensedImageUrl: String? = null,
    val licensedImageCredit: String? = null,
    val isBreakingOrLive: Boolean = false,
    val consolidatedSources: List<String> = listOf(publisherName)
)

data class SourceHealthRecord(
    val sourceId: String,
    val publisherName: String,
    val host: String,
    val status: String, // "healthy", "stale", "unreachable"
    val lastCheckedIso: String?,
    val httpStatusCode: Int?
)

/**
 * Feed Envelope per Section 6.1:
 * schemaVersion=1, generatedAt as UTC ISO timestamp, coverage list,
 * items array, sourceHealth array and optional nextCursor.
 */
data class FeedEnvelope(
    val schemaVersion: Int,
    val generatedAtUtcIso: String,
    val coverage: List<String>,
    val items: List<ArticleRecord>,
    val sourceHealth: List<SourceHealthRecord>,
    val nextCursor: String? = null
)

/**
 * Distinct Error & Load Contract per Section 6.5:
 * loading, loaded, empty, stale, offline and failed are separate states.
 * Preserve last successful content with its real timestamp.
 */
sealed class FeedLoadState {
    data object Loading : FeedLoadState()
    data class Loaded(
        val items: List<ArticleRecord>,
        val generatedAtUtcIso: String,
        val fetchedAtEpochMs: Long,
        val usedNationalFallback: Boolean = false
    ) : FeedLoadState()

    data class Empty(
        val scope: NewsScope,
        val cityLabel: String,
        val checkedAtEpochMs: Long,
        val reasonHi: String,
        val reasonEn: String
    ) : FeedLoadState()

    data class Stale(
        val items: List<ArticleRecord>,
        val lastSuccessfulAtEpochMs: Long,
        val staleReasonHi: String,
        val staleReasonEn: String
    ) : FeedLoadState()

    data class Offline(
        val cachedItems: List<ArticleRecord>,
        val lastCachedAtEpochMs: Long?,
        val offlineMessageHi: String,
        val offlineMessageEn: String
    ) : FeedLoadState()

    data class Failed(
        val errorMessageHi: String,
        val errorMessageEn: String,
        val preservedItems: List<ArticleRecord>,
        val lastSuccessfulAtEpochMs: Long?,
        val isSchemaRejection: Boolean = false
    ) : FeedLoadState()
}

/**
 * Allowlist Source Entry per Section 2.2:
 * Verified identity, feed URL, coverage, update cadence, terms and licence.
 */
data class SourceAllowlistEntry(
    val sourceId: String,
    val publisherNameHi: String,
    val publisherNameEn: String,
    val allowedHost: String,
    val feedUrl: String,
    val coverageScopes: List<NewsScope>,
    val updateCadenceMinutes: Int,
    val termsReviewed: Boolean,
    val defaultRightsStatus: RightsStatus,
    val licenceSummaryHi: String,
    val licenceSummaryEn: String
)

/**
 * Weather & Earthquake civic bulletin records per Section 2.6 & 3.4:
 * Must carry source-issued event time, area, units, severity, update/expiry and link.
 * Unavailable/stale must look different from a real zero or all-clear.
 */
enum class BulletinDataStatus {
    LIVE_VERIFIED,
    ALL_CLEAR_VERIFIED,
    STALE_CACHED,
    UNAVAILABLE_OFFLINE
}

data class WeatherAdvisoryRecord(
    val eventId: String,
    val providerName: String,
    val areaLabelHi: String,
    val areaLabelEn: String,
    val stateCode: String,
    val cityId: String?,
    val status: BulletinDataStatus,
    val temperatureCelsius: Double?, // Null when unavailable (never fake 0°C!)
    val rainfallMm: Double?,
    val humidityPercent: Int?,
    val windKmph: Double?,
    val unitsLabel: String = "°C / mm / km/h (IMD Metric)",
    val severityLabelHi: String,
    val severityLabelEn: String,
    val summaryHi: String,
    val summaryEn: String,
    val issuedAtIso: String?,
    val updatedAtIso: String?,
    val expiryAtIso: String?,
    val sourceUrl: String
)

data class EarthquakeEventRecord(
    val eventId: String,
    val providerName: String,
    val areaLabelHi: String,
    val areaLabelEn: String,
    val status: BulletinDataStatus,
    val magnitudeMw: Double?, // Null when unavailable or no event
    val depthKm: Double?,
    val latitudeApprox: Double?,
    val longitudeApprox: Double?,
    val unitsLabel: String = "Magnitude (Mw) • Depth (km)",
    val severityLabelHi: String,
    val severityLabelEn: String,
    val issuedAtIso: String?,
    val updatedAtIso: String?,
    val expiryAtIso: String?,
    val sourceUrl: String
)

/**
 * Feature Matrix Row per Section 7.3 & Additional User Instructions (Feature Gate):
 * Required / Implemented / Tested / Blocked columns + Power Tier / Wakeup / Network Cost.
 */
enum class FeatureGateStatus(val labelHi: String, val labelEn: String) {
    IMPLEMENTED("कार्यान्वित (Implemented)", "Implemented"),
    TESTED("परीक्षित (Tested)", "Tested"),
    PLANNED_STAGE("आगामी चरण (Planned)", "Planned Stage"),
    BLOCKED_POLICY("नीति द्वारा अवरुद्ध (Blocked)", "Blocked by Free/Privacy Policy")
}

data class FeatureMatrixItem(
    val id: String,
    val stageNumber: Int,
    val featureTitleHi: String,
    val featureTitleEn: String,
    val requiredBySpec: Boolean,
    val status: FeatureGateStatus,
    val unitTested: Boolean,
    val powerTierRequired: String,
    val wakeupsPerHourCost: String,
    val networkCallsPerHourCost: String,
    val notesHi: String,
    val notesEn: String
)

data class OpenDecisionRecord(
    val id: String,
    val decisionTitleHi: String,
    val decisionTitleEn: String,
    val currentAssumptionHi: String,
    val currentAssumptionEn: String,
    val ownerActionNeededHi: String,
    val ownerActionNeededEn: String
)
