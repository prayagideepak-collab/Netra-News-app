# SECURITY.md — Netra News Android Security & Privacy Policy

## 1. Zero Secrets & Credentials in Source
- No API keys, service-account JSONs, private keystores, tokens, or personal data are committed to source, fixtures, or build scripts.
- All feeds use unauthenticated public HTTPS endpoints from allowlisted domains (`pib.gov.in`, `ddnews.gov.in`, `newsonair.gov.in`, `mausam.imd.gov.in`, `seismo.gov.in`, `information.up.gov.in`).

## 2. Narrow Permissions & Location Privacy
- **Declared Permissions:** `INTERNET`, `ACCESS_NETWORK_STATE`, and `ACCESS_COARSE_LOCATION`.
- **Never Requested:** `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`, `CAMERA`, `RECORD_AUDIO`, `READ_CONTACTS`, `SEND_SMS`, `READ_EXTERNAL_STORAGE`.
- **Coordinate Scrubbing:** When the user explicitly requests foreground approximate city detection, raw latitude/longitude values are resolved against the packaged offline city gazetteer and immediately zeroed out in a `finally` block. Only `cityId`, `stateCode`, `countryCode`, and `selectionMode` are persisted.

## 3. External Browser Isolation (No Privileged WebView)
- External source links are validated for `https://` scheme and allowlisted host before launching via standard external browser `Intent.ACTION_VIEW`.
- No internal JavaScript-enabled `WebView` or `addJavascriptInterface` bridge is used anywhere in the application.
