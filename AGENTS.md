# AGENTS.md - Traceless SDK Development Guide

## Project Overview
**Main Module:** `traceless-analytic` - Android analytics SDK (Firebase-first)
**Sample App:** `app` - Reference implementation
**Language:** Kotlin 2.0.21 | **Target:** Android API 26+

---

## Build Commands

### Run Unit Tests
```bash
./gradlew :traceless-analytic:test          # Run SDK unit tests only
./gradlew :app:test                         # Run app unit tests
./gradlew test                              # Run all tests
```

### Run Instrumented Tests
```bash
./gradlew :traceless-analytic:connectedAndroidTest    # SDK instrumented tests
./gradlew connectedAndroidTest                         # All instrumented tests
```

### Build & Release
```bash
./gradlew assembleDebug         # Debug build
./gradlew assembleRelease       # Release build (AAR)
./gradlew :traceless-analytic:bundleReleaseAar  # Generate AAR
```

### Linting
```bash
./gradlew lint                  # Run all lint checks
./gradlew :traceless-analytic:lint    # SDK-specific lint
```

---

## Code Style Guidelines

### Imports
- Use explicit imports (no wildcard imports)
- Group imports: Android → Kotlin → Third-party → Internal
- Sort alphabetically within groups
```kotlin
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.app.traceless.analytic.internal.ContextHolder
```

### Naming Conventions
| Element | Convention | Example |
|---------|------------|---------|
| Public APIs | PascalCase | `Analytics.enterScreen()` |
| Internal classes | PascalCase | `EventDispatcher` |
| Private/helper classes | PascalCase with leading underscore | `_ScreenTracker` |
| Functions | camelCase | `dispatchEvent()` |
| Properties/variables | camelCase | `currentScreenName` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_QUEUE_SIZE` |
| Package names | lowercase, single words or snake_case | `com.app.traceless.analytic` |

### Public API Design (per PRD)
Follow the exact signatures from the PRD:
```kotlin
object Analytics {
    fun enterScreen(screenName: String)
    
    fun trackUI(
        elementId: String,
        action: String,
        extra: Map<String, Any>? = null
    )
}
```

### Error Handling
- **Never suppress type errors** (`as any`, `@ts-ignore`, `@ts-expect-error`)
- **Never use empty catch blocks**
- Throw meaningful exceptions for SDK misuse
- Return null for optional states (don't throw)
- Wrap Firebase calls with try-catch for graceful degradation

### State Management
Per PRD requirements:
- Minimal internal state: `currentScreenName: String?`, `sessionId: String`
- No stack, no singleton instances, no UUIDs
- Session-aware context only
- Offline: queue best-effort (don't crash)

### Function Design
- Max 20 lines per function (excluding comments)
- Single responsibility principle
- Pure functions preferred for business logic
- Pass dependencies explicitly (no implicit context)

### Documentation
- KDoc for all public APIs
- Include parameters, return values, and edge cases
- Internal APIs may have inline comments for complex logic
```kotlin
/**
 * Logs a screen view event.
 *
 * @param screenName Business identifier in snake_case (e.g., "home", "product_detail")
 * @throws IllegalArgumentException if screenName is blank
 */
fun enterScreen(screenName: String)
```

### Testing Guidelines
- Unit tests for core business logic
- Test edge cases: null screen_name, rapid calls, offline scenarios
- Mock Firebase dependencies for unit tests
- Name test methods descriptively: `givenNewSession_whenEnterScreen_thenEmitsScreenView()`

---

## SDK Core Architecture (per PRD)

```
Analytics (public API)
├── enterScreen(screenName) → screen_view event
└── trackUI(elementId, action, extra?) → ui_interaction event

Internal State:
├── currentScreenName: String? (reset on new session)
└── sessionId: String (stable per session)

Dispatchers:
└── FirebaseAdapter (ON by default)
```

### Event Types (Required)
1. **screen_view** - User viewing a business screen
2. **ui_interaction** - User action with screen context
3. **ad_impression** - Firebase auto (SDK does NOT log)

### Behavior Rules
| Scenario | Handling |
|----------|----------|
| App background | Don't reset screen state |
| New session | Reset currentScreenName |
| Offline | Queue events (best-effort) |
| Crash | Don't retry events |

---

## Code Organization

### Module Structure
```
traceless-analytic/src/main/kotlin/
├── com/app/traceless/analytic/
│   ├── Analytics.kt           # Public API
│   ├── internal/
│   │   ├── EventBuilder.kt    # Construct events with auto-context
│   │   ├── SessionManager.kt  # Session lifecycle & ID
│   │   ├── Dispatcher.kt      # Firebase adapter + queue
│   │   └── _State.kt          # Minimal internal state
│   └── TracelessAnalytics.kt  # Entry point
```

### File Naming
- One public class per file (matching filename)
- Internal/helper classes in same file when tightly coupled
- Max 400 lines per file
