package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AppLanguage
import com.example.domain.ArticleRecord
import com.example.domain.FeedLoadState
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.location.CitySelectionMode
import com.example.ui.theme.BreakingCrimson
import com.example.ui.theme.BreakingCrimsonBg
import com.example.ui.theme.NetraAmberDeep
import com.example.ui.theme.NetraAmberMuted
import com.example.ui.theme.NetraAmberPrimary
import com.example.ui.theme.NetraNavyDeep
import com.example.ui.theme.NetraNavyPrimary
import com.example.ui.theme.NewsprintIvory
import com.example.ui.theme.StaleAmber
import com.example.ui.theme.StaleAmberBg
import com.example.ui.theme.VerifiedEmerald
import com.example.ui.theme.VerifiedEmeraldBg

/**
 * Netra Family Masthead & Newspaper Feed Screen (Sections 1.2, 1.3, 2, 3, 6).
 */
@Composable
fun NetraFamilyHeader(
    uiState: NetraUiState,
    onToggleLanguage: () -> Unit,
    onRefresh: () -> Unit
) {
    val isHi = uiState.language == AppLanguage.HINDI
    val tier = uiState.powerTelemetry.effectiveTier

    Surface(
        color = NetraNavyPrimary,
        contentColor = NewsprintIvory,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top subtle banner artwork strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_netra_banner_1793947108723),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.32f,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    NetraNavyDeep.copy(alpha = 0.65f),
                                    NetraNavyPrimary
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHi) {
                            "नेत्र इकोसिस्टम • सत्यापित स्रोत समाचार पत्र"
                        } else {
                            "NETRA ECOSYSTEM • VERIFIED SOURCE NEWSPAPER"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = NetraAmberPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = Color(tier.badgeColorHex),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${uiState.powerTelemetry.batteryPercent}% • ${tier.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Main Masthead Row: App Name, Version, Date/Time, Language Toggle, Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isHi) "नेत्र न्यूज़" else "NETRA NEWS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = NewsprintIvory,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = NetraAmberPrimary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, NetraAmberPrimary),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "v1.0.0-stage1",
                                style = MaterialTheme.typography.labelSmall,
                                color = NetraAmberPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isHi) {
                            "शुक्रवार, 6 नवंबर 2026 • IST | कोई विज्ञापन नहीं • केवल सत्यापित स्रोत लिंक"
                        } else {
                            "Fri, 6 Nov 2026 • IST | Zero Ads in Live/Breaking • Verified Link-Out"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NewsprintIvory.copy(alpha = 0.8f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onToggleLanguage,
                        border = BorderStroke(1.dp, NetraAmberPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NetraAmberPrimary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("language_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHi) "EN" else "हिंदी",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_feed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isHi) "ताज़ा करें" else "Refresh feed",
                            tint = NetraAmberPrimary
                        )
                    }
                }
            }

            // Gold Amber Broadsheet Divider Rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(NetraAmberPrimary)
            )
        }
    }
}

@Composable
fun NewspaperFeedScreen(
    uiState: NetraUiState,
    savedArticleIds: Set<String>,
    onSelectScope: (NewsScope) -> Unit,
    onOpenCityPicker: () -> Unit,
    onRequestCoarseLocation: () -> Unit,
    onOpenArticleDetail: (ArticleRecord) -> Unit,
    onToggleBookmark: (ArticleRecord) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDiagnosticMode: (DiagnosticContractMode) -> Unit,
    onDismissLocationBanner: () -> Unit
) {
    val isHi = uiState.language == AppLanguage.HINDI
    var showAuditDrawer by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("newspaper_feed_list"),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. City Selector & Location Status Strip
        item {
            CityAndPolicyControlStrip(
                uiState = uiState,
                onOpenCityPicker = onOpenCityPicker,
                onRequestCoarseLocation = onRequestCoarseLocation,
                onDismissLocationBanner = onDismissLocationBanner
            )
        }

        // 2. Scope Tabs (Local, State, National, World [Stage-6])
        item {
            ScopeTabsRow(
                selectedScope = uiState.selectedScope,
                isHi = isHi,
                onSelectScope = onSelectScope
            )
        }

        // 3. Owner Acceptance Contract State Switcher (Compact verification strip)
        item {
            DiagnosticContractStrip(
                currentMode = uiState.diagnosticMode,
                isHi = isHi,
                onSelectMode = onSelectDiagnosticMode
            )
        }

        // 4. External Browser Security / Status Banner
        if (uiState.externalBrowserStatusBanner != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BreakingCrimsonBg),
                    border = BorderStroke(1.dp, BreakingCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = uiState.externalBrowserStatusBanner,
                        style = MaterialTheme.typography.bodySmall,
                        color = BreakingCrimson,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // 5. Distinct Error & Load Contract States (Section 6.5: Loading, Loaded, Empty, Stale, Offline, Failed)
        when (val feedState = uiState.feedLoadState) {
            is FeedLoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isHi) {
                                    "सत्यापित स्रोत और अधिकार अनुबंध की जांच हो रही है..."
                                } else {
                                    "Verifying source allowlist, schema v1 & rights contract..."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            is FeedLoadState.Empty -> {
                item {
                    EmptyFeedContractCard(
                        state = feedState,
                        isHi = isHi,
                        onSwitchToNational = { onSelectScope(NewsScope.NATIONAL) }
                    )
                }
            }

            is FeedLoadState.Stale -> {
                item {
                    ContractStateBanner(
                        icon = Icons.Default.History,
                        title = if (isHi) "पुराना डेटा (Stale Feed State)" else "Stale Feed State (>6h Old)",
                        subtitle = if (isHi) feedState.staleReasonHi else feedState.staleReasonEn,
                        timestampLabel = NetraViewModel.formatTimestamp(feedState.lastSuccessfulAtEpochMs),
                        containerColor = StaleAmberBg,
                        contentColor = StaleAmber
                    )
                }
                items(feedState.items, key = { it.id }) { article ->
                    BroadsheetArticleCard(
                        article = article,
                        isSaved = savedArticleIds.contains(article.id),
                        isHi = isHi,
                        onOpenDetail = { onOpenArticleDetail(article) },
                        onToggleBookmark = { onToggleBookmark(article) },
                        onOpenExternalUrl = { onOpenExternalUrl(article.canonicalSourceUrl) }
                    )
                }
            }

            is FeedLoadState.Offline -> {
                item {
                    ContractStateBanner(
                        icon = Icons.Default.WifiOff,
                        title = if (isHi) "ऑफ़लाइन मोड (Offline Cached State)" else "Offline Mode (Preserved Room Cache)",
                        subtitle = if (isHi) feedState.offlineMessageHi else feedState.offlineMessageEn,
                        timestampLabel = NetraViewModel.formatTimestamp(feedState.lastCachedAtEpochMs),
                        containerColor = StaleAmberBg,
                        contentColor = StaleAmber
                    )
                }
                items(feedState.cachedItems, key = { it.id }) { article ->
                    BroadsheetArticleCard(
                        article = article,
                        isSaved = savedArticleIds.contains(article.id),
                        isHi = isHi,
                        onOpenDetail = { onOpenArticleDetail(article) },
                        onToggleBookmark = { onToggleBookmark(article) },
                        onOpenExternalUrl = { onOpenExternalUrl(article.canonicalSourceUrl) }
                    )
                }
            }

            is FeedLoadState.Failed -> {
                item {
                    ContractStateBanner(
                        icon = Icons.Default.ErrorOutline,
                        title = if (isHi) {
                            if (feedState.isSchemaRejection) "असंगत स्कीमा अस्वीकृत (Schema Rejected)" else "फ़ीड त्रुटि (Failed State)"
                        } else {
                            if (feedState.isSchemaRejection) "Incompatible Schema Rejected" else "Feed Fetch Failed"
                        },
                        subtitle = if (isHi) feedState.errorMessageHi else feedState.errorMessageEn,
                        timestampLabel = NetraViewModel.formatTimestamp(feedState.lastSuccessfulAtEpochMs),
                        containerColor = BreakingCrimsonBg,
                        contentColor = BreakingCrimson
                    )
                }
                if (feedState.preservedItems.isNotEmpty()) {
                    item {
                        Text(
                            text = if (isHi) {
                                "नीचे अंतिम सफल सत्यापित कैश (Preserved Last Successful Content) सुरक्षित है:"
                            } else {
                                "Preserved Last Successful Content with original timestamps:"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(feedState.preservedItems, key = { it.id }) { article ->
                        BroadsheetArticleCard(
                            article = article,
                            isSaved = savedArticleIds.contains(article.id),
                            isHi = isHi,
                            onOpenDetail = { onOpenArticleDetail(article) },
                            onToggleBookmark = { onToggleBookmark(article) },
                            onOpenExternalUrl = { onOpenExternalUrl(article.canonicalSourceUrl) }
                        )
                    }
                }
            }

            is FeedLoadState.Loaded -> {
                if (feedState.usedNationalFallback) {
                    item {
                        ContractStateBanner(
                            icon = Icons.Default.Info,
                            title = if (isHi) "राष्ट्रीय सत्यापित फ़ॉलबैक सक्रिय" else "Verified National Fallback Active",
                            subtitle = if (isHi) {
                                "चयनित शहर के लिए कोई अलग आइटम न होने पर सत्यापित राष्ट्रीय स्रोत दिखाए जा रहे हैं।"
                            } else {
                                "Showing verified national sources as fallback for the current selection."
                            },
                            timestampLabel = feedState.generatedAtUtcIso,
                            containerColor = NetraAmberMuted,
                            contentColor = NetraAmberDeep
                        )
                    }
                }
                items(feedState.items, key = { it.id }) { article ->
                    BroadsheetArticleCard(
                        article = article,
                        isSaved = savedArticleIds.contains(article.id),
                        isHi = isHi,
                        onOpenDetail = { onOpenArticleDetail(article) },
                        onToggleBookmark = { onToggleBookmark(article) },
                        onOpenExternalUrl = { onOpenExternalUrl(article.canonicalSourceUrl) }
                    )
                }
            }
        }

        // 6. Transparency & Rights Guard Drawer (Retracted notices + Blocked test-001 proof)
        item {
            TransparencyAuditFooterCard(
                uiState = uiState,
                isExpanded = showAuditDrawer,
                onToggleExpand = { showAuditDrawer = !showAuditDrawer }
            )
        }
    }
}

@Composable
private fun CityAndPolicyControlStrip(
    uiState: NetraUiState,
    onOpenCityPicker: () -> Unit,
    onRequestCoarseLocation: () -> Unit,
    onDismissLocationBanner: () -> Unit
) {
    val isHi = uiState.language == AppLanguage.HINDI
    val loc = uiState.locationPreference

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationCity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isHi) loc.cityLabelHi else loc.cityLabelEn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isHi) {
                            "मोड: ${loc.selectionMode.labelHi} • कच्चे GPS निर्देशांक कभी सहेजे नहीं जाते"
                        } else {
                            "Mode: ${loc.selectionMode.labelEn} • Raw coordinates never persisted"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onOpenCityPicker,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("change_city_button")
                ) {
                    Text(
                        text = if (isHi) "शहर बदलें" else "Change City",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = onRequestCoarseLocation,
                    enabled = !uiState.isDetectingLocation,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetraNavyPrimary,
                        contentColor = NetraAmberPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("detect_coarse_city_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (uiState.isDetectingLocation) {
                            if (isHi) "जांच..." else "Detecting..."
                        } else {
                            if (isHi) "अनुमानित" else "Auto City"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // No-Ads During Breaking/Live Policy Pill
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
            color = NetraAmberMuted,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(1.dp, NetraAmberDeep.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = NetraAmberDeep,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isHi) {
                        "ब्रेकिंग/लाइव समाचार में शून्य विज्ञापन • सभी आइटम पर मूल प्रकाशक का नाम अनिवार्य"
                    } else {
                        "Zero Ads in Breaking/Live News • Publisher Named on Every Item • Free Tier"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = NetraNavyDeep,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Location feedback banner if present
        if (uiState.locationBannerMessageHi != null || uiState.locationBannerMessageEn != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isHi) {
                            uiState.locationBannerMessageHi.orEmpty()
                        } else {
                            uiState.locationBannerMessageEn.orEmpty()
                        },
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissLocationBanner,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeTabsRow(
    selectedScope: NewsScope,
    isHi: Boolean,
    onSelectScope: (NewsScope) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NewsScope.entries.forEach { scope ->
            val isSelected = selectedScope == scope
            FilterChip(
                selected = isSelected,
                onClick = { onSelectScope(scope) },
                label = {
                    Text(
                        text = if (isHi) scope.labelHi else scope.labelEn,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NetraNavyPrimary,
                    selectedLabelColor = NetraAmberPrimary
                ),
                modifier = Modifier.testTag("scope_tab_${scope.wireKey}")
            )
        }
    }
}

@Composable
private fun DiagnosticContractStrip(
    currentMode: DiagnosticContractMode,
    isHi: Boolean,
    onSelectMode: (DiagnosticContractMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isHi) {
                "अनुबंध 6.5 स्थिति निरीक्षक (Contract State Verifier):"
            } else {
                "Section 6.5 Error Contract State Verifier:"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DiagnosticContractMode.entries.forEach { mode ->
                AssistChip(
                    onClick = { onSelectMode(mode) },
                    label = {
                        Text(
                            text = if (isHi) mode.labelHi else mode.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    border = BorderStroke(
                        width = if (currentMode == mode) 1.5.dp else 1.dp,
                        color = if (currentMode == mode) NetraAmberDeep else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.testTag("diag_mode_${mode.name}")
                )
            }
        }
    }
}

@Composable
fun BroadsheetArticleCard(
    article: ArticleRecord,
    isSaved: Boolean,
    isHi: Boolean,
    onOpenDetail: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenExternalUrl: () -> Unit
) {
    val permittedSummary = remember(article) {
        com.example.domain.ValidationAndRightsEngine.getRightsPermittedSummary(article)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpenDetail() }
            .testTag("article_card_${article.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Meta Row: Breaking Badge / Scope / Category / Original Language Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (article.isBreakingOrLive) {
                        Surface(
                            color = BreakingCrimson,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isHi) "प्रमुख विज्ञप्ति • विज्ञापन-मुक्त" else "LIVE BULLETIN • NO ADS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = article.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Original Factual Headline Language Badge (No invented translator output)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "मूल भाषा: ${article.language.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Factual Headline (Serif Broadsheet Typography)
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mandatory Source Attribution + Rights Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Source",
                        tint = VerifiedEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = article.consolidatedSources.joinToString(" + "),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    color = if (article.rightsStatus == RightsStatus.LINK_ONLY) {
                        NetraAmberMuted
                    } else {
                        VerifiedEmeraldBg
                    },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(
                        1.dp,
                        if (article.rightsStatus == RightsStatus.LINK_ONLY) NetraAmberDeep else VerifiedEmerald
                    )
                ) {
                    Text(
                        text = if (isHi) article.rightsStatus.labelHi else article.rightsStatus.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (article.rightsStatus == RightsStatus.LINK_ONLY) NetraNavyDeep else VerifiedEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rights-Permitted Summary OR Neutral Link-Out Notice (Section 2.3)
            if (permittedSummary != null) {
                Text(
                    text = permittedSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHi) {
                            "अधिकार सुरक्षा (धारा 2.3): पूर्ण लेख या सारांश की नकल नहीं की गई है। पूरा समाचार पढ़ने के लिए नीचे प्रकाशक का आधिकारिक HTTPS लिंक खोलें।"
                        } else {
                            "Rights Guard (Sec 2.3): No prose or article text scraped. Open the publisher's canonical HTTPS link below to read at source."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Visible Correction Notice if present (Section 2.5)
            if (article.corrections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                article.corrections.forEach { corr ->
                    Surface(
                        color = StaleAmberBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, StaleAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (isHi) {
                                    "संशोधन सूचना (Corrected: ${corr.correctedAtIso})"
                                } else {
                                    "Correction Notice (Corrected: ${corr.correctedAtIso})"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StaleAmber,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = corr.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = NetraNavyDeep
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Timestamps & Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHi) {
                            "प्रकाशित: ${article.publishedAtIso ?: "अज्ञात"}"
                        } else {
                            "Published: ${article.publishedAtIso ?: "Unknown"}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (article.consolidatedSources.size > 1) {
                        Text(
                            text = if (isHi) {
                                "समेकित डुप्लीकेट लिंक (${article.consolidatedSources.size} स्रोत)"
                            } else {
                                "Consolidated duplicate link (${article.consolidatedSources.size} sources)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = VerifiedEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("bookmark_btn_${article.id}")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isHi) "बुकमार्क करें" else "Bookmark article",
                            tint = if (isSaved) NetraAmberDeep else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onOpenExternalUrl,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetraNavyPrimary,
                            contentColor = NetraAmberPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_source_btn_${article.id}")
                    ) {
                        Text(
                            text = if (isHi) "स्रोत लिंक खोलें" else "Open Source",
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

@Composable
private fun ContractStateBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    timestampLabel: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, contentColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("contract_state_banner")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NetraNavyDeep
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Timestamp: $timestampLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun EmptyFeedContractCard(
    state: FeedLoadState.Empty,
    isHi: Boolean,
    onSwitchToNational: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("empty_feed_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = NetraAmberDeep,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isHi) {
                    "कोई सत्यापित समाचार उपलब्ध नहीं (${state.scope.labelHi})"
                } else {
                    "No Verified News Items (${state.scope.labelEn})"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isHi) state.reasonHi else state.reasonEn,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (state.scope != NewsScope.NATIONAL) {
                OutlinedButton(
                    onClick = onSwitchToNational,
                    modifier = Modifier.testTag("fallback_national_button")
                ) {
                    Text(if (isHi) "सत्यापित राष्ट्रीय टैब देखें" else "View Verified National Tab")
                }
            }
        }
    }
}

@Composable
private fun TransparencyAuditFooterCard(
    uiState: NetraUiState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val isHi = uiState.language == AppLanguage.HINDI
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .testTag("toggle_audit_drawer_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHi) {
                            "पारदर्शिता लेजर: वापस लिए गए लेख (${uiState.retractedNotices.size}) व अवरुद्ध TEST आइटम (${uiState.rejectedAuditEntries.size})"
                        } else {
                            "Transparency Ledger: Retracted (${uiState.retractedNotices.size}) & Blocked TEST Items (${uiState.rejectedAuditEntries.size})"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                TextButton(onClick = onToggleExpand) {
                    Text(if (isExpanded) (if (isHi) "छुपाएं" else "Hide") else (if (isHi) "देखें" else "Inspect"))
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = if (isHi) {
                        "धारा 2.4, 2.5 व 6.3 प्रवर्तन: वापस लिए गए (RETRACTED) या TEST फिक्स्चर (id=test-001) को कभी भी मुख्य समाचार के रूप में प्रदर्शित नहीं किया जाता।"
                    } else {
                        "Sec 2.4, 2.5 & 6.3 Enforcement: RETRACTED items and TEST fixtures (id=test-001) are actively blocked from the live reader feed."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.rejectedAuditEntries.forEach { (item, reason) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "ID: ${item.id} • ${item.title}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Status: verification=${item.verificationStatus.wireKey}, rights=${item.rightsStatus.wireKey}",
                                style = MaterialTheme.typography.labelSmall,
                                color = BreakingCrimson
                            )
                            Text(
                                text = "Guard Reason: $reason",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item Detail & Provenance Sheet (Section 1.2, 2.1, 3.6):
 * Shows full evidence record and opens external HTTPS link without any privileged JS WebView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailProvenanceSheet(
    article: ArticleRecord,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onOpenExternalUrl: (String) -> Unit
) {
    BackHandler { onDismiss() }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (isHi) "समाचार प्रमाण एवं अधिकार रिकॉर्ड (Article Provenance)" else "Article Evidence & Rights Contract Record",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))

            ProvenanceRow("Article ID", article.id)
            ProvenanceRow("Publisher(s)", article.consolidatedSources.joinToString(", "))
            ProvenanceRow("Canonical HTTPS URL", article.canonicalSourceUrl)
            ProvenanceRow("Scope / Territory", "${article.scope.wireKey.uppercase()} (${article.countryCode ?: "IN"} / ${article.stateCode ?: "-"} / ${article.cityId ?: "-"})")
            ProvenanceRow("Source PublishedAt", article.publishedAtIso ?: "null (Unverified)")
            ProvenanceRow("VerifiedAt", article.verifiedAtIso ?: "null")
            ProvenanceRow("Verification Status", article.verificationStatus.wireKey)
            ProvenanceRow("Rights Status", "${article.rightsStatus.wireKey} (${if (isHi) article.rightsStatus.labelHi else article.rightsStatus.labelEn})")

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = NetraAmberMuted,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isHi) {
                        "सुरक्षा अनुबंध (धारा 3.6): बाहरी लिंक आपके सुरक्षित ब्राउज़र / कस्टम टैब में खुलते हैं। नेत्र न्यूज़ किसी भी बाहरी पेज के लिए जावास्क्रिप्ट ब्रिज वाला WebView नहीं चलाता।"
                    } else {
                        "Security Contract (Sec 3.6): Source links open in your external browser/custom tab. Netra News never loads external pages inside a privileged JavaScript WebView."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = NetraNavyDeep,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(if (isHi) "बंद करें" else "Close")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onOpenExternalUrl(article.canonicalSourceUrl) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NetraNavyPrimary,
                        contentColor = NetraAmberPrimary
                    )
                ) {
                    Text(if (isHi) "HTTPS स्रोत ब्राउज़र में खोलें" else "Open Canonical HTTPS Source")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProvenanceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.38f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.62f)
        )
    }
}
