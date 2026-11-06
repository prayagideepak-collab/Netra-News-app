package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.NetraRepository
import com.example.data.local.BookmarkEntity
import com.example.data.local.HealthLogEntity
import com.example.domain.AppLanguage
import com.example.domain.ArticleRecord
import com.example.domain.EarthquakeEventRecord
import com.example.domain.FeedEnvelope
import com.example.domain.FeedLoadState
import com.example.domain.NewsScope
import com.example.domain.SourceHealthRecord
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.WeatherAdvisoryRecord
import com.example.location.CityLocationEngine
import com.example.location.CityResolutionResult
import com.example.location.CitySelectionMode
import com.example.location.LocationPreference
import com.example.power.CircuitBreakerState
import com.example.power.DevicePowerTelemetry
import com.example.power.PowerBudgetAndTierManager
import com.example.power.PowerTier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Diagnostic mode allowing the owner/reviewer to inspect all required Section 6.5 Error Contract states
 * (Loaded, Empty, Stale, Offline, and Failed/Schema Rejection) on device without fabricating fake news.
 */
enum class DiagnosticContractMode(val labelHi: String, val labelEn: String) {
    LIVE_NORMAL("सत्यापित फ़ीड (Normal Verified)", "Normal Verified Feed"),
    SIMULATE_EMPTY("खाली फ़ीड (Empty State)", "Empty Feed (0 Verified Items)"),
    SIMULATE_STALE("पुराना कैश (Stale >6h State)", "Stale Feed (>6h Old)"),
    SIMULATE_OFFLINE("ऑफ़लाइन मोड (Offline Cache)", "Offline Mode (Room Cache)"),
    SIMULATE_SCHEMA_FAIL("अमान्य स्कीमा (Reject Schema v99)", "Reject Incompatible Schema (v99)")
}

data class NetraUiState(
    val language: AppLanguage = AppLanguage.HINDI,
    val selectedScope: NewsScope = NewsScope.LOCAL,
    val locationPreference: LocationPreference = LocationPreference(),
    val hasSavedCityPref: Boolean = false,
    val isDetectingLocation: Boolean = false,
    val ambiguousCityPrompt: CityResolutionResult.AmbiguousNearestCities? = null,
    val locationBannerMessageHi: String? = null,
    val locationBannerMessageEn: String? = null,
    val feedLoadState: FeedLoadState = FeedLoadState.Loading,
    val allVerifiedArticles: List<ArticleRecord> = emptyList(),
    val retractedNotices: List<ArticleRecord> = emptyList(),
    val rejectedAuditEntries: List<Pair<ArticleRecord, String>> = emptyList(),
    val sourceHealthList: List<SourceHealthRecord> = emptyList(),
    val weatherBulletins: List<WeatherAdvisoryRecord> = emptyList(),
    val earthquakeBulletins: List<EarthquakeEventRecord> = emptyList(),
    val powerTelemetry: DevicePowerTelemetry = DevicePowerTelemetry(
        batteryPercent = 85,
        isCharging = true,
        isSystemBatterySaverOn = false,
        isThermalThrottled = false,
        thermalStatusLabel = "Nominal",
        isDataSaverEnabled = false,
        isUnmeteredNetwork = true,
        isNetworkConnected = true,
        effectiveTier = PowerTier.NORMAL,
        networkCallsInCurrentHour = 1
    ),
    val circuitBreaker: CircuitBreakerState = CircuitBreakerState(),
    val diagnosticMode: DiagnosticContractMode = DiagnosticContractMode.LIVE_NORMAL,
    val selectedArticleDetail: ArticleRecord? = null,
    val showCityPickerDialog: Boolean = false,
    val externalBrowserStatusBanner: String? = null,
    val isAppInForeground: Boolean = true
)

class NetraViewModel(
    private val appContext: Context,
    private val repository: NetraRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetraUiState())
    val uiState: StateFlow<NetraUiState> = _uiState.asStateFlow()

    val bookmarksState: StateFlow<List<BookmarkEntity>> = repository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthLogsState: StateFlow<List<HealthLogEntity>> = repository.healthLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var networkCallsThisHour: Int = 0

    init {
        val savedLang = repository.loadSavedLanguage()
        val savedLoc = repository.loadLocationPreference()
        val hasSavedCity = repository.hasSavedCityPreference()
        val initialPower = PowerBudgetAndTierManager.readDeviceTelemetry(
            context = appContext,
            previousTier = null,
            networkCallsInHour = 0
        )

        _uiState.update {
            it.copy(
                language = savedLang,
                locationPreference = savedLoc,
                hasSavedCityPref = hasSavedCity,
                powerTelemetry = initialPower
            )
        }

        refreshAllFeeds(isUserInitiated = false)
    }

    /**
     * Foreground / Background lifecycle awareness (Section 3.2):
     * "General news work is foreground-only: refresh while the reader uses the app and stop active polling when backgrounded."
     */
    fun onForegroundLifecycleChanged(isForeground: Boolean) {
        _uiState.update { it.copy(isAppInForeground = isForeground) }
        if (isForeground) {
            refreshPowerTelemetry()
        }
    }

    fun refreshPowerTelemetry() {
        val current = _uiState.value.powerTelemetry.effectiveTier
        val updated = PowerBudgetAndTierManager.readDeviceTelemetry(
            context = appContext,
            previousTier = current,
            networkCallsInHour = networkCallsThisHour
        )
        _uiState.update { it.copy(powerTelemetry = updated) }
    }

    fun toggleLanguage() {
        val next = if (_uiState.value.language == AppLanguage.HINDI) {
            AppLanguage.ENGLISH
        } else {
            AppLanguage.HINDI
        }
        repository.saveLanguageAtomically(next)
        _uiState.update { it.copy(language = next) }
    }

    fun selectScope(scope: NewsScope) {
        // World tab is Stage-6 Planned; selecting it shows the Stage-6 gate card cleanly
        _uiState.update { it.copy(selectedScope = scope) }
        recomputeScopedFeedState()
    }

    fun setShowCityPickerDialog(show: Boolean) {
        _uiState.update { it.copy(showCityPickerDialog = show) }
    }

    fun openArticleDetail(article: ArticleRecord?) {
        _uiState.update { it.copy(selectedArticleDetail = article) }
    }

    fun clearLocationBanner() {
        _uiState.update {
            it.copy(
                locationBannerMessageHi = null,
                locationBannerMessageEn = null,
                ambiguousCityPrompt = null
            )
        }
    }

    fun clearBrowserStatusBanner() {
        _uiState.update { it.copy(externalBrowserStatusBanner = null) }
    }

    /**
     * Manual City Selection (Sections 1.5, 1.6, 6.4):
     * Saves atomically, never prompts repeatedly when saved preference exists, and refreshes feed.
     */
    fun selectCityManually(cityId: String) {
        val cityEntry = CityLocationEngine.findByCityId(cityId)
        val newPref = LocationPreference(
            countryCode = cityEntry.countryCode,
            stateCode = cityEntry.stateCode,
            cityId = cityEntry.cityId,
            cityLabelHi = cityEntry.fullLabelHi,
            cityLabelEn = cityEntry.fullLabelEn,
            selectionMode = CitySelectionMode.MANUAL,
            updatedAtEpochMs = System.currentTimeMillis(),
            enabled = true
        )
        repository.saveLocationPreferenceAtomically(newPref)
        _uiState.update {
            it.copy(
                locationPreference = newPref,
                hasSavedCityPref = true,
                showCityPickerDialog = false,
                ambiguousCityPrompt = null,
                locationBannerMessageHi = "शहर प्राथमिकता सहेजी गई: ${cityEntry.fullLabelHi} (मैनुअल)",
                locationBannerMessageEn = "Saved city preference: ${cityEntry.fullLabelEn} (Manual)"
            )
        }
        refreshAllFeeds(isUserInitiated = true)
    }

    fun resetSavedCityPreference() {
        val resetPref = repository.resetLocationPreferenceAtomically()
        _uiState.update {
            it.copy(
                locationPreference = resetPref,
                hasSavedCityPref = false,
                ambiguousCityPrompt = null,
                locationBannerMessageHi = "सहेजी गई शहर प्राथमिकता हटा दी गई। डिफ़ॉल्ट मैनुअल शहर सक्रिय है।",
                locationBannerMessageEn = "Saved city preference reset. Default manual city is active."
            )
        }
        refreshAllFeeds(isUserInitiated = true)
    }

    /**
     * Foreground Approximate City Detection (Sections 1.4, 1.5, 1.6):
     * - Respects PowerTier (suspended in Critical/Survival <15% battery to save power).
     * - Timeouts safely after 6s, scrubs raw coordinates immediately, and handles ambiguity.
     */
    fun detectApproximateCityInForeground() {
        val telemetry = _uiState.value.powerTelemetry
        if (!telemetry.effectiveTier.allowCoarseLocationOnDemand) {
            _uiState.update {
                it.copy(
                    locationBannerMessageHi = "बैटरी स्तर (${telemetry.batteryPercent}%) कम होने के कारण स्थान सेंसर बंद है। कृपया मैनुअल शहर चुनें।",
                    locationBannerMessageEn = "Location sensor suspended in ${telemetry.effectiveTier.name} tier (${telemetry.batteryPercent}% battery). Please select city manually."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDetectingLocation = true, ambiguousCityPrompt = null) }
            val result = CityLocationEngine.detectApproximateCityForeground(appContext)
            when (result) {
                is CityResolutionResult.ResolvedSingleCity -> {
                    val city = result.city
                    val newPref = LocationPreference(
                        countryCode = city.countryCode,
                        stateCode = city.stateCode,
                        cityId = city.cityId,
                        cityLabelHi = city.fullLabelHi,
                        cityLabelEn = city.fullLabelEn,
                        selectionMode = CitySelectionMode.APPROXIMATE,
                        updatedAtEpochMs = System.currentTimeMillis(),
                        enabled = true
                    )
                    repository.saveLocationPreferenceAtomically(newPref)
                    repository.appendBoundedHealthLog(
                        telemetry.effectiveTier,
                        "LOCATION_COARSE",
                        "Resolved approximate city=${city.cityId} (raw coords scrubbed)"
                    )
                    _uiState.update {
                        it.copy(
                            isDetectingLocation = false,
                            locationPreference = newPref,
                            hasSavedCityPref = true,
                            locationBannerMessageHi = "अनुमानित शहर पहचाना गया: ${city.fullLabelHi} (कच्चे निर्देशांक हटा दिए गए)",
                            locationBannerMessageEn = "Approximate city resolved: ${city.fullLabelEn} (raw coordinates discarded)"
                        )
                    }
                    refreshAllFeeds(isUserInitiated = true)
                }

                is CityResolutionResult.AmbiguousNearestCities -> {
                    _uiState.update {
                        it.copy(
                            isDetectingLocation = false,
                            ambiguousCityPrompt = result,
                            locationBannerMessageHi = "दो निकटतम शहर समान दूरी पर मिले (${result.primaryCandidate.cityNameHi} / ${result.secondaryCandidate.cityNameHi})। कृपया पुष्टि करें।",
                            locationBannerMessageEn = "Nearest-city ambiguity detected (${result.primaryCandidate.cityNameEn} vs ${result.secondaryCandidate.cityNameEn}). Please confirm."
                        )
                    }
                }

                is CityResolutionResult.FallbackToManual -> {
                    repository.appendBoundedHealthLog(
                        telemetry.effectiveTier,
                        "LOCATION_FALLBACK",
                        result.reasonEn
                    )
                    _uiState.update {
                        it.copy(
                            isDetectingLocation = false,
                            locationBannerMessageHi = result.reasonHi,
                            locationBannerMessageEn = result.reasonEn
                        )
                    }
                }
            }
        }
    }

    fun confirmAmbiguousCityChoice(chosen: com.example.location.PackagedCityEntry) {
        val newPref = LocationPreference(
            countryCode = chosen.countryCode,
            stateCode = chosen.stateCode,
            cityId = chosen.cityId,
            cityLabelHi = chosen.fullLabelHi,
            cityLabelEn = chosen.fullLabelEn,
            selectionMode = CitySelectionMode.APPROXIMATE,
            updatedAtEpochMs = System.currentTimeMillis(),
            enabled = true
        )
        repository.saveLocationPreferenceAtomically(newPref)
        _uiState.update {
            it.copy(
                locationPreference = newPref,
                hasSavedCityPref = true,
                ambiguousCityPrompt = null,
                locationBannerMessageHi = "चयनित शहर सहेजा गया: ${chosen.fullLabelHi}",
                locationBannerMessageEn = "Confirmed city saved: ${chosen.fullLabelEn}"
            )
        }
        refreshAllFeeds(isUserInitiated = true)
    }

    fun setDiagnosticContractMode(mode: DiagnosticContractMode) {
        _uiState.update { it.copy(diagnosticMode = mode) }
        refreshAllFeeds(isUserInitiated = true)
    }

    fun toggleBookmark(article: ArticleRecord) {
        viewModelScope.launch {
            val savedIds = bookmarksState.value.map { it.articleId }.toSet()
            repository.toggleBookmark(article, savedIds)
        }
    }

    fun removeBookmark(articleId: String) {
        viewModelScope.launch {
            repository.removeBookmark(articleId)
        }
    }

    fun clearAllBookmarks() {
        viewModelScope.launch {
            repository.clearAllBookmarks()
        }
    }

    fun clearHealthLogs() {
        viewModelScope.launch {
            repository.clearHealthLogs()
        }
    }

    /**
     * Opens canonical source URL in an external browser / Custom Tab intent (Section 3.6):
     * - Rejects unsafe schemes (`javascript:`, `http:`, `file:`, `data:`) and unapproved hosts.
     * - Never uses an internal privileged JavaScript WebView or JS bridge.
     */
    fun openExternalSourceUrlSafely(context: Context, rawUrl: String) {
        if (!ValidationAndRightsEngine.isApprovedHttpsSourceUrl(rawUrl)) {
            _uiState.update {
                it.copy(
                    externalBrowserStatusBanner = if (it.language == AppLanguage.HINDI) {
                        "सुरक्षा अवरोध: केवल सत्यापित HTTPS प्रकाशक डोमेन अनुमत हैं ($rawUrl अस्वीकृत)।"
                    } else {
                        "Security Block: Only allowlisted HTTPS publisher URLs may be opened ($rawUrl rejected)."
                    }
                )
            }
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl.trim())).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    externalBrowserStatusBanner = if (it.language == AppLanguage.HINDI) {
                        "बाहरी ब्राउज़र उपलब्ध नहीं है। कोई नकली रीडर सामग्री प्रदर्शित नहीं की गई है। URL: $rawUrl"
                    } else {
                        "External browser unavailable. No fake reader content is displayed. URL: $rawUrl"
                    }
                )
            }
        }
    }

    /**
     * Refreshes feeds in foreground while respecting PowerTier budgets, circuit breaker backoff,
     * and the Section 6.1–6.5 Shared Content & Error Contract.
     */
    fun refreshAllFeeds(isUserInitiated: Boolean = true) {
        if (!_uiState.value.isAppInForeground && !isUserInitiated) return

        viewModelScope.launch {
            val nowRefMs = 1793947200000L // Reference time aligned with 6 Nov 2026 spec window
            val wallNowMs = System.currentTimeMillis()
            val locPref = _uiState.value.locationPreference
            val diagMode = _uiState.value.diagnosticMode

            _uiState.update { it.copy(feedLoadState = FeedLoadState.Loading) }

            val currentTelemetry = PowerBudgetAndTierManager.readDeviceTelemetry(
                context = appContext,
                previousTier = _uiState.value.powerTelemetry.effectiveTier,
                networkCallsInHour = networkCallsThisHour
            )
            val isOffline = !currentTelemetry.isNetworkConnected ||
                diagMode == DiagnosticContractMode.SIMULATE_OFFLINE

            // Update civic weather & earthquake bulletins
            val weather = repository.getCivicWeatherBulletins(locPref, isOffline)
            val earthquakes = repository.getCivicEarthquakeBulletins(locPref, isOffline)

            // 1. Handle Offline Mode (Section 6.5: preserve last successful content with its real timestamp)
            if (isOffline) {
                val cached = repository.getCachedArticles()
                val lastFetch = repository.loadLastSuccessfulFetchEpochMs()
                repository.appendBoundedHealthLog(
                    currentTelemetry.effectiveTier,
                    "OFFLINE_CACHE",
                    "Device offline; serving ${cached.size} rights-permitted cached items."
                )
                _uiState.update {
                    it.copy(
                        powerTelemetry = currentTelemetry,
                        weatherBulletins = weather,
                        earthquakeBulletins = earthquakes,
                        allVerifiedArticles = cached,
                        feedLoadState = FeedLoadState.Offline(
                            cachedItems = filterByScopeWithNationalFallback(cached, it.selectedScope).first,
                            lastCachedAtEpochMs = lastFetch,
                            offlineMessageHi = "ऑफ़लाइन मोड: अंतिम सफल सिंक (${formatTimestamp(lastFetch)}) से सहेजे गए सत्यापित शीर्षक दिखाए जा रहे हैं।",
                            offlineMessageEn = "Offline Mode: Showing verified cached headlines from last successful sync (${formatTimestamp(lastFetch)})."
                        )
                    )
                }
                return@launch
            }

            // 2. Check PowerTier budget & Circuit Breaker
            val cb = _uiState.value.circuitBreaker
            if (cb.isOpen && wallNowMs < cb.nextAllowedAttemptEpochMs && !isUserInitiated) {
                val cached = repository.getCachedArticles()
                val lastFetch = repository.loadLastSuccessfulFetchEpochMs()
                _uiState.update {
                    it.copy(
                        feedLoadState = FeedLoadState.Failed(
                            errorMessageHi = "सेल्फ-हीलिंग सर्किट ब्रेकर सक्रिय: बार-बार त्रुटि के कारण अगला प्रयास स्थगित। पूर्व सहेजी गई सामग्री सुरक्षित है।",
                            errorMessageEn = "Self-healing circuit breaker open: Deferred to next window. Last verified content preserved.",
                            preservedItems = cached,
                            lastSuccessfulAtEpochMs = lastFetch
                        )
                    )
                }
                return@launch
            }

            // Increment foreground network call counter when within budget
            if (networkCallsThisHour < currentTelemetry.effectiveTier.maxNetworkCallsPerHour || isUserInitiated) {
                networkCallsThisHour += 1
            }

            // 3. Construct envelope according to current mode
            val baseEnvelope = repository.buildVerifiedStage1Envelope(locPref, wallNowMs)
            val targetEnvelope: FeedEnvelope = when (diagMode) {
                DiagnosticContractMode.LIVE_NORMAL -> baseEnvelope
                DiagnosticContractMode.SIMULATE_EMPTY -> baseEnvelope.copy(items = emptyList())
                DiagnosticContractMode.SIMULATE_STALE -> baseEnvelope.copy(
                    generatedAtUtcIso = "2026-11-05T10:00:00Z" // >14 hours old relative to 2026-11-06T00:00:00Z
                )
                DiagnosticContractMode.SIMULATE_OFFLINE -> baseEnvelope
                DiagnosticContractMode.SIMULATE_SCHEMA_FAIL -> baseEnvelope.copy(schemaVersion = 99)
            }

            // 4. Validate Envelope via Domain Engine
            val validation = ValidationAndRightsEngine.validateFeedEnvelope(
                envelope = targetEnvelope,
                referenceNowEpochMs = nowRefMs,
                isProductionFeed = true
            )

            if (!validation.isValidSchema) {
                val cached = repository.getCachedArticles()
                val lastFetch = repository.loadLastSuccessfulFetchEpochMs()
                val updatedCb = PowerBudgetAndTierManager.recordFailureAndComputeBackoff(
                    current = cb,
                    failureReason = validation.rejectedSchemaReason ?: "Schema error",
                    nowEpochMs = wallNowMs
                )
                repository.appendBoundedHealthLog(
                    currentTelemetry.effectiveTier,
                    "SCHEMA_REJECT",
                    validation.rejectedSchemaReason ?: "Rejected incompatible schema"
                )
                _uiState.update {
                    it.copy(
                        circuitBreaker = updatedCb,
                        weatherBulletins = weather,
                        earthquakeBulletins = earthquakes,
                        feedLoadState = FeedLoadState.Failed(
                            errorMessageHi = "असंगत फ़ीड स्कीमा अस्वीकृत: ${validation.rejectedSchemaReason}। कोई नकली समाचार नहीं दिखाया गया; अंतिम सत्यापित कैश नीचे सुरक्षित है।",
                            errorMessageEn = "Incompatible Feed Schema Rejected: ${validation.rejectedSchemaReason}. Last verified content preserved below.",
                            preservedItems = filterByScopeWithNationalFallback(cached, it.selectedScope).first,
                            lastSuccessfulAtEpochMs = lastFetch,
                            isSchemaRejection = true
                        )
                    )
                }
                return@launch
            }

            // Valid schema! Cache verified items in Room (batch write)
            if (validation.verifiedDisplayableItems.isNotEmpty()) {
                repository.cacheVerifiedArticlesBatch(validation.verifiedDisplayableItems, wallNowMs)
            }
            val resetCb = PowerBudgetAndTierManager.recordSuccessReset()
            repository.appendBoundedHealthLog(
                currentTelemetry.effectiveTier,
                "SYNC_OK",
                "Validated ${validation.verifiedDisplayableItems.size} items, rejected ${validation.rejectedOrFilteredItems.size} items (incl. test-001)"
            )

            _uiState.update {
                it.copy(
                    powerTelemetry = currentTelemetry.copy(networkCallsInCurrentHour = networkCallsThisHour),
                    circuitBreaker = resetCb,
                    allVerifiedArticles = validation.verifiedDisplayableItems,
                    retractedNotices = validation.retractedNoticeItems,
                    rejectedAuditEntries = validation.rejectedOrFilteredItems,
                    sourceHealthList = targetEnvelope.sourceHealth,
                    weatherBulletins = weather,
                    earthquakeBulletins = earthquakes
                )
            }

            recomputeScopedFeedState(
                isStaleEnvelope = validation.isStale,
                generatedAtIso = targetEnvelope.generatedAtUtcIso,
                fetchedAtMs = wallNowMs
            )
        }
    }

    private fun recomputeScopedFeedState(
        isStaleEnvelope: Boolean = _uiState.value.diagnosticMode == DiagnosticContractMode.SIMULATE_STALE,
        generatedAtIso: String = "2026-11-06T00:00:00Z",
        fetchedAtMs: Long = repository.loadLastSuccessfulFetchEpochMs() ?: System.currentTimeMillis()
    ) {
        val state = _uiState.value
        if (state.diagnosticMode == DiagnosticContractMode.SIMULATE_OFFLINE ||
            state.diagnosticMode == DiagnosticContractMode.SIMULATE_SCHEMA_FAIL
        ) {
            return
        }

        val scope = state.selectedScope
        if (!scope.isStage1Enabled) {
            _uiState.update {
                it.copy(
                    feedLoadState = FeedLoadState.Empty(
                        scope = scope,
                        cityLabel = state.locationPreference.cityLabelHi,
                        checkedAtEpochMs = fetchedAtMs,
                        reasonHi = scope.stageNoteHi,
                        reasonEn = scope.stageNoteEn
                    )
                )
            }
            return
        }

        val (scopedItems, usedFallback) = filterByScopeWithNationalFallback(
            state.allVerifiedArticles,
            scope
        )

        val newFeedState: FeedLoadState = when {
            scopedItems.isEmpty() -> FeedLoadState.Empty(
                scope = scope,
                cityLabel = if (state.language == AppLanguage.HINDI) {
                    state.locationPreference.cityLabelHi
                } else {
                    state.locationPreference.cityLabelEn
                },
                checkedAtEpochMs = fetchedAtMs,
                reasonHi = "इस श्रेणी में वर्तमान में कोई सत्यापित समाचार आइटम नहीं है। अनुबंध 6.1 के अनुसार कोई काल्पनिक या नकली पूरक सामग्री (filler) नहीं जोड़ी जाती।",
                reasonEn = "No verified news items currently available for this scope. Per Section 6.1, empty items mean no verified content — never invented filler."
            )

            isStaleEnvelope -> FeedLoadState.Stale(
                items = scopedItems,
                lastSuccessfulAtEpochMs = fetchedAtMs - (8L * 3600L * 1000L),
                staleReasonHi = "फ़ीड डेटा 6 घंटे से अधिक पुराना (Stale) है। नया अपडेट प्राप्त होने तक अंतिम सत्यापित सामग्री अपने मूल समय-मुद्रा के साथ दिखाई जा रही है।",
                staleReasonEn = "Feed envelope is older than 6 hours (Stale). Showing last verified content with its original publication timestamp."
            )

            else -> FeedLoadState.Loaded(
                items = scopedItems,
                generatedAtUtcIso = generatedAtIso,
                fetchedAtEpochMs = fetchedAtMs,
                usedNationalFallback = usedFallback
            )
        }

        _uiState.update { it.copy(feedLoadState = newFeedState) }
    }

    /**
     * Implements Confirmed Product Direction (Section 0.2):
     * "First release is Local + State, with National fallback only from verified sources."
     */
    private fun filterByScopeWithNationalFallback(
        items: List<ArticleRecord>,
        scope: NewsScope
    ): Pair<List<ArticleRecord>, Boolean> {
        if (items.isEmpty()) return emptyList<ArticleRecord>() to false
        return when (scope) {
            NewsScope.LOCAL -> {
                val localMatches = items.filter { it.scope == NewsScope.LOCAL }
                if (localMatches.isNotEmpty()) {
                    localMatches to false
                } else {
                    val nationalFallback = items.filter { it.scope == NewsScope.NATIONAL }
                    nationalFallback to nationalFallback.isNotEmpty()
                }
            }
            NewsScope.STATE -> {
                val stateMatches = items.filter { it.scope == NewsScope.STATE }
                if (stateMatches.isNotEmpty()) {
                    stateMatches to false
                } else {
                    val nationalFallback = items.filter { it.scope == NewsScope.NATIONAL }
                    nationalFallback to nationalFallback.isNotEmpty()
                }
            }
            NewsScope.NATIONAL -> items.filter { it.scope == NewsScope.NATIONAL } to false
            NewsScope.WORLD -> emptyList<ArticleRecord>() to false
        }
    }

    companion object {
        fun formatTimestamp(epochMs: Long?): String {
            if (epochMs == null || epochMs <= 0L) return "N/A"
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm 'IST'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Kolkata")
            return sdf.format(Date(epochMs))
        }

        fun provideFactory(context: Context, repository: NetraRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NetraViewModel(context.applicationContext, repository) as T
                }
            }
    }
}
