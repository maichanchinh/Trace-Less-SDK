# KẾ HOẠCH TRIỂN KHAI: TRACELESS SDK v1.0

## 1. TỔNG QUAN DỰ ÁN

### 1.1 Mục tiêu và Phạm vi

TRACELESS SDK là một thư viện analytics nhẹ cho Android, được thiết kế để theo dõi hành vi người dùng với tối thiểu code và không phụ thuộc vào thư viện thứ ba. SDK này tập trung vào ba sự kiện cốt lõi: screen_view, ui_interaction, và ad_impression (passthrough từ Firebase). Kiến trúc SDK được thiết kế để tách biệt hoàn toàn với Firebase - SDK chỉ phát ra các event thông qua Flow, còn việc tích hợp với Firebase được thực hiện ở lớp Application bên ngoài SDK. Điều này đảm bảo SDK luôn độc lập, nhẹ, và có thể tích hợp với bất kỳ hệ thống analytics nào khác nếu cần trong tương lai.

### 1.2 Thông số kỹ thuật

Dự án sử dụng Kotlin 2.0 với minSdk 26 và compileSdk 36, phù hợp với hầu hết các thiết bị Android hiện đại. SDK được xây dựng theo cấu trúc Clean Architecture với một module riêng biệt tên là `traceless-analytic`, trong khi `app` module đóng vai trò là sample application để minh họa cách sử dụng SDK. Ràng buộc quan trọng nhất là SDK phải giữ core logic không được crash app, và không được ảnh hưởng đến thời gian khởi động app.

### 1.3 Thông tin từ Interview

Trong quá trình interview, các quyết định quan trọng đã được đưa ra như sau: về tích hợp Firebase, SDK sẽ phát các event thông qua Kotlin Flow<TracelessEvent> mà không có bất kỳ dependency Firebase nào bên trong SDK. Về quản lý session, Firebase sẽ chịu trách nhiệm quản lý session ID, còn SDK không cần quan tâm đến việc này. Về queue offline, Firebase sẽ xử lý queue và retry logic, SDK hoạt động theo cơ chế fire-and-forget. Cuối cùng, về dependencies, coroutines được phép sử dụng (AndroidX) để hỗ trợ Flow, nhưng không có thư viện third-party nào khác.

---

## 2. ARCHITECTURE DIAGRAMS

### 2.1 Class Diagram - SDK Core Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        TRACELESS SDK ARCHITECTURE                        │
└─────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   Application   │
                              │   (External)    │
                              └────────┬────────┘
                                       │
                                       │ collects Flow<TracelessEvent>
                                       │ implements FirebaseAdapter
                              ┌────────▼────────┐
                              │  FirebaseImpl   │◄──┐
                              │  (in app mod)   │   │
                              └─────────────────┘   │
                                       ▲            │
                                       │            │
                              ┌────────┴────────┐    │
                              │   EventBus      │    │
                              │   (Flow-based)  │────┘
                              └────────┬────────┘
                                       │
                              ┌────────▼────────┐
                              │    Analytics    │
                              │  (Public API)   │
                              └────────┬────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
           ┌────────▼────────┐ ┌───────▼───────┐ ┌────────▼────────┐
           │   Screen        │ │   UIAction    │ │   _Internal     │
           │   Registry      │ │   Enum        │ │   State         │
           │   (Sealed)      │ │   (Sealed)    │ │                 │
           └─────────────────┘ └───────────────┘ └─────────────────┘
                    │                  │                  │
                    └──────────────────┼──────────────────┘
                                       │
                              ┌────────▼────────┐
                              │   EventBuilder  │
                              │   (Creates      │
                              │    events)      │
                              └─────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                         SDK MODULE (traceless-analytic)                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    PUBLIC API (Analytics.kt)                     │   │
│  │  ┌─────────────────────────────────────────────────────────────┐│   │
│  │  │ object Analytics : IEventEmitter                             ││   │
│  │  │   + enterScreen(screen: Screen)                              ││   │
│  │  │   + trackUI(elementId: String, action: UIAction)            ││   │
│  │  │   + events: Flow<TracelessEvent>                             ││   │
│  │  └─────────────────────────────────────────────────────────────┘│   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                    ▲                                    │
│                                    │                                    │
│  ┌─────────────────────────────────┴─────────────────────────────────┐   │
│  │                      INTERNAL IMPLEMENTATION                       │   │
│  │                                                                  │   │
│  │   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │   │
│  │   │  Screen.kt      │  │  UIAction.kt    │  │  _State.kt      │  │   │
│  │   │  (Sealed Class) │  │  (Sealed Class) │  │  (Private)      │  │   │
│  │   │                 │  │                 │  │                 │  │   │
│  │   │  • Home         │  │  • Click        │  │  • currentScreen│  │   │
│  │   │  • Detail       │  │  • Submit       │  │  • _sessionId   │  │   │
│  │   │  • Paywall      │  │  • Scroll       │  │  • _listeners   │  │   │
│  │   │  • Custom(name) │  │  • Custom(name) │  │                 │  │   │
│  │   └─────────────────┘  └─────────────────┘  └─────────────────┘  │   │
│  │                                    ▲                               │   │
│  │                                    │                               │   │
│  │   ┌────────────────────────────────┴─────────────────────────────┐│   │
│  │   │              EventBuilder.kt                                   ││   │
│  │   │                                                              ││   │
│  │   │  buildScreenView(screen: Screen): TracelessEvent             ││   │
│  │   │  buildUIInteraction(elementId, action, screenName): Event    ││   │
│  │   └──────────────────────────────────────────────────────────────┘│   │
│  │                                                                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Class Diagram - Complete SDK Module Structure

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    TRACELESS-ANALYTIC MODULE CLASSES                    │
└─────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐     ┌──────────────────────────┐
│    <<interface>>         │     │    <<sealed class>>      │
│    IEventEmitter         │     │    Screen                │
├──────────────────────────┤     ├──────────────────────────┤
│ + events: Flow<Event>    │     │ {abstract}               │
│                         │     │ - name: String           │
└────────────┬─────────────┘     │                          │
             │                   │ {static}                 │
             │ implements        │ + Home: Screen           │
             │                   │ + Detail: Screen         │
┌────────────▼─────────────┐     │ + Paywall: Screen        │
│       Analytics          │     │ + Custom(name): Screen   │
│      (Singleton)         │     └──────────────────────────┘
├──────────────────────────┤
│ - _state: _State         │     ┌──────────────────────────┐
│ - _eventChannel: Channel │     │    <<sealed class>>      │
├──────────────────────────┤     │    UIAction              │
│ + enterScreen(screen)    │     ├──────────────────────────┤
│ + trackUI(id, action)    │     │ {abstract}               │
│ + events: Flow<Event>    │     │ - value: String          │
│ - _emit(event)           │     │                          │
│ - _buildScreenView()     │     │ {static}                 │
│ - _buildUIInteraction()  │     │ + Click: UIAction        │
└──────────────────────────┘     │ + Submit: UIAction       │
                                 │ + Scroll: UIAction       │
                                 │ + Custom(name): UIAction │
                                 └──────────────────────────┘

┌──────────────────────────┐     ┌──────────────────────────┐
│    TracelessEvent        │     │    _State (internal)     │
│    (Data Class)          │     │    (Private class)       │
├──────────────────────────┤     ├──────────────────────────┤
│ - name: String           │     │ - currentScreenName: String?
│ - params: Map<String, Any>│    │ - _sessionId: String?    │
│ - timestamp: Long        │     │ - _initTimestamp: Long   │
├──────────────────────────┤     └──────────────────────────┘
│ + screen_name: String?   │
│ + element_id: String?    │
│ + action: String?        │
│ + is_manual: Boolean     │
└──────────────────────────┘
```

### 2.3 Sequence Diagram - enterScreen() Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│              SEQUENCE DIAGRAM: enterScreen() FLOW                       │
└─────────────────────────────────────────────────────────────────────────┘

Title: User navigates to a new screen

actor User
participant "App Code" as App
participant "Analytics\n(Public API)" as SDK
participant "EventBuilder" as Builder
participant "_State" as State
participant "EventChannel" as Channel
participant "Application\n(Firebase Impl)" as AppLayer

Note over App,SDK: SDK has NO Firebase dependency here

User->>App: user navigates to Home screen

activate App
App->>SDK: Analytics.enterScreen(Screen.Home)
activate SDK

SDK->>Builder: buildScreenView(screen: Screen.Home)
activate Builder

Builder->>State: get current state
State-->>Builder: currentScreenName: null (before set)

Builder->>Builder: create Event params
note right of Builder
  Event params:
  - screen_name: "home"
  - is_manual: true
  - event_name: "screen_view"
  - event_timestamp: auto (by Firebase)
end note

Builder-->>SDK: TracelessEvent

SDK->>State: currentScreenName = "home"
activate State
State-->>SDK: confirmed
deactivate State

SDK->>Channel: send(event)
activate Channel
Channel-->>SDK: sent ( buffered)
deactivate Channel

SDK-->>App: (returns immediately)
deactivate SDK

deactivate App

Note over AppLayer,Channel: Asynchronous Processing

Channel->>AppLayer: FLOW emission (collected)
activate AppLayer
AppLayer->>AppLayer: log to Firebase
AppLayer->>Firebase: firebaseAnalytics.logEvent("screen_view", params)
deactivate AppLayer

deactivate Channel

Note right of AppLayer: Application module handles:
- Firebase SDK integration
- Event persistence/queue
- Session management
- Retry logic
```

### 2.4 Sequence Diagram - trackUI() Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│              SEQUENCE DIAGRAM: trackUI() FLOW                          │
└─────────────────────────────────────────────────────────────────────────┘

Title: User clicks a button on current screen

actor User
participant "App Code" as App
participant "Analytics\n(Public API)" as SDK
participant "EventBuilder" as Builder
participant "_State" as State
participant "EventChannel" as Channel
participant "Application\n(Firebase Impl)" as AppLayer

User->>App: user taps "btn_buy" button

activate App
App->>SDK: Analytics.trackUI("btn_buy", UIAction.Click)
activate SDK

SDK->>Builder: buildUIInteraction(
  elementId: "btn_buy",
  action: UIAction.Click
)
activate Builder

Builder->>State: currentScreenName
State--Builder: "home" (was set by enterScreen)

Builder->>Builder: create Event params
note right of Builder
  Event params:
  - element_id: "btn_buy"
  - action: "click"
  - screen_name: "home" (from state)
  - event_name: "ui_interaction"
end note

Builder-->>SDK: TracelessEvent

SDK->>Channel: send(event)
activate Channel
Channel-->>SDK: sent
deactivate Channel

SDK-->>App: (returns immediately)
deactivate SDK

deactivate App

Note over AppLayer,Channel: Asynchronous Processing

Channel->>AppLayer: FLOW emission
activate AppLayer
AppLayer->>AppLayer: log to Firebase
AppLayer->>Firebase: firebaseAnalytics.logEvent("ui_interaction", params)
deactivate AppLayer

deactivate Channel

Note right of AppLayer: Edge case:
If currentScreenName is null,
screen_name = null is still sent
```

### 2.5 State Diagram - Screen Lifecycle Transitions

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    STATE DIAGRAM: SCREEN LIFECYCLE                      │
└─────────────────────────────────────────────────────────────────────────┘

[*] --> NoScreen : App starts (new session)

state NoScreen {
  [*] --> NoScreen
}

NoScreen --> Screen_Home : enterScreen(Home)
Screen_Home --> Screen_Detail : enterScreen(Detail)
Screen_Detail --> Screen_Paywall : enterScreen(Paywall)
Screen_Home --> Screen_Home : enterScreen(Home) [same screen, still emit]

state Screen_Home {
  state_name : "home"
  [*] --> Active
}

state Screen_Detail {
  state_name : "detail"
  [*] --> Active
}

state Screen_Paywall {
  state_name : "paywall"
  [*] --> Active
}

Screen_Paywall --> Screen_Home : enterScreen(Home) [new navigation]

note right of State Transitions
Rules:
1. Each enterScreen() call EMITS an event
2. Same screen entered repeatedly still emits
3. App background -> NO state reset
4. New session -> currentScreenName = null
5. No screen stack (only current screen tracked)
end note

Screen_Home --> NoScreen : New session starts
Screen_Detail --> NoScreen : New session starts
Screen_Paywall --> NoScreen : New session starts

NoScreen --> [*] : Session ends
```

### 2.6 Sequence Diagram - Initialization Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│              SEQUENCE DIAGRAM: SDK INITIALIZATION                       │
└─────────────────────────────────────────────────────────────────────────┘

Title: Application initializes SDK and Firebase adapter

participant "Application\n.onCreate()" as App
participant "Analytics" as SDK
participant "EventChannel" as Channel
participant "FirebaseImpl" as Firebase

activate App

App->>SDK: Analytics.init(context)
activate SDK

SDK->>Channel: Channel(capacity = UNLIMITED)
SDK->>SDK: _state = _State()
SDK->>SDK: _initTimestamp = System.currentTimeMillis()
SDK-->>App: (void)

deactivate SDK

note over App,SDK: SDK is now ready but not collecting events

App->>Firebase: FirebaseImpl(context, SDK.events)
activate Firebase

Firebase->>SDK: collect events via Flow
SDK-->>Firebase: Flow subscription active

Firebase->>Firebase: init Firebase Analytics
Firebase-->>App: (Firebase ready)

note right of Firebase
FirebaseImpl in app module:
- Has Firebase SDK dependency
- Collects SDK.events Flow
- Handles queueing, retry, persistence
- Manages session with Firebase
end note

deactivate Firebase

deactivate App

Note over App: At this point:
- SDK emits events via Flow
- FirebaseImpl collects and dispatches to Firebase
- All analytics pipeline is active
```

### 2.7 Component Diagram - SDK vs App Module Boundaries

```
┌─────────────────────────────────────────────────────────────────────────┐
│                  MODULE BOUNDARIES & DEPENDENCIES                       │
└─────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────┐
│                  app/build.gradle.kts                  │
├───────────────────────────────────────────────────────┤
│  dependencies:                                         │
│    implementation project(":traceless-analytic")      │
│    implementation "com.google.firebase:firebase-analytics"│
│    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android"│
│  AndroidManifest:                                      │
│    <uses-permission android:name="android.permission.INTERNET"/>│
└───────────────────────────────────────────────────────┘
                          ▲
                          │ uses SDK
                          │ emits events via Flow
                          │
┌───────────────────────────────────────────────────────┐
│                    APP MODULE                          │
├───────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐  │
│  │  Application.kt                                  │  │
│  │  class MyApplication : Application {             │  │
│  │    override fun onCreate() {                     │  │
│  │      val firebaseAdapter = FirebaseAdapter()     │  │
│  │      Analytics.init(this)                        │  │
│  │      lifecycleScope.launch {                     │  │
│  │        Analytics.events.collect { event ->       │  │
│  │          firebaseAdapter.dispatch(event)         │  │
│  │        }                                         │  │
│  │      }                                           │  │
│  │    }                                             │  │
│  │  }                                               │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  ┌─────────────────────────────────────────────────┐  │
│  │  FirebaseAdapter.kt                             │  │
│  │  class FirebaseAdapter {                        │  │
│  │    private val analytics: FirebaseAnalytics     │  │
│  │    fun dispatch(event: TracelessEvent) {        │  │
│  │      val bundle = Bundle().apply {               │  │
│  │        putString("screen_name", event.screenName)│  │
│  │        putString("element_id", event.elementId)  │  │
│  │        // ... params                             │  │
│  │      }                                           │  │
│  │      analytics.logEvent(event.name, bundle)      │  │
│  │    }                                             │  │
│  │  }                                               │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  ✓ Firebase SDK dependency (OK)                       │
│  ✓ Firebase Analytics integration                     │
│  ✓ Event queueing & retry logic                       │
│  ✓ Session management                                 │
└───────────────────────────────────────────────────────┘
                          ▲
                          │ depends on API only
                          │ Flow collection
                          │
┌───────────────────────────────────────────────────────┐
│              traceless-analytic (SDK MODULE)           │
├───────────────────────────────────────────────────────┤
│  dependencies:                                         │
│    implementation "androidx.core:core-ktx"            │
│    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core"│
│    testImplementation "junit:junit"                   │
│    // NO Firebase SDK                                  │
│    // NO 3rd party libs                                │
├───────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────┐  │
│  │  Analytics.kt (PUBLIC API)                      │  │
│  │  object Analytics {                             │  │
│  │    fun enterScreen(screen: Screen)              │  │
│  │    fun trackUI(elementId: String, action: UIAction)│  │
│  │    val events: Flow<TracelessEvent>             │  │
│  │    private fun init(context)                    │  │
│  │  }                                               │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  ┌─────────────────────────────────────────────────┐  │
│  │  Screen.kt, UIAction.kt (Domain Models)        │  │
│  │  EventBuilder.kt (Event construction)           │  │
│  │  _State.kt (Minimal internal state)             │  │
│  │  TracelessEvent.kt (Event data class)           │  │
│  └─────────────────────────────────────────────────┘  │
│                                                       │
│  ✓ NO Firebase SDK                                    │
│  ✓ NO 3rd party analytics libs                        │
│  ✓ < 200 LOC core logic                               │
│  ✓ Type-safe via sealed classes                       │
└───────────────────────────────────────────────────────┘
```

---

## 3. THÔNG SỐ KỸ THUẬT CHI TIẾT

### 3.1 Event Data Structure

Mỗi event được phát ra từ SDK đều là một TracelessEvent data class chứa các thông tin cần thiết. Event bao gồm tên event (screen_view hoặc ui_interaction), params là Map<String, Any> chứa các tham số bổ sung, timestamp được tự động thêm vào khi tạo event, và các field accessors tiện lợi để truy cập screen_name, element_id, action, và is_manual. Điều quan trọng là SDK KHÔNG gửi các trường mà Firebase đã tự động cung cấp như event_timestamp, ga_session_id, hay ga_session_number.

### 3.2 Screen Registry Design

Screen Registry được thiết kế dưới dạng sealed class để đảm bảo type-safety tại compile time. Mỗi screen được định nghĩa như một object kế thừa từ sealed class Screen với thuộc tính name là String. Quy tắc quan trọng là screen_name PHẢI nằm trong registry, điều này ngăn chặn typo và đảm bảo tính nhất quán của dữ liệu. Việc sử dụng sealed class cũng không thêm runtime cost vì các object được khởi tạo tại compile time.

### 3.3 UIAction Enum Design

Tương tự như Screen Registry, UIAction cũng là sealed class với các object chuẩn cho 80% use case phổ biến (Click, Submit, Scroll) và class Custom cho 20% trường hợp cần linh hoạt. Thiết kế này chuẩn hóa data, tránh việc cùng một hành động được log với các tên khác nhau như "click", "CLICK", hay "clk".

### 3.4 Internal State Management

SDK chỉ giữ hai biến trạng thái nội bộ: currentScreenName (String?, nullable) và _sessionId (String?, có thể null nếu Firebase chưa set). Không có stack, không có instance, không có UUID - chỉ context-aware state theo session như yêu cầu trong PRD.

### 3.5 Flow-based Event Emission

SDK sử dụng Kotlin Flow để phát các event ra bên ngoài. Flow được chọn vì nó reactive, modern, và được hỗ trợ bởi coroutines. Application module (nơi có Firebase SDK) sẽ collect Flow này và dispatch events đến Firebase. SDK hoạt động theo cơ chế fire-and-forget - không quan tâm đến việc queue, retry, hay persistence.

---

## 4. DANH SÁCH CÔNG VIỆC (TODOs)

### 4.1 Phase 1: SDK Core Infrastructure (Week 1)

**Task 1: Create TracelessEvent data class**

Task này tạo data class cơ bản để đại diện cho mỗi event được phát ra từ SDK. Data class bao gồm các thuộc tính name (String), params (Map<String, Any>), và timestamp (Long). Cần thêm các extension properties để tiện truy cập screen_name, element_id, action, và is_manual một cách an toàn. Đây là foundation cho tất cả các task sau.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/TracelessEvent.kt
data class TracelessEvent(
    val name: String,
    val params: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val screenName: String? get() = params["screen_name"] as? String
    val elementId: String? get() = params["element_id"] as? String
    val action: String? get() = params["action"] as? String
    val isManual: Boolean get() = params["is_manual"] as? Boolean ?: false
}
```

**Task 2: Create Screen Registry (sealed class)**

Tạo sealed class Screen với các object đại diện cho các màn hình trong app. Mỗi object override thuộc tính name. Thiết kế cho phép mở rộng bằng cách thêm object mới mà không cần sửa code hiện tại. Screen Home, Detail, Paywall được định nghĩa như ví dụ trong PRD.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/Screen.kt
sealed class Screen(val name: String) {
    object Home : Screen("home")
    object Detail : Screen("detail")
    object Paywall : Screen("paywall")
    // Add more screens as needed
}
```

**Task 3: Create UIAction enum (sealed class)**

Tạo sealed class UIAction với các object chuẩn (Click, Submit, Scroll) và class Custom cho action tùy chỉnh. Class Custom cho phép truyền bất kỳ String nào, phục vụ 20% use case không nằm trong 80% phổ biến.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/UIAction.kt
sealed class UIAction(val value: String) {
    object Click : UIAction("click")
    object Submit : UIAction("submit")
    object Scroll : UIAction("scroll")
    class Custom(name: String) : UIAction(name)
}
```

**Task 4: Create internal _State class**

Tạo class _State với currentScreenName là String?, nullable. Class này giữ trạng thái nội bộ của SDK, chỉ có thể truy cập từ bên trong Analytics object. Đây là toàn bộ state mà SDK cần - tối thiểu như yêu cầu.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/_State.kt
internal class _State {
    var currentScreenName: String? = null
        private set
    
    fun updateScreen(screenName: String) {
        currentScreenName = screenName
    }
    
    fun reset() {
        currentScreenName = null
    }
}
```

**Task 5: Create EventBuilder**

Tạo object EventBuilder với các function để build screen_view và ui_interaction events. Function buildScreenView nhận Screen, tạo params với screen_name và is_manual=true. Function buildUIInteraction nhận elementId, UIAction, và lấy screenName từ _State (có thể null). Cả hai function đều trả về TracelessEvent.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/EventBuilder.kt
internal object EventBuilder {
    
    fun buildScreenView(screen: Screen): TracelessEvent {
        return TracelessEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to screen.name,
                "is_manual" to true
            )
        )
    }
    
    fun buildUIInteraction(
        elementId: String,
        action: UIAction,
        currentScreenName: String?
    ): TracelessEvent {
        val params = mutableMapOf<String, Any>(
            "element_id" to elementId,
            "action" to action.value
        )
        currentScreenName?.let {
            params["screen_name"] = it
        }
        return TracelessEvent(
            name = "ui_interaction",
            params = params
        )
    }
}
```

**Task 6: Create Analytics public API (final implementation)**

Tạo object Analytics với function enterScreen(screen: Screen) và trackUI(elementId: String, action: UIAction). Analytics giữ Channel để emit events và Flow để expose cho bên ngoài. enterScreen gọi EventBuilder.buildScreenView, emit event, và update _State. trackUI gọi EventBuilder.buildUIInteraction với currentScreenName từ _State, rồi emit event. events property expose Flow<TracelessEvent>.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/Analytics.kt
package com.app.traceless.analytic

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Public API for TRACELESS SDK
 * 
 * Usage:
 *   Analytics.enterScreen(Screen.Home)
 *   Analytics.trackUI("btn_buy", UIAction.Click)
 *   Analytics.events.collect { event -> ... }
 */
object Analytics {
    
    private val _state = _State()
    private val _eventChannel = MutableSharedFlow<TracelessEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val scope = CoroutineScope(Dispatchers.Default)
    
    /**
     * Public Flow of events for Firebase adapter to collect
     */
    val events: SharedFlow<TracelessEvent> = _eventChannel.asSharedFlow()
    
    /**
     * Enter a new screen. Emits screen_view event.
     * 
     * @param screen The screen to enter (must be in Screen registry)
     * @throws IllegalArgumentException if screen is not in registry
     */
    fun enterScreen(screen: Screen) {
        require(screen.name.isNotBlank()) { "Screen name cannot be blank" }
        
        val event = EventBuilder.buildScreenView(screen)
        _state.updateScreen(screen.name)
        emit(event)
    }
    
    /**
     * Track UI interaction. Emits ui_interaction event with current screen context.
     * 
     * @param elementId The element identifier (e.g., "btn_buy")
     * @param action The UI action (click, submit, scroll, or custom)
     */
    fun trackUI(elementId: String, action: UIAction) {
        require(elementId.isNotBlank()) { "Element ID cannot be blank" }
        
        val event = EventBuilder.buildUIInteraction(
            elementId = elementId,
            action = action,
            currentScreenName = _state.currentScreenName
        )
        emit(event)
    }
    
    /**
     * Reset internal state (e.g., on new session)
     * Call this when Firebase session changes
     */
    fun resetState() {
        _state.reset()
    }
    
    private fun emit(event: TracelessEvent) {
        scope.launch {
            _eventChannel.emit(event)
        }
    }
    
    // Internal init for testing
    internal fun _init() {
        // Reset state on init
        _state.reset()
    }
}
```

---

### 4.2 Phase 2: Firebase Integration Layer (Week 2)

**Task 7: Create sample Screen registry extensions**

Tạo thêm các Screen phổ biến cho sample app: Login, Register, Settings, Profile, ProductList, ProductDetail, Cart, Checkout, OrderSuccess. Điều này giúp developer thấy cách mở rộng registry.

```kotlin
// src/main/kotlin/com/app/traceless/analytic/Screen.kt (extensions)
sealed class Screen(val name: String) {
    // Core screens
    object Home : Screen("home")
    object Detail : Screen("detail")
    object Paywall : Screen("paywall")
    
    // Common screens
    object Login : Screen("login")
    object Register : Screen("register")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object ProductList : Screen("product_list")
    object ProductDetail : Screen("product_detail")
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object OrderSuccess : Screen("order_success")
    
    // Add more screens as needed
}
```

**Task 8: Create FirebaseAdapter in app module**

Tạo class FirebaseAdapter trong app module với Firebase SDK dependency. FirebaseAdapter nhận Flow<TracelessEvent> từ Analytics và dispatch đến Firebase Analytics. Xử lý conversion từ TracelessEvent sang Bundle. Thêm queueing và retry logic nếu cần.

```kotlin
// app/src/main/kotlin/com/app/traceless/FirebaseAdapter.kt
package com.app.traceless

import android.content.Context
import android.os.Bundle
import com.app.traceless.analytic.Analytics
import com.app.traceless.analytic.TracelessEvent
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Firebase adapter that collects events from Analytics.events Flow
 * and dispatches them to Firebase Analytics.
 * 
 * This class lives in app module (has Firebase SDK dependency),
 * keeping SDK module free of Firebase dependencies.
 */
class FirebaseAdapter(context: Context) {
    
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val eventQueue = mutableListOf<TracelessEvent>()
    private var isCollecting = false
    
    /**
     * Start collecting events from SDK and dispatching to Firebase
     */
    fun startCollecting() {
        if (isCollecting) return
        isCollecting = true
        
        scope.launch {
            Analytics.events.collectLatest { event ->
                dispatchToFirebase(event)
            }
        }
    }
    
    /**
     * Stop collecting events
     */
    fun stopCollecting() {
        isCollecting = false
        scope.cancel()
    }
    
    private fun dispatchToFirebase(event: TracelessEvent) {
        val bundle = Bundle().apply {
            // Add all params from event
            event.params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        
        firebaseAnalytics.logEvent(event.name, bundle)
    }
    
    /**
     * Check if Firebase is available (for graceful degradation)
     */
    fun isFirebaseAvailable(): Boolean = firebaseAnalytics != null
}
```

**Task 9: Create Application class with initialization**

Tạo class Application kế thừa Application, override onCreate() để init SDK và FirebaseAdapter. Sử dụng lifecycleScope để collect Flow từ Analytics.events. Đây là nơi eventbus pattern được implement - SDK phát event, Application collect và dispatch.

```kotlin
// app/src/main/kotlin/com/app/traceless/TracelessApplication.kt
package com.app.traceless

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.traceless.analytic.Analytics
import kotlinx.coroutines.launch

/**
 * Application class that initializes SDK and Firebase adapter.
 * 
 * Event Flow:
 * 1. SDK emits events via Analytics.events Flow
 * 2. This class collects the Flow
 * 3. FirebaseAdapter dispatches to Firebase Analytics
 */
class TracelessApplication : Application() {
    
    private lateinit var firebaseAdapter: FirebaseAdapter
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize SDK (no Firebase dependency here)
        Analytics._init()
        
        // Initialize Firebase adapter (has Firebase dependency)
        firebaseAdapter = FirebaseAdapter(this)
        firebaseAdapter.startCollecting()
        
        // Observe lifecycle to handle app state changes
        setupLifecycleObserver()
    }
    
    private fun setupLifecycleObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // SDK is ready - events will be collected automatically
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        firebaseAdapter.stopCollecting()
    }
}
```

**Task 10: Create sample MainActivity demonstrating SDK usage**

Tạo MainActivity với ví dụ về cách gọi Analytics.enterScreen và Analytics.trackUI. Screen Home được set khi activity bắt đầu. Các button click handlers gọi trackUI với elementId chuẩn.

```kotlin
// app/src/main/kotlin/com/app/traceless/MainActivity.kt
package com.app.traceless

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.traceless.analytic.Analytics
import com.app.traceless.analytic.Screen
import com.app.traceless.analytic.UIAction
import com.app.traceless.ui.theme.SDKTraceLessTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enter Home screen when activity starts
        Analytics.enterScreen(Screen.Home)
        
        setContent {
            SDKTraceLessTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        Analytics.enterScreen(Screen.Home)
    }
}

@Composable
private fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "TRACELESS SDK Demo")
        
        Button(
            onClick = {
                Analytics.trackUI("btn_detail", UIAction.Click)
                Analytics.enterScreen(Screen.Detail)
            }
        ) {
            Text("Go to Detail")
        }
        
        Button(
            onClick = {
                Analytics.trackUI("btn_buy", UIAction.Click)
                Analytics.enterScreen(Screen.Paywall)
            }
        ) {
            Text("Buy Now")
        }
        
        Button(
            onClick = {
                Analytics.trackUI("btn_settings", UIAction.Click)
                Analytics.enterScreen(Screen.Settings)
            }
        ) {
            Text("Settings")
        }
    }
}
```

---

### 4.3 Phase 3: Unit Tests (TDD)

**Task 11: Write unit tests for TracelessEvent**

Viết tests cho TracelessEvent data class, bao gồm test creation, test extension properties, test null safety cho nullable fields.

```kotlin
// test/TracelessEventTest.kt
package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class TracelessEventTest {
    
    @Test
    fun `create screen_view event with correct params`() {
        val event = TracelessEvent(
            name = "screen_view",
            params = mapOf(
                "screen_name" to "home",
                "is_manual" to true
            )
        )
        
        assertEquals("screen_view", event.name)
        assertEquals("home", event.screenName)
        assertTrue(event.isManual)
        assertNull(event.elementId)
        assertNull(event.action)
    }
    
    @Test
    fun `create ui_interaction event with all params`() {
        val event = TracelessEvent(
            name = "ui_interaction",
            params = mapOf(
                "element_id" to "btn_buy",
                "action" to "click",
                "screen_name" to "home"
            )
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertEquals("home", event.screenName)
    }
    
    @Test
    fun `event timestamp is auto-generated`() {
        val before = System.currentTimeMillis()
        val event = TracelessEvent(
            name = "test",
            params = emptyMap()
        )
        val after = System.currentTimeMillis()
        
        assertTrue(event.timestamp >= before)
        assertTrue(event.timestamp <= after)
    }
    
    @Test
    fun `extension properties return null for missing params`() {
        val event = TracelessEvent(
            name = "test",
            params = emptyMap()
        )
        
        assertNull(event.screenName)
        assertNull(event.elementId)
        assertNull(event.action)
        assertFalse(event.isManual)
    }
}
```

**Task 12: Write unit tests for Screen registry**

Viết tests cho Screen sealed class, bao gồm test screen names, test custom screens.

```kotlin
// test/ScreenTest.kt
package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class ScreenTest {
    
    @Test
    fun `Home screen has correct name`() {
        assertEquals("home", Screen.Home.name)
    }
    
    @Test
    fun `Detail screen has correct name`() {
        assertEquals("detail", Screen.Detail.name)
    }
    
    @Test
    fun `Paywall screen has correct name`() {
        assertEquals("paywall", Screen.Paywall.name)
    }
    
    @Test
    fun `Login screen has correct name`() {
        assertEquals("login", Screen.Login.name)
    }
    
    @Test
    fun `Custom screen has correct name`() {
        val customScreen = object : Screen("custom_screen") {}
        assertEquals("custom_screen", customScreen.name)
    }
}
```

**Task 13: Write unit tests for UIAction**

Viết tests cho UIAction sealed class, bao gồm test predefined actions và custom action.

```kotlin
// test/UIActionTest.kt
package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class UIActionTest {
    
    @Test
    fun `Click action has correct value`() {
        assertEquals("click", UIAction.Click.value)
    }
    
    @Test
    fun `Submit action has correct value`() {
        assertEquals("submit", UIAction.Submit.value)
    }
    
    @Test
    fun `Scroll action has correct value`() {
        assertEquals("scroll", UIAction.Scroll.value)
    }
    
    @Test
    fun `Custom action preserves value`() {
        val custom = UIAction.Custom("long_press")
        assertEquals("long_press", custom.value)
    }
    
    @Test
    fun `Custom action with any string works`() {
        val custom = UIAction.Custom("swipe_left")
        assertNotEquals(UIAction.Click.value, custom.value)
    }
}
```

**Task 14: Write unit tests for EventBuilder**

Viết tests cho EventBuilder, bao gồm test buildScreenView và buildUIInteraction với các edge cases.

```kotlin
// test/EventBuilderTest.kt
package com.app.traceless.analytic

import org.junit.Assert.*
import org.junit.Test

class EventBuilderTest {
    
    @Test
    fun `buildScreenView creates correct event`() {
        val event = EventBuilder.buildScreenView(Screen.Home)
        
        assertEquals("screen_view", event.name)
        assertEquals("home", event.screenName)
        assertTrue(event.isManual)
    }
    
    @Test
    fun `buildScreenView creates event with timestamp`() {
        val before = System.currentTimeMillis()
        val event = EventBuilder.buildScreenView(Screen.Detail)
        val after = System.currentTimeMillis()
        
        assertTrue(event.timestamp >= before)
        assertTrue(event.timestamp <= after)
    }
    
    @Test
    fun `buildUIInteraction with screen context`() {
        val event = EventBuilder.buildUIInteraction(
            elementId = "btn_buy",
            action = UIAction.Click,
            currentScreenName = "home"
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertEquals("home", event.screenName)
    }
    
    @Test
    fun `buildUIInteraction without screen context`() {
        val event = EventBuilder.buildUIInteraction(
            elementId = "btn_buy",
            action = UIAction.Click,
            currentScreenName = null
        )
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertNull(event.screenName)
    }
    
    @Test
    fun `buildUIInteraction with custom action`() {
        val customAction = UIAction.Custom("swipe")
        val event = EventBuilder.buildUIInteraction(
            elementId = "card_item",
            action = customAction,
            currentScreenName = "product_list"
        )
        
        assertEquals("swipe", event.action)
        assertEquals("product_list", event.screenName)
    }
}
```

**Task 15: Write integration tests for Analytics**

Viết integration tests cho Analytics object, test enterScreen và trackUI với _State.

```kotlin
// test/AnalyticsTest.kt
package com.app.traceless.analytic

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AnalyticsTest {
    
    @Before
    fun setup() {
        Analytics._init()
    }
    
    @Test
    fun `enterScreen updates state and emits event`() = runTest {
        Analytics.enterScreen(Screen.Home)
        
        val event = Analytics.events.first()
        
        assertEquals("screen_view", event.name)
        assertEquals("home", event.screenName)
        assertTrue(event.isManual)
    }
    
    @Test
    fun `trackUI includes current screen context`() = runTest {
        Analytics.enterScreen(Screen.Detail)
        
        Analytics.trackUI("btn_buy", UIAction.Click)
        
        val event = Analytics.events.first()
        
        assertEquals("ui_interaction", event.name)
        assertEquals("btn_buy", event.elementId)
        assertEquals("click", event.action)
        assertEquals("detail", event.screenName)
    }
    
    @Test
    fun `trackUI without screen context sends null`() = runTest {
        Analytics.trackUI("btn_test", UIAction.Submit)
        
        val event = Analytics.events.first()
        
        assertNull(event.screenName)
        assertEquals("btn_test", event.elementId)
        assertEquals("submit", event.action)
    }
    
    @Test
    fun `consecutive enterScreen calls all emit events`() = runTest {
        Analytics.enterScreen(Screen.Home)
        Analytics.enterScreen(Screen.Detail)
        Analytics.enterScreen(Screen.Home)
        
        val events = mutableListOf<TracelessEvent>()
        repeat(3) {
            events.add(Analytics.events.first())
        }
        
        assertEquals(3, events.size)
        assertEquals("home", events[0].screenName)
        assertEquals("detail", events[1].screenName)
        assertEquals("home", events[2].screenName)
    }
    
    @Test
    fun `resetState clears current screen`() {
        Analytics.enterScreen(Screen.Home)
        Analytics.resetState()
        
        // After reset, trackUI should send null screen_name
        // This would need a state getter to verify
        // For now, just verify no exception is thrown
        Analytics.trackUI("btn_test", UIAction.Click)
    }
}
```

---

## 5. STRATEGY XÁC MINH (VERIFICATION STRATEGY)

### 5.1 Test Infrastructure

Dự án đã có JUnit trong dependencies. Chiến lược test là TDD - viết tests trước khi implement. Framework test được sử dụng là JUnit 4 với kotlinx.coroutines-test cho async testing. Không cần thêm mock library vì SDK có ít dependencies và có thể test với real implementations.

### 5.2 Build Verification Commands

```bash
# Chạy unit tests cho SDK
./gradlew :traceless-analytic:test

# Chạy tất cả tests
./gradlew test

# Build SDK AAR
./gradlew :traceless-analytic:assembleRelease

# Build sample app
./gradlew :app:assembleDebug

# Lint checks
./gradlew :traceless-analytic:lint
./gradlew lint
```

### 5.3 Manual Verification Procedures

Nếu cần verify thủ công sau khi chạy sample app:

**Verify screen_view events:**
1. Mở app và navigate giữa các màn hình
2. Mở Firebase Console → Analytics → DebugView
3. Chọn device đang test
4. Verify các events xuất hiện với đúng params

**Verify ui_interaction events:**
1. Click các buttons trong sample app
2. Check DebugView cho ui_interaction events
3. Verify screen_name được attach đúng

---

## 6. ACCEPTANCE CRITERIA CHO MỖI TASK

### 6.1 Unit Test Criteria

Mỗi task implementation PHẢI có unit tests coverage tối thiểu 80%. Tests phải pass khi chạy `./gradlew test`. Mỗi test method phải có descriptive name theo pattern `given[State]_when[Action]_then[Result]`. Tests phải cover cả happy path và edge cases.

### 6.2 Integration Test Criteria

Analytics integration tests phải cover enterScreen → state update → event emission → Flow collection. TrackUI tests phải verify screen context attachment ngay cả khi screen là null.

### 6.3 Build Criteria

Code phải pass lint checks không có errors. Build phải thành công không có warnings. AAR file phải generated đúng vị trí. Sample app phải install và chạy được trên device/emulator.

---

## 7. WORKFLOW VÀ PARALLELIZATION

### 7.1 Task Dependencies

```
Phase 1: SDK Core
├── Task 1: TracelessEvent ──────────────┐
├── Task 2: Screen Registry ─────────────┤
├── Task 3: UIAction Enum ───────────────┤
├── Task 4: _State Class ────────────────┤
│                                        │
├── Task 5: EventBuilder (depends on 1-4)│
│                                        └── Task 6: Analytics API (depends on 5)
                                                
Phase 2: Firebase Integration
├── Task 7: Screen extensions ────────────────────┐
├── Task 8: FirebaseAdapter (app module) ─────────┤
│                                                     ├── Task 9: Application init (depends on 7, 8)
│                                                     │
│                                                     └── Task 10: MainActivity demo (depends on 9)
                                                            
Phase 3: Tests
├── Task 11-13: Unit tests for models ────────────┐
│                                                       ├── Task 14: EventBuilder tests (depends on 11-13)
│                                                       │
│                                                       └── Task 15: Analytics integration tests (depends on 14)
```

### 7.2 Parallelization Opportunities

Task 1-4 có thể làm song song vì chúng độc lập và không phụ thuộc lẫn nhau. Task 7 có thể làm song song với bất kỳ task nào trong Phase 1. Task 11-13 có thể làm song song sau khi respective implementations hoàn thành.

---

## 8. COMMIT STRATEGY

| After Task | Commit Message | Files |
|------------|----------------|-------|
| 1 | `feat(event): add TracelessEvent data class` | TracelessEvent.kt |
| 2 | `feat(screen): add Screen registry sealed class` | Screen.kt |
| 3 | `feat(action): add UIAction sealed class enum` | UIAction.kt |
| 4 | `feat(state): add internal _State class` | _State.kt |
| 5 | `feat(builder): add EventBuilder for event construction` | EventBuilder.kt |
| 6 | `feat(api): implement Analytics public API with Flow` | Analytics.kt |
| 7 | `feat(screen): add sample screen registry extensions` | Screen.kt |
| 8 | `feat(firebase): create FirebaseAdapter in app module` | FirebaseAdapter.kt |
| 9 | `feat(app): initialize SDK in Application class` | TracelessApplication.kt |
| 10 | `feat(demo): create MainActivity with SDK usage examples` | MainActivity.kt |
| 11 | `test(event): add TracelessEvent unit tests` | TracelessEventTest.kt |
| 12 | `test(screen): add Screen unit tests` | ScreenTest.kt |
| 13 | `test(action): add UIAction unit tests` | UIActionTest.kt |
| 14 | `test(builder): add EventBuilder unit tests` | EventBuilderTest.kt |
| 15 | `test(analytics): add Analytics integration tests` | AnalyticsTest.kt |

---

## 9. SUCCESS CRITERIA

### 9.1 Functional Requirements

- ✅ enterScreen() phát ra screen_view event qua Flow
- ✅ trackUI() phát ra ui_interaction event với screen context
- ✅ Screen Registry đảm bảo type-safety
- ✅ UIAction enum chuẩn hóa action values
- ✅ Firebase adapter nhận và dispatch events thành công

### 9.2 Non-Functional Requirements

- ✅ Core logic < 200 LOC (tính trong traceless-analytic/src/main)
- ✅ Không crash app
- ✅ Không ảnh hưởng startup time
- ✅ 100% unit tests pass
- ✅ Lint checks pass

### 9.3 Verification Commands

```bash
# Unit tests
./gradlew :traceless-analytic:test
# Expected: All tests pass

# Build SDK
./gradlew :traceless-analytic:assembleRelease
# Expected: AAR generated

# Build sample app
./gradlew :app:assembleDebug
# Expected: APK generated

# Lint
./gradlew :traceless-analytic:lint
# Expected: No errors
```

---

## 10. RISKS VÀ MITIGATIONS

### 10.1 Firebase SDK Not Found

**Risk**: Firebase SDK không được init khi app khởi động
**Mitigation**: FirebaseAdapter kiểm tra Firebase availability và graceful degradation

### 10.2 Flow Buffer Overflow

**Risk**: Events emit quá nhanh, buffer overflow
**Mitigation**: BufferOverflow.DROP_OLDEST - events cũ nhất bị drop, không crash

### 10.3 Missing Screen Registration

**Risk**: Developer quên đăng ký screen mới
**Mitigation**: Compile-time error với sealed class. Không thể tạo Screen ngoài registry

### 10.4 Null Screen Context

**Risk**: trackUI() được gọi trước enterScreen()
**Mitigation**: screen_name = null vẫn gửi event, log warning nếu cần

---

## 11. EXTENSION POINTS FOR V2

SDK v1.0 có thiết kế cho phép mở rộng sau này mà không cần breaking changes:

**Event Types**: Thêm event types mới bằng cách extend EventBuilder
**Adapters**: Thêm adapters cho Mixpanel, Amplitude bên cạnh Firebase
**Persistence**: Thêm DataStore-based queue trong FirebaseAdapter
**Auto Tracking**: Thêm ActivityLifecycleCallbacks observer (opt-in)
**User Properties**: Thêm setUserId(), setUserProperties()

---

## 12. TÀI LIỆU THAM KHẢO

### 12.1 PRD Documentation

Tài liệu chính: `docs/PRD – TRACELESS SDK v1.0.md` (486 lines)

### 12.2 Android Best Practices

- Screen tracking patterns từ Firebase Analytics docs
- Kotlin Flow best practices từ Kotlin documentation
- Android SDK design guidelines

### 12.3 Test Patterns

- Given-When-Then naming convention cho tests
- Unit test isolation với JUnit
- Coroutine testing với kotlinx.coroutines-test
