# 1️⃣ PRD – TRACELESS SDK v1.0

## 1. Background & Goals

### 1.1 Stage & Scope

* Giai đoạn: **Core analytics foundation**
* Phiên bản: **v1.0**
* Scope: mobile app (Firebase-first)

### 1.2 Core problem

* Tracking hiện tại:

  * Dev gắn nhiều
  * Event rối
  * Phân tích nặng JOIN
* Cần:

  * SDK gọn
  * Data phẳng
  * Phân tích nhanh

### 1.3 Success metrics

**North Star**

* ≥ 95% UI interaction có `screen_name`
* ≥ 90% user flow tái dựng được chỉ bằng `screen_view`

**Guardrails**

* SDK < 200 LOC core logic
* Không crash
* Không ảnh hưởng startup

---

## 2. Delivery Plan

| Phase  | Output                        |
| ------ | ----------------------------- |
| Week 1 | SDK core + API                |
| Week 2 | Firebase adapter + sample app |
| Week 3 | QA + data validation          |
| Week 4 | Release + docs                |

**Release blocker**

* Thiếu `screen_name` trong `ui_interaction`
* Firebase reject event
* Crash liên quan SDK

---

## 3. Product Overview

### 3.1 One-liner

TRACELESS SDK cho phép app log **màn hình và hành vi chính** với **tối thiểu code**, phục vụ phân tích product & revenue.

### 3.2 Feature list

#### Core (v1)

* screen_view
* ui_interaction
* ad_impression (Firebase auto)
* session-aware context

#### Explicitly NOT build

* Auto UI scan
* Heatmap
* Funnel engine
* Custom attribution

---

## 4. Target Users & Scenarios

### Primary user

* Mobile developer
* Product / Analytics

### Core scenario

> “Tôi muốn biết user đang ở màn hình nào khi họ click / tạo doanh thu.”

---

## 5. Scope & Boundaries

### Included

* Context gán screen_name
* Firebase dispatch
* Offline safe (best-effort)

### Excluded

* Multi-provider analytics
* Backend
* Dashboard

---

# 6️⃣ DETAILED REQUIREMENTS – 3 EVENTS

---

## EVENT 1: `screen_view`

### 6.1 Purpose

Đánh dấu **user đang xem màn hình business nào**.

---

### 6.2 Trigger

* SDK gửi event **ngay khi dev gọi**:

```kotlin
Analytics.enterScreen("home")
```

---

### 6.3 Parameters

| Param         | Source | Rule                 |
| ------------- | ------ | -------------------- |
| `screen_name` | Dev    | snake_case, business |
| `session_id`  | SDK    | stable               |
| `timestamp`   | SDK    | event time           |

---

### 6.4 Business rules

* SDK set:

  ```text
  current_screen_name = screen_name
  ```
* Mỗi lần gọi → log 1 event
* Không dedupe

---

### 6.5 Edge cases

* Gọi 2 lần liên tiếp → 2 event
* Không gọi → không có data (chấp nhận)

---

### 6.6 Acceptance Criteria

**Given**

* Session mới

**When**

* enterScreen("home")
* enterScreen("detail")

**Then**

* Có 2 screen_view
* Timestamp tăng dần
* screen_name đúng

---

## EVENT 2: `ui_interaction`

### 6.1 Purpose

Ghi nhận **ý định hành động** của user trong ngữ cảnh màn hình.

---

### 6.2 Trigger

```kotlin
Analytics.trackUI("btn_buy", "click")
```

---

### 6.3 Parameters

| Param         | Source | Rule                   |
| ------------- | ------ | ---------------------- |
| `element_id`  | Dev    | btn_, tab_, item_      |
| `action`      | Dev    | click, submit          |
| `screen_name` | SDK    | từ current_screen_name |
| `session_id`  | SDK    | auto                   |
| `timestamp`   | SDK    | auto                   |

---

### 6.4 Business rules

* SDK **KHÔNG** yêu cầu dev truyền screen
* SDK đọc biến `current_screen_name`
* Nếu null → vẫn gửi event

---

### 6.5 Edge cases

* Click xảy ra trước screen_view → `screen_name = null`
* Rapid click → log đầy đủ

---

### 6.6 Acceptance Criteria

**Given**

* current_screen_name = "home"

**When**

* trackUI("btn_buy", "click")

**Then**

* ui_interaction.screen_name = "home"

---

## EVENT 3: `ad_impression`

### 6.1 Purpose

Thu doanh thu **thực**, không chỉnh sửa.

---

### 6.2 Source

* Firebase auto event (AdMob / mediation)

### 6.3 SDK behavior

* SDK **KHÔNG log lại**
* SDK **KHÔNG enrich**

---

### 6.4 Acceptance Criteria

* Revenue xuất hiện đúng trong BigQuery
* Không duplicate

---

# 7️⃣ HƯỚNG CODE & CẤU TRÚC SDK

## 7.1 Public API (final)

```kotlin
Analytics.enterScreen(screenName: String)

Analytics.trackUI(
  elementId: String,
  action: String,
  extra: Map<String, Any>? = null
)
```

---

## 7.2 Internal State (SDK)

```kotlin
var currentScreenName: String? = null
var sessionId: String
```

👉 **Không có stack, không có instance, không có UUID.**

---

## 7.3 Event Builder

```kotlin
buildEvent(
  name,
  params + autoContext
)
```

---

## 7.4 Dispatcher

```text
Dispatcher
 └── FirebaseAdapter (ON)
 └── Others (OFF)
```

---

# 8️⃣ HÀNH VI HỆ THỐNG (BEHAVIOR)

| Hành vi        | Cách xử lý              |
| -------------- | ----------------------- |
| App background | Không reset screen      |
| New session    | Reset currentScreenName |
| Offline        | Queue best-effort       |
| Crash          | Không retry             |

---

# 9️⃣ HƯỚNG PHÂN TÍCH SẢN PHẨM (CHỈ VỚI 3 EVENT)

## 9.1 User Flow

* ORDER BY timestamp
* screen_view sequence

## 9.2 UX Effectiveness

* Click / screen_view ratio
* Button CTR theo screen

## 9.3 Revenue Attribution

* Map `ad_impression` vào screen bằng time window

## 9.4 Funnel (logic data-side)

* ROW_NUMBER() over session
* Không cần depth trong SDK

---

# 🔚 KẾT LUẬN CUỐI (PM THỰC CHIẾN)

* Đây là **phiên bản “đủ dùng thật”**, không phải bản để khoe kiến trúc
* SDK:

  * Gọn
  * Ít state
  * Dễ maintain
* Data:

  * Phẳng
  * Query nhanh
  * Không JOIN đau đầu

**Câu chốt:**

> *TRACELESS không cố theo dõi mọi thứ – nó chỉ theo dõi những thứ đáng quyết định.*

