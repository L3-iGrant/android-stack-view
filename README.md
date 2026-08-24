# Android Stack View SDKs

Displays cards in a stacked, wallet-style layout, shipped as **two implementations** that
share the same behavior and configuration:

- **`stackview`** — a custom `RecyclerView.LayoutManager` for the classic View toolkit.
- **`stackview-compose`** — a `StackView` composable for Jetpack Compose.

Both consume a single `StackConfig` from **`stackview-core`**, so the look and feel stay
identical no matter which UI toolkit you use.

One card is **presented** (fully visible at the top), while the remaining cards are **collapsed** in a stack below, showing only a peek strip of each card. Tapping a collapsed card promotes it to the top with a smooth animation.

## Features

- **Presented + Stacked layout** — one card expanded at the top, the rest collapsed below
- **Tap to present** — tap any stacked card to bring it to the top
- **Pull-down stretch** — pull down at the top to fan out the stacked cards with a rubber-band effect; releases with a smooth snap-back animation
- **Scrollable stack** — scroll through the stack when cards overflow the screen
- **Presented card callback** — get notified when the already-presented card is tapped
- **Refresh support** — reset the stack to the first card after adding or removing items
- **Configurable** — peek height, animation duration, stretch resistance, and more
- **Two UI toolkits** — a `StackView` composable for Compose, or a `RecyclerView.LayoutManager` that works with any `RecyclerView.Adapter` — both driven by the same shared `StackConfig`

## Documentation

- **[Implementation Guide](docs/IMPLEMENTATION_GUIDE.md)** — full integration walkthrough for
  Compose and View, including GitHub Packages auth, config, and gotchas.
- **[Building & Releasing](docs/BUILDING_AND_RELEASING.md)** — build the modules, run the
  samples, and publish the SDKs.

## Installation

Add the GitHub Packages registry and the dependency to your project:

**settings.gradle.kts** (or root `build.gradle.kts`):
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/L3-iGrant/android-stack-view")
        }
    }
}
```

**app/build.gradle.kts** — pick the implementation for your UI toolkit (each pulls in
`stackview-core` transitively):
```kotlin
dependencies {
    // Classic View / RecyclerView
    implementation("io.igrant:stackview:<latest-version>")

    // Jetpack Compose
    implementation("io.igrant:stackview-compose:<latest-version>")
}
```

## Usage (Jetpack Compose)

```kotlin
val state = rememberStackViewState()
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
    onPresentedCardClick = { index -> /* open detail for movies[index] */ },
) { index, movie ->
    MovieCard(movie)   // your own card composable
}
```

- Tap a stacked card → it animates to the top.
- Tap the presented card → `onPresentedCardClick` fires.
- Pull down at the top → the stack fans out (rubber-band), snaps back on release.
- After adding/removing items, call `state.refresh()` to reset to the first card.

`StackConfig` values are in **pixels** (shared with the View SDK); convert from `dp` with
`LocalDensity` as shown above. See `sample-compose/` for a full demo.

## Usage (View / RecyclerView)

### 1. Set up the LayoutManager

```kotlin
val density = resources.displayMetrics.density

val stackLayoutManager = StackLayoutManager(
    config = StackConfig(
        collapsedPeekHeight = (45 * density).toInt(),
        stackTopMargin = (10 * density).toInt(),
        animationDuration = 350L
    )
)

recyclerView.layoutManager = stackLayoutManager
```

### 2. Handle card clicks

In your adapter, call `presentCard()` when a card is tapped:

```kotlin
recyclerView.adapter = MyAdapter(items) { position ->
    stackLayoutManager.presentCard(position, recyclerView)
}
```

### 3. Listen for presented card taps

Get a callback when the user taps the already-presented card:

```kotlin
stackLayoutManager.onPresentedCardClicked = { position ->
    // Navigate to detail screen, etc.
}
```

### 4. Refresh after data changes

When items are added or removed from the adapter, call `refresh()` to reset the stack back to the first card:

```kotlin
// After adding a new item
adapter.addItem(newItem)
stackLayoutManager.refresh(recyclerView)

// After removing an item
adapter.removeItem(position)
stackLayoutManager.refresh(recyclerView)
```

This cancels any running animations, resets scroll position, and presents the 0th card.

## Configuration

`StackConfig` controls the layout behavior:

| Parameter | Default | Description |
|---|---|---|
| `collapsedPeekHeight` | `120` | Height (px) of the visible strip for each collapsed card |
| `stackTopMargin` | `0` | Space (px) between the presented card and the stack |
| `animationDuration` | `350` | Duration (ms) for the present/dismiss animation |
| `stretchResistance` | `0.5` | Pull-to-stretch resistance (0.0–1.0). Lower = more resistance |
| `maxStretchDistance` | `800` | Maximum stretch distance (px). Caps the fan-out |
| `snapBackDuration` | `600` | Duration (ms) for the snap-back animation on release |

`StackConfig` lives in `stackview-core` and is shared by both SDKs. Its values are in
**pixels** — in Compose, convert from `dp` with `LocalDensity` (see the Compose usage above).

## API

### Compose — `StackView`

```kotlin
@Composable
fun <T> StackView(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: StackViewState = rememberStackViewState(),
    config: StackConfig = StackConfig(),
    onPresentedCardClick: (index: Int) -> Unit = {},
    cardContent: @Composable (index: Int, item: T) -> Unit,
)
```

| Parameter | Description |
|---|---|
| `items` | Backing data; one card is composed per item via `cardContent` |
| `state` | Hoisted `StackViewState`; defaults to a remembered instance |
| `config` | Layout/animation tuning (see [Configuration](#configuration)) |
| `onPresentedCardClick` | Invoked when the already-presented card is tapped again |
| `cardContent` | Renders a single card given its index and item |

**`StackViewState`** — created via `rememberStackViewState(initialPresentedIndex = 0)`; the
presented index survives configuration changes and process death.

| Property / Method | Description |
|---|---|
| `presentedIndex: Int` | Index of the currently presented card (read-only) |
| `present(index)` | Present a card at the given index with animation |
| `refresh()` | Reset state and present the 0th card. Call after adding/removing items |

### View — `StackLayoutManager`

| Property / Method | Description |
|---|---|
| `presentedPosition: Int` | Index of the currently presented card (read-only) |
| `presentCard(position, recyclerView)` | Present a card at the given position with animation |
| `onPresentedCardClicked: ((Int) -> Unit)?` | Callback when the presented card is tapped again |
| `refresh(recyclerView)` | Reset state and present the 0th card. Call after adding/removing items |

## Project Structure

```
stack-view/
├── stackview-core/         # Shared config — StackConfig (no View/Compose deps)
├── stackview/              # View SDK — RecyclerView.LayoutManager
│   └── src/main/java/io/igrant/stackview/
│       └── StackLayoutManager.kt
├── stackview-compose/      # Compose SDK
│   └── src/main/java/io/igrant/stackview/compose/
│       ├── StackView.kt        # StackView composable (custom Layout)
│       └── StackViewState.kt   # rememberStackViewState hoisted state
├── sample/                 # View sample (movie collection demo)
└── sample-compose/         # Compose sample (movie collection demo)
```

## Releasing

The three artifacts share **one version** (lockstep) and are published together. To cut a
release, create a GitHub Release with tag `v<version>`:

| Tag | Publishes |
|---|---|
| `v1.2.0` | `io.igrant:stackview-core`, `io.igrant:stackview`, `io.igrant:stackview-compose` — all at `1.2.0` |

The `publish` workflow reads the version from the tag and publishes all three modules at that
version. See **[Building & Releasing](docs/BUILDING_AND_RELEASING.md)** for building, manual
publishing, and POM verification.

## Requirements

- Min SDK: 24
- Kotlin
- AndroidX RecyclerView (for `stackview`)
- Jetpack Compose (for `stackview-compose`)

## Contributing

This project is maintained by [iGrant.io](https://igrant.io). For contributions, please open an issue or submit a pull request.

## License

Copyright (c) 2026 iGrant Technologies AB (iGrant.io), Sweden

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. See [LICENSE](LICENSE) for the full text.
