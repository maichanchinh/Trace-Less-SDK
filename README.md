# Traceless Analytics SDK

A lightweight Android analytics SDK for tracking screen views and UI interactions with Firebase Analytics integration.

## Table of Contents
1. [Overview](#overview)
2. [Installation](#installation)
3. [Quick Start](#quick-start)
4. [UIScreen Management](#uiscreen-management)
5. [UIAction Types](#uiaction-types)
6. [Logging](#logging)
7. [API Reference](#api-reference)
8. [Demo App Guide](#demo-app-guide)
9. [Troubleshooting](#troubleshooting)

---

## Overview

Traceless Analytics SDK provides a simple, type-safe API for tracking user behavior in Android applications. It focuses on core analytics events:

- **screen_view** - Track when users view business screens
- **ui_interaction** - Track UI actions with automatic screen context
- **ad_impression** - Auto-tracked by Firebase (SDK does not log)

### Key Features

- **Minimal API** - Only two public functions: `enterScreen()` and `trackUI()`
- **Extensible** - Interface + data class pattern for screens and actions
- **Session-aware** - Automatic screen context for UI interactions
- **Debug logging** - Optional Timber logging with runtime toggle
- **Firebase-first** - Seamless Firebase Analytics integration
- **Custom screens** - Define app-specific screens without SDK modification
- **Custom actions** - Define complex actions for your business needs

### Requirements

- **Android API 26+** (Android 8.0)
- **Kotlin 2.0.21+**
- **Firebase Analytics** (for event dispatch)

---

## Installation

Add the SDK to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.maichanchinh:traceless-analytic:2.0.0")
}
```

### Setup

Initialize the SDK in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Analytics.initialize()
    }
}
```

Add the `Application` class to your `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ... >
    <!-- Your activities -->
</application>
```

---

## Quick Start

### Track a Screen View

```kotlin
// In your Activity or Fragment
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_home)

    // Track screen view using predefined screen
    Analytics.enterScreen(Main)
}
```

### Track a UI Interaction

```kotlin
// Track button click
button.setOnClickListener {
    Analytics.trackUI("btn_buy", Click)
}

// Track form submit
submitButton.setOnClickListener {
    Analytics.trackUI("btn_submit", Submit)
}

// Track custom action
Analytics.trackUI("element_id", UIActionImpl("swipe"))
}
```

### Complete Example

```kotlin
class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Track screen view
        Analytics.enterScreen(Main)

        // Track UI interactions
        findViewById<Button>(R.id.btn_buy).setOnClickListener {
            Analytics.trackUI("btn_buy", Click)
        }
    }
}
```

---

## UIScreen Management

### Overview

The SDK uses an **interface + data class pattern** for screens, allowing external apps to define their own screens without modifying the SDK. This approach:

- Prevents external apps from creating subclasses of a sealed class
- Provides flexibility for custom screen definitions
- Maintains type safety through the `Screen` interface

### Predefined Screens

The SDK includes only essential screens:

```kotlin
// Basic screens
Splash  // Splash screen
Main    // Main/home screen
```

### Custom Screens

Define your own screens using `ScreenImpl`:

```kotlin
// Create a custom screen
val splashScreen = ScreenImpl("splash")
val onboardingScreen = ScreenImpl("onboarding")
val webViewScreen = ScreenImpl("web_view")

// Use custom screen
Analytics.enterScreen(splashScreen)

// Or inline
Analytics.enterScreen(ScreenImpl("custom_screen_name"))
```

#### Custom Screen Rules

- Name must be non-blank
- Maximum 50 characters
- Must start with lowercase letter
- Only lowercase letters, numbers, and underscores
- Must follow snake_case convention (e.g., "onboarding_step_1")

### Extending Screens for Your App

You can create your own screen definitions by implementing the `Screen` interface:

```kotlin
// Define your app's screens
object Home : ScreenImpl("home")
object Detail : ScreenImpl("detail")
object Profile : ScreenImpl("profile")
object Settings : ScreenImpl("settings")

// Or create a sealed class for your app's screens
sealed class AppScreen(override val name: String) : Screen {
    object Dashboard : AppScreen("dashboard")
    object ProductList : AppScreen("product_list")
    object ProductDetail : AppScreen("product_detail")
    object Cart : AppScreen("cart")
    object Checkout : AppScreen("checkout")
}

// Usage
Analytics.enterScreen(AppScreen.ProductDetail)
```

### Screen Naming Best Practices

For Product Owners and Developers:

| Screen Type | Naming Convention | Example |
|-------------|-------------------|---------|
| Business screens | snake_case | `home`, `product_detail`, `checkout` |
| Feature screens | snake_case | `onboarding`, `settings`, `profile` |
| Modal/detail views | snake_case | `product_variant_selector`, `address_form` |

**Why snake_case?**
- Consistent with Firebase Analytics conventions
- Easy to read and understand
- Works well with SQL-like queries in analytics tools

---

## UIAction Types

### Overview

The SDK provides **basic UI actions** out of the box and allows apps to define **complex actions** for their specific business needs. This separation ensures:

- Consistency for common interactions (clicks, inputs, etc.)
- Flexibility for domain-specific actions (add to cart, share, etc.)
- Clear mapping to product insights and KPIs

### Basic Actions (SDK Provided)

The SDK includes these fundamental UI actions:

```kotlin
// Basic interactions
Click     // User clicks on an element (buttons, links, icons)
Select    // User selects an item from a list, dropdown, or menu
Input     // User enters text or data in a form field
Swipe     // User swipes (left, right, up, down)
Submit    // User submits a form or completes an action
Toggle    // User toggles a switch, checkbox, or on/off element
Navigate  // User navigates to another screen or view
View      // User views content (expand section, open modal, view detail)
Refresh   // User refreshes content
Dismiss   // User dismisses (close, cancel, reject)
```

### Action to Product Insight Mapping

Understanding what each action tells you about user behavior:

| Action | Product Insight | Business Question |
|--------|-----------------|-------------------|
| **Click** | CTR, CTA effectiveness | Are users clicking on your calls-to-action? |
| **Select** | Preference distribution | What options do users choose most? |
| **Submit** | Conversion | Are users completing forms and actions? |
| **Input** | Form friction | Where do users struggle with data entry? |
| **Toggle** | Feature adoption | Which features are users enabling/disabling? |
| **Navigate** | IA clarity | Is your navigation structure intuitive? |
| **View** | Engagement quality | Are users exploring content deeply? |
| **Refresh** | Content freshness needs | Do users expect real-time updates? |
| **Dismiss** | Offer rejection | Why are users rejecting offers or content? |

### Custom Actions

For complex or domain-specific actions, create your own using `UIActionImpl`:

```kotlin
// E-commerce examples
val AddToCart = UIActionImpl("add_to_cart")
val RemoveFromCart = UIActionImpl("remove_from_cart")
val ShareProduct = UIActionImpl("share_product")
val AddToWishlist = UIActionImpl("add_to_wishlist")

// Social features
val LikePost = UIActionImpl("like_post")
val CommentOnPost = UIActionImpl("comment_post")
val FollowUser = UIActionImpl("follow_user")

// Usage
Analytics.trackUI("btn_add_to_cart", AddToCart)
Analytics.trackUI("btn_like", LikePost)
```

### Defining Custom Actions for Your App

Create a sealed class for type-safe custom actions:

```kotlin
// Define your app's actions
sealed class AppAction(override val value: String) : UIAction {
    object AddToCart : AppAction("add_to_cart")
    object RemoveFromCart : AppAction("remove_from_cart")
    object ShareProduct : AppAction("share_product")
    object WishlistToggle : AppAction("wishlist_toggle")
    object RateProduct : AppAction("rate_product")
    object ApplyCoupon : AppAction("apply_coupon")
}

// Usage
Analytics.trackUI("btn_add_to_cart", AppAction.AddToCart)
Analytics.trackUI("btn_share", AppAction.ShareProduct)
```

### Action Naming Best Practices

For Product Owners and Developers:

| Action Category | Naming Convention | Example |
|-----------------|-------------------|---------|
| Basic actions | Simple verbs | `click`, `submit`, `input` |
| Domain actions | snake_case with context | `add_to_cart`, `share_product` |
| Complex flows | descriptive | `complete_onboarding`, `subscription_upgrade` |

### When to Use Basic vs Custom Actions

**Use Basic Actions When:**
- Tracking fundamental UI interactions
- You want standardized insights (CTR, conversion, etc.)
- The action is common across many apps

**Use Custom Actions When:**
- Tracking domain-specific user behaviors
- You need detailed funnel analysis
- The action has unique business significance

### Example: Complete Tracking Setup

```kotlin
// Define your app's screens and actions
object Screens {
    val Splash = ScreenImpl("splash")
    val Home = ScreenImpl("home")
    val ProductList = ScreenImpl("product_list")
    val ProductDetail = ScreenImpl("product_detail")
    val Cart = ScreenImpl("cart")
    val Checkout = ScreenImpl("checkout")
}

sealed class Actions(override val value: String) : UIAction {
    object AddToCart : Actions("add_to_cart")
    object RemoveFromCart : Actions("remove_from_cart")
    object ApplyCoupon : Actions("apply_coupon")
    object SelectPayment : Actions("select_payment")
    object CompletePurchase : Actions("complete_purchase")
}

// In your Activity
class ProductDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Analytics.enterScreen(Screens.ProductDetail)
    }

    fun onAddToCartClicked() {
        Analytics.trackUI("btn_add_to_cart", Actions.AddToCart)
    }
}
```

---

## Logging

### Enable Debug Logging

Enable Timber logging during development to see analytics events in logcat:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Enable debug logging (development only)
        if (BuildConfig.DEBUG) {
            Analytics.setDebugMode(true)
            Timber.plant(Timber.DebugTree())
        }
        
        Analytics._init()
    }
}
```

### View Logs

Filter logs in logcat:

```bash
adb logcat -s TracelessLogger:D *:S
```

### Disable Logging (Production)

Disable logging for production builds:

```kotlin
// In release builds
Analytics.setDebugMode(false)  // Only ERROR level logs
```

### Log Format

All logs use the `[Traceless]` tag:

```
D/TracelessLogger: [Traceless] ENTER_SCREEN: home
D/TracelessLogger: [Traceless] TRACK_UI: btn_buy click (screen: home)
D/TracelessLogger: [Traceless] EMIT_EVENT: screen_view (screen: home)
```

---

## API Reference

### Analytics

Public API object for all analytics operations.

#### Methods

##### `enterScreen(screen: Screen)`

Logs a screen view event.

**Parameters:**
- `screen` - Screen instance (predefined or custom)

**Example:**
```kotlin
// Using predefined screen
Analytics.enterScreen(Main)

// Using custom screen
Analytics.enterScreen(ScreenImpl("product_detail"))
```

**Throws:**
- `IllegalArgumentException` if screen name is blank

---

##### `trackUI(elementId: String, action: UIAction)`

Logs a UI interaction event with automatic screen context.

**Parameters:**
- `elementId` - Element identifier (e.g., "btn_buy")
- `action` - UI action (basic or custom)

**Example:**
```kotlin
// Using basic action
Analytics.trackUI("btn_buy", Click)

// Using custom action
Analytics.trackUI("btn_add_to_cart", UIActionImpl("add_to_cart"))
```

**Throws:**
- `IllegalArgumentException` if elementId is blank

---

##### `setDebugMode(enabled: Boolean)`

Enables or disables debug logging.

**Parameters:**
- `enabled` - `true` for DEBUG level, `false` for ERROR only

**Example:**
```kotlin
Analytics.setDebugMode(true)   // Enable debug logs
Analytics.setDebugMode(false)  // Disable debug logs
```

---

##### `resetState()`

Resets internal state (e.g., on new Firebase session).

**Example:**
```kotlin
// Call when Firebase session changes
Analytics.resetState()
```

---

##### `_init()`

Initializes SDK state. Call from `Application.onCreate()`.

**Example:**
```kotlin
Analytics._init()
```

---

### Screen

Interface for screen identifiers with a data class implementation.

#### Interface

```kotlin
interface Screen {
    val name: String
}
```

#### Data Class Implementation

```kotlin
data class ScreenImpl(override val name: String) : Screen
```

#### Predefined Objects

```kotlin
Splash  // Splash screen
Main    // Main/home screen
```

#### Creating Custom Screens

```kotlin
// Using ScreenImpl directly
val splashScreen = ScreenImpl("splash")
val productDetail = ScreenImpl("product_detail")

// Usage
Analytics.enterScreen(splashScreen)
```

**Rules:**
- Name must be non-blank
- Maximum 50 characters
- Must start with lowercase letter
- Only lowercase letters, numbers, and underscores

---

### UIAction

Interface for UI action types with a data class implementation.

#### Interface

```kotlin
interface UIAction {
    val value: String
}
```

#### Data Class Implementation

```kotlin
data class UIActionImpl(override val value: String) : UIAction
```

#### Basic Actions (Predefined)

```kotlin
Click     // User clicks on an element
Select    // User selects an item
Input     // User enters text/data
Swipe     // User swipes
Submit    // User submits a form
Toggle    // User toggles a switch
Navigate  // User navigates
View      // User views content
Refresh   // User refreshes
Dismiss   // User dismisses
```

#### Creating Custom Actions

```kotlin
// Using UIActionImpl directly
val addToCart = UIActionImpl("add_to_cart")
val shareProduct = UIActionImpl("share_product")

// Usage
Analytics.trackUI("btn_add_to_cart", addToCart)
```

**Example:**
```kotlin
// Basic action
Analytics.trackUI("btn_buy", Click)

// Custom action
Analytics.trackUI("btn_add_to_cart", UIActionImpl("add_to_cart"))
```

---

### Flow Access

Access the events flow for Firebase integration:

```kotlin
class FirebaseAdapter {
    fun start() {
        Analytics.events.collect { event ->
            // Send event to Firebase
            FirebaseAnalytics.getInstance(context).logEvent(
                event.name,
                event.params.toBundle()
            )
        }
    }
}
```

---

## Demo App Guide

The included demo app demonstrates the SDK with a simple flow: **Splash → Home → Feature**

### Demo Structure

```
app/
├── SplashActivity.kt      # Splash screen with installSplashScreen()
├── HomeActivity.kt       # Main screen with navigation buttons
└── FeatureActivity.kt     # Feature demo with all analytics
```

### Run the Demo

```bash
# Build and install
./gradlew installDebug

# View logs
adb logcat -s TracelessLogger:D *:S
```

### Flow Overview

1. **SplashActivity** (2 seconds)
   - Uses Android 12+ `installSplashScreen()` API
   - Tracks custom screen: `ScreenImpl("splash")`
   - Navigates to HomeActivity

2. **HomeActivity**
   - Tracks predefined screen: `Main`
   - Displays buttons to navigate to FeatureActivity
   - Tracks button click events

3. **FeatureActivity**
   - Tracks custom screen: `ScreenImpl("feature")`
   - Demonstrates all analytics features:
     - Screen tracking
     - UI actions (Click, Submit, Input, etc.)
     - Timber logging visible in logcat

### Demo Code Examples

#### SplashActivity

```kotlin
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Track splash screen
        Analytics.enterScreen(ScreenImpl("splash"))

        // Keep splash for 2 seconds
        splashScreen.setKeepOnScreenCondition { true }

        lifecycleScope.launch {
            delay(2000)
            splashScreen.setKeepOnScreenCondition { false }

            // Navigate to HomeActivity
            startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
            finish()
        }
    }
}
```

#### HomeActivity

```kotlin
class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Track screen view
        Analytics.enterScreen(Main)

        // Navigate to FeatureActivity
        val goToFeatureButton = findViewById<Button>(R.id.btn_go_to_feature)
        goToFeatureButton.setOnClickListener {
            Analytics.trackUI("btn_go_to_feature", Click)
            startActivity(Intent(this, HomeActivity::class.java))
        }
    }
}
```

#### FeatureActivity

```kotlin
class FeatureActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Track screen view
        Analytics.enterScreen(ScreenImpl("feature"))

        // Track various UI interactions
        findViewById<Button>(R.id.btn_click).setOnClickListener {
            Analytics.trackUI("btn_click", Click)
        }

        findViewById<Button>(R.id.btn_submit).setOnClickListener {
            Analytics.trackUI("btn_submit", Submit)
        }

        findViewById<ScrollView>(R.id.scrollView).setOnScrollChangeListener { _, _, _, _, _ ->
            Analytics.trackUI("content_area", Swipe)
        }

        findViewById<Button>(R.id.btn_custom).setOnClickListener {
            Analytics.trackUI("btn_custom", UIActionImpl("swipe"))
        }
    }
}
```

### Expected Log Output

When running the demo with debug mode enabled, you should see:

```
D/TracelessLogger: [Traceless] ENTER_SCREEN: splash
D/TracelessLogger: [Traceless] EMIT_EVENT: screen_view (screen: splash)

D/TracelessLogger: [Traceless] ENTER_SCREEN: home
D/TracelessLogger: [Traceless] EMIT_EVENT: screen_view (screen: home)

D/TracelessLogger: [Traceless] TRACK_UI: btn_go_to_feature click (screen: home)
D/TracelessLogger: [Traceless] EMIT_EVENT: ui_interaction (screen: home)

D/TracelessLogger: [Traceless] ENTER_SCREEN: feature
D/TracelessLogger: [Traceless] EMIT_EVENT: screen_view (screen: feature)

D/TracelessLogger: [Traceless] TRACK_UI: btn_click click (screen: feature)
D/TracelessLogger: [Traceless] EMIT_EVENT: ui_interaction (screen: feature)
```

---

## Troubleshooting

### Events Not Appearing in Firebase

**Problem:** Events are not showing up in Firebase Analytics dashboard.

**Solutions:**
1. Verify Firebase is properly configured in your app
2. Check that `FirebaseAdapter` is collecting events:
   ```kotlin
   Analytics.events.collect { event ->
       // Verify this is called
       Log.d("Firebase", "Event: ${event.name}")
   }
   ```
3. Enable debug logging:
   ```kotlin
   Analytics.setDebugMode(true)
   adb logcat -s TracelessLogger:D
   ```
4. Check Firebase Analytics reporting delays (can take up to 24 hours)

### Screen Context Missing in UI Events

**Problem:** `screen_name` is null in `ui_interaction` events.

**Solution:** Ensure `enterScreen()` is called before `trackUI()`:

```kotlin
// Correct order
Analytics.enterScreen(Screen.Home)  // Sets screen context
Analytics.trackUI("btn_buy", UIAction.Click)  // Has screen context

// Incorrect order (screen_name will be null)
Analytics.trackUI("btn_buy", UIAction.Click)  // No screen context
Analytics.enterScreen(Screen.Home)  // Too late
```

### Debug Logs Not Showing

**Problem:** Timber logs not appearing in logcat.

**Solutions:**
1. Verify debug mode is enabled:
   ```kotlin
   Analytics.setDebugMode(true)
   ```
2. Ensure Timber is planted:
   ```kotlin
   Timber.plant(Timber.DebugTree())
   ```
3. Check logcat filter:
   ```bash
   adb logcat -s TracelessLogger:D *:S
   ```

### Custom Screen Name Validation Errors

**Problem:** `IllegalArgumentException` when creating custom screens.

**Solution:** Follow screen naming rules:
- Start with lowercase letter
- Use only lowercase letters, numbers, and underscores
- Maximum 50 characters
- Use snake_case convention

```kotlin
// Valid
ScreenImpl("splash")
ScreenImpl("onboarding_step_1")
ScreenImpl("product_detail_v2")

// Invalid (will throw exception)
ScreenImpl("Splash")              // Uppercase
ScreenImpl("1_screen")             // Starts with number
ScreenImpl("screen-name")           // Contains hyphen
ScreenImpl("very_long_screen_name_that_exceeds_maximum_length_limit")  // Too long
```

### Build Errors

**Problem:** Gradle build fails with dependency errors.

**Solutions:**
1. Ensure Android API 26+ is set:
   ```kotlin
   defaultConfig {
       minSdk = 26
   }
   ```
2. Check Kotlin version is 2.0.21+
3. Sync Gradle files: `./gradlew clean build`

### Timber Dependency in Release Builds

**Problem:** Timber is included in release AAR.

**Solution:** Verify Timber is using `debugImplementation`:

```kotlin
dependencies {
    debugImplementation("com.jakewharton.timber:timber:5.0.1")
    // NOT implementation() - this would include in release
}
```

Verify it's not in release:
```bash
./gradlew :traceless-analytic:assembleRelease
unzip -l traceless-analytic/build/outputs/aar/traceless-analytic-release.aar | grep timber
# Expected: No results
```

### Session State Not Resetting

**Problem:** Screen context persists across Firebase sessions.

**Solution:** Call `resetState()` when Firebase session changes:

```kotlin
// In your Firebase integration
firebaseAnalytics.setAnalyticsCollectionEnabled(true)
// Listen for session changes
Analytics.resetState()
```

---

## Additional Resources

- **PRD**: See `docs/PRD – TRACELESS SDK v1.0.md` for detailed requirements
- **AGENTS.md**: Development guidelines and code style
- **Demo App**: Full reference implementation in `app` module

---

## License

[Add your license information here]

---

## Support

For issues, questions, or contributions, please [add your contact information or repository link].
