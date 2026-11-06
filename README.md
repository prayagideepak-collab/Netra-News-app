# Netra News (नेत्र न्यूज़) — Android Stage-1 Implementation & Review Packet

**Specification Date:** 6 November 2026 | **Target Review Window:** 15 November 2026  
**Package Identity (`applicationId`):** `com.aistudio.netranews.kxvqpm` (`namespace = "com.example"`, `minSdk = 24`, `targetSdk = 36`)  
**Product Scope:** Hindi-first broadsheet newspaper reader with verified source attribution, strict rights contract enforcement (`LINK_ONLY` default for broadcasters; `ORIGINAL` for public-domain government releases), local-only bookmarks (Room), manual + foreground-only approximate city selection, civic Weather (IMD) and Earthquake (NCS) bulletins, and Ultra-Low Power 24/7 operation.

---

## 1. Setup, Build & Test Commands

```bash
# Run JVM Unit & Robolectric Contract Tests
gradle :app:testDebugUnitTest

# Assemble Debug APK Candidate
gradle assembleDebug
```

---

## 2. Architecture & Package Layout

- `com.example.domain` — Shared `FeedEnvelope` (`schemaVersion = 1`), `ArticleRecord`, `WeatherAdvisoryRecord`, `EarthquakeEventRecord`, `SourceAllowlistEntry`, and `ValidationAndRightsEngine` (schema guard, `test-001` blocker, future/missing timestamp guard, HTML/script sanitizer, HTTPS allowlist verifier, duplicate link consolidator).
- `com.example.power` — `PowerBudgetAndTierManager` (Adaptive Power Tiers: `NORMAL`, `SAVER`, `CRITICAL`, `SURVIVAL` with 2% hysteresis, thermal throttling drop, and exponential backoff circuit breaker).
- `com.example.location` — `CityLocationEngine` (Offline CC-BY 4.0 GeoNames subset for Indian states/cities, foreground-only `ACCESS_COARSE_LOCATION` with 6s timeout, immediate coordinate scrubbing in `finally` block, and nearest-city ambiguity prompt).
- `com.example.data` — `NetraLocalDatabase` (Room entities for rights-permitted article cache, local-only bookmarks, and bounded 100-row health logs) and `NetraRepository` (atomic preference commits and conditional `If-None-Match` HTTPS client).
- `com.example.settings` — `GovernanceLedgerData` (Stage 1–6 Feature & Policy Matrix, Power/Wakeup costs, and Open Decisions ledger).
- `com.example.ui` — Navy (`#0D1B2A`) & Amber (`#FFB703`) Jetpack Compose screens (`NewspaperFeedScreen`, `SavedBookmarksScreen`, `WeatherAndEarthquakesScreen`, `GovernanceAndSettingsScreen`).

---

## 3. Sample Feed Envelope & Labelled `TEST` Fixture (`test-001`)

Production feeds strictly reject `id = "test-001"` via `ValidationAndRightsEngine.isTestFixture(...)`:

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-11-06T00:00:00Z",
  "coverage": ["local:in-up-lucknow", "state:UP", "national:IN"],
  "items": [
    {
      "id": "test-001",
      "title": "TEST - feed unavailable example",
      "publisher": "TEST SOURCE",
      "sourceUrl": "https://pib.gov.in/test-fixture-placeholder",
      "publishedAt": null,
      "verificationStatus": "pending",
      "rightsStatus": "blocked"
    }
  ],
  "sourceHealth": []
}
```
