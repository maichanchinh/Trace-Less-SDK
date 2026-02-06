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

        // Collect events and dispatch to Firebase
        CoroutineScope(Dispatchers.Main).launch {
            Analytics.listenEvents().collectLatest { event ->
                Firebase.analytics.logEvent(event.name) {
                    event.params.forEach { (key, value) ->
                        when (value) {
                            is String -> param(key, value)
                            is Long -> param(key, value)
                            is Double -> param(key, value)
                        }
                    }
                }
            }
        }
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
├── listenEvents() → SharedFlow<TracelessEvent> for Firebase integration
└── resetState() → Reset on new session
```

**Key**: Use `listenEvents()` to collect events and dispatch to Firebase Analytics.

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

### Parameter Key Convention

Custom parameter keys must follow **snake_case** convention:

- ✅ Valid: `user_id`, `total_amount`, `item_price`, `campaign_id`
- ✅ camelCase auto-converted: `userId` → `user_id`, `totalAmount` → `total_amount`
- ❌ Invalid: `USER-ID`, `total-amount`, `userId!`, `TOTAL_AMOUNT`

```kotlin
// snake_case - used as-is
Analytics.trackUI("btn_buy", UIAction.Click, mapOf(
    "user_id" to "12345",
    "total_amount" to 99.99
))

// camelCase - auto-converted to snake_case (logs warning in debug mode)
Analytics.trackUI("btn_buy", UIAction.Click, mapOf(
    "userId" to "12345",      // → user_id
    "totalAmount" to 99.99    // → total_amount
))

// Invalid - throws IllegalArgumentException
Analytics.trackUI("btn_buy", UIAction.Click, mapOf(
    "USER-ID" to "12345"      // ❌ throws error
))
```

**Reserved Keys** (avoid overriding):
- `screen_name`
- `element_id`
- `action`
- `is_manual`

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

## Firebase Integration

Traceless SDK emits events via `SharedFlow` that you collect and dispatch to Firebase Analytics:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize SDK
        Analytics.enableDebug()
        Analytics.initialize()

        // 2. Collect events and dispatch to Firebase
        CoroutineScope(Dispatchers.Main).launch {
            Analytics.listenEvents().collectLatest { event ->
                // Debug logging
                Timber.tag("Analytics").d(
                    "[Event] ${event.name} | " +
                    "screen: ${event.screenName} | " +
                    "element: ${event.elementId} | " +
                    "action: ${event.action} | " +
                    "params: ${event.params}"
                )

                // 3. Dispatch to Firebase Analytics
                dispatchToFirebase(event)
            }
        }
    }

    private fun dispatchToFirebase(event: TracelessEvent) {
        val firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(event.name) {
            event.params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Boolean -> param(key, value)
                }
            }
        }
    }
}
```

### Event Structure

Each `TracelessEvent` contains:
- `name: String` - Event name ("screen_view", "ui_interaction")
- `params: Map<String, Any>` - Event parameters
- `timestamp: Long` - Unix timestamp
- Extension properties:
  - `screenName: String?` - Current screen name
  - `elementId: String?` - UI element identifier
  - `action: String?` - UI action performed
  - `isManual: Boolean` - Whether event is manually tracked

### Flow Behavior

- Uses `SharedFlow` with `replay=0` - new collectors don't receive past events
- Buffer capacity: 64 events with `DROP_OLDEST` strategy
- Collected on `Dispatchers.Main` for safe Firebase calls

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
