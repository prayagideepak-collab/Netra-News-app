package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BookmarkEntity
import com.example.domain.AppLanguage
import com.example.domain.BulletinDataStatus
import com.example.domain.EarthquakeEventRecord
import com.example.domain.WeatherAdvisoryRecord
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
 * Saved Bookmarks Screen (Section 1.2, 3.5, 4.1 Stage 2):
 * Bookmarks and preferences stay local by default. No account or cloud sync.
 */
@Composable
fun SavedBookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    language: AppLanguage,
    onRemoveBookmark: (String) -> Unit,
    onClearAllBookmarks: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }
    val isHi = language == AppLanguage.HINDI

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_bookmarks_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NetraAmberMuted),
                border = BorderStroke(1.dp, NetraAmberDeep),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = NetraNavyDeep,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) {
                                "100% स्थानीय बुकमार्क (Local Room Storage Only)"
                            } else {
                                "100% On-Device Bookmarks (No Cloud Sync)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = NetraNavyDeep,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHi) {
                                "धारा 3.5: आपके सहेजे गए लेख केवल आपके फ़ोन पर रहते हैं। कोई लॉगिन खाता, क्लाउड सिंक या विज्ञापन ट्रैकिंग नहीं।"
                            } else {
                                "Sec 3.5: Saved items stay strictly on your phone. Zero cloud accounts, sync, or advertising IDs."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NetraNavyDeep
                        )
                    }
                    if (bookmarks.isNotEmpty()) {
                        OutlinedButton(
                            onClick = onClearAllBookmarks,
                            modifier = Modifier.testTag("clear_all_bookmarks_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isHi) "सब हटाएं" else "Clear")
                        }
                    }
                }
            }
        }

        if (bookmarks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isHi) "कोई सहेजा गया समाचार नहीं है" else "No Saved Articles Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHi) {
                                "समाचार फ़ीड में किसी भी सत्यापित आइटम पर बुकमार्क आइकन दबाकर उसे ऑफ़लाइन संदर्भ के लिए यहाँ सहेजें।"
                            } else {
                                "Tap the bookmark icon on any verified article in the newspaper feed to save its source link locally."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(bookmarks, key = { it.articleId }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("saved_item_${item.articleId}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${item.publisherName} • ${item.category}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VerifiedEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!item.permittedSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.permittedSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isHi) {
                                    "सहेजा गया: ${NetraViewModel.formatTimestamp(item.savedAtEpochMs)}"
                                } else {
                                    "Saved: ${NetraViewModel.formatTimestamp(item.savedAtEpochMs)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                IconButton(onClick = { onRemoveBookmark(item.articleId) }) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkRemove,
                                        contentDescription = if (isHi) "बुकमार्क हटाएं" else "Remove bookmark",
                                        tint = BreakingCrimson
                                    )
                                }
                                Button(
                                    onClick = { onOpenExternalUrl(item.canonicalSourceUrl) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NetraNavyPrimary,
                                        contentColor = NetraAmberPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isHi) "स्रोत खोलें" else "Open Source",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Weather & Earthquakes Civic Bulletins Screen (Sections 1.2, 2.6, 3.3, 3.4, 4.1 Stage 4 & 5):
 * - Source-issued event time, area, units, severity, update/expiry and link.
 * - Unavailable/stale looks unmistakably different from a real zero or all-clear.
 * - Explicit disclaimer: NEVER call this an emergency early-warning or earthquake prediction system.
 * - Stage-5 Background Alerts explicitly disabled in Stage 1 with plain explanation.
 */
@Composable
fun WeatherAndEarthquakesScreen(
    uiState: NetraUiState,
    onOpenExternalUrl: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }
    val isHi = uiState.language == AppLanguage.HINDI
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Weather, 1 = Earthquakes

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("weather_earthquakes_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Mandatory Non-Early-Warning Safety & Compliance Banner (Section 2.6 & 3.3)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BreakingCrimsonBg),
                border = BorderStroke(1.5.dp, BreakingCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = BreakingCrimson,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isHi) {
                                "महत्वपूर्ण सूचना (धारा 2.6): यह आपातकालीन पूर्व-चेतावनी (Early-Warning) प्रणाली नहीं है"
                            } else {
                                "Mandatory Notice (Sec 2.6): NOT an Emergency Early-Warning System"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = BreakingCrimson,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHi) {
                                "ये पृष्ठ केवल भारत मौसम विज्ञान विभाग (IMD) और राष्ट्रीय भूकंप विज्ञान केंद्र (NCS) के आधिकारिक बुलेटिन लिंक दिखाते हैं। अनुपलब्ध या ऑफ़लाइन डेटा को कभी भी '0' या 'सब सुरक्षित' (All-Clear) के रूप में नहीं दिखाया जाता।"
                            } else {
                                "These pages display official IMD & NCS source bulletins only. Unavailable or stale states are visually distinct from a real zero or all-clear. No earthquake prediction or instant alert delivery is claimed."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = NetraNavyDeep
                        )
                    }
                }
            }
        }

        // 2. Sub-tabs: Weather (IMD) vs Earthquakes (NCS)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    label = {
                        Text(
                            text = if (isHi) "मौसम बुलेटिन (IMD Weather)" else "Weather Bulletins (IMD)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Thunderstorm,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetraNavyPrimary,
                        selectedLabelColor = NetraAmberPrimary,
                        selectedLeadingIconColor = NetraAmberPrimary
                    ),
                    modifier = Modifier.testTag("subtab_weather")
                )

                FilterChip(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    label = {
                        Text(
                            text = if (isHi) "भूकंप लॉग (NCS Earthquakes)" else "Earthquakes (NCS)",
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NetraNavyPrimary,
                        selectedLabelColor = NetraAmberPrimary,
                        selectedLeadingIconColor = NetraAmberPrimary
                    ),
                    modifier = Modifier.testTag("subtab_earthquakes")
                )
            }
        }

        // 3. Bulletin Cards
        if (selectedSubTab == 0) {
            items(uiState.weatherBulletins, key = { it.eventId }) { record ->
                WeatherBulletinCard(
                    record = record,
                    isHi = isHi,
                    onOpenSource = { onOpenExternalUrl(record.sourceUrl) }
                )
            }
        } else {
            items(uiState.earthquakeBulletins, key = { it.eventId }) { record ->
                EarthquakeBulletinCard(
                    record = record,
                    isHi = isHi,
                    onOpenSource = { onOpenExternalUrl(record.sourceUrl) }
                )
            }
        }

        // 4. Stage-5 Background Alert Pilot Card (Explicitly disabled with plain explanation per Section 3.3 & 7.3)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHi) {
                                "चरण-5 पृष्ठभूमि अलर्ट पायलट (Stage-5 Planned — अभी अक्षम)"
                            } else {
                                "Stage-5 Background Alert Pilot (Planned — Disabled in Stage 1)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHi) {
                            "स्पष्टीकरण (धारा 3.3 व 7.3): सहेजे गए शहर पर आधारित पृष्ठभूमि मौसम/भूकंप अलर्ट केवल चरण-5 पायलट का हिस्सा हैं। निःशुल्क पुश ट्रांसपोर्ट की व्यवहार्यता समीक्षा और डोज़ (Doze) स्वीकृति परीक्षण पूरा होने तक यह बटन अक्षम रखा गया है। (POST_NOTIFICATIONS अनुमति अभी नहीं मांगी जाती)।"
                        } else {
                            "Explanation (Sec 3.3 & 7.3): Saved-city background alerts are scoped to Stage-5 only after a free push transport feasibility review and real-phone Doze acceptance testing. Disabled in Stage-1 (no notification permission requested)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.testTag("disabled_stage5_alert_button")
                    ) {
                        Text(
                            if (isHi) {
                                "पृष्ठभूमि अलर्ट सक्षम करें (चरण-5 तक अक्षम)"
                            } else {
                                "Enable Background Alerts (Disabled until Stage-5)"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherBulletinCard(
    record: WeatherAdvisoryRecord,
    isHi: Boolean,
    onOpenSource: () -> Unit
) {
    val isUnavailable = record.status == BulletinDataStatus.UNAVAILABLE_OFFLINE ||
        record.status == BulletinDataStatus.STALE_CACHED

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isUnavailable) StaleAmber else VerifiedEmerald
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Distinct Status Badge (Unavailable/Stale vs Verified)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isUnavailable) StaleAmberBg else VerifiedEmeraldBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isUnavailable) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = StaleAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isHi) record.severityLabelHi else record.severityLabelEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUnavailable) StaleAmber else VerifiedEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "ID: ${record.eventId}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isHi) record.areaLabelHi else record.areaLabelEn,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = record.providerName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Numeric Metrics Box: Visually distinguishes NULL (Unavailable/Unparsed) from 0!
            Surface(
                color = if (isUnavailable) StaleAmberBg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Units: ${record.unitsLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (record.temperatureCelsius == null) {
                            if (isHi) {
                                "संख्यात्मक मान: [अनुपलब्ध / कोई कृत्रिम संख्या नहीं — यह 0°C नहीं है]"
                            } else {
                                "Numeric Reading: [UNAVAILABLE / Link-Only Mode — Distinct from 0°C]"
                            }
                        } else {
                            "${record.temperatureCelsius} °C"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnavailable) StaleAmber else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHi) record.summaryHi else record.summaryEn,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Issued: ${record.issuedAtIso ?: "N/A"} • Updated: ${record.updatedAtIso ?: "N/A"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Expiry: ${record.expiryAtIso ?: "N/A"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetraNavyPrimary,
                    contentColor = NetraAmberPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isHi) "आधिकारिक IMD बुलेटिन खोलें (HTTPS)" else "Open Official IMD Bulletin (HTTPS)")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EarthquakeBulletinCard(
    record: EarthquakeEventRecord,
    isHi: Boolean,
    onOpenSource: () -> Unit
) {
    val isUnavailable = record.status == BulletinDataStatus.UNAVAILABLE_OFFLINE ||
        record.status == BulletinDataStatus.STALE_CACHED

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isUnavailable) StaleAmber else VerifiedEmerald
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(
                color = if (isUnavailable) StaleAmberBg else VerifiedEmeraldBg,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isHi) record.severityLabelHi else record.severityLabelEn,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnavailable) StaleAmber else VerifiedEmerald,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isHi) record.areaLabelHi else record.areaLabelEn,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${record.providerName} • Event ID: ${record.eventId}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = if (isUnavailable) StaleAmberBg.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Units: ${record.unitsLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (record.magnitudeMw == null) {
                            if (isHi) {
                                "तीव्रता (Magnitude): [कोई कृत्रिम मान नहीं — आधिकारिक NCS लॉग लिंक देखें]"
                            } else {
                                "Magnitude (Mw): [No Fabricated Value — Verify on Official NCS Portal]"
                            }
                        } else {
                            "Mw ${record.magnitudeMw} • Depth ${record.depthKm ?: "-"} km"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnavailable) StaleAmber else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Issued: ${record.issuedAtIso ?: "N/A"} • Updated: ${record.updatedAtIso ?: "N/A"} • Expiry: ${record.expiryAtIso ?: "N/A"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onOpenSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NetraNavyPrimary,
                    contentColor = NetraAmberPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isHi) "आधिकारिक NCS भूकंप पोर्टल खोलें (HTTPS)" else "Open Official NCS Seismology Portal (HTTPS)")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
