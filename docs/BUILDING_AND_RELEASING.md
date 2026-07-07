# Building & Releasing

Maintainer guide for building the modules, running the samples, and publishing the SDKs to
GitHub Packages. For *consuming* the SDK, see [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md).

- [Modules](#modules)
- [Prerequisites](#prerequisites)
- [Building](#building)
- [Running the samples](#running-the-samples)
- [Versioning](#versioning)
- [Releasing](#releasing)
- [Publishing manually](#publishing-manually-from-a-laptop)

---

## Modules

| Module | Artifact | Contents |
|---|---|---|
| `stackview-core` | `io.igrant:stackview-core` | Shared `StackConfig` (no View/Compose deps) |
| `stackview` | `io.igrant:stackview` | View `StackLayoutManager`; depends on core |
| `stackview-compose` | `io.igrant:stackview-compose` | Compose `StackView`; depends on core |
| `sample` | — (app) | View demo, not published |
| `sample-compose` | — (app) | Compose demo, not published |

`stackview` and `stackview-compose` each expose core via `api(project(":stackview-core"))`,
so consumers get `StackConfig` transitively.

## Prerequisites

- **JDK 17**
- **Android SDK** — Gradle needs its location via a `local.properties` at the repo root
  (git-ignored). Create it once:

  ```properties
  sdk.dir=/Users/<you>/Library/Android/sdk
  ```

  (or set the `ANDROID_HOME` environment variable instead).

The Gradle wrapper (`./gradlew`) pins the Gradle version; no local Gradle install needed.

## Building

```bash
# Build every publishable library (release AARs)
./gradlew :stackview-core:assembleRelease \
          :stackview:assembleRelease \
          :stackview-compose:assembleRelease

# Build a single library's AAR — output lands in <module>/build/outputs/aar/
./gradlew :stackview-compose:assembleRelease
# -> stackview-compose/build/outputs/aar/stackview-compose-release.aar

# Full build incl. both sample apps (good pre-release sanity check)
./gradlew build
```

## Running the samples

Requires a connected device or running emulator (`adb devices` to check).

```bash
./gradlew :sample-compose:installDebug   # Compose demo
./gradlew :sample:installDebug           # View demo
```

Then launch from the launcher, or:

```bash
adb shell am start -n io.igrant.stackview.sample.compose/.MainActivity
```

## Versioning

The three artifacts share **one version** (lockstep) — they are always released together at the
same version. The version is supplied at release time via `-PVERSION_NAME`; each module's
`build.gradle.kts` reads it (`version = findProperty("VERSION_NAME") ?: "1.0.0"`). Because
`stackview` and `stackview-compose` depend on `stackview-core` as a project dependency, their
published POMs reference `stackview-core` at the same version automatically.

> A single lockstep version keeps releases simple: one number, one GitHub Release, no
> cross-artifact version bookkeeping. A Compose-only change still re-publishes all three at the
> new version (the unchanged artifacts are just re-stamped) — harmless and intentional.

## Releasing

Releases are automated by [`.github/workflows/publish.yml`](../.github/workflows/publish.yml),
which triggers when a **GitHub Release** is published. Create a release with tag `v<version>`:

| Tag | Publishes |
|---|---|
| `v1.2.0` | `io.igrant:stackview-core`, `io.igrant:stackview`, `io.igrant:stackview-compose` — all at `1.2.0` |

The workflow strips the leading `v` from the tag and runs all three modules' publish tasks with
`-PVERSION_NAME=<version>`. That's the whole release: one tag, one version, three artifacts.

### Maintaining GitHub Releases

- **One Release per version.** Tag `v<version>`, title e.g. `v1.2.0`. The Releases page stays a
  clean chronological list — one entry per version, no per-artifact interleaving.
- **Notes / changelog.** Summarize what changed across the SDK in the release body; GitHub's
  "Generate release notes" works well here since every release covers the same version bump.
- **Versions are immutable.** GitHub Packages won't let you overwrite a published version — if a
  release is botched, bump the patch (`v1.2.1`) rather than re-pushing `v1.2.0`.
- **Pre-releases.** Tags like `v1.2.0-rc1` publish a pre-release version; mark the GitHub Release
  as a pre-release so it doesn't take the "Latest" badge.

### Verify a POM before releasing

To see exactly what a published POM will contain (including the core version it references):

```bash
./gradlew :stackview-compose:generatePomFileForReleasePublication -PVERSION_NAME=1.2.0
cat stackview-compose/build/publications/release/pom-default.xml
```

## Publishing manually (from a laptop)

You normally publish via the GitHub Release above. To publish by hand:

1. Provide a GitHub token with the `write:packages` scope, either as env vars or in
   `~/.gradle/gradle.properties`:

   ```properties
   gpr.user=your-github-username
   gpr.token=ghp_xxxxxxxxxxxxxxxxxxxx
   ```

   (The build also accepts `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables.)

2. Publish all three modules at the shared version:

   ```bash
   ./gradlew \
     :stackview-core:publishReleasePublicationToGitHubPackagesRepository \
     :stackview:publishReleasePublicationToGitHubPackagesRepository \
     :stackview-compose:publishReleasePublicationToGitHubPackagesRepository \
     -PVERSION_NAME=1.2.0
   ```
