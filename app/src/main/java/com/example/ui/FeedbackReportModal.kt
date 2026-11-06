package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FeedbackDraftEntity
import com.example.domain.FeedbackIssueType
import com.example.domain.ValidationAndRightsEngine
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
 * Section 8 (Owner Addition, v2 & v3): Feedback, Private Review & Minimal Data Sheet.
 * - Supports "Report article issue" (with articleId prefilled) and "General feedback" (articleId null).
 * - Issue types: inaccuracy, brokenLink, outdated, other, general.
 * - Required description (20-2000 chars), optional comments (<=500), optional contact (<=120).
 * - Allows anonymous submission without an account or contact.
 * - Rejects scripts/active markup.
 * - Because private storage / reviewer endpoint is not yet connected in Stage 1, remote submit is
 *   honestly disabled with a clear unavailable message (never simulating success) and preserves
 *   optional local drafts on the user's device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackReportModalSheet(
    prefilledArticleId: String?,
    prefilledArticleTitle: String?,
    initialIssueType: FeedbackIssueType,
    feedbackBannerMessage: String?,
    feedbackBannerIsError: Boolean,
    savedDrafts: List<FeedbackDraftEntity>,
    isHi: Boolean,
    onDismiss: () -> Unit,
    onSaveLocalDraft: (
        articleId: String?,
        articleTitle: String?,
        issueType: FeedbackIssueType,
        description: String,
        optionalComments: String?,
        optionalContact: String?
    ) -> Unit,
    onRemoveContactFromDraft: (String) -> Unit,
    onDeleteDraft: (String) -> Unit
) {
    BackHandler { onDismiss() }

    var selectedIssueType by rememberSaveable { mutableStateOf(initialIssueType) }
    var descriptionText by rememberSaveable { mutableStateOf("") }
    var commentsText by rememberSaveable { mutableStateOf("") }
    var contactText by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("feedback_report_sheet")
        ) {
            Text(
                text = if (prefilledArticleId != null) {
                    if (isHi) "लेख समस्या रिपोर्ट करें (Report Article Issue)" else "Report Article Issue (Section 8)"
                } else {
                    if (isHi) "सामान्य प्रतिक्रिया (General Feedback)" else "General Feedback (Section 8)"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Prefilled Article ID Banner (if reporting a specific article)
            if (prefilledArticleId != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Article ID (Prefilled): $prefilledArticleId",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!prefilledArticleTitle.isNullOrBlank()) {
                            Text(
                                text = prefilledArticleTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else {
                Text(
                    text = if (isHi) {
                        "बिना किसी खाते या संपर्क जानकारी के गुमनाम प्रतिक्रिया समर्थित है।"
                    } else {
                        "Anonymous feedback without an account or contact details is fully supported."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Issue Type Chips (inaccuracy / brokenLink / outdated / other / general)
            Text(
                text = if (isHi) "1. समस्या का प्रकार (issueType):" else "1. Issue Type (issueType):",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeedbackIssueType.entries.forEach { type ->
                    FilterChip(
                        selected = selectedIssueType == type,
                        onClick = { selectedIssueType = type },
                        label = {
                            Text(
                                text = if (isHi) type.labelHi else type.labelEn,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NetraNavyPrimary,
                            selectedLabelColor = NetraAmberPrimary
                        ),
                        modifier = Modifier.testTag("feedback_type_${type.wireKey}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Required Description (20-2000 characters, plain text only)
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = {
                    Text(
                        if (isHi) {
                            "2. विवरण आवश्यक (20–2000 अक्षर, केवल सादा पाठ)*"
                        } else {
                            "2. Description Required (20–2000 chars, plain text)*"
                        }
                    )
                },
                supportingText = {
                    val len = descriptionText.trim().length
                    Text(
                        text = if (isHi) {
                            "अक्षर: $len / ${ValidationAndRightsEngine.FEEDBACK_DESC_MAX_CHARS} (न्यूनतम ${ValidationAndRightsEngine.FEEDBACK_DESC_MIN_CHARS}) • कृपया विवरण में व्यक्तिगत या संवेदनशील डेटा न लिखें।"
                        } else {
                            "Chars: $len / ${ValidationAndRightsEngine.FEEDBACK_DESC_MAX_CHARS} (min ${ValidationAndRightsEngine.FEEDBACK_DESC_MIN_CHARS}) • Do not include personal or child data in descriptions."
                        }
                    )
                },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_description_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Optional Comments (v3 addition)
            OutlinedTextField(
                value = commentsText,
                onValueChange = { commentsText = it },
                label = {
                    Text(
                        if (isHi) {
                            "3. अतिरिक्त टिप्पणी (वैकल्पिक • Optional Comments)"
                        } else {
                            "3. Additional Comments (Optional, max 500 chars)"
                        }
                    )
                },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_comments_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Optional Contact
            OutlinedTextField(
                value = contactText,
                onValueChange = { contactText = it },
                label = {
                    Text(
                        if (isHi) {
                            "4. संपर्क जानकारी (पूरी तरह वैकल्पिक • बिना संपर्क के भी भेजें)"
                        } else {
                            "4. Optional Contact (Leave blank for anonymous report)"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feedback_contact_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Mandatory Privacy, Contact & Retention Notice (Section 8.6 & 8.7)
            Card(
                colors = CardDefaults.cardColors(containerColor = NetraAmberMuted),
                border = BorderStroke(1.dp, NetraAmberDeep),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = NetraNavyDeep,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) {
                                "डेटा न्यूनतमकरण और प्रतिधारण सूचना (धारा 8.6–8.7)"
                            } else {
                                "Minimal Data, Contact & Retention Notice (Sec 8.6–8.7)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = NetraNavyDeep
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHi) {
                            "• वैकल्पिक संपर्क का उपयोग केवल फॉलो-अप के लिए होता है और इसे केवल अधिकृत समीक्षक देख सकते हैं।\n" +
                                "• प्रस्तावित प्रतिधारण डिफ़ॉल्ट: समाधान के 30 दिनों के भीतर संपर्क हटा दिया जाता है और अनसुलझी प्रतिक्रिया 90 दिनों में हटा दी जाती है (लॉन्च से पहले कानूनी/DPDP समीक्षा आवश्यक)।\n" +
                                "• फीडबैक कभी भी सार्वजनिक फ़ीड JSON, GitHub कमिट या एनालिटिक्स में नहीं भेजा जाता और न ही यह स्वतः verificationStatus बदलता है।"
                        } else {
                            "• Optional contact is used strictly for follow-up with limited reviewer access. No account, ad tracker, or location is attached.\n" +
                                "• Proposed retention defaults: delete optional contact within 30 days after resolution and unresolved feedback within 90 days (subject to pre-launch legal/DPDP review).\n" +
                                "• Feedback never enters public feed JSON or GitHub issues and never self-verifies a source."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NetraNavyDeep
                    )
                }
            }

            // Validation / Draft Status Banner
            if (feedbackBannerMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = if (feedbackBannerIsError) BreakingCrimsonBg else VerifiedEmeraldBg,
                    border = BorderStroke(1.dp, if (feedbackBannerIsError) BreakingCrimson else VerifiedEmerald),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = feedbackBannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (feedbackBannerIsError) BreakingCrimson else VerifiedEmerald,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section 8.9 Honest Unavailable Remote Submit Card + Active Local Draft Button
            Card(
                colors = CardDefaults.cardColors(containerColor = StaleAmberBg),
                border = BorderStroke(1.dp, StaleAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = StaleAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHi) {
                                "धारा 8.9: रिमोट सर्वर सबमिट अभी अक्षम (कोई नकली रसीद नहीं)"
                            } else {
                                "Section 8.9: Private Server Submit Disabled (No Simulated Delivery)"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = StaleAmber
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHi) {
                            "निजी स्टोरेज (Cloudflare Worker + D1) और समीक्षक प्रमाणीकरण अभी कनेक्ट नहीं हैं। नकली सफलता दिखाने के बजाय रिमोट सबमिट बटन को ईमानदारी से अक्षम किया गया है। आप नीचे अपना ड्राफ्ट स्थानीय रूप से फ़ोन पर सहेज सकते हैं।"
                        } else {
                            "Private review storage (Cloudflare Worker + D1) is not yet connected. Per Section 8.9, remote submit is honestly disabled rather than simulating success. You may save or manage a local draft on your device below."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = NetraNavyDeep
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Disabled Remote Submit Button (Section 8.9 requirement)
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("disabled_remote_feedback_submit_btn")
                        ) {
                            Text(
                                text = if (isHi) "सर्वर पर भेजें (अक्षम)" else "Submit to Server (Disabled)",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        // Working Local Draft Save Button
                        Button(
                            onClick = {
                                onSaveLocalDraft(
                                    prefilledArticleId,
                                    prefilledArticleTitle,
                                    selectedIssueType,
                                    descriptionText,
                                    commentsText.ifBlank { null },
                                    contactText.ifBlank { null }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NetraNavyPrimary,
                                contentColor = NetraAmberPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_local_feedback_draft_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isHi) "स्थानीय ड्राफ्ट सहेजें" else "Save Local Draft",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // Saved Local Drafts List (with Delete Contact & Delete Draft actions)
            if (savedDrafts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHi) {
                        "फ़ोन पर सहेजे गए स्थानीय ड्राफ्ट (${savedDrafts.size}):"
                    } else {
                        "Local Drafts on This Device (${savedDrafts.size}):"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                savedDrafts.forEach { draft ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Draft ID: ${draft.feedbackId} • Type: ${draft.issueTypeWire} ${if (draft.articleId != null) "• Article: ${draft.articleId}" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = draft.description,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                            if (!draft.optionalContact.isNullOrBlank()) {
                                Text(
                                    text = "Contact: ${draft.optionalContact}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NetraAmberDeep
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                if (!draft.optionalContact.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { onRemoveContactFromDraft(draft.feedbackId) }
                                    ) {
                                        Text(
                                            text = if (isHi) "संपर्क हटाएं" else "Delete Contact Only",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = { onDeleteDraft(draft.feedbackId) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = BreakingCrimson,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = if (isHi) "ड्राफ्ट हटाएं" else "Delete Draft",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BreakingCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(if (isHi) "बंद करें" else "Close")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
