# Security, Privacy & Governance Policy — Netra News (`नेत्र न्यूज़`)

## 1. Strict Permission & Privacy Boundaries (Sections 1.4–1.6, 5.1–5.5, 8.6–8.8)

1. **No Fine or Background Location**:
   - The app never requests `ACCESS_FINE_LOCATION` or `ACCESS_BACKGROUND_LOCATION`.
   - `ACCESS_COARSE_LOCATION` is optional and requested only when the user explicitly triggers foreground city detection.
   - Raw latitude/longitude coordinates are matched in-memory against the packaged offline Indian city centroid gazetteer (`CityLocationEngine.kt`) and **immediately discarded**. Coordinates are never written to disk, logs, or network requests.
2. **No Prohibited Fallback Profiling**:
   - The app never accesses cell tower IDs (`CellInfo`), Wi-Fi BSSIDs, OpenCellID, Mozilla Location Services, or IP-geolocation profiling APIs.
3. **External Links & Sanitisation**:
   - All feed fields (`title`, `publisherName`, `permittedSummary`, `corrections`, `verificationReason`) are sanitised as plain text (`ValidationAndRightsEngine.sanitizeString`), stripping `<script>`, HTML tags, and `javascript:`/`data:` URI schemes.
   - Only `https://` URLs from allowlisted hosts (`pib.gov.in`, `ddnews.gov.in`, `newsonair.gov.in`, `up.gov.in`, `mausam.imd.gov.in`, `seismo.gov.in`, `earthquake.usgs.gov`) are permitted to open in an external browser/Custom Tab with `Intent.FLAG_ACTIVITY_NEW_TASK`.
   - Items with `verificationStatus = BLOCKED` or `editorialState = RETRACTED` never expose clickable unsafe external URLs (`ValidationAndRightsEngine.isClickableSourceLinkAllowed == false`).
   - No privileged JavaScript `WebView` with `addJavascriptInterface` or file access is used anywhere in the application.
4. **Section 8 Feedback Privacy, Minimal Data & Honest Disabled Endpoint**:
   - Readers can prepare article issue reports (`inaccuracy`, `brokenLink`, `outdated`, `other`) or `general` feedback anonymously without an account or contact details.
   - Input validation rejects `<script>`, HTML angle brackets, and `javascript:` schemes, and enforces a 15-second cooldown, max 5 submissions per 10-minute window, and a hidden honeypot field.
   - **Honest Unavailable Submit (Section 8.9)**: Because the private review backend (`Cloudflare Worker + D1`) is not yet connected in Stage-1, remote network submission is visibly disabled and never simulated. Users can save local drafts on their device and delete optional contact info or drafts at any time.
   - **Retention Policy (Section 8.6)**: When backend storage is connected, optional contact info is used solely for follow-up and deleted within 30 days after resolution; unresolved feedback is deleted within 90 days (subject to pre-launch legal/DPDP review).

## 2. Vulnerability & Issue Reporting

- Do not commit secrets, signing keystores, API tokens, or private URLs to the repository.
- Report security or rights-compliance issues through the owner's designated GitHub repository security advisory channel (`prayagideepak-collab/Netra-News-Web`).
