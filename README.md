# Traceless Analytics SDK

SDK Android nhẹ cân để tracking screen views và UI interactions.

## Quick Start

### Installation

```kotlin
dependencies {
    implementation("io.github.maichanchinh:traceless-analytic:2.0.0")
}
```

### Setup

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Analytics.enableDebug()
        Analytics.initialize()
    }
}
```

### Basic Usage

```kotlin
// Track screen
Analytics.enterScreen(UIScreen.Main)

// Track UI action
Analytics.trackUI("btn_buy", UIAction.Click)
```

---

## Custom Screens

```kotlin
// Predefined
UIScreen.Splash
UIScreen.Main

// Custom screens
sealed class AppScreens : UIScreen {
    data object Home : AppScreens("home")
    data object ProductDetail : AppScreens("product_detail")
}

// Usage
Analytics.enterScreen(AppScreens.ProductDetail)
```

**Naming**: snake_case, max 50 chars, lowercase + underscores only.

---

## UI Actions

```kotlin
// Basic actions
UIAction.Click
UIAction.Select
UIAction.Input
UIAction.Swipe
UIAction.Submit
UIAction.Toggle
UIAction.Navigate
UIAction.View
UIAction.Refresh
UIAction.Dismiss
```

### Cách sử dụng

`trackUI(elementId, action)`:
- **elementId**: Tên UI element (`"btn_buy"`, `"input_email"`)
- **action**: Loại hành động (`Click`, `Submit`, `Swipe`)

```kotlin
Analytics.trackUI("btn_add_to_cart", UIAction.Click)
Analytics.trackUI("input_email", UIAction.Input)
```

### Custom Actions

```kotlin
object LongPress : UIAction("long_press")
Analytics.trackUI("image_preview", LongPress)
```

---

## Jetpack Compose

### Helper (Khuyên dùng)

```kotlin
import com.app.traceless.analytic.compose.TrackScreen

@Composable
fun ProductDetailScreen() {
    TrackScreen(Screens.ProductDetail)  // Auto track 1 lần

    Button(onClick = {
        Analytics.trackUI("btn_buy", UIAction.Click)
    }) {
        Text("Buy")
    }
}
```

### LaunchedEffect

```kotlin
@Composable
fun MyScreen() {
    LaunchedEffect(Unit) {  // Track 1 lần
        Analytics.enterScreen(Screens.Home)
    }

    Button(onClick = {
        Analytics.trackUI("btn_go", UIAction.Click)
    }) {
        Text("Go")
    }
}
```

### Multi-screen

```kotlin
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf(Screens.Home) }

    LaunchedEffect(currentScreen) {  // Auto track khi thay đổi
        Analytics.enterScreen(currentScreen)
    }

    when (currentScreen) {
        Screens.Home -> HomeScreen()
        Screens.Detail -> DetailScreen()
    }
}
```

### ⚠️ Tránh lỗi

```kotlin
// ❌ SAI - Track screen trong onClick
Button(onClick = {
    Analytics.enterScreen(Screens.Detail)  // ĐỪNG!
})

// ❌ SAI - Track trong cả onCreate và onResume
override fun onCreate() {
    Analytics.enterScreen(Screens.Home)
}
override fun onResume() {
    Analytics.enterScreen(Screens.Home)  // Duplicate!
}
```

---

## API Reference

```kotlin
Analytics.initialize()                    // Khởi tạo SDK
Analytics.enterScreen(screen: UIScreen)  // Track screen view
Analytics.trackUI(id: String, action: UIAction)  // Track UI interaction
Analytics.enableDebug() / disableDebug() // Debug mode
```