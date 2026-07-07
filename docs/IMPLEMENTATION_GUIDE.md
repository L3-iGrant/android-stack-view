# Implementation Guide

How to integrate the Stack View SDK into an app. There are two implementations — pick the
one that matches your UI toolkit. Both are driven by the same `StackConfig` from
`stackview-core`, so they behave identically.

- [Add the dependency](#1-add-the-dependency)
- [Jetpack Compose](#2a-jetpack-compose)
- [View / RecyclerView](#2b-view--recyclerview)
- [Configuration](#3-configuration)
- [Behavior & gotchas](#4-behavior--gotchas)

---

## 1. Add the dependency

Artifacts are published to **GitHub Packages**, which requires authentication even to *read*.

**a) Add the repository** — in `settings.gradle.kts` (or root `build.gradle.kts`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/L3-iGrant/android-stack-view")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.token").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

**b) Provide credentials** — a GitHub Personal Access Token with the `read:packages` scope.
Put them in `~/.gradle/gradle.properties` (never commit them):

```properties
gpr.user=your-github-username
gpr.token=ghp_xxxxxxxxxxxxxxxxxxxx
```

**c) Add the artifact** for your toolkit — each pulls in `stackview-core` transitively, so
you never depend on it directly:

```kotlin
dependencies {
    // Jetpack Compose
    implementation("io.igrant:stackview-compose:<latest-version>")

    // …or Classic View / RecyclerView
    implementation("io.igrant:stackview:<latest-version>")
}
```

---

## 2a. Jetpack Compose

The entry point is the `StackView` composable plus a hoisted `StackViewState`.

```kotlin
@Composable
fun MovieWallet(movies: List<Movie>) {
    val state = rememberStackViewState()

    // StackConfig is in PIXELS — convert from dp once.
    val density = LocalDensity.current
    val config = remember(density) {
        with(density) {
            StackConfig(
                collapsedPeekHeight = 48.dp.roundToPx(),
                stackTopMargin = 12.dp.roundToPx(),
                animationDuration = 350L,
            )
        }
    }

    StackView(
        items = movies,
        modifier = Modifier.fillMaxSize(),
        state = state,
        config = config,
        onPresentedCardClick = { index ->
            // The already-presented card was tapped again → open a detail screen, etc.
            openDetail(movies[index])
        },
    ) { index, movie ->
        // Your own card composable. Give cards a consistent height (see gotchas).
        MovieCard(movie)
    }
}
```

### Presenting a specific card programmatically

Tapping a stacked card presents it automatically. To do it from your own code, call
`present` from a coroutine (it's a plain function but usually launched from an event):

```kotlin
val scope = rememberCoroutineScope()
Button(onClick = { state.present(3) }) { Text("Show card 3") }
// or simply: state.present(3) inside an onClick lambda
```

### After adding or removing items

Call `refresh()` so the stack resets to the first card and drops any stale selection:

```kotlin
movies.add(0, newMovie)   // e.g. a SnapshotStateList
state.refresh()
```

### `StackViewState` API

| Member | Description |
|---|---|
| `rememberStackViewState(initialPresentedIndex = 0)` | Create/remember the state. Survives recomposition, config changes and process death. |
| `presentedIndex: Int` | Index of the currently presented (top) card. Read-only. |
| `present(index: Int)` | Present the card at `index` with the reflow animation. |
| `refresh()` | Reset to the first card. Call after items change. |

---

## 2b. View / RecyclerView

`StackLayoutManager` is a `RecyclerView.LayoutManager` — use it with any adapter.

```kotlin
val density = resources.displayMetrics.density

val stackLayoutManager = StackLayoutManager(
    config = StackConfig(
        collapsedPeekHeight = (48 * density).toInt(),
        stackTopMargin = (12 * density).toInt(),
        animationDuration = 350L,
    )
)
recyclerView.layoutManager = stackLayoutManager

// Present a card when it's tapped — your adapter forwards the click position.
recyclerView.adapter = MovieAdapter(movies) { position ->
    stackLayoutManager.presentCard(position, recyclerView)
}

// Get notified when the already-presented card is tapped again.
stackLayoutManager.onPresentedCardClicked = { position ->
    openDetail(movies[position])
}
```

After changing the data set, reset the stack:

```kotlin
adapter.addItem(newMovie)          // your adapter's insert
stackLayoutManager.refresh(recyclerView)
```

### `StackLayoutManager` API

| Member | Description |
|---|---|
| `StackLayoutManager(config)` | Construct with a `StackConfig`. |
| `presentedPosition: Int` | Index of the currently presented card. Read-only. |
| `presentCard(position, recyclerView)` | Present a card with animation. |
| `onPresentedCardClicked: ((Int) -> Unit)?` | Callback when the presented card is tapped again. |
| `refresh(recyclerView)` | Reset state and present the 0th card. |

> The layout manager disables the RecyclerView's default `ItemAnimator` (it would fight the
> custom card animation). Don't re-enable it.

---

## 3. Configuration

`StackConfig` (from `stackview-core`) is shared by both SDKs. **All distances are in pixels.**

| Parameter | Default | Description |
|---|---|---|
| `collapsedPeekHeight` | `120` | Height (px) of the visible strip for each collapsed card |
| `stackTopMargin` | `0` | Space (px) between the presented card and the stack |
| `animationDuration` | `350` | Duration (ms) for the present animation |
| `stretchResistance` | `0.5` | Pull-to-stretch resistance (0.0–1.0). Lower = more resistance |
| `maxStretchDistance` | `800` | Maximum stretch distance (px). Caps the fan-out |
| `snapBackDuration` | `600` | Duration (ms) for the snap-back animation on release |

In Compose, convert `dp` → `px` with `LocalDensity` (`48.dp.roundToPx()`). In Views, multiply
by `resources.displayMetrics.density`.

---

## 4. Behavior & gotchas

- **Cards should share a height.** The peek grid is built off the presented card's measured
  height. If cards have consistent design/height (the usual case) the overlap is clean; wildly
  varying heights will not stack evenly. Give your card a fixed height for predictable results.
- **The bottom card of the stack renders fully.** Nothing is drawn on top of it, so it shows
  its whole content rather than a peek strip. This is expected wallet-stack behavior.
- **Pull-down stretch + snap-back** happens at the top of the stack — pulling down fans the
  cards out with a rubber-band feel and snaps back on release.
- **Compose state is hoisted.** Keep the `StackViewState` from `rememberStackViewState()` if
  you need to read/drive `presentedIndex`; it's preserved across config changes and process
  death (only the index is saved; transient scroll/stretch reset).
- **`StackConfig` is pixels, not dp** — the single most common integration mistake. Convert.

See the runnable demos: [`sample-compose/`](../sample-compose) (Compose) and
[`sample/`](../sample) (View).
