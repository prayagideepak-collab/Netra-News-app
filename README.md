# Netra News (`नेत्र न्यूज़`) — Android Stage-1 Candidate (Spec v3)

**Netra Ecosystem • Verified Civic Press**  
**Build Specification Date:** 6 November 2026 (v3 — 12:28 IST Repo Audit + Sections 7 & 8)  
**Target Window:** 15 November 2026 (Owner-Led AI-Assisted Generation)  
**Reference Web Repository Audited:** `https://github.com/prayagideepak-collab/Netra-News-Web` (Head commit `804931da1228cfbe3b1da8462ac9d95198db1fe4`, 10/10 domain tests passing)

---

## 1. Honest Current Status (Required / Implemented / Tested / Blocked)

This repository contains the **Stage-1 Android application source and unit-tested verification contracts** for **Netra News (`नेत्र न्यूज़`)**. Per Build Specification v3 Section 1.5 and Section 9.3, **nothing is claimed as live production deployment, signed store release, connected private feedback backend, or publisher-approved full-text syndication** until owner-verified:

| Area | Stage-1 Status | Evidence in Codebase |
| :--- | :--- | :--- |
| **Hindi-First Broadsheet UI + English Toggle** | **Implemented & Tested** | `NewspaperFeedScreen.kt`, `Type.kt` (`Noto Serif` + `Plus Jakarta Sans`), factual headlines never auto-translated |
| **Rollout Tabs (`Local -> State -> National`, `World` Staged)** | **Implemented & Tested** | `NewsScope` in `ArticleContract.kt`; `World` visibly marked staged |
| **Section 7 Source Verification Indicators (`Verified`, `Pending`, `Blocked`)** | **Implemented & Tested** | `VerificationStatus` (`[✓ VERIFIED]`, `[! PENDING]`, `[✕ BLOCKED]`), screen-reader announcements, honest scope disclosure, `Pending`/`Blocked` warning filter, no Breaking/Live for `Pending`/`Blocked`, `EditorialState.RETRACTED` kept separate |
| **Section 8 Feedback, Issue Reports & Local Drafts (Honest Disabled Submit)** | **Implemented & Tested** | `FeedbackReportModal.kt`, `ValidationAndRightsEngine.validateFeedbackSubmission`, `FeedbackDraftEntity` in Room; remote submit honestly disabled until Cloudflare Worker + D1 is connected |
| **Schema v1 + `test-001` Production Guard** | **Implemented & Tested** | `ValidationAndRightsEngine.kt` + `ExampleUnitTest.kt` |
| **Publisher Rights Enforcement (`LINK_ONLY`, `ORIGINAL`, `LICENSED_SUMMARY`, `BLOCKED`)** | **Implemented & Tested** | `ValidationAndRightsEngine.getRightsPermittedSummary` + `CachedArticleEntity` |
| **Manual Country/State/City + Optional Coarse Foreground Location** | **Implemented & Tested** | `CityLocationEngine.kt` (packaged CC-BY 4.0 Indian centroid gazetteer; raw coordinates discarded immediately) |
| **Offline Room Cache & Bookmarks (`SavedBookmarkEntity`)** | **Implemented & Tested** | `NetraLocalDatabase.kt` (v2), `SavedBookmarksScreen.kt` |
| **Weather (IMD) & Earthquakes (NCS/USGS) Bulletins** | **Implemented & Tested** | `WeatherAndEarthquakesScreen.kt` with official timestamps & stale badges |
| **Ultra-Low Power 24/7 Adaptive Tier Manager (`NORMAL`, `SAVER`, `CRITICAL`, `SURVIVAL`)** | **Implemented & Tested** | `PowerBudgetAndTierManager.kt` + hysteresis + thermal drop + circuit breaker |
| **Live Remote Production Feed & Publisher Permissions** | **Planned / Blocked** | Owner decision & written publisher replies required before switching from packaged verified sample snapshot |

---

## 2. Architecture & Package Identity

- **Namespace:** `com.example` (preserved for AI Studio template compatibility)
- **Candidate Application ID:** `com.aistudio.netranews.kxvqpm` (provisional; final production package ID and signing identity require owner sign-off per Section 9.4)
- **Permissions Declared (`AndroidManifest.xml`):**
  - `android.permission.INTERNET`
  - `android.permission.ACCESS_NETWORK_STATE`
  - `android.permission.ACCESS_COARSE_LOCATION` (requested **only** when the user explicitly taps **"Coarse Auto / अनुमानित स्थान"**)
  - **Zero** fine location, background location, contacts, microphone, camera, SMS, or storage permissions.

---

## 3. Running Local Verification & Unit Tests

```bash
gradle :app:testDebugUnitTest
```

Unit tests in `ExampleUnitTest.kt` and `ExampleRobolectricTest.kt` verify:
1. Incompatible `schemaVersion` rejection.
2. Production guard rejecting `id="test-001"` (`SPEC_TEST_FIXTURE_001`).
3. Missing, invalid, or future `publishedAt` timestamp rejection.
4. Stale envelope (`>18h`) and expired item (`expiryIso`) handling.
5. HTTPS source allowlist validation and HTML/script tag stripping.
6. Duplicate canonical URL consolidation without losing publisher attribution.
7. `LINK_ONLY` and `BLOCKED` summary suppression both in UI and before writing to Room SQLite cache.
8. **Section 7 Verification Indicators**: `Verified`, `Pending`, and `Blocked` labels, monochrome symbols, screen-reader announcement (`Source: [publisher] • Source verification: [status]`), fallback to `Pending` on missing/unknown/malformed status, stale `verificationCheckedAt` detection, withheld unsafe links on `Blocked` or `Retracted` items, `Pending`/`Blocked` exclusion from Breaking/Live, and rejection of unauthorized client status transitions.
9. **Section 8 Feedback & Private Review**: `articleId` prefill, 20–2000 char description validation, `optionalComments`, script/HTML rejection, honeypot & 15s cooldown spam guard, anonymous submission without contact, 30-day contact / 90-day unresolved retention rules, unauthenticated reviewer access denial, idempotency token deduplication, and honest disabled submit (`DisabledBackendSavedLocalDraftOnly`) when the private endpoint is unconnected.
10. Nearest-city resolution, ambiguity detection (`<35 km` delta), and out-of-bounds manual fallback.
11. Adaptive power tiers (`100%`, `15%`, `5%`), hysteresis, thermal throttling drop, and circuit breaker backoff.
