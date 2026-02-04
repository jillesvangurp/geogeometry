# AGENTS.md

## Project overview

GeoGeometry is a Kotlin Multiplatform geospatial library. The codebase prioritizes:

- Kotlin-first APIs and multiplatform compatibility.
- Minimal runtime dependencies.
- Strong test coverage for edge cases in geometry and coordinate conversions.
- Public API stability for a published library.

## Guard rails

1. Multiplatform stays intact
   - Keep implementations portable and avoid platform-only shortcuts unless there is an explicit `expect/actual` design.
   - Keep the existing Kotlin targets working: JVM, JS (IR), Linux (x64/arm64), Mingw, macOS (x64/arm64), iOS (arm64/x64/simulator arm64), and wasmJs.
   - Avoid introducing platform-specific behavior in `commonMain` unless there is an `expect/actual` design.

2. Dependency policy (strict)
   - Do **not** add new library dependencies, Gradle plugins, or repositories without explicit prior permission.
   - When changing existing dependencies, prefer existing patterns (e.g. refreshVersions `_` notation).

3. Build and test expectations
   - Use the Gradle wrapper: `./gradlew`.
   - Prefer small, targeted checks first; run a broader build before finalizing significant changes.
   - Some test tasks are intentionally disabled in `build.gradle.kts` (iOS simulator and wasm test tasks); do not re-enable unless asked.
   - Prefer Kotlin `@Test` with Kotest assertions style already used in `commonTest`.

4. Domain-specific correctness
   - Preserve coordinate order conventions from the README:
     - GeoJSON-style arrays are `[longitude, latitude]`.
     - Separate parameters are typically `latitude, longitude`.
   - Be extra careful with antimeridian/pole edge cases; add tests when behavior changes.

5. Licensing
   - Preserve license requirements: repository is MIT, with Apache-license context retained where already documented (notably `GeoHashUtils` history).

## Repository skills

Use repository-local skills from `skills/` when applicable.

### Available skills

- geogeometry-maintainer: Contribution workflow and safety checks for this repository.
  - file: `skills/geogeometry-maintainer/SKILL.md`
