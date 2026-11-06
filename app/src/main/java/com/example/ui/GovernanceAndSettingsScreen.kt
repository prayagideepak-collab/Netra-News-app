package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.HealthLogEntity
import com.example.domain.AppLanguage
import com.example.domain.FeatureGateStatus
import com.example.domain.ValidationAndRightsEngine
import com.example.location.CityLocationEngine
import com.example.location.PackagedCityEntry
import com.example.power.PowerBudgetAndTierManager
import com.example.settings.GovernanceLedgerData
import com.example.ui.theme.BreakingCrimson
import com.example.ui.theme.BreakingCrimsonBg
import com.example.ui.theme.NetraAmberDeep
import com.example.ui.theme.NetraAmberMuted
import com.example.ui.theme.NetraAmberPrimary
import com.example.ui.theme.NetraNavyDeep
import com.example.ui.theme.NetraNavyPrimary
import com.example.ui.theme.StaleAmber
import com.example.ui.theme.StaleAmberBg
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldBg

/**
 * Governance, Settings, Power Budget, Feature Matrix, Source Ledger & Open Decisions Screen
 * (Sections 1.2, 4, 5, 6, 7 & Additional User Instructions: Ultra-Low Power 24/7 Operation).
 */
@Composable
fun GovernanceAndSettingsScreen(
    uiState: NetraUiState,
    healthLogs: List<HealthLogEntity>,
    onOpenCityPicker: () -> Unit,
    onRequestCoarseLocation: () -> Unit,
    onResetSavedCity: () -> Unit,
    onClearHealthLogs: () -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }
    val isHi = uiState.language == AppLanguage.HINDI
    var selectedSection by remember { mutableIntStateOf(0) }
    // 0 = Feature Matrix & Decisions, 1 = Power & Self-Healing, 2 = Sources & Privacy, 3 = Location & About

    val sectionTitles = if (isHi) {
        listOf(
            "फीचर मैट्रिक्स व निर्णय",
            "पावर बजट (24/7 Tier-0)",
            "स्रोत लेजर व गोपनीयता",
            "स्थान व नेत्र परिवार"
        )
    } else {
        listOf(
            "Feature Matrix & Decisions",
            "Power Budget & 24/7",
            "Source Ledger & Privacy",
            "Location & Netra Family"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("governance_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Selector Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sectionTitles.forEachIndexed { index, title ->
                    FilterChip(
                        selected = selectedSection == index,
                        onClick = { selectedSection = index },
                        label = { Text(title, style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NetraNavyPrimary,
                            selectedLabelColor = NetraAmberPrimary
                        ),
                        modifier = Modifier.testTag("gov_tab_$index")
                    )
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // Feature Matrix (Required / Implemented / Tested / Blocked + Power/Wakeup/Network cost)
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NetraNavyPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isHi) {
                                    "धारा 7.3 फीचर व रिलीज़ गेट मैट्रिक्स (Required / Implemented / Tested / Blocked)"
                                } else {
                                    "Section 7.3 Feature & Release Gate Matrix"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = NetraAmberPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHi) {
                                    "प्रत्येक फ़ीचर अपने पावर टियर, प्रति घंटा वेकअप (Wakeups/hr) और नेटवर्क बजट की घोषणा करता है।"
                                } else {
                                    "Every feature declares its Power Tier, Wakeup cost/hr, Network cost/hr, and verification status."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                items(GovernanceLedgerData.FEATURE_MATRIX, key = { it.id }) { item ->
                    val badgeBg = when (item.status) {
                        FeatureGateStatus.IMPLEMENTED, FeatureGateStatus.TESTED -> VerifiedEmeraldBg
                        FeatureGateStatus.PLANNED_STAGE -> StaleAmberBg
                        FeatureGateStatus.BLOCKED_POLICY -> BreakingCrimsonBg
                    }
                    val badgeFg = when (item.status) {
                        FeatureGateStatus.IMPLEMENTED, FeatureGateStatus.TESTED -> VerifiedEmerald
                        FeatureGateStatus.PLANNED_STAGE -> StaleAmber
                        FeatureGateStatus.BLOCKED_POLICY -> BreakingCrimson
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = badgeBg,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isHi) item.status.labelHi else item.status.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = badgeFg,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "Req: ${if (item.requiredBySpec) "YES" else "NO"} • Tested: ${if (item.unitTested) "YES" else "STAGE-GATE"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHi) item.featureTitleHi else item.featureTitleEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHi) item.notesHi else item.notesEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "Power Tier: ${item.powerTierRequired}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Wakeups: ${item.wakeupsPerHourCost} • Network: ${item.networkCallsPerHourCost}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 7.4 Assumptions & Open Decisions
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isHi) {
                            "धारा 7.4: स्पष्ट मान्यताएं और अनसुलझे निर्णय (Assumptions & Open Decisions)"
                        } else {
                            "Section 7.4: Explicit Assumptions & Unresolved Open Decisions"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(GovernanceLedgerData.OPEN_DECISIONS, key = { it.id }) { dec ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isHi) dec.decisionTitleHi else dec.decisionTitleEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHi) dec.currentAssumptionHi else dec.currentAssumptionEn,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHi) {
                                    "स्वामी कार्रवाई (Owner Action): ${dec.ownerActionNeededHi}"
                                } else {
                                    "Owner Action: ${dec.ownerActionNeededEn}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NetraAmberDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            1 -> {
                // Ultra-Low Power 24/7 Operation & Self-Healing Monitor
                val tel = uiState.powerTelemetry
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(tel.effectiveTier.badgeColorHex)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = Color(tel.effectiveTier.badgeColorHex)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isHi) {
                                        "सक्रिय पावर टियर: ${tel.effectiveTier.labelHi}"
                                    } else {
                                        "Active Power Tier: ${tel.effectiveTier.labelEn}"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isHi) tel.effectiveTier.descriptionHi else tel.effectiveTier.descriptionEn,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Battery: ${tel.batteryPercent}% • Charging: ${tel.isCharging} • System Saver: ${tel.isSystemBatterySaverOn}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Thermal: ${tel.thermalStatusLabel} • Data Saver: ${tel.isDataSaverEnabled} • Unmetered: ${tel.isUnmeteredNetwork}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = "Wakeups/hr: ${tel.backgroundWakeupsInCurrentHour} (Budget: ${tel.effectiveTier.maxWakeupsPerHour}) • Network Calls/hr: ${tel.networkCallsInCurrentHour}/${tel.effectiveTier.maxNetworkCallsPerHour}",
                                style = MaterialTheme.typography.labelMedium,
                                color = VerifiedEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Idle 24h Drain Target: <= ${tel.idleDrainTarget24hPercent}% (Screen Off) • Survival 3h @ 5% Target: ~${tel.survival3hDrainTargetPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isHi) "Tier-0 आवश्यक कार्य (24/7 Essential Functions)" else "Tier-0 Essential Functionality List",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            PowerBudgetAndTierManager.TIER_0_ESSENTIAL_FUNCTIONS.forEach { (hi, en) ->
                                Text(
                                    text = "• ${if (isHi) hi else en}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isHi) {
                                        "सीमित रोटेटेड हेल्थ लॉग (Max 100 • ${healthLogs.size} प्रविष्टियाँ)"
                                    } else {
                                        "Bounded Rotated Health Log (Max 100 • ${healthLogs.size} rows)"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedButton(onClick = onClearHealthLogs) {
                                    Text(if (isHi) "साफ़ करें" else "Clear", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                text = "Circuit Breaker Failures: ${uiState.circuitBreaker.consecutiveFailures}/${uiState.circuitBreaker.maxRetryCap} • Open: ${uiState.circuitBreaker.isOpen}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            healthLogs.take(8).forEach { log ->
                                Text(
                                    text = "[${log.powerTier}] ${log.eventType}: ${log.message}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                // Source Allowlist Ledger & Privacy Policy
                item {
                    Text(
                        text = if (isHi) {
                            "धारा 2.2 और 5.5: अनुमत स्रोत और लाइसेंस लेजर (Allowlisted Sources Ledger)"
                        } else {
                            "Section 2.2 & 5.5: Allowlisted Sources & Rights Ledger"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(ValidationAndRightsEngine.ALLOWLISTED_SOURCES, key = { it.sourceId }) { src ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isHi) src.publisherNameHi else src.publisherNameEn,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Host: https://${src.allowedHost} • Cadence: ${src.updateCadenceMinutes}m • Default Rights: ${src.defaultRightsStatus.wireKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = VerifiedEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHi) src.licenceSummaryHi else src.licenceSummaryEn,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            3 -> {
                // Location Controls, Dataset Licence, What's New & Netra Family Links
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isHi) "स्थान एवं गोपनीयता नियंत्रण (धारा 1.4–1.6, 6.4)" else "Location & Privacy Controls (Sec 1.4–1.6, 6.4)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Current City ID: ${uiState.locationPreference.cityId} (${uiState.locationPreference.selectionMode.wireKey})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Dataset: ${CityLocationEngine.DATASET_NAME}\nLicence: ${CityLocationEngine.DATASET_LICENCE} (${CityLocationEngine.DATASET_VERSION})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onOpenCityPicker,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NetraNavyPrimary,
                                        contentColor = NetraAmberPrimary
                                    )
                                ) {
                                    Text(if (isHi) "मैनुअल शहर चुनें" else "Manual City")
                                }
                                OutlinedButton(onClick = onRequestCoarseLocation) {
                                    Text(if (isHi) "अनुमानित स्थान" else "Coarse Auto")
                                }
                                OutlinedButton(
                                    onClick = onResetSavedCity,
                                    modifier = Modifier.testTag("reset_city_pref_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isHi) "रीसेट" else "Reset")
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isHi) "नेत्र परिवार लिंक व नया क्या है (Netra Family & What's New)" else "Netra Ecosystem Family & What's New",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            GovernanceLedgerData.NETRA_ECOSYSTEM_LINKS.forEach { (title, descHi, descEn) ->
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (isHi) descHi else descEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Manual Country / State / City Selection Dialog + Nearest-City Ambiguity Resolver (Section 1.5, 1.6, 6.4).
 */
@Composable
fun ManualCityPickerDialog(
    currentCityId: String,
    isHi: Boolean,
    onSelectCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val states = remember { CityLocationEngine.getStates() }
    val currentCity = remember(currentCityId) { CityLocationEngine.findByCityId(currentCityId) }
    var selectedStateCode by remember { mutableStateOf(currentCity.stateCode) }
    val citiesInState = remember(selectedStateCode) {
        CityLocationEngine.getCitiesForState(selectedStateCode)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isHi) "देश / राज्य / शहर चुनें (Manual City)" else "Select Country / State / City",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isHi) {
                        "देश: भारत (IN) • ऑफ़लाइन CC-BY 4.0 गज़ेटियर (बिना GPS के कार्यरत)"
                    } else {
                        "Country: India (IN) • Offline CC-BY 4.0 Gazetteer (Works with Location Off)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHi) "1. राज्य चुनें (State):" else "1. Select State:",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    states.forEach { (code, label) ->
                        FilterChip(
                            selected = selectedStateCode == code,
                            onClick = { selectedStateCode = code },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("state_chip_$code")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHi) "2. शहर चुनें (City):" else "2. Select City:",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                citiesInState.forEach { city ->
                    Surface(
                        color = if (city.cityId == currentCityId) {
                            NetraAmberMuted
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onSelectCity(city.cityId) }
                            .testTag("city_option_${city.cityId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isHi) city.fullLabelHi else city.fullLabelEn,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (city.cityId == currentCityId) NetraNavyDeep else MaterialTheme.colorScheme.onSurface
                            )
                            if (city.cityId == currentCityId) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = VerifiedEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHi) "बंद करें" else "Cancel")
            }
        }
    )
}

@Composable
fun AmbiguousCityConfirmationDialog(
    primary: PackagedCityEntry,
    secondary: PackagedCityEntry,
    isHi: Boolean,
    onConfirmCity: (PackagedCityEntry) -> Unit,
    onOpenManualPicker: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onOpenManualPicker,
        title = {
            Text(
                text = if (isHi) "निकटतम शहर की पुष्टि करें (Ambiguity Check)" else "Confirm Nearest City",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (isHi) {
                        "धारा 6.4: अनुमानित स्थान दो शहरों के निकट है। कच्चे निर्देशांक हटा दिए गए हैं। कृपया अपना सही शहर चुनें:"
                    } else {
                        "Sec 6.4: Approximate coarse location is near two cities. Raw coordinates have been discarded. Please confirm your city:"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onConfirmCity(primary) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isHi) primary.fullLabelHi else primary.fullLabelEn)
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { onConfirmCity(secondary) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isHi) secondary.fullLabelHi else secondary.fullLabelEn)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenManualPicker) {
                Text(if (isHi) "अन्य शहर चुनें" else "Choose Another Manually")
            }
        }
    )
}
