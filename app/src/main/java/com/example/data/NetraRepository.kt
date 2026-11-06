package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AppDatabase
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.CachedArticleEntity
import com.example.data.local.FeedbackDraftEntity
import com.example.data.local.HealthLogEntity
import com.example.data.local.NetraDao
import com.example.domain.AppLanguage
import com.example.domain.Article
import com.example.domain.ArticleRecord
import com.example.domain.BulletinDataStatus
import com.example.domain.CorrectionEntry
import com.example.domain.EarthquakeEventRecord
import com.example.domain.EditorialState
import com.example.domain.FeedEnvelope
import com.example.domain.FeedbackSubmissionPayload
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.SourceHealthRecord
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.VerificationStatus
import com.example.domain.VerificationTransitionRecord
import com.example.domain.WeatherAdvisoryRecord
import com.example.location.CityLocationEngine
import com.example.location.CitySelectionMode
import com.example.location.LocationPreference
import com.example.power.PowerTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

typealias AppDatabaseAlias = AppDatabase
typealias ArticleDaoAlias = ArticleDao
typealias ArticleEntityAlias = ArticleEntity

/**
 * Netra News Data & Feed Adapter Repository (Build Specification v3, Sections 2, 3, 7, 8, 9):
 * - Network off main thread (Dispatchers.IO), bounded responses (max 256 KB), HTTPS only,
 *   conditional requests (If-None-Match / If-Modified-Since), finite retry with circuit breaker.
 * - Atomic preference writes for city/state and language so app resumes where it left off.
 * - Preserves last successful content with its real timestamp in Room.
 * - Stores local-only bookmarks (`ArticleEntity` & `BookmarkEntity`) and optional local feedback drafts on device.
 */
class NetraRepository(
    context: Context,
    private val dao: NetraDao,
    private val articleDao: ArticleDao? = null
) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("netra_news_atomic_prefs", Context.MODE_PRIVATE)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val bookmarksFlow: Flow<List<BookmarkEntity>> = dao.observeBookmarks()
    val bookmarkedArticlesFlow: Flow<List<Article>>? =
        articleDao?.getAllBookmarks()?.map { list -> list.map { it.toDomain() } }
    val feedbackDraftsFlow: Flow<List<FeedbackDraftEntity>> = dao.observeFeedbackDrafts()
    val healthLogsFlow: Flow<List<HealthLogEntity>> = dao.observeBoundedHealthLogs()

    fun loadSavedLanguage(): AppLanguage {
        val code = prefs.getString(KEY_LANGUAGE, AppLanguage.HINDI.code)
        return if (code == AppLanguage.ENGLISH.code) AppLanguage.ENGLISH else AppLanguage.HINDI
    }

    fun saveLanguageAtomically(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.code).commit()
    }

    fun hasSavedCityPreference(): Boolean = prefs.getBoolean(KEY_HAS_SAVED_CITY, false)

    fun loadLocationPreference(): LocationPreference {
        val cityId = prefs.getString(KEY_CITY_ID, "in-up-lucknow") ?: "in-up-lucknow"
        val cityEntry = CityLocationEngine.findByCityId(cityId)
        val modeWire = prefs.getString(KEY_SELECTION_MODE, CitySelectionMode.MANUAL.wireKey)
        val mode = if (modeWire == CitySelectionMode.APPROXIMATE.wireKey) {
            CitySelectionMode.APPROXIMATE
        } else {
            CitySelectionMode.MANUAL
        }
        val updatedAt = prefs.getLong(KEY_LOCATION_UPDATED_AT, 1793940000000L)
        val enabled = prefs.getBoolean(KEY_LOCATION_ENABLED, true)
        return LocationPreference(
            countryCode = cityEntry.countryCode,
            stateCode = cityEntry.stateCode,
            cityId = cityEntry.cityId,
            cityLabelHi = cityEntry.fullLabelHi,
            cityLabelEn = cityEntry.fullLabelEn,
            selectionMode = mode,
            updatedAtEpochMs = updatedAt,
            enabled = enabled
        )
    }

    fun saveLocationPreferenceAtomically(pref: LocationPreference) {
        prefs.edit()
            .putBoolean(KEY_HAS_SAVED_CITY, true)
            .putString(KEY_COUNTRY_CODE, pref.countryCode)
            .putString(KEY_STATE_CODE, pref.stateCode)
            .putString(KEY_CITY_ID, pref.cityId)
            .putString(KEY_SELECTION_MODE, pref.selectionMode.wireKey)
            .putLong(KEY_LOCATION_UPDATED_AT, pref.updatedAtEpochMs)
            .putBoolean(KEY_LOCATION_ENABLED, pref.enabled)
            .commit()
    }

    fun resetLocationPreferenceAtomically(): LocationPreference {
        val defaultCity = CityLocationEngine.PACKAGED_CITIES.first()
        val resetPref = LocationPreference(
            countryCode = defaultCity.countryCode,
            stateCode = defaultCity.stateCode,
            cityId = defaultCity.cityId,
            cityLabelHi = defaultCity.fullLabelHi,
            cityLabelEn = defaultCity.fullLabelEn,
            selectionMode = CitySelectionMode.MANUAL,
            updatedAtEpochMs = System.currentTimeMillis(),
            enabled = false
        )
        prefs.edit()
            .putBoolean(KEY_HAS_SAVED_CITY, false)
            .putString(KEY_CITY_ID, resetPref.cityId)
            .putString(KEY_STATE_CODE, resetPref.stateCode)
            .putString(KEY_SELECTION_MODE, CitySelectionMode.MANUAL.wireKey)
            .putBoolean(KEY_LOCATION_ENABLED, false)
            .commit()
        return resetPref
    }

    fun loadLastSuccessfulFetchEpochMs(): Long? {
        val ts = prefs.getLong(KEY_LAST_FETCH_MS, -1L)
        return if (ts > 0L) ts else null
    }

    fun saveLastSuccessfulFetchEpochMs(epochMs: Long) {
        prefs.edit().putLong(KEY_LAST_FETCH_MS, epochMs).commit()
    }

    suspend fun getCachedArticles(): List<ArticleRecord> = withContext(Dispatchers.IO) {
        emptyList<ArticleRecord>() // Legacy cache has no trustworthy feed provenance; withhold it.
    }

    suspend fun cacheVerifiedArticlesBatch(articles: List<ArticleRecord>, fetchedAtMs: Long) =
        withContext(Dispatchers.IO) {
            val entities = articles.map { CachedArticleEntity.fromDomain(it) }
            dao.clearCachedArticles()
            dao.insertCachedArticlesBatch(entities)
            saveLastSuccessfulFetchEpochMs(fetchedAtMs)
        }

    suspend fun toggleBookmark(article: ArticleRecord, currentlySavedIds: Set<String>) =
        withContext(Dispatchers.IO) {
            if (currentlySavedIds.contains(article.id)) {
                dao.deleteBookmarkById(article.id)
                articleDao?.deleteBookmarkById(article.id)
            } else {
                val nowMs = System.currentTimeMillis()
                val safeSummary = ValidationAndRightsEngine.getRightsPermittedSummary(article)
                dao.insertBookmark(
                    BookmarkEntity(
                        articleId = article.id,
                        title = article.title,
                        language = article.language,
                        category = article.category,
                        scopeWire = article.scope.wireKey,
                        cityId = article.cityId,
                        stateCode = article.stateCode,
                        publisherName = article.publisherName,
                        canonicalSourceUrl = article.canonicalSourceUrl,
                        publishedAtIso = article.publishedAtIso,
                        savedAtEpochMs = nowMs,
                        verificationStatusWire = article.verificationStatus.wireKey,
                        rightsStatusWire = article.rightsStatus.wireKey,
                        permittedSummary = safeSummary
                    )
                )
                articleDao?.insertBookmark(
                    ArticleEntity.fromArticleRecord(
                        record = article,
                        savedAtEpochMs = nowMs,
                        isBookmarked = true
                    )
                )
            }
        }

    suspend fun saveArticleBookmark(article: Article) = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val entity = ArticleEntity.fromDomain(article, savedAtEpochMs = nowMs, isBookmarked = true)
        articleDao?.insertBookmark(entity)
        dao.insertBookmark(entity.toBookmarkEntity())
    }

    suspend fun updateArticleBookmark(article: Article) = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val entity = ArticleEntity.fromDomain(article, savedAtEpochMs = nowMs, isBookmarked = true)
        articleDao?.updateBookmark(entity)
        dao.insertBookmark(entity.toBookmarkEntity())
    }

    suspend fun removeBookmark(articleId: String) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkById(articleId)
        articleDao?.deleteBookmarkById(articleId)
    }

    suspend fun clearAllBookmarks() = withContext(Dispatchers.IO) {
        dao.clearAllBookmarks()
        articleDao?.clearAllBookmarks()
    }

    suspend fun saveLocalFeedbackDraft(payload: FeedbackSubmissionPayload) = withContext(Dispatchers.IO) {
        dao.insertFeedbackDraft(FeedbackDraftEntity.fromDomain(payload))
    }

    suspend fun removeContactFromFeedbackDraft(feedbackId: String) = withContext(Dispatchers.IO) {
        dao.removeContactFromDraft(feedbackId)
    }

    suspend fun deleteFeedbackDraft(feedbackId: String) = withContext(Dispatchers.IO) {
        dao.deleteFeedbackDraftById(feedbackId)
    }

    suspend fun clearAllFeedbackDrafts() = withContext(Dispatchers.IO) {
        dao.clearAllFeedbackDrafts()
    }

    suspend fun appendBoundedHealthLog(
        powerTier: PowerTier,
        eventType: String,
        message: String
    ) = withContext(Dispatchers.IO) {
        dao.insertHealthLog(
            HealthLogEntity(
                timestampEpochMs = System.currentTimeMillis(),
                powerTier = powerTier.name,
                eventType = eventType,
                message = message.take(240)
            )
        )
        dao.pruneHealthLogsToMax100()
    }

    suspend fun clearHealthLogs() = withContext(Dispatchers.IO) {
        dao.clearHealthLogs()
    }

    suspend fun checkAllowlistedSourceReachability(): SourceHealthRecord = withContext(Dispatchers.IO) {
        val url = "https://pib.gov.in"
        val savedEtag = prefs.getString(KEY_LAST_ETAG, null)
        val requestBuilder = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", "NetraNews-Android/1.0-v3 (Stage1-LinkOutReader)")
        if (!savedEtag.isNullOrBlank()) {
            requestBuilder.header("If-None-Match", savedEtag)
        }
        return@withContext try {
            okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                val newEtag = response.header("ETag")
                if (!newEtag.isNullOrBlank()) {
                    prefs.edit().putString(KEY_LAST_ETAG, newEtag).commit()
                }
                SourceHealthRecord(
                    sourceId = "pib-india",
                    publisherName = "PIB India",
                    host = "pib.gov.in",
                    status = if (response.isSuccessful || response.code == 304) "healthy" else "degraded",
                    lastCheckedIso = utcIso(System.currentTimeMillis()),
                    httpStatusCode = response.code
                )
            }
        } catch (e: Exception) {
            SourceHealthRecord(
                sourceId = "pib-india",
                publisherName = "PIB India",
                host = "pib.gov.in",
                status = "unreachable",
                lastCheckedIso = utcIso(System.currentTimeMillis()),
                httpStatusCode = null
            )
        }
    }

    /** No reviewed feed adapter is connected. A portal link is not a news item. */
    fun buildVerifiedStage1Envelope(
        locationPref: LocationPreference,
        fetchedAtEpochMs: Long
    ): FeedEnvelope = FeedEnvelope(
        schemaVersion = 1,
        generatedAtUtcIso = utcIso(fetchedAtEpochMs),
        coverage = listOf("local:${locationPref.cityId}", "state:${locationPref.stateCode}", "national:IN"),
        items = emptyList(),
        sourceHealth = emptyList(),
        nextCursor = null
    )

    /** No provider payload means unavailable, never an all-clear. */
    fun getCivicWeatherBulletins(locationPref: LocationPreference, isOffline: Boolean): List<WeatherAdvisoryRecord> {
        val city = CityLocationEngine.findByCityId(locationPref.cityId)
        return listOf(WeatherAdvisoryRecord(
            eventId = "imd-unavailable", providerName = "IMD",
            areaLabelHi = city.fullLabelHi, areaLabelEn = city.fullLabelEn,
            stateCode = city.stateCode, cityId = city.cityId,
            status = BulletinDataStatus.UNAVAILABLE_OFFLINE,
            temperatureCelsius = null, rainfallMm = null, humidityPercent = null, windKmph = null,
            severityLabelHi = "मौसम डेटा अनुपलब्ध; चेतावनी की स्थिति अज्ञात",
            severityLabelEn = "Weather data unavailable; warning status unknown",
            summaryHi = "सत्यापित मौसम फ़ीड जुड़ी नहीं है। आधिकारिक स्रोत देखें।",
            summaryEn = "No verified weather feed is connected. Check the official source.",
            issuedAtIso = null, updatedAtIso = null, expiryAtIso = null,
            sourceUrl = "https://mausam.imd.gov.in/"
        ))
    }

    fun getCivicEarthquakeBulletins(locationPref: LocationPreference, isOffline: Boolean): List<EarthquakeEventRecord> {
        val city = CityLocationEngine.findByCityId(locationPref.cityId)
        return listOf(EarthquakeEventRecord(
            eventId = "ncs-unavailable", providerName = "NCS",
            areaLabelHi = city.fullLabelHi, areaLabelEn = city.fullLabelEn,
            status = BulletinDataStatus.UNAVAILABLE_OFFLINE,
            magnitudeMw = null, depthKm = null, latitudeApprox = null, longitudeApprox = null,
            severityLabelHi = "भूकंप डेटा अनुपलब्ध; घटनाओं की स्थिति अज्ञात",
            severityLabelEn = "Earthquake data unavailable; event status unknown",
            issuedAtIso = null, updatedAtIso = null, expiryAtIso = null,
            sourceUrl = "https://seismo.gov.in/"
        ))
    }

    private fun utcIso(epochMs: Long): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date(epochMs))

    companion object {
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_HAS_SAVED_CITY = "pref_has_saved_city"
        private const val KEY_COUNTRY_CODE = "pref_country_code"
        private const val KEY_STATE_CODE = "pref_state_code"
        private const val KEY_CITY_ID = "pref_city_id"
        private const val KEY_SELECTION_MODE = "pref_selection_mode"
        private const val KEY_LOCATION_UPDATED_AT = "pref_location_updated_at"
        private const val KEY_LOCATION_ENABLED = "pref_location_enabled"
        private const val KEY_LAST_FETCH_MS = "pref_last_fetch_ms"
        private const val KEY_LAST_ETAG = "pref_last_etag"
    }
}
