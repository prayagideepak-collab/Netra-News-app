package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Location Model & Packaged City Resolver per Sections 1.4, 1.5, 1.6, and 6.4:
 * - ACCESS_COARSE_LOCATION only if user chooses automatic city in foreground.
 * - Never request fine or background location.
 * - Timeout safely, derive a city via an approved free/local method, and DISCARD raw coordinates.
 * - No OpenCellID, Mozilla Location Services, paid geo API, or IP-based fallback profiling.
 * - Manual Country/State/City remains available on every failure.
 */

enum class CitySelectionMode(val wireKey: String, val labelHi: String, val labelEn: String) {
    MANUAL("manual", "मैनुअल चयन (Manual)", "Manual Selection"),
    APPROXIMATE("approximate", "अनुमानित स्थान (Coarse Foreground)", "Approximate (Coarse Foreground)")
}

/**
 * Persisted Location Preference per Section 6.4:
 * countryCode, stateCode, cityId, cityLabel, selectionMode=manual/approximate, updatedAt, enabled.
 * Raw coordinates are NEVER persisted.
 */
data class LocationPreference(
    val countryCode: String = "IN",
    val stateCode: String = "UP",
    val cityId: String = "in-up-lucknow",
    val cityLabelHi: String = "लखनऊ, उत्तर प्रदेश",
    val cityLabelEn: String = "Lucknow, Uttar Pradesh",
    val selectionMode: CitySelectionMode = CitySelectionMode.MANUAL,
    val updatedAtEpochMs: Long = 0L,
    val enabled: Boolean = true
)

data class PackagedCityEntry(
    val cityId: String,
    val countryCode: String,
    val stateCode: String,
    val stateNameHi: String,
    val stateNameEn: String,
    val cityNameHi: String,
    val cityNameEn: String,
    val centroidLat: Double,
    val centroidLon: Double
) {
    val fullLabelHi: String get() = "$cityNameHi, $stateNameHi"
    val fullLabelEn: String get() = "$cityNameEn, $stateNameEn"
}

sealed class CityResolutionResult {
    data class ResolvedSingleCity(
        val city: PackagedCityEntry,
        val distanceKm: Double
    ) : CityResolutionResult()

    data class AmbiguousNearestCities(
        val primaryCandidate: PackagedCityEntry,
        val secondaryCandidate: PackagedCityEntry,
        val primaryDistanceKm: Double,
        val secondaryDistanceKm: Double
    ) : CityResolutionResult()

    data class FallbackToManual(
        val reasonHi: String,
        val reasonEn: String
    ) : CityResolutionResult()
}

object CityLocationEngine {

    const val DATASET_NAME = "Netra Packaged Indian Civic Gazetteer (Derived from GeoNames Open Data)"
    const val DATASET_LICENCE = "CC-BY 4.0 (GeoNames.org Open Gazetteer Subset - No API Key Required)"
    const val DATASET_VERSION = "2026.11-stage1"
    const val COARSE_TIMEOUT_MS = 6_000L
    const val AMBIGUITY_DELTA_KM = 35.0
    const val MAX_MATCH_RADIUS_KM = 250.0

    val PACKAGED_CITIES: List<PackagedCityEntry> = listOf(
        // Uttar Pradesh
        PackagedCityEntry("in-up-lucknow", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "लखनऊ", "Lucknow", 26.8467, 80.9462),
        PackagedCityEntry("in-up-kanpur", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "कानपुर", "Kanpur", 26.4499, 80.3319),
        PackagedCityEntry("in-up-varanasi", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "वाराणसी", "Varanasi", 25.3176, 82.9739),
        PackagedCityEntry("in-up-prayagraj", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "प्रयागराज", "Prayagraj", 25.4358, 81.8463),
        PackagedCityEntry("in-up-agra", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "आगरा", "Agra", 27.1767, 78.0081),
        PackagedCityEntry("in-up-gorakhpur", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "गोरखपुर", "Gorakhpur", 26.7606, 83.3732),
        PackagedCityEntry("in-up-noida", "IN", "UP", "उत्तर प्रदेश", "Uttar Pradesh", "नोएडा (गौतम बुद्ध नगर)", "Noida", 28.5355, 77.3910),
        // Delhi NCR
        PackagedCityEntry("in-dl-newdelhi", "IN", "DL", "दिल्ली एनसीआर", "Delhi NCR", "नई दिल्ली", "New Delhi", 28.6139, 77.2090),
        // Bihar
        PackagedCityEntry("in-br-patna", "IN", "BR", "बिहार", "Bihar", "पटना", "Patna", 25.5941, 85.1376),
        PackagedCityEntry("in-br-gaya", "IN", "BR", "बिहार", "Bihar", "गया", "Gaya", 24.7914, 85.0002),
        // Madhya Pradesh
        PackagedCityEntry("in-mp-bhopal", "IN", "MP", "मध्य प्रदेश", "Madhya Pradesh", "भोपाल", "Bhopal", 23.2599, 77.4126),
        PackagedCityEntry("in-mp-indore", "IN", "MP", "मध्य प्रदेश", "Madhya Pradesh", "इंदौर", "Indore", 22.7196, 75.8577),
        // Rajasthan
        PackagedCityEntry("in-rj-jaipur", "IN", "RJ", "राजस्थान", "Rajasthan", "जयपुर", "Jaipur", 26.9124, 75.7873),
        PackagedCityEntry("in-rj-jodhpur", "IN", "RJ", "राजस्थान", "Rajasthan", "जोधपुर", "Jodhpur", 26.2389, 73.0243),
        // Uttarakhand
        PackagedCityEntry("in-uk-dehradun", "IN", "UK", "उत्तराखंड", "Uttarakhand", "देहरादून", "Dehradun", 30.3165, 78.0322),
        // Jharkhand & Chhattisgarh
        PackagedCityEntry("in-jh-ranchi", "IN", "JH", "झारखंड", "Jharkhand", "रांची", "Ranchi", 23.3441, 85.3096),
        PackagedCityEntry("in-cg-raipur", "IN", "CG", "छत्तीसगढ़", "Chhattisgarh", "रायपुर", "Raipur", 21.2514, 81.6296),
        // Maharashtra
        PackagedCityEntry("in-mh-mumbai", "IN", "MH", "महाराष्ट्र", "Maharashtra", "मुंबई", "Mumbai", 19.0760, 72.8777),
        PackagedCityEntry("in-mh-pune", "IN", "MH", "महाराष्ट्र", "Maharashtra", "पुणे", "Pune", 18.5204, 73.8567),
        // Gujarat, Karnataka, Tamil Nadu, West Bengal
        PackagedCityEntry("in-gj-ahmedabad", "IN", "GJ", "गुजरात", "Gujarat", "अहमदाबाद", "Ahmedabad", 23.0225, 72.5714),
        PackagedCityEntry("in-ka-bengaluru", "IN", "KA", "कर्नाटक", "Karnataka", "बेंगलुरु", "Bengaluru", 12.9716, 77.5946),
        PackagedCityEntry("in-tn-chennai", "IN", "TN", "तमिलनाडु", "Tamil Nadu", "चेन्नई", "Chennai", 13.0827, 80.2707),
        PackagedCityEntry("in-wb-kolkata", "IN", "WB", "पश्चिम बंगाल", "West Bengal", "कोलकाता", "Kolkata", 22.5726, 88.3639)
    )

    fun findByCityId(cityId: String?): PackagedCityEntry =
        PACKAGED_CITIES.firstOrNull { it.cityId == cityId } ?: PACKAGED_CITIES.first()

    fun getStates(): List<Pair<String, String>> =
        PACKAGED_CITIES.map { it.stateCode to "${it.stateNameHi} (${it.stateNameEn})" }.distinctBy { it.first }

    fun getCitiesForState(stateCode: String): List<PackagedCityEntry> =
        PACKAGED_CITIES.filter { it.stateCode.equals(stateCode, ignoreCase = true) }

    /**
     * Pure Haversine distance in kilometers between two coordinates.
     */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Resolves coarse coordinates to a packaged city and checks for nearest-city ambiguity (Section 6.4).
     * Does NOT store or log the input coordinates.
     */
    fun resolveCoordinatesToCity(coarseLat: Double, coarseLon: Double): CityResolutionResult {
        if (coarseLat.isNaN() || coarseLon.isNaN() || coarseLat !in -90.0..90.0 || coarseLon !in -180.0..180.0) {
            return CityResolutionResult.FallbackToManual(
                reasonHi = "अमान्य स्थान संकेत प्राप्त हुआ। कृपया अपना शहर मैनुअल रूप से चुनें।",
                reasonEn = "Invalid coarse coordinate received. Please select your city manually."
            )
        }

        val ranked = PACKAGED_CITIES.map { city ->
            city to haversineKm(coarseLat, coarseLon, city.centroidLat, city.centroidLon)
        }.sortedBy { it.second }

        val closest = ranked.firstOrNull()
            ?: return CityResolutionResult.FallbackToManual(
                reasonHi = "शहर डेटासेट खाली है। मैनुअल चयन का उपयोग करें।",
                reasonEn = "City dataset empty. Use manual selection."
            )

        if (closest.second > MAX_MATCH_RADIUS_KM) {
            return CityResolutionResult.FallbackToManual(
                reasonHi = "आपका अनुमानित स्थान समर्थित भारतीय शहरों की परिधि से बाहर है। कृपया मैनुअल रूप से शहर चुनें।",
                reasonEn = "Approximate location is outside supported city coverage radius. Please choose a city manually."
            )
        }

        val secondClosest = ranked.getOrNull(1)
        if (secondClosest != null && (secondClosest.second - closest.second) <= AMBIGUITY_DELTA_KM) {
            return CityResolutionResult.AmbiguousNearestCities(
                primaryCandidate = closest.first,
                secondaryCandidate = secondClosest.first,
                primaryDistanceKm = closest.second,
                secondaryDistanceKm = secondClosest.second
            )
        }

        return CityResolutionResult.ResolvedSingleCity(
            city = closest.first,
            distanceKm = closest.second
        )
    }

    fun hasCoarseLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Foreground-only approximate location resolution (Section 1.4, 1.5, 1.6):
     * - Checks ACCESS_COARSE_LOCATION permission.
     * - Uses LocationManager.NETWORK_PROVIDER (or PASSIVE_PROVIDER fallback) with a strict 6s timeout.
     * - Immediately zeroes out temporary coordinates in `finally` block and removes listener.
     */
    @SuppressLint("MissingPermission")
    suspend fun detectApproximateCityForeground(context: Context): CityResolutionResult {
        if (!hasCoarseLocationPermission(context)) {
            return CityResolutionResult.FallbackToManual(
                reasonHi = "अनुमानित स्थान (ACCESS_COARSE_LOCATION) अनुमति प्रदान नहीं की गई है। मैनुअल शहर चयन सक्रिय है।",
                reasonEn = "Approximate location (ACCESS_COARSE_LOCATION) permission denied. Manual city selection remains active."
            )
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return CityResolutionResult.FallbackToManual(
                reasonHi = "डिवाइस लोकेशन सर्विस अनुपलब्ध है। कृपया शहर मैनुअल रूप से चुनें।",
                reasonEn = "Device LocationManager unavailable. Please select city manually."
            )

        val isNetworkProviderEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }

        if (!isNetworkProviderEnabled) {
            return CityResolutionResult.FallbackToManual(
                reasonHi = "नेटवर्क स्थान प्रदाता (Location Provider) बंद है। समाचार पठन मैनुअल शहर के साथ जारी है।",
                reasonEn = "Network location provider is turned off. News continues with manual city selection."
            )
        }

        var tempLat = 0.0
        var tempLon = 0.0
        try {
            val lastKnown: Location? = try {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: Exception) {
                null
            }

            val freshOrCached: Location? = if (lastKnown != null &&
                (System.currentTimeMillis() - lastKnown.time) < 30L * 60L * 1000L
            ) {
                lastKnown
            } else {
                withTimeoutOrNull(COARSE_TIMEOUT_MS) {
                    suspendCancellableCoroutine { cont ->
                        val listener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                locationManager.removeUpdates(this)
                                if (cont.isActive) cont.resume(location)
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                            override fun onProviderEnabled(provider: String) {}
                            override fun onProviderDisabled(provider: String) {
                                locationManager.removeUpdates(this)
                                if (cont.isActive) cont.resume(null)
                            }
                        }
                        try {
                            locationManager.requestLocationUpdates(
                                LocationManager.NETWORK_PROVIDER,
                                0L,
                                0f,
                                listener,
                                Looper.getMainLooper()
                            )
                            cont.invokeOnCancellation {
                                locationManager.removeUpdates(listener)
                            }
                        } catch (e: Exception) {
                            locationManager.removeUpdates(listener)
                            if (cont.isActive) cont.resume(lastKnown)
                        }
                    }
                } ?: lastKnown
            }

            if (freshOrCached == null) {
                return CityResolutionResult.FallbackToManual(
                    reasonHi = "अनुमानित स्थान समय-सीमा (6 सेकंड) समाप्त। कृपया अपना राज्य व शहर मैनुअल रूप से चुनें।",
                    reasonEn = "Approximate location timed out safely (6s). Please select your State & City manually."
                )
            }

            tempLat = freshOrCached.latitude
            tempLon = freshOrCached.longitude
            return resolveCoordinatesToCity(tempLat, tempLon)
        } finally {
            // Explicitly scrub raw coordinates from memory immediately after deriving city ID
            tempLat = 0.0
            tempLon = 0.0
        }
    }
}
