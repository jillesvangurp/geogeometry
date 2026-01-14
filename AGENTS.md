# Agent Guidelines

- **Project shape:** Kotlin Multiplatform library for geospatial geometry. Shared code lives in `src/commonMain/kotlin` (e.g., `com.jillesvangurp.geo` and `com.jillesvangurp.geojson` packages) and shared tests in `src/commonTest/kotlin`. Build and target configuration is centralized in `build.gradle.kts`; keep changes aligned with the multiplatform setup.
- **Multiplatform only:** Keep implementations portable—avoid JVM-specific APIs or platform-only shortcuts unless there is an explicit expect/actual need.
- **Testing style:** Prefer Kotlin’s `@Test` annotations paired with Kotest assertions (`io.kotest.matchers` and related helpers) already used in `commonTest`.
- **Dependencies:** Do not introduce new library dependencies without explicit direction.
- **Public API:** This is a published library; avoid backward-incompatible changes to the public API unless specifically requested.
