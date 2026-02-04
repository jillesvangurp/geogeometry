---
name: geogeometry-maintainer
description: Use this skill for code, build, and test changes in geogeometry to preserve multiplatform support, geospatial correctness, and strict no-new-dependencies policy.
---

# geogeometry-maintainer

Use this skill when making code, test, or build changes in this repository.

## Goal

Keep changes safe for a Kotlin Multiplatform geospatial library while preserving current project constraints.

## Required constraints

1. Do not add dependencies without permission
   - No new external libraries, plugins, or repositories.
   - Do not introduce version pins inconsistent with current build style.

2. Keep multiplatform compatibility
   - Treat `commonMain` as platform-agnostic.
   - Do not remove or silently break existing Kotlin targets.
   - Any platform-specific behavior should be explicit and justified.

3. Keep geospatial semantics stable
   - Respect coordinate ordering conventions:
     - arrays/GeoJSON: `[longitude, latitude]`
     - separate params: `latitude, longitude`
   - Prefer adding tests for edge cases (antimeridian, poles, conversion round trips).

## Working checklist

1. Read `README.md` and `build.gradle.kts` before non-trivial edits.
2. Limit scope to requested change; avoid drive-by refactors.
3. Run targeted tests first, then a broader verification if change surface is larger.
4. Call out risks or unverified paths in the final handoff.

## Verification suggestions

- Fast path:
  - `./gradlew commonTest`
  - `./gradlew jvmTest`
- Broader path:
  - `./gradlew build`

Note: some iOS/wasm test tasks are intentionally disabled in the build script; do not alter this unless explicitly requested.
