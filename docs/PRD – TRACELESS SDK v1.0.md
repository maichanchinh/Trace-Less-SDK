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

| Phase | Output                        |
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

> "Tôi muốn biết user đang ở màn hình nào khi họ click / tạo doanh thu."

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
Analytics.enterScreen(Screen.Home)
```

---

### 6.3 Parameters

| Param         | Type   | Source | Rule                        |
| ------------- | ------ | ------ | --------------------------- |
| `screen_name` | string | SDK    | từ Screen registry         |
| `is_manual`   | bool   | SDK    | true (luôn là manual event) |

**Lưu ý:** Firebase đã tự động cung cấp `event_timestamp`, `ga_session_id`, `ga_session_number`. SDK KHÔNG cần gửi lại các field này.

---

### 6.4 Screen Registry (Sealed Class)

```kotlin
sealed class Screen(val name: String) {
  object Home : Screen("home")
  object Detail : Screen("detail")
  object Paywall : Screen("paywall")
  // Thêm các screen khác theo nhu cầu
}
```

**Quy tắc:**

* `screen_name` **phải nằm trong registry**
* Compile-time safety
* Không thêm runtime cost

---

### 6.5 Business rules

* Mỗi lần gọi `enterScreen` → 1 event
* SDK set `current_screen_name`
* SDK set `is_manual = true` (phân biệt với auto-tracking nếu có sau này)
* Không cho dev override param hệ thống

---

### 6.6 Edge cases

* Enter cùng screen liên tiếp → vẫn log (mỗi lần gọi = 1 event)
* Background → foreground → screen mới → log bình thường
* Session mới bắt đầu → `current_screen_name` được reset

---

### 6.7 Acceptance Criteria

**Given**

* Session mới

**When**

* enterScreen(Screen.Home)
* enterScreen(Screen.Detail)
* enterScreen(Screen.Home) (lần thứ 2)

**Then**

* Có 3 `screen_view` events
* `screen_name = "home"`, `"detail"`, `"home"`
* `is_manual = true` cho tất cả
* Firebase tự có timestamp, session_id

---

## EVENT 2: `ui_interaction`

### 6.1 Purpose

Ghi nhận **ý định hành động** của user trong ngữ cảnh màn hình.

---

### 6.2 Trigger

```kotlin
Analytics.trackUI(
  elementId = "btn_buy",
  action = UIAction.Click
)
```

---

### 6.3 Parameters

| Param         | Type   | Source | Rule                    |
| ------------- | ------ | ------ | ----------------------- |
| `element_id`  | string | Dev    | chuẩn prefix (btn_, ...) |
| `action`      | string | SDK    | enum hoặc custom string |
| `screen_name` | string | SDK    | từ current_screen_name  |

**Lưu ý:** Firebase đã tự động cung cấp `event_timestamp`, `ga_session_id`. SDK KHÔNG cần gửi lại.

---

### 6.4 Action Enum (Recommended)

```kotlin
sealed class UIAction(val value: String) {
  object Click : UIAction("click")
  object Submit : UIAction("submit")
  object Scroll : UIAction("scroll")
  class Custom(val name: String) : UIAction(name)
}
```

**Quy tắc:**

* 80% case dùng enum chuẩn (Click, Submit, Scroll)
* 20% case linh hoạt dùng `Custom("tên_action")`

---

### 6.5 Business rules

* SDK **KHÔNG** yêu cầu dev truyền screen
* SDK đọc biến `current_screen_name`
* Nếu `current_screen_name` null → `screen_name = null` (vẫn gửi event)
* SDK tự gắn `screen_name`, không cho dev override

---

### 6.6 Edge cases

* Click xảy ra trước `screen_view` → `screen_name = null`
* Rapid click → log đầy đủ

---

### 6.7 Acceptance Criteria

**Given**

* `current_screen_name = "home"`

**When**

* trackUI("btn_buy", UIAction.Click)

**Then**

* `ui_interaction.screen_name = "home"`
* `ui_interaction.element_id = "btn_buy"`
* `ui_interaction.action = "click"`

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
object Analytics {
  fun enterScreen(screen: Screen)
  
  fun trackUI(
    elementId: String,
    action: UIAction
  )
}
```

---

## 7.2 Internal State (SDK)

```kotlin
var currentScreenName: String? = null
var sessionId: String
```

**Quy tắc:**

* **Không có** stack, không có instance, không có UUID
* **Không có** screen_depth (tính bằng SQL nếu cần)
* Session-aware context only

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

```
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

# 10️⃣ CÁC HƯỚNG PHÂN TÍCH SẢN PHẨM CHỈ VỚI 3 EVENT (CORE)

## 10.1 Phân tích Flow & Drop-off (CORE)

**Dựa trên:** `screen_view`

Bạn trả lời được:

* User đi qua bao nhiêu màn hình?
* Drop mạnh ở screen nào?
* Flow phổ biến nhất của user trả tiền?

**Ví dụ**

```
home → detail → paywall → exit
```

---

## 10.2 Phân tích UX hiệu quả

**Dựa trên:** `ui_interaction + screen_view`

Metric:

* CTR button theo screen
* Interaction / screen view ratio
* Screen "đông người xem – ít hành động"

**Câu hỏi trả lời được**

* Nút này có nên đổi vị trí?
* Screen này có overload không?

---

## 10.3 Phân tích Revenue theo Screen

**Dựa trên:** `ad_impression + screen_view`

Cách làm:

* Map ad_impression vào screen bằng time window

Bạn biết:

* Screen nào kiếm tiền tốt nhất
* Screen nào nhiều view nhưng revenue thấp

---

## 10.4 Phân tích Chất lượng Flow (Depth-based - SQL)

**Dựa trên:** `screen_view` + SQL

Nếu cần phân tích depth:

```sql
SELECT 
  screen_name,
  ROW_NUMBER() OVER (PARTITION BY user_pseudo_id, ga_session_id ORDER BY event_timestamp) as depth
FROM events
WHERE event_name = 'screen_view'
```

Insight:

* User đi sâu đến đâu thì bắt đầu thoát?
* App đang "dài" hay "ngắn"?

---

## 10.5 Phân tích Feature-level (gián tiếp)

**Dựa trên:** `ui_interaction`

* btn_buy click rate
* submit / view ratio
* Feature adoption theo screen

---

# 🔚 KẾT LUẬN CUỐI (PM THỰC CHIẾN)

* ✅ 3 event này **ĐỦ dùng cho 80% product decision**
* ✅ SDK gọn, dev nhẹ, data không rác
* ✅ Không khóa kiến trúc cho tương lai
* ✅ Firebase đã có sẵn timestamp, session_id → SDK không thừa
* ✅ Screen Registry đảm bảo type-safety, không typo
* ✅ Action Enum chuẩn hóa data, tránh "click" vs "CLICK" vs "clk"
