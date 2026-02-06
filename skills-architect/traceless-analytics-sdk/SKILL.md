---
name: traceless-analytics-sdk
description: Traceless Analytics SDK for Android - Firebase-first analytics SDK for tracking screen views and UI interactions. Use when implementing, modifying, or extending Android analytics tracking with this SDK: (1) Screen view tracking with Analytics.enterScreen(), (2) UI interaction tracking with Analytics.trackUI(), (3) Jetpack Compose integration, (4) Debugging analytics issues, (5) Writing tests for analytics functionality. Supports custom screens/actions, session-aware context management, and Compose helpers.
---

# Traceless Analytics SDK

## Overview

Firebase-first Android analytics SDK for tracking screen views and UI interactions with session-aware context management.

## Installation

```kotlin
dependencies {
    implementation("io.github.maichanchinh:traceless-analytic:2.0.0")
}
```

## Quick Start

```kotlin
// Setup in Application.onCreate()
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Analytics.enableDebug()
        Analytics.initialize()
    }
}

// Track screen views
Analytics.enterScreen(UIScreen.Main)
Analytics.enterScreen(UIScreen.Splash)

// Track UI interactions
Analytics.trackUI("btn_buy", UIAction.Click)
Analytics.trackUI("input_email", UIAction.Input)

// With custom parameters
Analytics.enterScreen(Screen.Home, mapOf("source" to "notification"))
Analytics.trackUI("btn_buy", UIAction.Click, mapOf("value" to 99.99))
```

## Core API

```
Analytics (public API)
├── initialize() → Initialize SDK
├── enableDebug() / disableDebug() → Debug mode
├── enterScreen(screen: UIScreen, customParams?) → screen_view event
├── trackUI(elementId: String, action: UIAction, customParams?) → ui_interaction event
└── resetState() → Reset on new session
```

### Custom Parameters

Both `enterScreen()` and `trackUI()` accept optional custom parameters:

```kotlin
// Screen view with custom params
Analytics.enterScreen(
    screen = Screen.Home,
    customParams = mapOf(
        "source" to "deeplink",
        "campaign" to "summer2024"
    )
)

// UI interaction with custom params
Analytics.trackUI(
    elementId = "btn_buy",
    action = UIAction.Click,
    customParams = mapOf(
        "value" to 99.99,
        "currency" to "USD",
        "item_id" to "prod_123"
    )
)
```

**Note**: Custom params merge with default params. Overlapping keys will override defaults.

### Screen Naming Rules

- Use snake_case format
- Max 50 characters
- Lowercase letters and underscores only
- Business identifiers only (e.g., "home", "product_detail")

### Predefined UI Actions

```
Click, Input, Submit, Toggle, Swipe, Navigate,
View, Refresh, Dismiss, Select
```

Custom actions: `object LongPress : UIAction("long_press")`

## Jetpack Compose Integration

```kotlin
// Recommended: TrackScreen helper
@Composable
fun ProductDetailScreen() {
    TrackScreen(Screens.ProductDetail)  // Auto track once

    Button(onClick = {
        Analytics.trackUI("btn_buy", UIAction.Click)
    }) {
        Text("Buy")
    }
}

// Manual: LaunchedEffect pattern
@Composable
fun MyScreen() {
    LaunchedEffect(Unit) {
        Analytics.enterScreen(Screens.Home)
    }
}
```

## Session Management

| Scenario | Handling |
|----------|----------|
| App background | Don't reset screen state |
| New session | Reset currentScreenName |
| Offline | Queue events (best-effort) |
| Crash | Don't retry events |

```kotlin
// Reset on new Firebase session
fun onNewFirebaseSession() {
    Analytics.resetState()
}
```

## Key Principles

1. **Minimal State**: Keep internal state minimal (currentScreenName, sessionId)
2. **Session-Aware**: Events include session context automatically
3. **Graceful Degradation**: Handle Firebase failures without crashing
4. **Best-Effort Offline**: Queue events when offline, don't block
5. **Single Responsibility**: Each function does one thing well

## Common Pitfalls

**Don't track screen in onClick** - Use `LaunchedEffect(Unit)` or `TrackScreen` helper instead.

**Avoid duplicate tracking** - Track screen only once per screen lifecycle, not in both onCreate and onResume.

**Handle configuration changes** - Use `savedInstanceState` check to avoid re-tracking on rotation.
