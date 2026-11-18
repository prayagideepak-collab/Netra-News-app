package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BookmarkReconciliationSummary
import com.example.data.local.ArticleEntity
import com.example.data.local.BookmarkEntity
import com.example.domain.AppLanguage
import com.example.domain.BulletinDataStatus
import com.example.domain.EarthquakeEventRecord
import com.example.domain.EditorialState
import com.example.domain.OfflineSyncStatus
import com.example.domain.RightsStatus
import com.example.domain.VerificationStatus
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
 * Saved Bookmarks & Offline Reading Mode Screen (Section 1.2, 3.5, 4.1 Stage 2 + Offline Reading Mode):
 * - Allows users to bookmark articles and read them completely offline without internet access.
 * - Clearly marks saved articles as OFFLINE CONTENT and surfaces graceful sync status when an article
 *   is updated, retracted/blocked, or removed from the upstream live feed.
 */
@Composable
fun SavedBookmarksScreen(
    bookmarks: List<BookmarkEntity>,
    offlineArticles: List<ArticleEntity> = emptyList(),
    language: AppLanguage,
    isOfflineMode: Boolean = false,
    reconciliationSummary: BookmarkReconciliationSummary? = null,
    onOpenOfflineArticle: (ArticleEntity) -> Unit = {},
    onRemoveBookmark: (String) -> Unit,
    onClearAllBookmarks: () -> Unit,
    onOpenExternalUrl: (String) -> Unit,
    onNavigateBackHome: () -> Unit
) {
    BackHandler { onNavigateBackHome() }
    val isHi = language == AppLanguage.HINDI

    // Merge Room ArticleEntity (primary offline reading table) with any legacy BookmarkEntity rows
    val effectiveArticles: List<ArticleEntity> = remember(offlineArticles, bookmarks) {
        if (offlineArticles.isNotEmpty()) {
            offlineArticles
        } else {
            bookmarks.map { b ->
                ArticleEntity(
                    id = b.articleId,
                    title = b.title,
                    language = b.language,
                    category = b.category,
                    scope = b.scopeWire,
                    cityId = b.cityId,
                    publisherName = b.publisherName,
                    sourceUrl = b.canonicalSourceUrl,
                    publishedAt = b.publishedAtIso,
                    fetchedAt = b.savedAtEpochMs,
                    verificationStatus = b.verificationStatusWire,
                    rightsStatus = b.rightsStatusWire,
                    stateCode = b.stateCode,
                    permittedSummary = b.permittedSummary,
                    savedAtEpochMs = b.savedAtEpochMs,
                    isBookmarked = true,
                    offlineSyncStatus = OfflineSyncStatus.AVAILABLE_OFFLINE.wireKey
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("saved_bookmarks_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Offline Reading Mode Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isOfflineMode) StaleAmberBg else NetraAmberMuted
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isOfflineMode) StaleAmber else NetraAmberDeep
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOfflineMode) Icons.Default.CloudOff else Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = if (isOfflineMode) StaleAmber else VerifiedEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHi) {
                                    "ऑफ़लाइन पठन मोड • सहेजे गए लेख (${effectiveArticles.size})"
                                } else {
                                    "Offline Reading Mode • Saved Articles (${effectiveArticles.size})"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = NetraNavyDeep,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isOfflineMode) {
                                    if (isHi) {
                                        "इंटरनेट बंद है — नीचे सभी बुकमार्क किए गए लेख बिना नेटवर्क के पढ़ने के लिए उपलब्ध हैं।"
                                    } else {
                                        "No internet connection — all saved articles below remain readable offline from Room storage."
                                    }
                                } else {
                                    if (isHi) {
                                        "100% स्थानीय Room स्टोरेज (बिना इंटरनेट के पठनीय)। लाइव फ़ीड अपडेट होने पर सहेजे गए लेख सुरक्षित रूप से अद्यतन होते हैं।"
                                    } else {
                                        "100% On-Device Offline Storage (Readable without internet). Saved copies reconcile updates & removals gracefully."
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = NetraNavyDeep
                            )
                        }
                        if (effectiveArticles.isNotEmpty()) {
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

                    if (reconciliationSummary != null &&
                        (reconciliationSummary.updatedCount > 0 ||
                            reconciliationSummary.retractedOrBlockedCount > 0 ||
                            reconciliationSummary.removedFromLiveFeedCount > 0)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NetraAmberDeep),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isHi) {
                                    "सिंक स्थिति: ${reconciliationSummary.updatedCount} अद्यतन • ${reconciliationSummary.retractedOrBlockedCount} वापस/अवरुद्ध • ${reconciliationSummary.removedFromLiveFeedCount} लाइव से हटाए (ऑफ़लाइन सुरक्षित)"
                                } else {
                                    "Sync reconciliation: ${reconciliationSummary.updatedCount} updated • ${reconciliationSummary.retractedOrBlockedCount} retracted/blocked • ${reconciliationSummary.removedFromLiveFeedCount} archived offline"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = NetraNavyDeep,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (effectiveArticles.isEmpty()) {
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
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = NetraAmberDeep,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isHi) "कोई ऑफ़लाइन लेख सहेजा नहीं गया है" else "No Offline Articles Saved Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isHi) {
                                "समाचार फ़ीड में किसी भी लेख पर बुकमार्क आइकन दबाएं ताकि आप उसे बिना इंटरनेट के भी यहाँ पढ़ सकें।"
                            } else {
                                "Tap the bookmark icon on any article in the newspaper feed to save a verified copy for offline reading."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(effectiveArticles, key = { it.id }) { entity ->
                val domainArticle = entity.toDomain()
                val syncStatus = entity.offlineSyncEnum
                val isWithheld = syncStatus == OfflineSyncStatus.RETRACTED_OR_BLOCKED ||
                    domainArticle.verificationStatus == VerificationStatus.BLOCKED ||
                    domainArticle.editorialState == EditorialState.RETRACTED

                val syncBadgeBg = when (syncStatus) {
                    OfflineSyncStatus.AVAILABLE_OFFLINE -> VerifiedEmeraldBg
                    OfflineSyncStatus.UPDATED_FROM_FEED -> NetraAmberMuted
                    OfflineSyncStatus.REMOVED_FROM_LIVE_FEED -> StaleAmberBg
                    OfflineSyncStatus.RETRACTED_OR_BLOCKED -> BreakingCrimsonBg
                }
                val syncBadgeFg = when (syncStatus) {
                    OfflineSyncStatus.AVAILABLE_OFFLINE -> VerifiedEmerald
                    OfflineSyncStatus.UPDATED_FROM_FEED -> NetraAmberDeep
                    OfflineSyncStatus.REMOVED_FROM_LIVE_FEED -> StaleAmber
                    OfflineSyncStatus.RETRACTED_OR_BLOCKED -> BreakingCrimson
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        width = if (isWithheld) 1.5.dp else 1.dp,
                        color = if (isWithheld) BreakingCrimson else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenOfflineArticle(entity) }
                        .testTag("saved_item_${entity.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top Row: OFFLINE CONTENT badge + Reconciliation Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = NetraNavyPrimary,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DownloadDone,
                                        contentDescription = null,
                                        tint = NetraAmberPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHi) "ऑफ़लाइन सामग्री" else "OFFLINE CONTENT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NetraAmberPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                color = syncBadgeBg,
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, syncBadgeFg)
                            ) {
                                Text(
                                    text = if (isHi) syncStatus.labelHi else syncStatus.labelEn,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = syncBadgeFg,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${entity.publisherName} • ${entity.category}",
                            style = MaterialTheme.typography.labelMedium,
                            color = VerifiedEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        SourceVerificationStatusBadge(
                            status = domainArticle.verificationStatus,
                            isHi = isHi
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = entity.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Offline Body / Notice Handling (Graceful update/removal/rights enforcement)
                        if (isWithheld) {
                            Surface(
                                color = BreakingCrimsonBg,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, BreakingCrimson),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isHi) {
                                        "संपादकीय/अधिकार अवरोध: यह लेख स्रोत द्वारा वापस लिया या अवरुद्ध किया गया है। असुरक्षित पाठ हटा दिया गया है। कारण: ${entity.verificationReason ?: "अपुष्ट"}"
                                    } else {
                                        "Retracted / Blocked Notice: Source retracted or blocked this item during sync; cached prose and link have been withheld. Reason: ${entity.verificationReason ?: "Withheld"}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BreakingCrimson,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        } else if (!domainArticle.safePermittedSummary.isNullOrBlank()) {
                            Text(
                                text = domainArticle.safePermittedSummary.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isHi) {
                                        "केवल-लिंक बुकमार्क (LINK_ONLY): शीर्षक और स्रोत प्रमाण ऑफ़लाइन उपलब्ध हैं; पूर्ण बाहरी लेख के लिए इंटरनेट आवश्यक है।"
                                    } else {
                                        "Link-Only Offline Bookmark: Headline & provenance are readable offline; external publisher link requires internet."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        if (syncStatus == OfflineSyncStatus.REMOVED_FROM_LIVE_FEED) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isHi) {
                                    "सूचना: यह लेख अब लाइव फ़ीड में नहीं है, लेकिन आपकी ऑफ़लाइन पठन सूची में सुरक्षित रखा गया है।"
                                } else {
                                    "Notice: This article is no longer in the active live feed, but your saved offline copy is preserved."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = StaleAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isHi) {
                                        "सहेजा गया: ${NetraViewModel.formatTimestamp(entity.savedAtEpochMs)}"
                                    } else {
                                        "Saved: ${NetraViewModel.formatTimestamp(entity.savedAtEpochMs)}"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isHi) "ऑफ़लाइन पढ़ने के लिए टैप करें" else "Tap card to read offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onRemoveBookmark(entity.id) },
                                    modifier = Modifier.testTag("remove_offline_bookmark_${entity.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkRemove,
                                        contentDescription = if (isHi) "ऑफ़लाइन सूची से हटाएं" else "Remove offline article",
                                        tint = BreakingCrimson
                                    )
                                }
                                Button(
                                    onClick = { onOpenExternalUrl(entity.sourceUrl) },
                                    enabled = !isWithheld && !isOfflineMode,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NetraNavyPrimary,
                                        contentColor = NetraAmberPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when {
                                            isWithheld -> if (isHi) "लिंक अवरुद्ध" else "Withheld"
                                            isOfflineMode -> if (isHi) "ऑफ़लाइन (लिंक बंद)" else "Offline"
                                            else -> if (isHi) "स्रोत खोलें" else "Open Source"
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    if (!isWithheld && !isOfflineMode) {
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
}

/**
 * In-App Offline Reader Modal Sheet allowing full inspection of saved offline content
 * without requiring an internet connection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineArticleReaderSheet(
    entity: ArticleEntity,
    isHi: Boolean,
    isOfflineMode: Boolean,
    onDismiss: () -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onOpenExternalUrl: (String) -> Unit
) {
    BackHandler { onDismiss() }
    val article = entity.toDomain()
    val syncStatus = entity.offlineSyncEnum
    val isWithheld = syncStatus == OfflineSyncStatus.RETRACTED_OR_BLOCKED ||
        article.verificationStatus == VerificationStatus.BLOCKED ||
        article.editorialState == EditorialState.RETRACTED

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("offline_reader_sheet")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = NetraNavyPrimary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isHi) "ऑफ़लाइन पठन मोड • OFFLINE CONTENT" else "OFFLINE READING MODE",
                        style = MaterialTheme.typography.labelSmall,
                        color = NetraAmberPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = NetraAmberMuted,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (isHi) syncStatus.labelHi else syncStatus.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = NetraNavyDeep,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${article.publisherName} • ${article.category} • ${article.scope.wireKey.uppercase()}",
                style = MaterialTheme.typography.labelLarge,
                color = VerifiedEmerald
            )
            Spacer(modifier = Modifier.height(8.dp))
            SourceVerificationStatusBadge(status = article.verificationStatus, isHi = isHi)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            if (isWithheld) {
                Surface(
                    color = BreakingCrimsonBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BreakingCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isHi) {
                            "यह लेख संपादकीय समीक्षा के दौरान वापस लिया या अवरुद्ध किया गया था। पाठ और असुरक्षित लिंक हटा दिए गए हैं। कारण: ${entity.verificationReason ?: "अपुष्ट"}"
                        } else {
                            "This saved article was retracted or blocked during a feed update. Its summary and external URL have been withheld. Reason: ${entity.verificationReason ?: "Withheld"}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = BreakingCrimson,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else if (!article.safePermittedSummary.isNullOrBlank()) {
                Text(
                    text = article.safePermittedSummary.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                Text(
                    text = if (isHi) {
                        "यह एक केवल-लिंक (LINK_ONLY) सहेजा गया लेख है। शीर्षक, प्रकाशक और सत्यापन विवरण ऑफ़लाइन पठनीय हैं; पूर्ण लेख ऑनलाइन स्रोत URL पर उपलब्ध है।"
                    } else {
                        "This bookmark is a LINK_ONLY attribution record. Headline, publisher, and verification provenance are preserved offline; full article prose remains at the publisher's HTTPS portal."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Published: ${article.publishedAt ?: "Unknown"} • Saved Offline: ${NetraViewModel.formatTimestamp(entity.savedAtEpochMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Source URL: ${if (isWithheld) "[Withheld]" else article.sourceUrl}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        onRemoveBookmark(entity.id)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkRemove,
                        contentDescription = null,
                        tint = BreakingCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isHi) "ऑफ़लाइन से हटाएं" else "Remove Offline")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isHi) "बंद करें" else "Close")
                    }
                    Button(
                        onClick = { onOpenExternalUrl(article.sourceUrl) },
                        enabled = !isWithheld && !isOfflineMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NetraNavyPrimary,
                            contentColor = NetraAmberPrimary
                        )
                    ) {
                        Text(
                            if (isOfflineMode) {
                                if (isHi) "इंटरनेट आवश्यक" else "Needs Internet"
                            } else {
                                if (isHi) "स्रोत खोलें" else "Open Source"
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
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
