package com.example.settings

import com.example.domain.FeatureGateStatus
import com.example.domain.FeatureMatrixItem
import com.example.domain.OpenDecisionRecord

/**
 * Governance, Stage Matrix, Open Decisions, and Review Packet Ledger (Build Specification v3, 6 Nov 2026).
 */
object GovernanceLedgerData {

    const val SPEC_DATE_LABEL = "Build Spec v3 | 6 Nov 2026 (12:28 IST Repo Audit) | Target: 15 Nov 2026"
    const val APP_VERSION_LABEL = "v1.0.0-stage1-v3"
    const val WEB_REFERENCE_REPO = "github.com/prayagideepak-collab/Netra-News-Web (Audited Head: 804931da1228cfbe3b1da8462ac9d95198db1fe4)"
    const val PACKAGE_ID_DECISION = "com.aistudio.netranews.kxvqpm (Namespace: com.example, minSdk 24, targetSdk 36)"

    /**
     * Feature Matrix per Section 10.3 & Power Gate:
     * Columns: Required / Implemented / Tested / Blocked + Power Tier, Wakeup Cost, Network Cost.
     */
    val FEATURE_MATRIX: List<FeatureMatrixItem> = listOf(
        FeatureMatrixItem(
            id = "stage1-reader",
            stageNumber = 1,
            featureTitleHi = "चरण-1: मैनुअल शहर समाचार पठन, स्रोत श्रेय व HTTPS लिंक-आउट",
            featureTitleEn = "Stage 1: Manual-City Reader, Source Attribution & HTTPS Link-Out",
            requiredBySpec = true,
            status = FeatureGateStatus.IMPLEMENTED,
            unitTested = true,
            powerTierRequired = "Tier-0 (All Tiers: Normal -> Survival)",
            wakeupsPerHourCost = "0 wakeups/hr (Foreground only)",
            networkCallsPerHourCost = "1–12 calls/hr (Conditional ETag/HTTPS)",
            notesHi = "स्थानीय, राज्य व राष्ट्रीय टैब; प्रकाशक नाम व अधिकार स्थिति (LINK_ONLY / ORIGINAL) प्रत्येक कार्ड पर।",
            notesEn = "Local, State & National tabs; publisher name & rights badge on every item; external HTTPS Custom Tab/Browser."
        ),
        FeatureMatrixItem(
            id = "v3-verification-indicators",
            stageNumber = 1,
            featureTitleHi = "v3 अतिरिक्त (धारा 7): स्रोत सत्यापन संकेतक (Verified / Pending / Blocked)",
            featureTitleEn = "v3 Addition (Sec 7): Source Verification Indicators (Verified / Pending / Blocked)",
            requiredBySpec = true,
            status = FeatureGateStatus.TESTED,
            unitTested = true,
            powerTierRequired = "Tier-0 (All Tiers)",
            wakeupsPerHourCost = "0 wakeups/hr",
            networkCallsPerHourCost = "0 extra calls",
            notesHi = "होम कार्ड व विवरण पर 'Source: [publisher] • Source verification: [status]'; अज्ञात स्थिति Pending पर फ़ॉलबैक; Pending/Blocked ब्रेकिंग में वर्जित; मोनोक्रोम व TalkBack सुलभ।",
            notesEn = "Shows 'Source: [publisher] • Source verification: [Verified/Pending/Blocked]'; malformed falls back to Pending; Pending/Blocked barred from Breaking; monochrome & TalkBack accessible."
        ),
        FeatureMatrixItem(
            id = "v3-feedback-minimal-data",
            stageNumber = 1,
            featureTitleHi = "v3 अतिरिक्त (धारा 8): लेख त्रुटि रिपोर्ट, सामान्य प्रतिक्रिया व स्थानीय ड्राफ्ट",
            featureTitleEn = "v3 Addition (Sec 8): Report Article Issue, General Feedback & Local Drafts",
            requiredBySpec = true,
            status = FeatureGateStatus.TESTED,
            unitTested = true,
            powerTierRequired = "Tier-0 (Local Draft) / Normal-Saver (When Endpoint Ready)",
            wakeupsPerHourCost = "0 wakeups/hr",
            networkCallsPerHourCost = "0 calls in Stage 1 (Private D1 Endpoint Unconnected -> Honest Disabled Submit)",
            notesHi = "20–2000 अक्षर, स्क्रिप्ट अस्वीकृति, स्पैम कूलडाउन/हनीपॉट, बिना खाते के गुमनाम ड्राफ्ट; निजी सर्वर जुड़ने तक रिमोट सबमिट ईमानदारी से अक्षम (कोई नकली सफलता नहीं)।",
            notesEn = "20–2000 chars, script rejection, cooldown/honeypot, anonymous without account/contact; remote submit honestly disabled until private Worker+D1 endpoint is connected."
        ),
        FeatureMatrixItem(
            id = "stage1-schema-rights",
            stageNumber = 1,
            featureTitleHi = "चरण-1: साझा स्कीमा (v1), अधिकार प्रवर्तन, संपादकीय वापसी (Retracted) व test-001 अवरोधन",
            featureTitleEn = "Stage 1: Shared Schema (v1), Rights Contract, Editorial Retraction & test-001 Guard",
            requiredBySpec = true,
            status = FeatureGateStatus.TESTED,
            unitTested = true,
            powerTierRequired = "Tier-0 (All Tiers)",
            wakeupsPerHourCost = "0 wakeups/hr",
            networkCallsPerHourCost = "0 extra calls",
            notesHi = "test-001, भविष्य का समय, असत्यापित/अवरुद्ध लेख और गैर-HTTPS URL अस्वीकृत; Retraction एक अलग editorialState के रूप में चिह्नित।",
            notesEn = "Rejects test-001, future/missing timestamps, and unapproved hosts; Retraction tracked as a separate editorialState."
        ),
        FeatureMatrixItem(
            id = "stage2-bookmarks-prefs",
            stageNumber = 2,
            featureTitleHi = "चरण-2: स्थानीय बुकमार्क (Room) व स्थायी शहर प्राथमिकताएं",
            featureTitleEn = "Stage 2: Local Bookmarks (Room) & Atomic City Preferences",
            requiredBySpec = true,
            status = FeatureGateStatus.IMPLEMENTED,
            unitTested = true,
            powerTierRequired = "Tier-0 (All Tiers)",
            wakeupsPerHourCost = "0 wakeups/hr",
            networkCallsPerHourCost = "0 network calls (100% On-Device)",
            notesHi = "कोई क्लाउड खाता या सिंक नहीं। बुकमार्क और शहर चयन पूरी तरह ऑफ़लाइन काम करते हैं।",
            notesEn = "No cloud account or sync. Bookmarks and city preferences persist atomically on device."
        ),
        FeatureMatrixItem(
            id = "stage3-coarse-location",
            stageNumber = 3,
            featureTitleHi = "चरण-3: अग्रभूमि अनुमानित शहर पहचान (ACCESS_COARSE_LOCATION)",
            featureTitleEn = "Stage 3: Foreground Approximate City Detection (ACCESS_COARSE_LOCATION)",
            requiredBySpec = true,
            status = FeatureGateStatus.IMPLEMENTED,
            unitTested = true,
            powerTierRequired = "Normal / Saver Tiers Only (Suspended <15%)",
            wakeupsPerHourCost = "0 background wakeups (On-demand 6s timeout)",
            networkCallsPerHourCost = "0 geo API calls (Offline CC-BY Gazetteer)",
            notesHi = "कच्चे निर्देशांक तुरंत हटा दिए जाते हैं; निकटतम-शहर अस्पष्टता जांच व मैनुअल विकल्प सदा उपलब्ध।",
            notesEn = "Raw coordinates scrubbed in finally block; nearest-city ambiguity prompt & manual fallback always ready."
        ),
        FeatureMatrixItem(
            id = "stage4-weather-earthquake",
            stageNumber = 4,
            featureTitleHi = "चरण-4: सत्यापित मौसम (IMD) व भूकंप (NCS) सूचना बुलेटिन",
            featureTitleEn = "Stage 4: Verified Weather (IMD) & Earthquake (NCS) Civic Bulletins",
            requiredBySpec = true,
            status = FeatureGateStatus.IMPLEMENTED,
            unitTested = true,
            powerTierRequired = "Tier-0 (Cached) / Normal-Critical (Refresh)",
            wakeupsPerHourCost = "0 wakeups/hr (Foreground pull)",
            networkCallsPerHourCost = "<= 2 calls/hr batched",
            notesHi = "अनुपलब्ध/पुराना डेटा वास्तविक शून्य (0) से अलग दिखता है। पूर्व-चेतावनी प्रणाली का दावा नहीं।",
            notesEn = "Unavailable/stale visually distinct from real zero/all-clear. Explicit non-early-warning disclaimer."
        ),
        FeatureMatrixItem(
            id = "stage5-background-alerts",
            stageNumber = 5,
            featureTitleHi = "चरण-5: पृष्ठभूमि मौसम/भूकंप अलर्ट पायलट (निःशुल्क ट्रांसपोर्ट समीक्षा लंबित)",
            featureTitleEn = "Stage 5: Background Weather/Earthquake Alert Pilot",
            requiredBySpec = false,
            status = FeatureGateStatus.PLANNED_STAGE,
            unitTested = false,
            powerTierRequired = "Stage-5 Pilot (Deferred)",
            wakeupsPerHourCost = "0 in Stage 1 (Disabled)",
            networkCallsPerHourCost = "0 in Stage 1 (Disabled)",
            notesHi = "चरण-5 के लिए नियोजित: निःशुल्क पुश ट्रांसपोर्ट व वास्तविक फ़ोन डोज़ (Doze) परीक्षण से पहले बटन अक्षम।",
            notesEn = "Planned Stage-5: Disabled in UI with plain explanation until free push transport & phone Doze acceptance."
        ),
        FeatureMatrixItem(
            id = "stage6-world-coverage",
            stageNumber = 6,
            featureTitleHi = "चरण-6: विश्व समाचार (World Tab) एवं व्यापक प्रकाशक अनुबंध",
            featureTitleEn = "Stage 6: World News Tab & Expanded Publisher Licences",
            requiredBySpec = false,
            status = FeatureGateStatus.PLANNED_STAGE,
            unitTested = false,
            powerTierRequired = "Stage-6 (Deferred)",
            wakeupsPerHourCost = "0 in Stage 1",
            networkCallsPerHourCost = "0 in Stage 1",
            notesHi = "चरण-6 के लिए नियोजित: अंतरराष्ट्रीय स्रोत और प्रकाशक लिखित अनुमति की पुष्टि तक अक्षम।",
            notesEn = "Planned Stage-6: Disabled until international source allowlist & publisher rights verification."
        ),
        FeatureMatrixItem(
            id = "blocked-paid-ai-cloud",
            stageNumber = 0,
            featureTitleHi = "नीति अवरोध: क्लाउड सिंक, सशुल्क Maps API, और AI जनरेटेड समाचार/अनुवाद",
            featureTitleEn = "Policy Gate: Cloud Auth/Sync, Paid Maps/Geo APIs & AI Headline Generation",
            requiredBySpec = false,
            status = FeatureGateStatus.BLOCKED_POLICY,
            unitTested = true,
            powerTierRequired = "Blocked (0 mAh drain)",
            wakeupsPerHourCost = "0 wakeups/hr",
            networkCallsPerHourCost = "0 calls/hr",
            notesHi = "अनुबंध 1.6, 2.4 व 3.5 द्वारा अवरुद्ध: कोई कार्ड/सशुल्क API नहीं, कोई क्लाउड लॉगिन नहीं, और तथ्यात्मक सुर्खियों पर कोई AI अनुवाद/कल्पना नहीं।",
            notesEn = "Blocked per Spec 1.6, 2.4 & 3.5: Free-only (no paid Maps/AI quota), local-only bookmarks (no cloud auth), and zero AI-invented headlines or translations."
        )
    )

    /**
     * Explicit Assumptions & Unresolved Open Decisions per Section 10.4 (Updated for v3):
     */
    val OPEN_DECISIONS: List<OpenDecisionRecord> = listOf(
        OpenDecisionRecord(
            id = "dec-repo-target",
            decisionTitleHi = "1. संदर्भ वेब रिपॉजिटरी (12:28 IST अपडेट) और एंड्रॉइड परिनियोजन लक्ष्य",
            decisionTitleEn = "1. Reference Web Repository (12:28 IST Update) & Android Target",
            currentAssumptionHi = "स्वामी ने वेब संदर्भ रिपॉजिटरी github.com/prayagideepak-collab/Netra-News-Web (सत्यापित हेड: 804931d, 10 डोमेन परीक्षण उत्तीर्ण) प्रदान की है। यह एंड्रॉइड ऐप उसी साझा स्कीमा (v1) और सत्यापन संकेतकों का पालन करता है।",
            currentAssumptionEn = "Owner supplied github.com/prayagideepak-collab/Netra-News-Web (audited head 804931d, 10 domain tests passing). This Android codebase mirrors the shared schema v1, verification indicators, and feedback contract.",
            ownerActionNeededHi = "एंड्रॉइड समीक्षा शाखा (review branch) या मोनोरेपो सबफ़ोल्डर गंतव्य की पुष्टि करें।",
            ownerActionNeededEn = "Confirm whether Android source merges into a branch of Netra-News-Web or a dedicated Android repo."
        ),
        OpenDecisionRecord(
            id = "dec-feedback-endpoint",
            decisionTitleHi = "2. निजी प्रतिक्रिया स्टोरेज एंडपॉइंट और प्रतिधारण नीति (Section 8 Private Feedback Endpoint)",
            decisionTitleEn = "2. Private Feedback Endpoint (Cloudflare Worker + D1) & Retention Approval",
            currentAssumptionHi = "निजी HTTPS सबमिशन एंडपॉइंट (जैसे Cloudflare Worker + D1 निःशुल्क टियर) अभी तक लाइव नहीं जोड़ा गया है। धारा 8.9 के अनुसार, रिमोट सबमिट को ईमानदारी से अक्षम रखा गया है और केवल स्थानीय डिवाइस ड्राफ्ट (Room) सहेजा जाता है।",
            currentAssumptionEn = "Private HTTPS feedback endpoint (e.g., Cloudflare Worker + D1 free plan) is not yet provisioned. Per Section 8.9, remote submit is honestly disabled (never simulated) and optional local drafts are preserved on device.",
            ownerActionNeededHi = "निःशुल्क कोटा जांच के बाद निजी एंडपॉइंट URL और 30/90-दिन प्रतिधारण नीति (DPDP समीक्षा सहित) को मंजूरी दें।",
            ownerActionNeededEn = "Approve the 30-day contact / 90-day unresolved retention defaults after legal/DPDP review and supply the verified private HTTPS worker endpoint."
        ),
        OpenDecisionRecord(
            id = "dec-android-signing",
            decisionTitleHi = "3. अंतिम पैकेज नाम, minSdk और प्रोडक्शन साइनिंग पहचान",
            decisionTitleEn = "3. Final Package ID, minSdk & Production Signing Identity",
            currentAssumptionHi = "applicationId='com.aistudio.netranews.kxvqpm', minSdk=24, targetSdk=36। कोई प्राइवेट कीस्टोर Git में नहीं रखा गया है।",
            currentAssumptionEn = "applicationId='com.aistudio.netranews.kxvqpm', minSdk=24, targetSdk=36. Debug build uses container debug keystore; production signing keys excluded from Git.",
            ownerActionNeededHi = "प्रोडक्शन APK रिलीज़ से पहले अंतिम पैकेज नाम और सुरक्षित ऑफ़लाइन अपलोड कीस्टोर प्रमाणपत्र की पुष्टि करें।",
            ownerActionNeededEn = "Establish final production package name and hardware-backed upload keystore before first public APK."
        ),
        OpenDecisionRecord(
            id = "dec-publisher-rights",
            decisionTitleHi = "4. प्रकाशक लिखित अनुमति और पूर्ण-पाठ अधिकार (Publisher Permissions)",
            decisionTitleEn = "4. Permitted Source Feeds & Publisher Rights Replies",
            currentAssumptionHi = "पूर्व में भेजे गए प्रकाशक अनुमति अनुरोधों के उत्तर अभी असत्यापित हैं। इसलिए गैर-सरकारी फ़ीड डिफ़ॉल्ट रूप से LINK_ONLY (केवल शीर्षक + बाहरी लिंक) पर हैं।",
            currentAssumptionEn = "Publisher permission replies are not yet verified. All broadcasters default to strict LINK_ONLY attribution; only public-domain official releases (PIB/IMD/NCS) show brief permitted metadata.",
            ownerActionNeededHi = "प्रत्येक प्रकाशक की लिखित अनुमति को स्रोत लेजर (Source Ledger) में दर्ज करें।",
            ownerActionNeededEn = "Record signed publisher licensing replies before enabling licensed summaries or thumbnails."
        ),
        OpenDecisionRecord(
            id = "dec-language-policy",
            decisionTitleHi = "5. स्रोत-भाषा बनाम अनुवाद नीति (Source-Language Policy)",
            decisionTitleEn = "5. Source-Language & Translation Policy",
            currentAssumptionHi = "हिंदी-प्रथम (Hindi-first) इंटरफ़ेस के साथ अंग्रेज़ी विकल्प। तथ्यात्मक समाचार शीर्षकों पर कोई स्वचालित या AI मशीनी अनुवाद नहीं किया जाता।",
            currentAssumptionEn = "Hindi-first UI with English toggle. Factual article headlines are rendered strictly in their verified source language without machine translation.",
            ownerActionNeededHi = "द्विभाषी प्रकाशक फ़ीड उपलब्ध होने पर ही समानांतर शीर्षक जोड़ें।",
            ownerActionNeededEn = "Only pair Hindi/English headlines when the source publisher issues official bilingual bulletins."
        ),
        OpenDecisionRecord(
            id = "dec-alert-thresholds",
            decisionTitleHi = "6. आपातकालीन अलर्ट थ्रेशोल्ड और मुद्रीकरण सीमाएं (Alert Thresholds & Monetisation)",
            decisionTitleEn = "6. Alert Thresholds & Monetisation Boundaries (Section 10.4)",
            currentAssumptionHi = "चरण-1 में पुश नोटिफिकेशन अक्षम हैं (कोई POST_NOTIFICATIONS अनुमति नहीं)। लाइव या ब्रेकिंग न्यूज़ और आपातकालीन बुलेटिन के दौरान कोई विज्ञापन या ट्रैकर नहीं है।",
            currentAssumptionEn = "Push notifications are disabled in Stage-1 (no POST_NOTIFICATIONS requested). Zero ads or third-party trackers during Live, Breaking News, or civic bulletins.",
            ownerActionNeededHi = "चरण-5 में IMD/NCS अलर्ट के लिए न्यूनतम भूकंप परिमाण (Mw) और गंभीर मौसम थ्रेशोल्ड को अंतिम रूप दें।",
            ownerActionNeededEn = "Confirm minimum earthquake magnitude (Mw) and IMD alert severity thresholds before enabling Stage-5 notifications."
        )
    )

    val NETRA_ECOSYSTEM_LINKS: List<Triple<String, String, String>> = listOf(
        Triple(
            "नेत्र न्यूज़ वेब (Netra-News-Web • Audited Head 804931d)",
            "संदर्भ वेब रिपॉजिटरी: github.com/prayagideepak-collab/Netra-News-Web (10 डोमेन परीक्षण उत्तीर्ण)।",
            "Reference Web Repository: github.com/prayagideepak-collab/Netra-News-Web (10 domain tests passing)."
        ),
        Triple(
            "नेत्र न्यूज़ एंड्रॉइड (Netra News Android Stage-1 v3)",
            "चरण-1 सत्यापित समाचार रीडर (यह ऐप) — स्थानीय, राज्य व राष्ट्रीय स्रोत लिंक-आउट, सत्यापन संकेतक व निजी प्रतिक्रिया ड्राफ्ट।",
            "Stage-1 Verified Newspaper Reader (Current App) — Local, State & National source link-out, verification indicators & local feedback drafts."
        ),
        Triple(
            "नेत्र ह्यूमन सेफ्टी (Netra Human Safety - Separate Product)",
            "नोट: नेत्र न्यूज़, नेत्र ह्यूमन सेफ्टी से अलग ऐप है। हैंडऑफ़ के दौरान सत्यापित गंतव्य URL जोड़े जाएंगे।",
            "Note: Netra News is distinct from Netra Human Safety. Verified family destination URLs to be supplied at owner handoff."
        )
    )
}
