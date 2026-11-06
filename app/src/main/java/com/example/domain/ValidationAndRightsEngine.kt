package com.example.domain

import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Pure Domain Validation, Sanitization, Source Verification Indicators, Rights Enforcement,
 * and Minimal-Data Feedback Validation Engine.
 * Strictly implements Sections 2, 3.6, 7, 8, and 9 of the Netra News Build Specification v3 (6 Nov 2026).
 */
object ValidationAndRightsEngine {

    const val SUPPORTED_SCHEMA_VERSION = 1
    const val FUTURE_CLOCK_SKEW_TOLERANCE_SECONDS = 300L // 5 minutes max tolerance
    const val STALE_THRESHOLD_MS = 6L * 60L * 60L * 1000L // 6 hours

    // Feedback validation & spam limits (Section 8.4 & 8.5)
    const val FEEDBACK_DESC_MIN_CHARS = 20
    const val FEEDBACK_DESC_MAX_CHARS = 2000
    const val FEEDBACK_COMMENTS_MAX_CHARS = 500
    const val FEEDBACK_CONTACT_MAX_CHARS = 120
    const val FEEDBACK_COOLDOWN_MS = 15_000L
    const val FEEDBACK_MAX_PER_WINDOW = 5
    const val FEEDBACK_RATE_WINDOW_MS = 10L * 60L * 1000L // 10 minutes
    const val CONTACT_RETENTION_DAYS_AFTER_RESOLUTION = 30
    const val UNRESOLVED_FEEDBACK_RETENTION_DAYS = 90

    private val ARTICLE_ID_REGEX = Regex("^[a-zA-Z0-9._-]{2,80}$")
    private val SCRIPT_OR_MARKUP_REGEX = Regex("(?is)(<script|<iframe|<object|<embed|javascript\\s*:|vbscript\\s*:|data\\s*:\\s*text/html|on\\w+\\s*=)")

    /**
     * Official & Reputable Source Allowlist (Section 2.2 & 9.2).
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
     * Explicit TEST fixture from Section 9.3:
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
        verificationCheckedAtIso = null,
        verificationReason = "Labelled TEST fixture (test-001) — blocked from production feeds.",
        verificationStatus = VerificationStatus.PENDING,
        rightsStatus = RightsStatus.BLOCKED,
        editorialState = EditorialState.ACTIVE,
        permittedSummary = "TEST fixture summary — must never render in production feed."
    )

    data class EnvelopeValidationResult(
        val isValidSchema: Boolean,
        val rejectedSchemaReason: String? = null,
        val verifiedDisplayableItems: List<ArticleRecord> = emptyList(),
        val warningStatusRecords: List<ArticleRecord> = emptyList(), // Pending / Blocked warning records (Section 7.4)
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
     * Validates canonical source URL (Section 3.6 & 9.2):
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
        if (uri.rawUserInfo != null) return false
        return ALLOWED_HOSTS.any { allowed ->
            host == allowed || host.endsWith(".$allowed")
        }
    }

    /**
     * Section 7.4: "Blocked content has no scraped body/media or clickable unsafe URL;
     * show a neutral withheld notice and correction reason instead."
     */
    fun isClickableSourceLinkAllowed(article: ArticleRecord): Boolean {
        if (article.verificationStatus == VerificationStatus.BLOCKED) return false
        if (article.rightsStatus == RightsStatus.BLOCKED) return false
        return isApprovedHttpsSourceUrl(article.canonicalSourceUrl)
    }

    /**
     * Section 7.2 & 7.3:
     * "Show Source: [publisher] next to Source verification: [status]."
     * "Text labels must be announced by screen readers and retain meaning in monochrome/high contrast."
     */
    fun formatAttributionAndVerificationLine(article: ArticleRecord, isHindi: Boolean): String {
        val publisher = article.consolidatedSources.joinToString(" + ").ifBlank { article.publisherName }
        return if (isHindi) {
            "स्रोत (Source): $publisher • स्रोत सत्यापन (Source verification): ${article.verificationStatus.labelHi}"
        } else {
            "Source: $publisher • Source verification: ${article.verificationStatus.labelEn}"
        }
    }

    fun formatScreenReaderVerificationAnnouncement(article: ArticleRecord, isHindi: Boolean): String {
        val publisher = article.consolidatedSources.joinToString(" and ").ifBlank { article.publisherName }
        return if (isHindi) {
            "स्रोत: $publisher. स्रोत सत्यापन स्थिति: ${article.verificationStatus.labelHi}. यह केवल स्रोत सत्यापन स्थिति है, प्रत्येक तथ्य की गारंटी नहीं।"
        } else {
            "Source: $publisher. Source verification: ${article.verificationStatus.labelEn}. Recorded source verification status, not a guarantee that every fact is true."
        }
    }

    /**
     * Checks if verificationCheckedAtIso is stale (>6 hours old) relative to referenceNowEpochMs (Section 7.6).
     */
    fun isVerificationCheckStale(verificationCheckedAtIso: String?, referenceNowEpochMs: Long): Boolean {
        val checkedMs = parseSourceTimestampEpochMs(verificationCheckedAtIso) ?: return true
        return (referenceNowEpochMs - checkedMs) > STALE_THRESHOLD_MS
    }

    /**
     * Enforces Section 7.4 & 7.5 normalization on any ArticleRecord:
     * - Pending or Blocked items can NEVER appear in Breaking or Live (`isBreakingOrLive = false`).
     * - Blocked items have no scraped body (`permittedSummary = null`) or media (`licensedImageUrl = null`).
     */
    fun normalizeArticleForVerificationAndRights(raw: ArticleRecord): ArticleRecord {
        val sanitizedTitle = sanitizeString(raw.title)
        val sanitizedPublisher = sanitizeString(raw.publisherName)
        val sanitizedSummary = sanitizeString(raw.permittedSummary).ifBlank { null }
        val sanitizedReason = sanitizeString(raw.verificationReason).ifBlank { null }

        val effectiveBreaking = if (raw.verificationStatus != VerificationStatus.VERIFIED ||
            raw.rightsStatus == RightsStatus.BLOCKED ||
            raw.editorialState == EditorialState.RETRACTED
        ) {
            false // Section 7.4: "Pending content must not appear in Breaking or Live."
        } else {
            raw.isBreakingOrLive
        }

        val effectiveSummary = when {
            raw.verificationStatus == VerificationStatus.BLOCKED -> null
            raw.rightsStatus == RightsStatus.BLOCKED || raw.rightsStatus == RightsStatus.LINK_ONLY -> null
            raw.editorialState == EditorialState.RETRACTED -> null
            else -> sanitizedSummary
        }

        val effectiveImage = if (raw.verificationStatus == VerificationStatus.BLOCKED ||
            raw.rightsStatus == RightsStatus.BLOCKED ||
            raw.rightsStatus == RightsStatus.LINK_ONLY
        ) {
            null
        } else {
            raw.licensedImageUrl
        }

        return raw.copy(
            title = sanitizedTitle,
            publisherName = sanitizedPublisher,
            permittedSummary = effectiveSummary,
            verificationReason = sanitizedReason,
            licensedImageUrl = effectiveImage,
            isBreakingOrLive = effectiveBreaking,
            corrections = raw.corrections.map {
                CorrectionEntry(
                    correctedAtIso = sanitizeString(it.correctedAtIso),
                    reason = sanitizeString(it.reason)
                )
            }
        )
    }

    /**
     * Section 7.5: "Only authorised editorial review can change status.
     * Feed imports and user feedback cannot self-verify a source.
     * Record status transitions, review time and reason."
     */
    fun applyEditorialStatusTransition(
        article: ArticleRecord,
        newStatus: VerificationStatus,
        actorRole: String,
        reviewedAtIso: String,
        reason: String
    ): Result<ArticleRecord> {
        val cleanRole = actorRole.trim().lowercase()
        if (cleanRole != "authorised_editorial_reviewer") {
            return Result.failure(
                SecurityException("Unauthorized actor '$actorRole': Feed imports and user feedback cannot self-verify or alter verificationStatus.")
            )
        }
        val cleanReason = sanitizeString(reason)
        if (cleanReason.length < 5) {
            return Result.failure(IllegalArgumentException("Editorial status transition requires a recorded reason."))
        }
        if (parseSourceTimestampEpochMs(reviewedAtIso) == null) {
            return Result.failure(IllegalArgumentException("Editorial status transition requires a valid ISO review timestamp."))
        }
        val transition = VerificationTransitionRecord(
            fromStatus = article.verificationStatus,
            toStatus = newStatus,
            reviewedAtIso = reviewedAtIso,
            reviewerRole = actorRole,
            reason = cleanReason
        )
        val updated = normalizeArticleForVerificationAndRights(
            article.copy(
                verificationStatus = newStatus,
                verificationCheckedAtIso = reviewedAtIso,
                verificationReason = cleanReason,
                statusTransitions = article.statusTransitions + transition
            )
        )
        return Result.success(updated)
    }

    /**
     * Checks whether an article is a TEST fixture (Section 2.4 & 9.3).
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
     * Parses an ISO-8601 timestamp with timezone offset (Section 2.1 & 9.5).
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

    fun isTimestampVerifiedAndNotFuture(
        publishedAtIso: String?,
        referenceNowEpochMs: Long
    ): Boolean {
        val epochMs = parseSourceTimestampEpochMs(publishedAtIso) ?: return false
        val maxAllowedMs = referenceNowEpochMs + (FUTURE_CLOCK_SKEW_TOLERANCE_SECONDS * 1000L)
        return epochMs <= maxAllowedMs
    }

    fun isArticleExpired(expiryIso: String?, referenceNowEpochMs: Long): Boolean {
        if (expiryIso.isNullOrBlank()) return false
        val expiryMs = parseSourceTimestampEpochMs(expiryIso) ?: return false
        return referenceNowEpochMs > expiryMs
    }

    fun getRightsPermittedSummary(article: ArticleRecord): String? {
        if (article.verificationStatus == VerificationStatus.BLOCKED ||
            article.editorialState == EditorialState.RETRACTED
        ) {
            return null
        }
        return when (article.rightsStatus) {
            RightsStatus.ORIGINAL, RightsStatus.LICENSED -> {
                val sanitized = sanitizeString(article.permittedSummary)
                sanitized.ifBlank { null }
            }
            RightsStatus.LINK_ONLY, RightsStatus.BLOCKED -> null
        }
    }

    /**
     * Validates an entire FeedEnvelope (Section 7 & 9.1 - 9.5):
     * - Rejects schemaVersion != 1 visibly.
     * - Rejects TEST fixtures in production mode.
     * - Separates RETRACTED items (`editorialState == RETRACTED`).
     * - Separates `Pending` and `Blocked` warning/status records from ordinary verified stories
     *   (never rendering `Pending` or `Blocked` as current verified stories or in Breaking/Live).
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
        val warningRecords = mutableListOf<ArticleRecord>()
        val rejected = mutableListOf<Pair<ArticleRecord, String>>()
        val retractedNotices = mutableListOf<ArticleRecord>()

        for (rawItem in envelope.items) {
            val item = normalizeArticleForVerificationAndRights(rawItem)

            // 1. Check TEST fixture rule
            if (isProductionFeed && isTestFixture(item)) {
                rejected.add(item to "Rejected TEST fixture in production feed (Section 2.4 / 9.3)")
                continue
            }

            // 2. Check empty ID or title
            if (item.id.isBlank() || item.title.isBlank() || item.publisherName.isBlank()) {
                rejected.add(item to "Rejected missing mandatory id, title, or publisherName")
                continue
            }

            // 3. Separate retracted items (Section 7.5 & 9.2: retraction is a separate editorialState)
            if (item.editorialState == EditorialState.RETRACTED) {
                retractedNotices.add(item)
                rejected.add(item to "Withheld from active news: EditorialState is RETRACTED")
                continue
            }

            // 4. Handle Pending or Blocked records (Section 7.4: warning/status record, never an ordinary verified story)
            if (item.verificationStatus == VerificationStatus.PENDING ||
                item.verificationStatus == VerificationStatus.BLOCKED ||
                item.rightsStatus == RightsStatus.BLOCKED
            ) {
                warningRecords.add(item)
                rejected.add(
                    item to "Withheld from verified feed: verificationStatus=${item.verificationStatus.wireKey}, rightsStatus=${item.rightsStatus.wireKey}"
                )
                continue
            }

            // 5. Check HTTPS & Allowlist host for VERIFIED items
            if (!isApprovedHttpsSourceUrl(item.canonicalSourceUrl)) {
                val demotedWarning = item.copy(
                    verificationStatus = VerificationStatus.BLOCKED,
                    isBreakingOrLive = false,
                    permittedSummary = null,
                    verificationReason = "Blocked unsafe or non-allowlisted source URL: ${item.canonicalSourceUrl}"
                )
                warningRecords.add(demotedWarning)
                rejected.add(item to "Rejected non-HTTPS or non-allowlisted canonicalSourceUrl: ${item.canonicalSourceUrl}")
                continue
            }

            // 6. Check publishedAt timestamp (missing or future times treated as unverified -> Pending warning)
            if (!isTimestampVerifiedAndNotFuture(item.publishedAtIso, referenceNowEpochMs)) {
                val demotedPending = item.copy(
                    verificationStatus = VerificationStatus.PENDING,
                    isBreakingOrLive = false,
                    verificationReason = "Missing or future publishedAt timestamp (${item.publishedAtIso}); treated as Pending."
                )
                warningRecords.add(demotedPending)
                rejected.add(item to "Withheld as unverified: Missing or future publishedAt timestamp (${item.publishedAtIso})")
                continue
            }

            // 7. Check expiry
            if (isArticleExpired(item.expiryIso, referenceNowEpochMs)) {
                rejected.add(item to "Withheld: Article expired at ${item.expiryIso}")
                continue
            }

            verifiedCandidates.add(item)
        }

        val consolidated = consolidateDuplicateLinks(verifiedCandidates)

        return EnvelopeValidationResult(
            isValidSchema = true,
            verifiedDisplayableItems = consolidated,
            warningStatusRecords = warningRecords,
            rejectedOrFilteredItems = rejected,
            retractedNoticeItems = retractedNotices,
            isStale = isStale
        )
    }

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

    // =========================================================================
    // Section 8: Feedback, Private Review & Minimal Data Validation Engine
    // =========================================================================

    /**
     * Validates a user feedback or article issue report (Section 8.1, 8.4, 8.5, 8.6):
     * - Checks honeypot field (must be blank)
     * - Enforces submission cooldown (15s) and rate window cap (max 5 in 10 mins)
     * - Validates optional articleId format
     * - Rejects scripts / active markup explicitly (Section 8.4: "Reject scripts/active markup")
     * - Enforces description length 20..2000 characters
     * - Enforces optional comments (<=500) and optional contact (<=120) limits
     * - Allows anonymous submission without an account or contact
     */
    fun validateFeedbackSubmission(
        rawPayload: FeedbackSubmissionPayload,
        recentAttemptTimestampsMs: List<Long>,
        nowEpochMs: Long
    ): FeedbackValidationOutcome {
        // 1. Spam Honeypot check
        if (rawPayload.honeypotField.isNotBlank()) {
            return FeedbackValidationOutcome.SpamOrRateLimited(
                reasonHi = "स्पैम सुरक्षा (Honeypot) द्वारा फ़ॉर्म अस्वीकृत किया गया।",
                reasonEn = "Submission rejected by automated spam honeypot guard."
            )
        }

        // 2. Cooldown & Bounded Rate Limit check
        val lastAttempt = recentAttemptTimestampsMs.maxOrNull()
        if (lastAttempt != null && (nowEpochMs - lastAttempt) < FEEDBACK_COOLDOWN_MS) {
            val waitSec = ((FEEDBACK_COOLDOWN_MS - (nowEpochMs - lastAttempt)) / 1000L).coerceAtLeast(1L)
            return FeedbackValidationOutcome.SpamOrRateLimited(
                reasonHi = "कृपया अगली प्रतिक्रिया से पहले $waitSec सेकंड प्रतीक्षा करें (Submission Cooldown)।",
                reasonEn = "Submission cooldown active. Please wait ${waitSec}s before trying again."
            )
        }
        val attemptsInWindow = recentAttemptTimestampsMs.count { (nowEpochMs - it) <= FEEDBACK_RATE_WINDOW_MS }
        if (attemptsInWindow >= FEEDBACK_MAX_PER_WINDOW) {
            return FeedbackValidationOutcome.SpamOrRateLimited(
                reasonHi = "दर सीमा (Rate Limit) समाप्त: 10 मिनट में अधिकतम $FEEDBACK_MAX_PER_WINDOW रिपोर्ट अनुमत हैं।",
                reasonEn = "Rate limit reached: Maximum $FEEDBACK_MAX_PER_WINDOW reports per 10 minutes."
            )
        }

        // 3. Reject active HTML / scripts explicitly before storing
        val fieldsToInspect = listOfNotNull(
            rawPayload.articleId,
            rawPayload.description,
            rawPayload.optionalComments,
            rawPayload.optionalContact
        )
        if (fieldsToInspect.any { SCRIPT_OR_MARKUP_REGEX.containsMatchIn(it) || it.contains("<") || it.contains(">") }) {
            return FeedbackValidationOutcome.InvalidInput(
                reasonHi = "सुरक्षा अस्वीकृति: विवरण या संपर्क में सक्रिय HTML, स्क्रिप्ट या टैग (< >) अनुमत नहीं हैं। केवल सादा पाठ (Plain Text) लिखें।",
                reasonEn = "Security Rejection: Active HTML, scripts, or angle-bracket markup are rejected. Please enter plain text only."
            )
        }

        // 4. Validate optional articleId format
        val cleanArticleId = rawPayload.articleId?.trim()?.ifBlank { null }
        if (cleanArticleId != null && !ARTICLE_ID_REGEX.matches(cleanArticleId)) {
            return FeedbackValidationOutcome.InvalidInput(
                reasonHi = "अमान्य लेख आईडी (articleId) प्रारूप।",
                reasonEn = "Invalid articleId format."
            )
        }

        // 5. Validate description length (20..2000 characters)
        val cleanDesc = sanitizeString(rawPayload.description)
        if (cleanDesc.length < FEEDBACK_DESC_MIN_CHARS || cleanDesc.length > FEEDBACK_DESC_MAX_CHARS) {
            return FeedbackValidationOutcome.InvalidInput(
                reasonHi = "विवरण $FEEDBACK_DESC_MIN_CHARS से $FEEDBACK_DESC_MAX_CHARS अक्षरों के बीच होना चाहिए (वर्तमान: ${cleanDesc.length} अक्षर)।",
                reasonEn = "Description must be between $FEEDBACK_DESC_MIN_CHARS and $FEEDBACK_DESC_MAX_CHARS characters (current: ${cleanDesc.length})."
            )
        }

        // 6. Validate optional comments and optional contact length
        val cleanComments = sanitizeString(rawPayload.optionalComments).ifBlank { null }
        if (cleanComments != null && cleanComments.length > FEEDBACK_COMMENTS_MAX_CHARS) {
            return FeedbackValidationOutcome.InvalidInput(
                reasonHi = "अतिरिक्त टिप्पणी अधिकतम $FEEDBACK_COMMENTS_MAX_CHARS अक्षरों की हो सकती है।",
                reasonEn = "Optional comments must not exceed $FEEDBACK_COMMENTS_MAX_CHARS characters."
            )
        }

        val cleanContact = sanitizeString(rawPayload.optionalContact).ifBlank { null }
        if (cleanContact != null && cleanContact.length > FEEDBACK_CONTACT_MAX_CHARS) {
            return FeedbackValidationOutcome.InvalidInput(
                reasonHi = "वैकल्पिक संपर्क अधिकतम $FEEDBACK_CONTACT_MAX_CHARS अक्षरों का हो सकता है।",
                reasonEn = "Optional contact must not exceed $FEEDBACK_CONTACT_MAX_CHARS characters."
            )
        }

        return FeedbackValidationOutcome.ValidReadyForSubmissionOrDraft(
            sanitizedPayload = rawPayload.copy(
                articleId = cleanArticleId,
                description = cleanDesc,
                optionalComments = cleanComments,
                optionalContact = cleanContact,
                honeypotField = ""
            )
        )
    }

    /**
     * Evaluates Feedback Delivery per Section 8.4 & 8.9:
     * - "Do not promise delivery before server confirms a durable insert and returns a receipt ID;
     *   retries use an idempotency token."
     * - "If private storage or reviewer access is not ready, disable submit with an honest unavailable
     *   message and preserve only an optional local draft on the user device. Never simulate success."
     */
    fun processFeedbackDelivery(
        validatedPayload: FeedbackSubmissionPayload,
        isPrivateEndpointConnected: Boolean,
        simulateNetworkFailure: Boolean = false,
        existingIdempotencyReceipts: Map<String, String> = emptyMap(),
        serverNowIso: String = "2026-11-06T07:00:00Z"
    ): FeedbackDeliveryResult {
        if (!isPrivateEndpointConnected) {
            return FeedbackDeliveryResult.DisabledBackendSavedLocalDraftOnly(
                localDraftId = validatedPayload.feedbackId,
                honestUnavailableMessageHi = "निजी समीक्षा स्टोरेज (Private Endpoint / D1) अभी जुड़ा नहीं है। नकली सफलता दिखाने के बजाय रिमोट सबमिट को ईमानदारी से अक्षम रखा गया है। आपका ड्राफ्ट केवल आपके फ़ोन पर सहेजा गया है।",
                honestUnavailableMessageEn = "Private review storage endpoint is not yet connected. Remote submit is honestly disabled rather than simulating success; your draft is saved locally on this device only."
            )
        }

        // A boolean or local idempotency map is not evidence of a durable server insert.
        // No transport is implemented, so this helper must never mint a receipt.
        return FeedbackDeliveryResult.NetworkOrServerFailure(
            errorMessageHi = "सर्वर रसीद अनुपलब्ध। कोई रिमोट सबमिशन नहीं हुआ।",
            errorMessageEn = "No server receipt available. Nothing was submitted remotely.",
            canRetryWithSameIdempotencyToken = true
        )
    }

    /**
     * Section 8.7 & 8.8: Retention & Private Reviewer Access Guard.
     * - No unauthenticated admin page.
     * - Delete optional contact within 30 days after resolution, and unresolved feedback within 90 days.
     */
    fun applyRetentionAndReviewerGuard(
        record: FeedbackSubmissionPayload,
        isReviewerAuthenticated: Boolean,
        daysSinceCreated: Int,
        daysSinceResolved: Int?
    ): Result<FeedbackSubmissionPayload?> {
        if (!isReviewerAuthenticated) {
            return Result.failure(SecurityException("Unauthenticated reviewer access denied (Section 8.8)."))
        }
        // Unresolved feedback deleted after 90 days
        if (record.reviewStatus != FeedbackReviewStatus.RESOLVED && daysSinceCreated > UNRESOLVED_FEEDBACK_RETENTION_DAYS) {
            return Result.success(null)
        }
        // Resolved feedback: scrub optionalContact after 30 days
        if (record.reviewStatus == FeedbackReviewStatus.RESOLVED &&
            daysSinceResolved != null &&
            daysSinceResolved > CONTACT_RETENTION_DAYS_AFTER_RESOLUTION
        ) {
            return Result.success(record.copy(optionalContact = null))
        }
        return Result.success(record)
    }
}
