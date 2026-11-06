package com.example.domain

import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Pure Domain Validation, Sanitization, Source Allowlist & Rights Enforcement Engine.
 * Strictly implements Sections 2, 3.6, and 6 of the Netra News Build Specification.
 */
object ValidationAndRightsEngine {

    const val SUPPORTED_SCHEMA_VERSION = 1
    const val FUTURE_CLOCK_SKEW_TOLERANCE_SECONDS = 300L // 5 minutes max tolerance
    const val STALE_THRESHOLD_MS = 6L * 60L * 60L * 1000L // 6 hours

    /**
     * Official & Reputable Source Allowlist (Section 2.2 & 6.2).
     * RSS availability is NOT treated as a republication licence; commercial/agency sources
     * default to LINK_ONLY until explicit publisher permission is recorded.
     */
    val ALLOWLISTED_SOURCES: List<SourceAllowlistEntry> = listOf(
        SourceAllowlistEntry(
            sourceId = "pib-india",
            publisherNameHi = "पत्र सूचना कार्यालय (PIB India)",
            publisherNameEn = "Press Information Bureau (PIB India)",
            allowedHost = "pib.gov.in",
            feedUrl = "https://pib.gov.in/RssMain.aspx",
            coverageScopes = listOf(NewsScope.LOCAL, NewsScope.STATE, NewsScope.NATIONAL),
            updateCadenceMinutes = 30,
            termsReviewed = true,
            defaultRightsStatus = RightsStatus.ORIGINAL,
            licenceSummaryHi = "भारत सरकार की आधिकारिक सार्वजनिक विज्ञप्ति (स्रोत श्रेय सहित संक्षिप्त विवरण अनुमत)",
            licenceSummaryEn = "Official Government of India Public Release (summary permitted with attribution)"
        ),
        SourceAllowlistEntry(
            sourceId = "dd-news",
            publisherNameHi = "डीडी न्यूज़ (DD News)",
            publisherNameEn = "DD News (Prasar Bharati)",
            allowedHost = "ddnews.gov.in",
            feedUrl = "https://ddnews.gov.in/rss",
            coverageScopes = listOf(NewsScope.LOCAL, NewsScope.STATE, NewsScope.NATIONAL),
            updateCadenceMinutes = 30,
            termsReviewed = true,
            defaultRightsStatus = RightsStatus.LINK_ONLY,
            licenceSummaryHi = "प्रसार भारती सार्वजनिक प्रसारक — पूर्ण लेख पुनः प्रकाशन अनुमति प्रमाणित होने तक केवल शीर्षक व स्रोत लिंक",
            licenceSummaryEn = "Prasar Bharati Public Broadcaster — Link-out only until full text republication permission is verified"
        ),
        SourceAllowlistEntry(
            sourceId = "air-news",
            publisherNameHi = "आकाशवाणी समाचार (NewsOnAir)",
            publisherNameEn = "All India Radio (NewsOnAir)",
            allowedHost = "newsonair.gov.in",
            feedUrl = "https://newsonair.gov.in/rss",
            coverageScopes = listOf(NewsScope.LOCAL, NewsScope.STATE, NewsScope.NATIONAL),
            updateCadenceMinutes = 30,
            termsReviewed = true,
            defaultRightsStatus = RightsStatus.LINK_ONLY,
            licenceSummaryHi = "आकाशवाणी क्षेत्रीय व राष्ट्रीय बुलेटिन — केवल स्रोत-लेबल लिंक-आउट (Link-Only)",
            licenceSummaryEn = "AIR Regional & National Bulletins — Source-labelled link-out only"
        ),
        SourceAllowlistEntry(
            sourceId = "imd-mausam",
            publisherNameHi = "भारत मौसम विज्ञान विभाग (IMD)",
            publisherNameEn = "India Meteorological Department (IMD)",
            allowedHost = "mausam.imd.gov.in",
            feedUrl = "https://mausam.imd.gov.in/imd_latest/contents/subdivisionwise-warning.php",
            coverageScopes = listOf(NewsScope.LOCAL, NewsScope.STATE, NewsScope.NATIONAL),
            updateCadenceMinutes = 60,
            termsReviewed = true,
            defaultRightsStatus = RightsStatus.ORIGINAL,
            licenceSummaryHi = "आधिकारिक मौसम बुलेटिन एवं चेतावनी डेटा (गैर-आपातकालीन सूचना प्रदर्शन)",
            licenceSummaryEn = "Official meteorological bulletin data (informational display with source link)"
        ),
        SourceAllowlistEntry(
            sourceId = "ncs-seismo",
            publisherNameHi = "राष्ट्रीय भूकंप विज्ञान केंद्र (NCS)",
            publisherNameEn = "National Center for Seismology (NCS)",
            allowedHost = "seismo.gov.in",
            feedUrl = "https://seismo.gov.in/",
            coverageScopes = listOf(NewsScope.LOCAL, NewsScope.STATE, NewsScope.NATIONAL),
            updateCadenceMinutes = 60,
            termsReviewed = true,
            defaultRightsStatus = RightsStatus.ORIGINAL,
            licenceSummaryHi = "आधिकारिक भूकंप घटना लॉग (यह भूकंप भविष्यवाणी या पूर्व-चेतावनी प्रणाली नहीं है)",
            licenceSummaryEn = "Official seismic event bulletin (Not an earthquake prediction or early-warning system)"
        ),
        SourceAllowlistEntry(
            sourceId = "up-info",
            publisherNameHi = "सूचना एवं जनसंपर्क विभाग (UP DIPR)",
            publisherNameEn = "State Information Directorate (UP DIPR)",
            allowedHost = "information.up.gov.in",
            feedUrl = "https://information.up.gov.in/",
            coverageScopes = listOf(NewsScope.LOCAL, NewsScope.STATE),
            updateCadenceMinutes = 60,
            termsReviewed = true,
            defaultRightsStatus = RightsStatus.LINK_ONLY,
            licenceSummaryHi = "राज्य सूचना निदेशालय — स्रोत लिंक-आउट मोड",
            licenceSummaryEn = "State Information Directorate — Source link-out mode"
        )
    )

    private val ALLOWED_HOSTS: Set<String> = ALLOWLISTED_SOURCES.map { it.allowedHost.lowercase() }.toSet()

    /**
     * Explicit TEST fixture from Section 6.3:
     * id=test-001; title=TEST - feed unavailable example; publisher=TEST SOURCE;
     * publishedAt=null; verificationStatus=pending; rightsStatus=blocked.
     * Production build MUST reject this fixture from ever entering production feeds.
     */
    val SPEC_TEST_FIXTURE_001 = ArticleRecord(
        id = "test-001",
        title = "TEST - feed unavailable example",
        language = "en",
        category = "System Test",
        scope = NewsScope.LOCAL,
        countryCode = "IN",
        stateCode = "UP",
        cityId = "in-up-lucknow",
        publisherName = "TEST SOURCE",
        canonicalSourceUrl = "https://pib.gov.in/test-fixture-placeholder",
        publishedAtIso = null,
        fetchedAtEpochMs = 1793940000000L,
        verifiedAtIso = null,
        verificationStatus = VerificationStatus.PENDING,
        rightsStatus = RightsStatus.BLOCKED,
        permittedSummary = "TEST fixture summary — must never render in production feed."
    )

    data class EnvelopeValidationResult(
        val isValidSchema: Boolean,
        val rejectedSchemaReason: String? = null,
        val verifiedDisplayableItems: List<ArticleRecord> = emptyList(),
        val rejectedOrFilteredItems: List<Pair<ArticleRecord, String>> = emptyList(),
        val retractedNoticeItems: List<ArticleRecord> = emptyList(),
        val isStale: Boolean = false
    )

    /**
     * Sanitizes imported strings (Section 2.5 & 3.6):
     * Strips active HTML, <script> blocks, inline event handlers, and control chars.
     */
    fun sanitizeString(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), "")
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), "")
            .replace(Regex("(?is)<[^>]+>"), "")
            .replace(Regex("(?i)javascript\\s*:"), "")
            .replace(Regex("(?i)data\\s*:"), "")
            .replace(Regex("(?i)vbscript\\s*:"), "")
            .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .trim()
    }

    /**
     * Validates canonical source URL (Section 3.6 & 6.2):
     * Must be HTTPS, valid URI, no unsafe schemes, and host must be in allowlist.
     */
    fun isApprovedHttpsSourceUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        val uri = try {
            URI(trimmed)
        } catch (e: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "https") return false
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        if (uri.RawUserInfo != null) return false
        return ALLOWED_HOSTS.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }

    private val URI.RawUserInfo: String?
        get() = this.rawUserInfo

    /**
     * Checks whether an article is a TEST fixture (Section 2.4 & 6.3).
     * Test fixtures must be marked TEST and must NEVER enter production feeds.
     */
    fun isTestFixture(article: ArticleRecord): Boolean {
        val cleanId = article.id.trim()
        val cleanTitle = article.title.trim()
        val cleanPub = article.publisherName.trim()
        return cleanId.equals("test-001", ignoreCase = true) ||
            cleanId.startsWith("test-", ignoreCase = true) ||
            cleanTitle.startsWith("TEST -", ignoreCase = true) ||
            cleanTitle.startsWith("[TEST]", ignoreCase = true) ||
            cleanPub.equals("TEST SOURCE", ignoreCase = true)
    }

    /**
     * Parses an ISO-8601 timestamp with timezone offset (Section 2.1 & 6.5).
     * Rejects strings without timezone or invalid format.
     * Never substitutes the device clock for missing/invalid timestamps.
     */
    fun parseSourceTimestampEpochMs(isoWithTz: String?): Long? {
        if (isoWithTz.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(isoWithTz.trim()).toInstant().toEpochMilli()
        } catch (e: DateTimeParseException) {
            try {
                Instant.parse(isoWithTz.trim()).toEpochMilli()
            } catch (e2: DateTimeParseException) {
                null
            }
        }
    }

    /**
     * Checks if source timestamp is missing, unparseable, or in the future relative to reference clock.
     * Section 6.5: "Treat source future times or missing event times as unverified,
     * never replace them with the device clock."
     */
    fun isTimestampVerifiedAndNotFuture(
        publishedAtIso: String?,
        referenceNowEpochMs: Long
    ): Boolean {
        val epochMs = parseSourceTimestampEpochMs(publishedAtIso) ?: return false
        val maxAllowedMs = referenceNowEpochMs + (FUTURE_CLOCK_SKEW_TOLERANCE_SECONDS * 1000L)
        return epochMs <= maxAllowedMs
    }

    /**
     * Checks if an article has passed its optional expiry timestamp.
     */
    fun isArticleExpired(expiryIso: String?, referenceNowEpochMs: Long): Boolean {
        if (expiryIso.isNullOrBlank()) return false
        val expiryMs = parseSourceTimestampEpochMs(expiryIso) ?: return false
        return referenceNowEpochMs > expiryMs
    }

    /**
     * Enforces Rights Contract on an article's summary (Section 2.3 & 6.2):
     * Until permission is proven (LINK_ONLY), do not display scraped prose or summaries;
     * only ORIGINAL or LICENSED items may expose permittedSummary.
     */
    fun getRightsPermittedSummary(article: ArticleRecord): String? {
        return when (article.rightsStatus) {
            RightsStatus.ORIGINAL, RightsStatus.LICENSED -> {
                val sanitized = sanitizeString(article.permittedSummary)
                sanitized.ifBlank { null }
            }
            RightsStatus.LINK_ONLY, RightsStatus.BLOCKED -> null
        }
    }

    /**
     * Validates an entire FeedEnvelope (Section 6.1 - 6.5):
     * 1. Rejects schemaVersion != 1 visibly.
     * 2. Rejects TEST fixtures in production mode.
     * 3. Rejects non-HTTPS or unapproved source URLs.
     * 4. Rejects missing or future timestamps as unverified.
     * 5. Filters out PENDING, RETRACTED, and BLOCKED items from verified feed (separating RETRACTED notices).
     * 6. Consolidates duplicate canonical URLs while preserving all source attributions.
     */
    fun validateFeedEnvelope(
        envelope: FeedEnvelope,
        referenceNowEpochMs: Long,
        isProductionFeed: Boolean = true
    ): EnvelopeValidationResult {
        if (envelope.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return EnvelopeValidationResult(
                isValidSchema = false,
                rejectedSchemaReason = "Incompatible schemaVersion=${envelope.schemaVersion}. Expected schemaVersion=$SUPPORTED_SCHEMA_VERSION."
            )
        }

        val generatedAtMs = parseSourceTimestampEpochMs(envelope.generatedAtUtcIso)
        if (generatedAtMs == null || generatedAtMs > referenceNowEpochMs + (FUTURE_CLOCK_SKEW_TOLERANCE_SECONDS * 1000L)) {
            return EnvelopeValidationResult(
                isValidSchema = false,
                rejectedSchemaReason = "Invalid or future envelope generatedAt timestamp: ${envelope.generatedAtUtcIso}"
            )
        }

        val isStale = (referenceNowEpochMs - generatedAtMs) > STALE_THRESHOLD_MS

        val verifiedCandidates = mutableListOf<ArticleRecord>()
        val rejected = mutableListOf<Pair<ArticleRecord, String>>()
        val retractedNotices = mutableListOf<ArticleRecord>()

        for (rawItem in envelope.items) {
            val sanitizedTitle = sanitizeString(rawItem.title)
            val sanitizedPublisher = sanitizeString(rawItem.publisherName)
            val sanitizedSummary = sanitizeString(rawItem.permittedSummary)
            val item = rawItem.copy(
                title = sanitizedTitle,
                publisherName = sanitizedPublisher,
                permittedSummary = sanitizedSummary.ifBlank { null },
                corrections = rawItem.corrections.map {
                    CorrectionEntry(
                        correctedAtIso = sanitizeString(it.correctedAtIso),
                        reason = sanitizeString(it.reason)
                    )
                }
            )

            // 1. Check TEST fixture rule
            if (isProductionFeed && isTestFixture(item)) {
                rejected.add(item to "Rejected TEST fixture in production feed (Section 2.4 / 6.3)")
                continue
            }

            // 2. Check empty ID or title
            if (item.id.isBlank() || item.title.isBlank() || item.publisherName.isBlank()) {
                rejected.add(item to "Rejected missing mandatory id, title, or publisherName")
                continue
            }

            // 3. Check HTTPS & Allowlist host
            if (!isApprovedHttpsSourceUrl(item.canonicalSourceUrl)) {
                rejected.add(item to "Rejected non-HTTPS or non-allowlisted canonicalSourceUrl: ${item.canonicalSourceUrl}")
                continue
            }

            // 4. Separate retracted items so corrections/retractions remain transparent but not active news
            if (item.verificationStatus == VerificationStatus.RETRACTED) {
                retractedNotices.add(item)
                rejected.add(item to "Withheld from active news: VerificationStatus is RETRACTED")
                continue
            }

            // 5. Never render PENDING or BLOCKED as current verified news
            if (item.verificationStatus != VerificationStatus.VERIFIED) {
                rejected.add(item to "Withheld: VerificationStatus is ${item.verificationStatus.wireKey}")
                continue
            }
            if (item.rightsStatus == RightsStatus.BLOCKED) {
                rejected.add(item to "Withheld: RightsStatus is BLOCKED")
                continue
            }

            // 6. Check publishedAt timestamp (missing or future times treated as unverified)
            if (!isTimestampVerifiedAndNotFuture(item.publishedAtIso, referenceNowEpochMs)) {
                rejected.add(item to "Withheld as unverified: Missing or future publishedAt timestamp (${item.publishedAtIso})")
                continue
            }

            // 7. Check expiry
            if (isArticleExpired(item.expiryIso, referenceNowEpochMs)) {
                rejected.add(item to "Withheld: Article expired at ${item.expiryIso}")
                continue
            }

            // 8. Enforce rights-based summary stripping
            val rightsCompliantItem = item.copy(
                permittedSummary = getRightsPermittedSummary(item)
            )
            verifiedCandidates.add(rightsCompliantItem)
        }

        // 9. Consolidate duplicate canonical URLs without losing source attribution (Section 2.5)
        val consolidated = consolidateDuplicateLinks(verifiedCandidates)

        return EnvelopeValidationResult(
            isValidSchema = true,
            verifiedDisplayableItems = consolidated,
            rejectedOrFilteredItems = rejected,
            retractedNoticeItems = retractedNotices,
            isStale = isStale
        )
    }

    /**
     * Consolidates articles sharing the same canonicalSourceUrl while preserving every source's attribution.
     */
    fun consolidateDuplicateLinks(items: List<ArticleRecord>): List<ArticleRecord> {
        val grouped = items.groupBy { it.canonicalSourceUrl.trim().lowercase() }
        return grouped.values.map { duplicates ->
            val primary = duplicates.first()
            val allPublishers = duplicates
                .flatMap { it.consolidatedSources + it.publisherName }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
            val allCorrections = duplicates.flatMap { it.corrections }.distinct()
            primary.copy(
                consolidatedSources = allPublishers,
                corrections = allCorrections
            )
        }
    }
}
