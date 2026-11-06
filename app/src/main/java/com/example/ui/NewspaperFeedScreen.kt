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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.AppLanguage
import com.example.domain.ArticleRecord
import com.example.domain.EditorialState
import com.example.domain.FeedLoadState
import com.example.domain.NewsScope
import com.example.domain.RightsStatus
import com.example.domain.ValidationAndRightsEngine
import com.example.domain.VerificationStatus
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
 * Netra Family Masthead & Newspaper Feed Screen (Build Specification v3, Sections 1.2, 2, 3, 7, 8, 9).
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
                            "नेत्र इकोसिस्टम • सत्यापित स्रोत समाचार पत्र (Spec v3)"
                        } else {
                            "NETRA ECOSYSTEM • VERIFIED SOURCE NEWSPAPER (Spec v3)"
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
                                text = "v1.0.0-v3",
                                style = MaterialTheme.typography.labelSmall,
                                color = NetraAmberPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isHi) {
                            "6 नवंबर 2026 • IST | कोई विज्ञापन नहीं • स्रोत सत्यापन संकेतक सक्रिय"
                        } else {
                            "6 Nov 2026 • IST | Zero Ads in Live/Breaking • Verification Indicators"
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(NetraAmberPrimary)
            )
        }
    }
}

/**
 * Reusable Monochrome & Screen-Reader Accessible Source Verification Indicator Badge (Section 7.1–7.4).
 * Allowed values: Verified, Pending, Blocked.
 * Includes icon + explicit text label + distinct border style so meaning never relies on colour alone.
 */
@Composable
fun SourceVerificationStatusBadge(
    status: VerificationStatus,
    isHi: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        VerificationStatus.VERIFIED -> VerifiedEmeraldBg
        VerificationStatus.PENDING -> StaleAmberBg
        VerificationStatus.BLOCKED -> BreakingCrimsonBg
    }
    val fgColor = when (status) {
        VerificationStatus.VERIFIED -> VerifiedEmerald
        VerificationStatus.PENDING -> StaleAmber
        VerificationStatus.BLOCKED -> BreakingCrimson
    }
    val icon = when (status) {
        VerificationStatus.VERIFIED -> Icons.Default.CheckCircle
        VerificationStatus.PENDING -> Icons.Default.HourglassEmpty
        VerificationStatus.BLOCKED -> Icons.Default.Block
    }
    val textLabel = if (isHi) {
        "स्रोत सत्यापन (Source verification): ${status.labelHi}"
    } else {
        "Source verification: ${status.labelEn}"
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.5.dp, fgColor),
        modifier = modifier.semantics {
            contentDescription = textLabel
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${status.monochromeSymbol} $textLabel",
                style = MaterialTheme.typography.labelSmall,
                color = fgColor,
                fontWeight = FontWeight.Bold
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
    onReportArticleIssue: (ArticleRecord) -> Unit,
    onOpenGeneralFeedback: () -> Unit,
    onToggleBookmark: (ArticleRecord) -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectDiagnosticMode: (DiagnosticContractMode) -> Unit,
    onToggleWarningRecordsOnHome: () -> Unit,
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

        // 5. Distinct Error & Load Contract States (Section 9.5)
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
                                    "सत्यापित स्रोत, सत्यापन संकेतक और अधिकार अनुबंध की जांच हो रही है..."
                                } else {
                                    "Verifying source allowlist, verificationStatus & rights contract..."
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
                        onReportIssue = { onReportArticleIssue(article) },
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
                        onReportIssue = { onReportArticleIssue(article) },
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
                            onReportIssue = { onReportArticleIssue(article) },
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
                        onReportIssue = { onReportArticleIssue(article) },
                        onToggleBookmark = { onToggleBookmark(article) },
                        onOpenExternalUrl = { onOpenExternalUrl(article.canonicalSourceUrl) }
                    )
                }
            }
        }

        // 6. Section 7.4 Pending & Blocked Status/Warning Records Section (Distinct from ordinary verified stories)
        if (uiState.warningStatusRecords.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isHi) {
                                "धारा 7.4: स्रोत स्थिति/चेतावनी रिकॉर्ड (Pending & Blocked — सामान्य समाचार नहीं)"
                            } else {
                                "Section 7.4: Source Status/Warning Records (Pending & Blocked)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onToggleWarningRecordsOnHome) {
                            Text(
                                if (uiState.showWarningStatusRecordsOnHome) {
                                    if (isHi) "छुपाएं" else "Hide"
                                } else {
                                    if (isHi) "दिखाएं (${uiState.warningStatusRecords.size})" else "Show (${uiState.warningStatusRecords.size})"
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.showWarningStatusRecordsOnHome) {
                items(uiState.warningStatusRecords, key = { it.id }) { warningItem ->
                    BroadsheetArticleCard(
                        article = warningItem,
                        isSaved = savedArticleIds.contains(warningItem.id),
                        isHi = isHi,
                        onOpenDetail = { onOpenArticleDetail(warningItem) },
                        onReportIssue = { onReportArticleIssue(warningItem) },
                        onToggleBookmark = { onToggleBookmark(warningItem) },
                        onOpenExternalUrl = { onOpenExternalUrl(warningItem.canonicalSourceUrl) }
                    )
                }
            }
        }

        // 7. Transparency & Rights Guard Drawer (Retracted editorialState + Blocked test-001 proof)
        item {
            TransparencyAuditFooterCard(
                uiState = uiState,
                isExpanded = showAuditDrawer,
                onToggleExpand = { showAuditDrawer = !showAuditDrawer }
            )
        }

        // 8. Section 8.1 Newspaper Footer General Feedback Affordance
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHi) {
                                "प्रतिक्रिया एवं सुधार (धारा 8 • बिना खाते के)"
                            } else {
                                "Feedback & Corrections (Sec 8 • No Account Needed)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHi) {
                                "किसी लेख या ऐप के बारे में सामान्य प्रतिक्रिया भेजें या स्थानीय ड्राफ्ट सहेजें।"
                            } else {
                                "Submit general feedback or manage local drafts with minimal data."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onOpenGeneralFeedback,
                        modifier = Modifier.testTag("footer_general_feedback_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Feedback,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isHi) "प्रतिक्रिया" else "Feedback")
                    }
                }
            }
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
                        "ब्रेकिंग/लाइव में शून्य विज्ञापन • स्रोत सत्यापन स्थिति केवल स्रोत रिकॉर्ड दर्शाती है, प्रत्येक तथ्य की गारंटी नहीं"
                    } else {
                        "Zero Ads in Breaking/Live • Source verification shows recorded source status, not a guarantee of every fact"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = NetraNavyDeep,
                    fontWeight = FontWeight.Medium
                )
            }
        }

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
                "अनुबंध 9.5 स्थिति निरीक्षक (Contract State Verifier):"
            } else {
                "Section 9.5 Error Contract State Verifier:"
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

/**
 * Broadsheet Article Card (Updated for Build Specification v3, Sections 2, 7, 8):
 * - Section 7.2 & 7.3: Shows `Source: [publisher]` next to `Source verification: [Verified/Pending/Blocked]`
 *   beside the publisher and BEFORE the timestamp.
 * - Section 7.4: If `Pending` or `Blocked` is shown, renders as a distinct warning/status record
 *   (never in Breaking/Live; `Blocked` has no scraped body/media or clickable unsafe URL).
 * - Section 8.1: Includes "Report article issue" button prefilling `articleId`.
 */
@Composable
fun BroadsheetArticleCard(
    article: ArticleRecord,
    isSaved: Boolean,
    isHi: Boolean,
    onOpenDetail: () -> Unit,
    onReportIssue: () -> Unit,
    onToggleBookmark: () -> Unit,
    onOpenExternalUrl: () -> Unit
) {
    val permittedSummary = remember(article) {
        ValidationAndRightsEngine.getRightsPermittedSummary(article)
    }
    val isWarningRecord = article.verificationStatus != VerificationStatus.VERIFIED ||
        article.rightsStatus == RightsStatus.BLOCKED
    val isLinkClickable = remember(article) {
        ValidationAndRightsEngine.isClickableSourceLinkAllowed(article)
    }
    val srAnnouncement = remember(article, isHi) {
        ValidationAndRightsEngine.formatScreenReaderVerificationAnnouncement(article, isHi)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (article.verificationStatus) {
                VerificationStatus.VERIFIED -> MaterialTheme.colorScheme.surface
                VerificationStatus.PENDING -> StaleAmberBg.copy(alpha = 0.28f)
                VerificationStatus.BLOCKED -> BreakingCrimsonBg.copy(alpha = 0.25f)
            }
        ),
        border = BorderStroke(
            width = if (isWarningRecord) 1.5.dp else 1.dp,
            color = when (article.verificationStatus) {
                VerificationStatus.VERIFIED -> MaterialTheme.colorScheme.outline
                VerificationStatus.PENDING -> StaleAmber
                VerificationStatus.BLOCKED -> BreakingCrimson
            }
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onOpenDetail() }
            .semantics { contentDescription = srAnnouncement }
            .testTag("article_card_${article.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Warning Record Header Banner if Pending or Blocked (Section 7.4)
            if (isWarningRecord) {
                Surface(
                    color = if (article.verificationStatus == VerificationStatus.BLOCKED) BreakingCrimsonBg else StaleAmberBg,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (article.verificationStatus == VerificationStatus.BLOCKED) {
                            if (isHi) {
                                "चेतावनी रिकॉर्ड (Blocked Status Record): यह सामान्य सत्यापित समाचार नहीं है। असुरक्षित लिंक व पाठ रोका गया है।"
                            } else {
                                "Warning Status Record (Blocked): Not a verified news story. Unsafe link & scraped prose withheld."
                            }
                        } else {
                            if (isHi) {
                                "चेतावनी रिकॉर्ड (Pending Status Record): स्रोत सत्यापन लंबित है; ब्रेकिंग/लाइव में वर्जित।"
                            } else {
                                "Warning Status Record (Pending): Source verification pending; excluded from Breaking/Live."
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (article.verificationStatus == VerificationStatus.BLOCKED) BreakingCrimson else StaleAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Top Meta Row: Breaking Badge (only when VERIFIED!) / Scope / Editorial State / Language Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (article.isBreakingOrLive && article.verificationStatus == VerificationStatus.VERIFIED) {
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
                    if (article.editorialState != EditorialState.ACTIVE) {
                        Surface(
                            color = StaleAmberBg,
                            border = BorderStroke(1.dp, StaleAmber),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isHi) article.editorialState.labelHi else article.editorialState.labelEn,
                                style = MaterialTheme.typography.labelSmall,
                                color = StaleAmber,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

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

            // Factual Headline
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section 7.2 & 7.3: Show "Source: [publisher]" next to "Source verification: [status]" BEFORE timestamp!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(10.dp)
            ) {
                Text(
                    text = if (isHi) {
                        "स्रोत (Source): ${article.consolidatedSources.joinToString(" + ")}"
                    } else {
                        "Source: ${article.consolidatedSources.joinToString(" + ")}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SourceVerificationStatusBadge(
                        status = article.verificationStatus,
                        isHi = isHi,
                        modifier = Modifier.testTag("verification_badge_${article.id}")
                    )

                    Surface(
                        color = if (article.rightsStatus == RightsStatus.LINK_ONLY) {
                            NetraAmberMuted
                        } else if (article.rightsStatus == RightsStatus.BLOCKED) {
                            BreakingCrimsonBg
                        } else {
                            VerifiedEmeraldBg
                        },
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(
                            1.dp,
                            if (article.rightsStatus == RightsStatus.LINK_ONLY) {
                                NetraAmberDeep
                            } else if (article.rightsStatus == RightsStatus.BLOCKED) {
                                BreakingCrimson
                            } else {
                                VerifiedEmerald
                            }
                        )
                    ) {
                        Text(
                            text = "Rights: ${article.rightsStatus.wireKey}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (article.rightsStatus == RightsStatus.BLOCKED) BreakingCrimson else NetraNavyDeep,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Rights-Permitted Summary OR Neutral Withheld/Link-Out Notice (Section 2.3 & 7.4)
            if (article.verificationStatus == VerificationStatus.BLOCKED || article.rightsStatus == RightsStatus.BLOCKED) {
                Surface(
                    color = BreakingCrimsonBg,
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, BreakingCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHi) {
                            "तटस्थ रोकी गई सूचना (Withheld Notice - धारा 7.4): इस अवरुद्ध रिकॉर्ड के लिए कोई स्क्रैप किया गया पाठ, मीडिया या असुरक्षित लिंक उपलब्ध नहीं है। कारण: ${article.verificationReason ?: "अधिकार/स्रोत अपुष्ट"}"
                        } else {
                            "Neutral Withheld Notice (Sec 7.4): No scraped body, media, or clickable unsafe URL is shown for this Blocked record. Reason: ${article.verificationReason ?: "Unverified source/rights"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = BreakingCrimson,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else if (permittedSummary != null) {
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

            // Visible Correction Notice if present (Section 2.5 & 7.4)
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

            // Timestamp Row (Placed strictly AFTER Publisher & Source Verification per Section 7.3)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHi) {
                            "प्रकाशित समय: ${article.publishedAtIso ?: "अज्ञात"}"
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
                    // Section 8.1: Report Article Issue button on every item
                    IconButton(
                        onClick = onReportIssue,
                        modifier = Modifier.testTag("report_issue_btn_${article.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = if (isHi) "लेख समस्या रिपोर्ट करें" else "Report article issue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

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
                        enabled = isLinkClickable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetraNavyPrimary,
                            contentColor = NetraAmberPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_source_btn_${article.id}")
                    ) {
                        Text(
                            text = if (!isLinkClickable) {
                                if (isHi) "लिंक अवरुद्ध" else "Link Blocked"
                            } else {
                                if (isHi) "स्रोत लिंक खोलें" else "Open Source"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (isLinkClickable) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHi) {
                            "पारदर्शिता लेजर: संपादकीय वापसी (${uiState.retractedNotices.size}) व अवरुद्ध TEST आइटम (${uiState.rejectedAuditEntries.size})"
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
                        "धारा 2.4, 7.5 व 9.3 प्रवर्तन: वापस लिए गए (editorialState=RETRACTED) या TEST फिक्स्चर (id=test-001) को कभी भी मुख्य समाचार के रूप में प्रदर्शित नहीं किया जाता।"
                    } else {
                        "Sec 2.4, 7.5 & 9.3 Enforcement: Retracted items (editorialState=RETRACTED) and TEST fixtures (id=test-001) are actively blocked from the verified reader feed."
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
                                text = "verificationStatus=${item.verificationStatus.wireKey} | editorialState=${item.editorialState.wireKey} | rightsStatus=${item.rightsStatus.wireKey}",
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
 * Item Detail & Provenance Sheet (Updated for Build Specification v3, Sections 1.2, 2.1, 3.6, 7.3, 8.1):
 * - Repeats `Source: [publisher]` and `Source verification: [status]` near attribution
 *   with `verificationCheckedAt`, `verificationReason`, and the original source link when safe.
 * - Includes "Report article issue" button prefilling `articleId`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailProvenanceSheet(
    article: ArticleRecord,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onReportIssue: () -> Unit,
    onOpenExternalUrl: (String) -> Unit
) {
    BackHandler { onDismiss() }
    val isLinkClickable = remember(article) {
        ValidationAndRightsEngine.isClickableSourceLinkAllowed(article)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("article_detail_sheet")
        ) {
            Text(
                text = if (isHi) "समाचार प्रमाण, स्रोत सत्यापन एवं अधिकार रिकॉर्ड" else "Article Evidence, Source Verification & Rights Record",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Section 7.3: Repeat Source & Source Verification Badge near attribution
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isHi) {
                            "स्रोत (Source): ${article.consolidatedSources.joinToString(", ")}"
                        } else {
                            "Source: ${article.consolidatedSources.joinToString(", ")}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SourceVerificationStatusBadge(
                        status = article.verificationStatus,
                        isHi = isHi
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isHi) {
                            "ईमानदार दायरा (धारा 7.2): यह इस आइटम के लिए दर्ज की गई स्रोत सत्यापन स्थिति है, प्रत्येक तथ्य के सत्य होने की गारंटी नहीं।"
                        } else {
                            "Honest Scope (Sec 7.2): This is the source verification status recorded for the item, not a guarantee that every fact is true."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ProvenanceRow("Article ID", article.id)
            ProvenanceRow("Source Verification", "${article.verificationStatus.labelEn} (${article.verificationStatus.wireKey})")
            ProvenanceRow("verificationCheckedAt", article.verificationCheckedAtIso ?: "null (Pending/Unchecked)")
            ProvenanceRow("verificationReason", article.verificationReason ?: "null")
            ProvenanceRow("Editorial State", "${article.editorialState.labelEn} (${article.editorialState.wireKey})")
            ProvenanceRow("Rights Status", "${article.rightsStatus.wireKey} (${if (isHi) article.rightsStatus.labelHi else article.rightsStatus.labelEn})")
            ProvenanceRow("Source PublishedAt", article.publishedAtIso ?: "null (Unverified)")
            ProvenanceRow(
                "Original Source Link",
                if (isLinkClickable) article.canonicalSourceUrl else "[Withheld / Blocked Unsafe URL]"
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onReportIssue,
                    modifier = Modifier.testTag("detail_report_issue_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHi) "त्रुटि रिपोर्ट करें" else "Report Issue")
                }

                Row {
                    TextButton(onClick = onDismiss) {
                        Text(if (isHi) "बंद करें" else "Close")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = { onOpenExternalUrl(article.canonicalSourceUrl) },
                        enabled = isLinkClickable,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetraNavyPrimary,
                            contentColor = NetraAmberPrimary
                        )
                    ) {
                        Text(
                            if (!isLinkClickable) {
                                if (isHi) "लिंक अवरुद्ध" else "Link Blocked"
                            } else {
                                if (isHi) "HTTPS स्रोत खोलें" else "Open HTTPS Source"
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ProvenanceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.40f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.60f)
        )
    }
}
