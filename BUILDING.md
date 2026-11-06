# Building and verification limits

Install JDK 17, Android SDK platform 36.1 / build-tools 36.0.0, and Gradle 9.3.1. This matches the existing project version catalog and wrapper properties. The repository does not yet contain the Gradle wrapper launcher or JAR; CI installs the exact Gradle version explicitly. Do not claim wrapper-complete builds.

Run `gradle --no-daemon :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug`.

CI performs compile, local unit tests and lint, not phone acceptance, screenshots or a signed installable APK. Existing debug/release signing points to absent keystores. No keys are generated or signing settings changed here. Signed assembly remains blocked until the owner supplies the authorized per-app signing setup securely. Never commit a keystore or password.

A green job is necessary, not sufficient for release. The disconnected feed remains empty; weather and earthquakes are unavailable; feedback is local-draft only. No private service is connected. Do not call local helper output a durable server receipt.
