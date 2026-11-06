package com.example.power

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlin.math.min
import kotlin.random.Random

/**
 * Core Design Principle — Ultra-Low Power, Reliable 24/7 Operation & Self-Healing Manager.
 *
 * Priority Order (conflicts resolved top-down):
 * 1. Device safety and Android/Play policy compliance
 * 2. Minimum battery consumption
 * 3. Reliable essential functionality (Tier-0)
 * 4. Automatic recovery and state restoration
 * 5. Performance and advanced features
 */
enum class PowerTier(
    val levelIndex: Int,
    val labelHi: String,
    val labelEn: String,
    val badgeColorHex: Long,
    val maxWakeupsPerHour: Int,
    val maxNetworkCallsPerHour: Int,
    val allowAnimations: Boolean,
    val allowCoarseLocationOnDemand: Boolean,
    val allowManualRefresh: Boolean,
    val descriptionHi: String,
    val descriptionEn: String
) {
    NORMAL(
        levelIndex = 0,
        labelHi = "सामान्य (Normal Tier)",
        labelEn = "Normal (>30% / Charging)",
        badgeColorHex = 0xFF15803D,
        maxWakeupsPerHour = 0, // Stage 1 news is strictly foreground-only (0 background wakeups)
        maxNetworkCallsPerHour = 12,
        allowAnimations = true,
        allowCoarseLocationOnDemand = true,
        allowManualRefresh = true,
        descriptionHi = "बैटरी >30% या चार्जिंग: पूर्ण पठन सुविधाएँ व बैच्ड फ़ोरग्राउंड सिंक।",
        descriptionEn = "Battery >30% or Charging: Full reader features & batched foreground sync."
    ),
    SAVER(
        levelIndex = 1,
        labelHi = "बैटरी सेवर (Saver Tier)",
        labelEn = "Saver (15–30% / Battery Saver)",
        badgeColorHex = 0xFFB45309,
        maxWakeupsPerHour = 0,
        maxNetworkCallsPerHour = 6,
        allowAnimations = false,
        allowCoarseLocationOnDemand = true,
        allowManualRefresh = true,
        descriptionHi = "बैटरी 15–30% या सेवर चालू: गैर-आवश्यक सिंक बंद, कम रेंडरिंग व लंबा अंतराल।",
        descriptionEn = "Battery 15–30% or Battery Saver on: Longer intervals, no non-essential sync, reduced rendering."
    ),
    CRITICAL(
        levelIndex = 2,
        labelHi = "क्रिटिकल (Critical <15%)",
        labelEn = "Critical (<15% Battery)",
        badgeColorHex = 0xFFB91C1C,
        maxWakeupsPerHour = 0,
        maxNetworkCallsPerHour = 3,
        allowAnimations = false,
        allowCoarseLocationOnDemand = false,
        allowManualRefresh = true,
        descriptionHi = "बैटरी <15%: केवल Tier-0 आवश्यक पठन व कैश, स्वचालित स्थान सेंसर बंद।",
        descriptionEn = "Battery <15%: Tier-0 essential reader & cache only, location sensors suspended."
    ),
    SURVIVAL(
        levelIndex = 3,
        labelHi = "सर्वाइवल (Survival <5%)",
        labelEn = "Survival (<5% Battery)",
        badgeColorHex = 0xFF7F1D1D,
        maxWakeupsPerHour = 0,
        maxNetworkCallsPerHour = 1,
        allowAnimations = false,
        allowCoarseLocationOnDemand = false,
        allowManualRefresh = false,
        descriptionHi = "बैटरी <5%: न्यूनतम Tier-0 ऑफ़लाइन कैश पठन, डार्क OLED मोड, कोई सेंसर या एनिमेशन नहीं।",
        descriptionEn = "Battery <5%: Minimal Tier-0 cached reading, OLED dark mode, no sensors/animations, network only on explicit override."
    );

    fun dropOneTierForThermal(): PowerTier {
        return entries.getOrElse(levelIndex + 1) { SURVIVAL }
    }
}

data class DevicePowerTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isSystemBatterySaverOn: Boolean,
    val isThermalThrottled: Boolean,
    val thermalStatusLabel: String,
    val isDataSaverEnabled: Boolean,
    val isUnmeteredNetwork: Boolean,
    val isNetworkConnected: Boolean,
    val effectiveTier: PowerTier,
    val networkCallsInCurrentHour: Int,
    val backgroundWakeupsInCurrentHour: Int = 0, // Strictly 0 in Stage-1 foreground reader
    val idleDrainTarget24hPercent: Double = 1.0,
    val survival3hDrainTargetPercent: Double = 0.8
)

/**
 * Self-Healing Circuit Breaker State (without battery drain).
 * Restarts with exponential backoff and jitter, hard retry cap (3), and circuit breaker
 * that stops recovery attempts and defers to the next scheduled window.
 */
data class CircuitBreakerState(
    val consecutiveFailures: Int = 0,
    val maxRetryCap: Int = 3,
    val isOpen: Boolean = false,
    val nextAllowedAttemptEpochMs: Long = 0L,
    val lastFailureReason: String? = null
)

object PowerBudgetAndTierManager {

    const val HYSTERESIS_PERCENT = 2 // 2% hysteresis buffer to prevent tier flapping
    const val MAX_RETRY_CAP = 3
    private const val BASE_BACKOFF_MS = 4_000L
    private const val CIRCUIT_OPEN_DEFER_WINDOW_MS = 15L * 60L * 1000L // Defer 15 mins when circuit trips

    val TIER_0_ESSENTIAL_FUNCTIONS: List<Pair<String, String>> = listOf(
        "1. सत्यापित समाचार शीर्षक व स्रोत लिंक पठन (ऑफ़लाइन Room कैश सहित)" to
            "1. Verified News Headline & Source Link-Out Reading (including Offline Room Cache)",
        "2. मैनुअल देश/राज्य/शहर चयन व स्थानीय बुकमार्क सुरक्षा" to
            "2. Manual Country/State/City Selection & Local-Only Saved Bookmarks",
        "3. आधिकारिक मौसम (IMD) एवं भूकंप (NCS) स्रोत बुलेटिन व समय-सीमा जांच" to
            "3. Official Weather (IMD) & Earthquake (NCS) Source Bulletins & Stale/Expiry Verification",
        "4. सुरक्षित बाहरी HTTPS ब्राउज़र लिंक-आउट (बिना जावास्क्रिप्ट ब्रिज या वेबव्यू के)" to
            "4. Safe External HTTPS Browser Link-Out (Zero WebView JS Bridge)"
    )

    /**
     * Pure calculation of PowerTier with 2% hysteresis and thermal drop rule.
     * Directly unit-testable at 100%, 15%, 5%, Battery Saver, and Thermal Throttling.
     */
    fun computeAdaptiveTier(
        batteryPercent: Int,
        isCharging: Boolean,
        isBatterySaverOn: Boolean,
        isThermalThrottled: Boolean,
        previousTier: PowerTier? = null
    ): PowerTier {
        val rawTier = when {
            batteryPercent < 5 -> PowerTier.SURVIVAL
            batteryPercent < 15 -> {
                if (previousTier == PowerTier.SURVIVAL && batteryPercent < (5 + HYSTERESIS_PERCENT)) {
                    PowerTier.SURVIVAL
                } else {
                    PowerTier.CRITICAL
                }
            }
            isCharging && batteryPercent >= 15 -> PowerTier.NORMAL
            batteryPercent <= 30 || isBatterySaverOn -> {
                if (previousTier == PowerTier.CRITICAL && batteryPercent < (15 + HYSTERESIS_PERCENT)) {
                    PowerTier.CRITICAL
                } else {
                    PowerTier.SAVER
                }
            }
            else -> {
                if (previousTier == PowerTier.SAVER && !isBatterySaverOn && batteryPercent < (30 + HYSTERESIS_PERCENT)) {
                    PowerTier.SAVER
                } else {
                    PowerTier.NORMAL
                }
            }
        }

        // Rule: "Thermal throttling: drop one tier immediately."
        return if (isThermalThrottled) {
            rawTier.dropOneTierForThermal()
        } else {
            rawTier
        }
    }

    /**
     * Computes exponential backoff with jitter and trips the circuit breaker after MAX_RETRY_CAP.
     */
    fun recordFailureAndComputeBackoff(
        current: CircuitBreakerState,
        failureReason: String,
        nowEpochMs: Long,
        randomJitterMs: Long = Random.nextLong(250L, 1250L)
    ): CircuitBreakerState {
        val nextFailures = current.consecutiveFailures + 1
        if (nextFailures >= current.maxRetryCap) {
            return current.copy(
                consecutiveFailures = nextFailures,
                isOpen = true,
                nextAllowedAttemptEpochMs = nowEpochMs + CIRCUIT_OPEN_DEFER_WINDOW_MS,
                lastFailureReason = "Circuit Breaker OPEN after $nextFailures failures: $failureReason"
            )
        }
        val exponentialDelay = BASE_BACKOFF_MS * (1L shl min(nextFailures, 4))
        val totalDelay = exponentialDelay + randomJitterMs
        return current.copy(
            consecutiveFailures = nextFailures,
            isOpen = false,
            nextAllowedAttemptEpochMs = nowEpochMs + totalDelay,
            lastFailureReason = failureReason
        )
    }

    fun recordSuccessReset(): CircuitBreakerState = CircuitBreakerState()

    /**
     * Reads current device battery, saver, thermal, and connectivity state using event/sticky snapshots
     * (no polling loops or wake locks).
     */
    fun readDeviceTelemetry(
        context: Context,
        previousTier: PowerTier?,
        networkCallsInHour: Int
    ): DevicePowerTelemetry {
        val batteryIntent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            85
        }
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatterySaver = powerManager?.isPowerSaveMode == true

        var isThermalThrottled = false
        var thermalLabel = "Nominal"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            val thermalStatus = powerManager.currentThermalStatus
            isThermalThrottled = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE
            thermalLabel = when (thermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "None (Cool)"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate (Throttling -1 Tier)"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe (Throttling -1 Tier)"
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Critical/Emergency"
                else -> "Nominal"
            }
        }

        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connManager?.activeNetwork
        val capabilities = activeNetwork?.let { connManager.getNetworkCapabilities(it) }
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val isUnmetered = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        val isDataSaver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && connManager != null) {
            connManager.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        } else {
            false
        }

        val tier = computeAdaptiveTier(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            isBatterySaverOn = isBatterySaver,
            isThermalThrottled = isThermalThrottled,
            previousTier = previousTier
        )

        return DevicePowerTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            isSystemBatterySaverOn = isBatterySaver,
            isThermalThrottled = isThermalThrottled,
            thermalStatusLabel = thermalLabel,
            isDataSaverEnabled = isDataSaver,
            isUnmeteredNetwork = isUnmetered,
            isNetworkConnected = isConnected,
            effectiveTier = tier,
            networkCallsInCurrentHour = networkCallsInHour
        )
    }
}
