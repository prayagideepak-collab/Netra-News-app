package com.example.domain

/**
 * Core Domain Data Class 'Article' (Build Specification v3, Sections 2.1, 3.5 & 7.1):
 * Represents a verified or status-labelled news article with mandatory provenance,
 * geographic scope, source attribution, timestamps, verificationStatus, and rightsStatus.
 */
data class Article(
    val id: String,
    val title: String,
    val language: String,
    val category: String,
    val scope: NewsScope,
    val cityId: String?,
    val publisherName: String,
    val sourceUrl: String,
    val publishedAt: String?,
    val fetchedAt: Long,
    val verificationStatus: VerificationStatus,
    val rightsStatus: RightsStatus,
    val stateCode: String? = null,
    val countryCode: String? = "IN",
    val permittedSummary: String? = null,
    val verificationCheckedAt: String? = publishedAt,
    val verificationReason: String? = null,
    val editorialState: EditorialState = EditorialState.ACTIVE
) {
    /**
     * Secondary convenience constructor accepting wire strings for scope, verificationStatus, and rightsStatus.
     */
    constructor(
        id: String,
        title: String,
        language: String,
        category: String,
        scope: String,
        cityId: String?,
        publisherName: String,
        sourceUrl: String,
        publishedAt: String?,
        fetchedAt: Long,
        verificationStatus: VerificationStatus,
        rightsStatus: String
    ) : this(
        id = id,
        title = title,
        language = language,
        category = category,
        scope = NewsScope.fromWire(scope) ?: NewsScope.NATIONAL,
        cityId = cityId,
        publisherName = publisherName,
        sourceUrl = sourceUrl,
        publishedAt = publishedAt,
        fetchedAt = fetchedAt,
        verificationStatus = verificationStatus,
        rightsStatus = RightsStatus.fromWire(rightsStatus)
    )

    /**
     * Enforces Section 2.2 & Section 7.4 rights and verification rules:
     * Summaries are only exposed when rightsStatus is ORIGINAL or LICENSED
     * and verificationStatus is not BLOCKED and editorialState is not RETRACTED.
     */
    val safePermittedSummary: String?
        get() = if (
            rightsStatus == RightsStatus.LINK_ONLY ||
            rightsStatus == RightsStatus.BLOCKED ||
            verificationStatus == VerificationStatus.BLOCKED ||
            editorialState == EditorialState.RETRACTED
        ) {
            null
        } else {
            permittedSummary?.takeIf { it.isNotBlank() }
        }

    /**
     * Converts this [Article] domain object to the extended [ArticleRecord] contract.
     */
    fun toArticleRecord(): ArticleRecord = ArticleRecord(
        id = id,
        title = title,
        language = language,
        category = category,
        scope = scope,
        countryCode = countryCode,
        stateCode = stateCode,
        cityId = cityId,
        publisherName = publisherName,
        canonicalSourceUrl = sourceUrl,
        publishedAtIso = publishedAt,
        fetchedAtEpochMs = fetchedAt,
        verifiedAtIso = verificationCheckedAt,
        verificationCheckedAtIso = verificationCheckedAt,
        verificationReason = verificationReason,
        verificationStatus = verificationStatus,
        rightsStatus = rightsStatus,
        editorialState = editorialState,
        permittedSummary = safePermittedSummary
    )

    companion object {
        /**
         * Creates an [Article] domain object from an [ArticleRecord].
         */
        fun fromArticleRecord(record: ArticleRecord): Article = Article(
            id = record.id,
            title = record.title,
            language = record.language,
            category = record.category,
            scope = record.scope,
            cityId = record.cityId,
            publisherName = record.publisherName,
            sourceUrl = record.canonicalSourceUrl,
            publishedAt = record.publishedAtIso,
            fetchedAt = record.fetchedAtEpochMs,
            verificationStatus = record.verificationStatus,
            rightsStatus = record.rightsStatus,
            stateCode = record.stateCode,
            countryCode = record.countryCode,
            permittedSummary = record.permittedSummary,
            verificationCheckedAt = record.verificationCheckedAtIso,
            verificationReason = record.verificationReason,
            editorialState = record.editorialState
        )
    }
}
