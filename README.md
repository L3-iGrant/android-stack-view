# Android Stack View SDKs

A custom `RecyclerView.LayoutManager` for Android that displays cards in a stacked, wallet-style layout.

One card is **presented** (fully visible at the top), while the remaining cards are **collapsed** in a stack below, showing only a peek strip of each card. Tapping a collapsed card promotes it to the top with a smooth animation.

## Features

- **Presented + Stacked layout** — one card expanded at the top, the rest collapsed below
- **Tap to present** — tap any stacked card to bring it to the top
- **Pull-down stretch** — pull down at the top to fan out the stacked cards with a rubber-band effect; releases with a smooth snap-back animation
- **Scrollable stack** — scroll through the stack when cards overflow the screen
- **Presented card callback** — get notified when the already-presented card is tapped
- **Refresh support** — reset the stack to the first card after adding or removing items
- **Configurable** — peek height, animation duration, stretch resistance, and more
- **Standard RecyclerView** — works with any `RecyclerView.Adapter`

## Installation

Add the GitHub Packages registry and the dependency to your project:

**settings.gradle.kts** (or root `build.gradle.kts`):
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/L3-iGrant/stack-view")
        }
    }
}
```

**app/build.gradle.kts**:
```kotlin
dependencies {
    implementation("io.igrant:stackview:<latest-version>")
}
```

## Usage

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

## API

### StackLayoutManager

| Property / Method | Description |
|---|---|
| `presentedPosition: Int` | Index of the currently presented card (read-only) |
| `presentCard(position, recyclerView)` | Present a card at the given position with animation |
| `onPresentedCardClicked: ((Int) -> Unit)?` | Callback when the presented card is tapped again |
| `refresh(recyclerView)` | Reset state and present the 0th card. Call after adding/removing items |

## Project Structure

```
stack-view/
├── stackview/          # Library module
│   └── src/main/java/io/igrant/stackview/
│       ├── StackLayoutManager.kt
│       └── StackConfig.kt
└── sample/             # Sample app (movie collection demo)
    └── src/main/java/io/igrant/stackview/sample/
        ├── MainActivity.kt         # Stack view with FAB to add movies
        ├── CardAdapter.kt          # Adapter with dynamic add/remove
        ├── MovieDetailActivity.kt  # Detail page with remove action
        └── MoviePreferences.kt     # SharedPreferences persistence
```

## Requirements

- Min SDK: 24
- Kotlin
- AndroidX RecyclerView

## License

Copyright (c) 2026 iGrant.io
