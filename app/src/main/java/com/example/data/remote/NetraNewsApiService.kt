package com.example.data.remote

import com.example.domain.Article
import com.example.domain.ArticleRecord
import com.example.domain.CorrectionEntry
import com.example.domain.EditorialState
import com.example.domain.FeedEnvelope
import com.example.domain.FeedbackSubmissionPayload
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.SourceHealthRecord
import com.example.domain.VerificationStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Moshi DTOs matching the shared Netra News Feed Envelope Schema (v1) & Section 8 Feedback Contract.
 * KSP (`moshi-kotlin-codegen`) generates compile-time adapters via @JsonClass(generateAdapter = true).
 */

@JsonClass(generateAdapter = true)
data class CorrectionDto(
    @Json(name = "correctedAt") val correctedAt: String,
    @Json(name = "reason") val reason: String
) {
    fun toDomain(): CorrectionEntry = CorrectionEntry(
        correctedAtIso = correctedAt,
        reason = reason
    )
}

@JsonClass(generateAdapter = true)
data class ArticleDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "language") val language: String = "hi",
    @Json(name = "category") val category: String = "General",
    @Json(name = "scope") val scope: String = "national",
    @Json(name = "countryCode") val countryCode: String? = "IN",
    @Json(name = "stateCode") val stateCode: String? = null,
    @Json(name = "cityId") val cityId: String? = null,
    @Json(name = "publisherName") val publisherName: String,
    @Json(name = "sourceUrl") val sourceUrl: String,
    @Json(name = "publishedAt") val publishedAt: String? = null,
    @Json(name = "fetchedAt") val fetchedAt: Long = 0L,
    @Json(name = "verifiedAt") val verifiedAt: String? = null,
    @Json(name = "verificationCheckedAt") val verificationCheckedAt: String? = null,
    @Json(name = "verificationReason") val verificationReason: String? = null,
    @Json(name = "verificationStatus") val verificationStatus: String? = null,
    @Json(name = "rightsStatus") val rightsStatus: String? = null,
    @Json(name = "editorialState") val editorialState: String? = null,
    @Json(name = "permittedSummary") val permittedSummary: String? = null,
    @Json(name = "corrections") val corrections: List<CorrectionDto> = emptyList(),
    @Json(name = "expiry") val expiry: String? = null,
    @Json(name = "isBreakingOrLive") val isBreakingOrLive: Boolean = false
) {
    fun toArticleRecord(defaultFetchedAtMs: Long): ArticleRecord {
        val vStatus = VerificationStatus.fromWire(verificationStatus)
        val rStatus = RightsStatus.fromWire(rightsStatus)
        val eState = EditorialState.fromWire(editorialState)
        val resolvedFetchedAt = if (fetchedAt > 0L) fetchedAt else defaultFetchedAtMs
        return ArticleRecord(
            id = id,
            title = title,
            language = language,
            category = category,
            scope = NewsScope.fromWire(scope) ?: NewsScope.NATIONAL,
            countryCode = countryCode,
            stateCode = stateCode,
            cityId = cityId,
            publisherName = publisherName,
            canonicalSourceUrl = sourceUrl,
            publishedAtIso = publishedAt,
            fetchedAtEpochMs = resolvedFetchedAt,
            verifiedAtIso = verifiedAt ?: verificationCheckedAt,
            verificationCheckedAtIso = verificationCheckedAt ?: verifiedAt,
            verificationReason = verificationReason,
            verificationStatus = vStatus,
            rightsStatus = rStatus,
            editorialState = eState,
            permittedSummary = permittedSummary,
            corrections = corrections.map { it.toDomain() },
            expiryIso = expiry,
            isBreakingOrLive = isBreakingOrLive
        )
    }

    fun toArticle(defaultFetchedAtMs: Long): Article =
        Article.fromArticleRecord(toArticleRecord(defaultFetchedAtMs))
}

@JsonClass(generateAdapter = true)
data class SourceHealthDto(
    @Json(name = "sourceId") val sourceId: String,
    @Json(name = "publisherName") val publisherName: String,
    @Json(name = "host") val host: String,
    @Json(name = "status") val status: String,
    @Json(name = "lastChecked") val lastChecked: String? = null,
    @Json(name = "httpStatusCode") val httpStatusCode: Int? = null
) {
    fun toDomain(): SourceHealthRecord = SourceHealthRecord(
        sourceId = sourceId,
        publisherName = publisherName,
        host = host,
        status = status,
        lastCheckedIso = lastChecked,
        httpStatusCode = httpStatusCode
    )
}

@JsonClass(generateAdapter = true)
data class FeedEnvelopeDto(
    @Json(name = "schemaVersion") val schemaVersion: Int,
    @Json(name = "generatedAt") val generatedAt: String,
    @Json(name = "coverage") val coverage: List<String> = emptyList(),
    @Json(name = "items") val items: List<ArticleDto> = emptyList(),
    @Json(name = "sourceHealth") val sourceHealth: List<SourceHealthDto> = emptyList(),
    @Json(name = "nextCursor") val nextCursor: String? = null
) {
    fun toDomain(fetchedAtMs: Long): FeedEnvelope = FeedEnvelope(
        schemaVersion = schemaVersion,
        generatedAtUtcIso = generatedAt,
        coverage = coverage,
        items = items.map { it.toArticleRecord(fetchedAtMs) },
        sourceHealth = sourceHealth.map { it.toDomain() },
        nextCursor = nextCursor
    )
}

@JsonClass(generateAdapter = true)
data class FeedbackRequestDto(
    @Json(name = "feedbackId") val feedbackId: String,
    @Json(name = "idempotencyToken") val idempotencyToken: String,
    @Json(name = "articleId") val articleId: String?,
    @Json(name = "issueType") val issueType: String,
    @Json(name = "description") val description: String,
    @Json(name = "optionalComments") val optionalComments: String?,
    @Json(name = "optionalContact") val optionalContact: String?,
    @Json(name = "createdAtClientEpochMs") val createdAtClientEpochMs: Long
) {
    companion object {
        fun fromDomain(payload: FeedbackSubmissionPayload): FeedbackRequestDto = FeedbackRequestDto(
            feedbackId = payload.feedbackId,
            idempotencyToken = payload.idempotencyToken,
            articleId = payload.articleId,
            issueType = payload.issueType.wireKey,
            description = payload.description,
            optionalComments = payload.optionalComments,
            optionalContact = payload.optionalContact,
            createdAtClientEpochMs = payload.createdAtClientEpochMs
        )
    }
}

@JsonClass(generateAdapter = true)
data class FeedbackReceiptDto(
    @Json(name = "receiptId") val receiptId: String,
    @Json(name = "serverTimestamp") val serverTimestamp: String,
    @Json(name = "deduplicated") val deduplicated: Boolean = false
)

/**
 * Retrofit API Service for Netra News external HTTPS feeds and designated private feedback review endpoint.
 */
interface NetraNewsApiService {

    @GET("feeds/v1/{scope}.json")
    suspend fun fetchFeedEnvelope(
        @Path("scope") scope: String,
        @Query("cityId") cityId: String? = null,
        @Header("If-None-Match") ifNoneMatch: String? = null
    ): Response<FeedEnvelopeDto>

    @POST("api/v1/feedback")
    suspend fun submitFeedbackReport(
        @Header("X-Idempotency-Token") idempotencyToken: String,
        @Body request: FeedbackRequestDto
    ): Response<FeedbackReceiptDto>
}

/**
 * Configures Moshi, OkHttpClient, and Retrofit with MoshiConverterFactory for external HTTPS API calls.
 */
object NetraNetworkClient {

    /**
     * Default placeholder HTTPS base URL aligned with Section 1 & Section 8.9:
     * Live external feed & private Worker+D1 endpoints remain disconnected in Stage-1 until
     * owner verification and backend provisioning complete.
     */
    const val STAGE1_PLACEHOLDER_BASE_URL = "https://netra-news.example.invalid/"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun createApiService(baseUrl: String): NetraNewsApiService {
        require(baseUrl.startsWith("https://")) {
            "Security Policy (Sec 3.6): External API base URL must use HTTPS."
        }
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NetraNewsApiService::class.java)
    }
}
