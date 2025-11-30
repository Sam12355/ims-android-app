# Stock Nexus iOS Application Development Guide

## Project Overview

Stock Nexus is an inventory management system for restaurant/retail operations. This guide provides comprehensive documentation for developing the iOS version with identical functionality, UI/UX, and features as the Android app.

**Target Platforms:** iOS 15.0+, iPadOS 15.0+
**Primary Color:** #E6002A (Stock Nexus Red)
**Design Language:** Material-inspired with iOS adaptations

---

## Table of Contents

1. [User Roles & Permissions](#1-user-roles--permissions)
2. [Authentication Flow](#2-authentication-flow)
3. [Dashboard Features](#3-dashboard-features)
4. [Inventory Management](#4-inventory-management)
5. [Moveout Lists](#5-moveout-lists)
6. [ICA Delivery](#6-ica-delivery)
7. [User Management](#7-user-management)
8. [Organization Management](#8-organization-management)
9. [Analytics](#9-analytics)
10. [Reports](#10-reports)
11. [Activity Logs](#11-activity-logs)
12. [Notifications](#12-notifications)
13. [Settings](#13-settings)
14. [Messaging/Chat](#14-messagingchat)
15. [Real-Time Features](#15-real-time-features)
16. [Data Models](#16-data-models)
17. [API Reference](#17-api-reference)
18. [UI/UX Guidelines](#18-uiux-guidelines)
19. [Recommended iOS Framework](#19-recommended-ios-framework)

---

## 1. User Roles & Permissions

### Role Hierarchy

| Role | Code | Level |
|------|------|-------|
| Admin | `admin` | 4 - Full system access |
| Manager | `manager` | 3 - Full branch operations |
| Assistant Manager | `assistant_manager` | 2 - Most branch operations |
| Staff | `staff` | 1 - Limited front-line access |

### Access Matrix

```
Feature                  | Admin | Manager | Asst.Mgr | Staff
-------------------------|-------|---------|----------|------
Dashboard                |   ✅   |    ✅    |    ✅     |  ✅
Manage Items             |   ✅   |    ✅    |    ✅     |  ❌
Stock Out                |   ✅   |    ✅    |    ✅     |  ✅
Stock In (Add Stock)     |   ✅   |    ✅    |    ✅     |  ❌
Record Stock In (Submit) |   ❌   |    ❌    |    ❌     |  ✅
Moveout Lists            |   ✅   |    ✅    |    ✅     |  ❌
ICA Delivery             |   ✅   |    ✅    |    ✅     |  ❌
Manage Staff             |   ✅   |    ✅    |    ✅     |  ❌
Branch Management        |   ✅   |    ❌    |    ❌     |  ❌
District Management      |   ✅   |    ❌    |    ❌     |  ❌
Region Management        |   ✅   |    ❌    |    ❌     |  ❌
Analytics                |   ✅   |    ✅    |    ✅     |  ❌
Reports                  |   ✅   |    ✅    |    ✅     |  ❌
Activity Logs            |   ✅   |    ✅    |    ❌     |  ❌
Calendar Events (Add)    |   ✅   |    ✅    |    ✅     |  ❌
Calendar Events (View)   |   ✅   |    ✅    |    ✅     |  ✅
Notifications            |   ✅   |    ✅    |    ✅     |  ✅
Settings                 |   ✅   |    ✅    |    ✅     |  ✅
Messaging                |   ✅   |    ✅    |    ✅     |  ✅
```

---

## 2. Authentication Flow

### Sign In Screen
**Requirements:**
- Email input field (keyboard type: email)
- Password input field (secure text entry)
- "Remember Me" toggle
- "Sign In" button
- "Create Account" link
- "Forgot Password" link
- Show/hide password toggle

**Validation:**
- Email must be valid format
- Password minimum 6 characters
- Show inline error messages

**On Success:**
- Store auth token in Keychain
- Store user profile in UserDefaults
- Navigate to Dashboard

### Sign Up Screen
**Requirements:**
- Full Name input
- Email input
- Password input
- Confirm Password input
- Terms acceptance checkbox
- "Create Account" button

**Validation:**
- Passwords must match
- Email must be unique
- All fields required

**Post-Registration:**
- New users go to "Pending Access" state
- Display message: "Your account is pending approval"
- Manager/Admin must activate account

### Pending Access Screen
**Requirements:**
- Show pending status message
- "Check Status" button to refresh
- Auto-check every 30 seconds
- Once approved, auto-navigate to Dashboard

### Splash Screen
**Requirements:**
- App logo centered
- Auto-check stored auth token
- If valid token: Navigate to Dashboard
- If no token: Navigate to Sign In

---

## 3. Dashboard Features

### Overview Layout
**Components:**
1. **Stats Cards Row** (horizontal scroll)
   - Total Items card (blue)
   - Below Threshold card (orange) 
   - Low Stock card (yellow)
   - Critical Stock card (red)

2. **Weather Widget**
   - Current temperature
   - Weather condition icon
   - City name (branch location)

3. **Calendar Events Section**
   - Today's events highlighted
   - "View All" button for full calendar
   - Upcoming 7 days preview

4. **Moveout Lists Section** (Manager/Asst.Mgr only)
   - Active moveout lists with progress bars
   - "View History" toggle
   - "Generate New" button

5. **Quick Actions Grid**
   - Stock Out shortcut
   - Stock In shortcut (role-dependent)
   - Reports shortcut
   - Settings shortcut

### Stats Card Detail Modal
**When tapping any stats card:**
- Show full-screen modal
- List all items in that category
- Item name, current quantity, threshold
- Search functionality
- Sort by name/quantity

### Calendar Events
**Event Types:**
- `reorder` - Items to reorder
- `delivery` - Expected deliveries
- `alert` - Stock alerts
- `expiry` - Expiring items
- `usage_spike` - High usage periods

**Add Event Dialog (Manager/Asst.Mgr):**
- Title input
- Event type dropdown
- Date picker
- Description (optional)
- "Save" button

### Moveout Lists Section
**Active Lists:**
- List title
- Progress bar (completed/total items)
- Items remaining count
- Tap to expand and see items

**Processing Moveout Items:**
- Show item name, requested quantity
- Input field for completed quantity
- "Mark Complete" button
- Records completion timestamp and user

### Pull-to-Refresh
- All dashboard data refreshes
- Weather updates
- Calendar events reload
- Moveout lists refresh

---

## 4. Inventory Management

### 4.1 Manage Items (`/items`)
**Access:** Manager, Assistant Manager only

**List View:**
- Searchable list of all items
- Group by category with section headers
- Each item shows: name, category, current stock, threshold
- Color indicator for stock level

**Add Item Form:**
```
Required Fields:
- Name (text)
- Category (dropdown: Fruits, Vegetables, Dairy, Meat, etc.)
- Unit of Measure (dropdown: pieces, kg, liters, cases, etc.)
- Threshold Level (number)
- Low Level (number)
- Critical Level (number)

Optional Fields:
- Description (text)
- SKU (text)
- Barcode (text with scanner option)
- Image URL (text or camera/gallery picker)
- Storage Temperature (text)
- Base Unit (text)
- Packaging Unit (text)
- Units Per Package (number)
```

**Edit Item:**
- Pre-fill all fields
- Same validation as add
- Show delete button

**Delete Item:**
- Confirmation dialog
- Soft delete (mark as inactive)

### 4.2 Stock Out (`/stock`)
**Access:** All roles

**View Layout:**
- Category tabs at top (horizontal scroll)
- Search bar (per-category or global toggle)
- Summary statistics row:
  - Total items count
  - Low stock count
  - Critical count

**Stock Item Card:**
```
┌─────────────────────────────────┐
│ [Image] Item Name               │
│         Category • Unit         │
│         Current: 25 / Thresh: 30│
│         ████████░░ 83%          │
│                      [ - ]      │
└─────────────────────────────────┘
```

**Stock Out Dialog:**
- Item name (read-only)
- Current quantity (read-only)
- Unit type selector (base/package)
- Quantity to remove (number input)
- Reason/notes (optional)
- "Remove Stock" button

**Validation:**
- Cannot remove more than available
- Quantity must be positive
- Show warning if going below threshold

### 4.3 Stock In (`/stock_in`)
**Access:** Manager, Assistant Manager only

**Split View Layout:**
- Left panel: Receipts list
- Right panel: Document viewer

**Receipts List:**
- Submitted receipt cards
- Each shows: supplier, date, submitter, status
- Tap to load document in viewer

**Document Viewer:**
- Full receipt image/PDF
- Pinch-to-zoom
- Pan gesture support
- "Close" button

**Stock List View:**
- Same as Stock Out layout
- Plus button instead of minus
- Filter chips: All, Low, Critical

**Add Stock Dialog:**
- Same structure as Stock Out
- But adds to quantity instead

### 4.4 Record Stock In (`/record_stock_in`)
**Access:** Staff only

**Submit Receipt Form:**
```
Required:
- Supplier (dropdown)
  Options: Gronsakshuset, Kvalitetsfisk, Spendrups, Tingstad, Other

Optional:
- Remarks (text area)
- Receipt Document (image/file picker)
```

**My Receipts List:**
- Show all receipts I've submitted
- Status indicator (pending/processed)
- Date submitted
- Supplier name

---

## 5. Moveout Lists

**Access:** Manager, Assistant Manager only

### List View
- Active lists tab
- Completed lists tab
- Generate new button (FAB)

### Moveout List Card
```
┌────────────────────────────────────┐
│ Moveout List - Nov 30, 2025        │
│ Branch: ICA Maxi Örnsköldsvik      │
│ ████████████░░░░ 8/12 items        │
│ Created by: John Manager           │
│                         [Expand ▼] │
└────────────────────────────────────┘
```

### Expanded View (Item List)
- Item name
- Category
- Requested quantity
- Available quantity
- Status: Pending / Completed ✓
- Completed by (if done)

### Process Item Dialog
- Item name (read-only)
- Requested: X units
- Available: Y units
- Actual completed (number input, default = requested)
- "Mark Complete" button

### Generate Moveout Dialog
**Step 1: Filter Stock**
- Category filter
- Stock level filter (all/low/critical)
- Search by name

**Step 2: Select Items**
- Checkbox list of items
- Show current quantity
- Input requested quantity per item

**Step 3: Confirm**
- Review selected items
- Total items count
- "Generate List" button

**Notifications:**
- FCM sent to all staff when list generated
- Shows creator name and date
- If created after 10 AM Swedish time, schedules morning reminder for 10 AM next day

---

## 6. ICA Delivery

**Access:** Manager, Assistant Manager only

### List View
- Date range filter (start/end date pickers)
- List of delivery records
- Sorted by date descending

### Delivery Record Card
```
┌────────────────────────────────────┐
│ Nov 30, 2025                       │
│ Type: Salmon and Rolls             │
│ Time: Lunch                        │
│ Quantity: 15                       │
│              [Edit] [Delete]       │
└────────────────────────────────────┘
```

### Add/Edit Delivery Dialog
```
Fields:
- Date (date picker)
- Delivery Type (dropdown):
  - Salmon and Rolls
  - Combo
  - Salmon and Avocado Rolls
  - Vegan Combo
  - Goma Wakame
  
- Time Period (dropdown):
  - Lunch
  - Dinner
  
- Quantity (number input)
```

---

## 7. User Management

**Access:** Admin, Manager, Assistant Manager

### Staff List View
**Filters:**
- Region dropdown (Admin only)
- District dropdown
- Branch dropdown
- Role filter (all/staff/assistant_manager/manager)
- Search by name/email

**Staff Card:**
```
┌────────────────────────────────────┐
│ [Photo] John Smith                 │
│         Staff • Position           │
│         john@example.com           │
│         Branch: ICA Maxi           │
│         Status: ● Active           │
│                    [Edit] [Delete] │
└────────────────────────────────────┘
```

### Add Staff Form
```
Required:
- Full Name (text)
- Email (email keyboard)
- Role (dropdown)
- Branch (dropdown)

Optional:
- Phone (phone keyboard)
- Position (text)
- Photo (camera/gallery)
```

### Edit Staff
- Same fields as add
- Cannot edit email
- Show activate button for pending accounts

### Activate Staff
- For pending accounts
- Confirmation dialog
- Sends notification to user

---

## 8. Organization Management

**Access:** Admin only

### Branch Management
**List View:**
- All branches grouped by region/district
- Search functionality
- Add branch button

**Branch Card:**
- Branch name
- Address
- Phone, Email
- Manager name
- District/Region info

**Add/Edit Branch:**
- Name, Address, Phone, Email
- Select District (auto-selects Region)
- Assign Manager (dropdown of managers)

### District Management
**List View:**
- Districts grouped by region
- Branch count per district

**Add/Edit District:**
- District name
- Select Region

### Region Management
**List View:**
- All regions
- District count per region

**Add/Edit Region:**
- Region name only

---

## 9. Analytics

**Access:** Manager, Assistant Manager only

### Key Metrics Cards
- Total Items (with trend arrow)
- Low Stock Items (count)
- Active Users (with trend)
- Stock Movements (24h count)

### Charts

**1. Category Distribution (Pie Chart)**
- Interactive pie chart
- Shows item count per category
- Tap slice for details modal

**2. Stock Movements (Bar Chart)**
- Stock In (green bars)
- Stock Out (red bars)
- Time period selector: Daily/Weekly/Monthly

**3. Top Items (Horizontal Bar)**
- Top 10 most moved items
- Shows total movements

**4. Usage Trends (Line Chart)**
- Item-specific usage over time
- Item selector dropdown
- 7-day, 30-day, 90-day periods

### Category Breakdown List
- Table view of categories
- Columns: Category, Item Count, Low Stock, Critical

---

## 10. Reports

**Access:** Manager, Assistant Manager only

### Report Types (Tab Bar)
1. **Stock Levels**
2. **Stock Movements**
3. **Soft Drinks Weekly**

### Stock Levels Report
- Current stock for all items
- Columns: Item, Category, Current, Threshold, Status
- Color-coded status cells
- Export to PDF button

### Stock Movements Report
- Month filter dropdown
- Shows all stock in/out transactions
- Columns: Date, Item, Type (In/Out), Quantity, User
- Summary totals at bottom

### Soft Drinks Weekly Report
**Filters:**
- Week period selector (2/4/8/12 weeks)

**Content:**
- Soft drink items only
- Weekly usage trends
- Line chart visualization
- Data table below chart

### Export/Share
- Generate PDF button
- Share sheet integration
- Email, AirDrop, Save to Files

---

## 11. Activity Logs

**Access:** Admin, Manager only

### Filter Options
- Activity Type: All / Stock / General
- Date range filter

### Activity Log Entry
```
┌────────────────────────────────────┐
│ Stock Out - Coca Cola              │
│ Quantity: 5 pieces                 │
│ By: John Staff                     │
│ Nov 30, 2025 at 14:32             │
└────────────────────────────────────┘
```

### Activity Types Tracked
- `user_login` - User signed in
- `user_logout` - User signed out
- `item_created` - New item added
- `item_updated` - Item modified
- `item_deleted` - Item removed
- `stock_in` - Stock added
- `stock_out` - Stock removed
- `stock_initialized` - New stock record
- `staff_created` - Staff added
- `staff_updated` - Staff modified
- `staff_deleted` - Staff removed
- `profile_updated` - Profile changed

---

## 12. Notifications

### Notification Types
- `stock_alert` - Low/critical stock warnings
- `event_reminder` - Calendar event reminders
- `moveout` - Moveout list notifications
- `new_message` - Chat messages
- `general` - System notifications

### Notifications Screen
**Layout with Accordions:**
```
┌────────────────────────────────────┐
│ ▼ Unread Notifications (5)        │
├────────────────────────────────────┤
│   [Notification cards...]          │
├────────────────────────────────────┤
│ ▶ Read Notifications (23)         │
└────────────────────────────────────┘
```

**Notification Card:**
```
┌────────────────────────────────────┐
│ [Icon] Stock Alert                 │
│ Coca Cola is below threshold       │
│ Current: 5, Threshold: 20          │
│ 2 hours ago                   [✓]  │
└────────────────────────────────────┘
```

### Notification Dropdown (Top Bar)
- Bell icon with badge count
- Tap opens dropdown panel
- Shows 5 most recent unread
- "View All" link to full screen

### Mark as Read
- Tap notification card to dismiss
- "Mark All as Read" button in toolbar
- Swipe to dismiss individual

### HTML Stripping
- Remove HTML tags from notification messages
- Display plain text only

---

## 13. Settings

**Access:** All roles

### Profile Section
- Profile photo (tap to change)
- Name (editable)
- Email (read-only)
- Phone (editable)
- Position (editable)
- Role (read-only)
- Branch (read-only)

### Photo Upload
- Camera option
- Photo library option
- Crop to square
- Upload to server
- Show loading indicator

### Notification Preferences
**Toggle switches for:**
- Email Notifications
- SMS Notifications
- WhatsApp Notifications
- Stock Level Alerts
- Event Reminders
- Soft Drink Trends

### Notification Scheduling
**For each notification type:**
```
Frequency: [Daily ▼]

If Daily:
  Time: [10:00 AM ▼]

If Weekly:
  Day: [Monday ▼]
  Time: [10:00 AM ▼]

If Monthly:
  Date: [1st ▼]
  Time: [10:00 AM ▼]
```

### Account Actions
- Change Password button
- Sign Out button

---

## 14. Messaging/Chat

### Inbox Screen
**Thread List:**
```
┌────────────────────────────────────┐
│ [●Photo] John Manager              │
│          Hey, can you check the... │
│          2:30 PM              (3)  │
└────────────────────────────────────┘
```
- Green dot = online
- Badge = unread count
- Tap photo to view full size

**Compose Button (FAB):**
- Opens user picker dialog
- Search all staff
- Select user → opens chat

### Chat Screen
**Layout:**
- Top bar: User name, online status, back button
- Message list (scrollable)
- Input area at bottom

**Message Bubble:**
```
Sent (right, blue):
                    ┌─────────────────┐
                    │ Hello! How are  │
                    │ you today?      │
                    │        2:30 PM ✓✓│
                    └─────────────────┘

Received (left, gray):
┌─────────────────┐
│ I'm good,       │
│ thanks!         │
│ 2:31 PM         │
└─────────────────┘
```

**Message Status Icons:**
- ✓ Single tick = Sent
- ✓✓ Double tick (gray) = Delivered
- ✓✓ Double tick (blue) = Read

**Typing Indicator:**
- Shows "John is typing..." at bottom
- Animated dots

**Auto-scroll:**
- Scroll to bottom on new message
- Unless user has scrolled up

### User Picker Dialog
- Search input at top
- List of all staff (excluding self)
- User card: photo, name, role
- Tap to start conversation

---

## 15. Real-Time Features

### Socket.IO Events

**Connection:**
```swift
// Connect with auth token
socket.connect(auth: ["token": authToken])

// Join branch room
socket.emit("join-branch", ["branchId": branchId])

// Join personal room  
socket.emit("join-room", ["userId": userId])

// Register FCM token
socket.emit("registerDevice", ["userId": userId, "fcmToken": token, "platform": "ios"])
```

**Notifications:**
```swift
// Listen for notification updates
socket.on("notification-update") { data in
    // Update notification badge
    // Show local notification if app in background
}
```

**Messaging:**
```swift
// Send message
socket.emit("new_message", [
    "senderId": senderId,
    "receiverId": receiverId,
    "content": content
])

// Receive message
socket.on("new_message") { data in
    // Add to messages list
    // Update UI
}

// Typing events
socket.emit("user_typing", ["receiverId": receiverId])
socket.emit("user_stop_typing", ["receiverId": receiverId])

socket.on("user_typing") { data in
    // Show typing indicator
}

// Message status
socket.emit("messageDelivered", ["messageId": id])
socket.emit("messagesRead", ["senderId": senderId])

socket.on("messageDelivered") { data in
    // Update tick marks
}
socket.on("messagesRead") { data in
    // Update to blue ticks
}
```

**Online Status:**
```swift
// Get online members
socket.emit("get-online-members")

socket.on("online-members") { data in
    // Update online indicators
}

socket.on("user-online") { data in
    // Show online dot
}

socket.on("user-offline") { data in
    // Remove online dot
}
```

### Push Notifications (APNs)

**Register for push:**
```swift
// Request permission
UNUserNotificationCenter.requestAuthorization(options: [.alert, .badge, .sound])

// Get APNs token
func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    // Send to server
}
```

**Handle notification:**
```swift
// Foreground
func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) {
    // Show banner if not from current conversation
}

// Background tap
func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) {
    // Navigate to relevant screen
}
```

**Notification Payload Structure:**
```json
{
    "aps": {
        "alert": {
            "title": "Moveout Request",
            "body": "Item × Quantity"
        },
        "badge": 5,
        "sound": "default"
    },
    "data": {
        "type": "moveout",
        "notificationId": "uuid",
        "senderId": "uuid"
    }
}
```

**Notification Suppression:**
- Don't show if sender is current user
- Don't show if chat with sender is open
- Don't show duplicates (check notificationId)

---

## 16. Data Models

### User/Profile
```swift
struct User: Codable {
    let id: String
    let email: String
    let name: String
    let phone: String?
    let photoUrl: String?
    let role: UserRole
    let position: String?
    let branchId: String?
    let branchName: String?
    let districtName: String?
    let regionName: String?
    let createdAt: String
    let updatedAt: String?
    let accessCount: Int?
    
    // Notification settings
    let emailNotifications: Bool?
    let smsNotifications: Bool?
    let whatsappNotifications: Bool?
    let stockAlerts: Bool?
    let eventReminders: Bool?
    let softDrinkTrends: Bool?
}

enum UserRole: String, Codable {
    case admin, manager, assistant_manager, staff
}
```

### Item
```swift
struct Item: Codable {
    let id: String
    let name: String
    let category: String
    let description: String?
    let sku: String?
    let barcode: String?
    let unitOfMeasure: String
    let thresholdLevel: Int
    let lowLevel: Int
    let criticalLevel: Int
    let imageUrl: String?
    let storageTemperature: String?
    let baseUnit: String?
    let packagingUnit: String?
    let unitsPerPackage: Int?
    let isActive: Bool
    let createdAt: String
}
```

### Stock
```swift
struct StockItem: Codable {
    let id: String
    let itemId: String
    let branchId: String
    let currentQuantity: Int
    let reservedQuantity: Int?
    let availableQuantity: Int?
    let item: Item?
}
```

### MoveoutList
```swift
struct MoveoutList: Codable {
    let id: String
    let title: String
    let description: String?
    let branchId: String
    let status: MoveoutStatus
    let createdBy: String
    let createdByName: String?
    let completedBy: String?
    let completedAt: String?
    let items: [MoveoutItem]
    let createdAt: String
}

struct MoveoutItem: Codable {
    let id: String
    let itemId: String
    let itemName: String
    let category: String?
    let availableQuantity: Int
    let requestingQuantity: Int
    let isCompleted: Bool
    let completedAt: String?
    let completedBy: String?
    let completedByName: String?
}

enum MoveoutStatus: String, Codable {
    case draft, active, completed
}
```

### CalendarEvent
```swift
struct CalendarEvent: Codable {
    let id: String
    let title: String
    let description: String?
    let eventDate: String
    let eventType: EventType
    let branchId: String
    let createdAt: String
}

enum EventType: String, Codable {
    case reorder, delivery, alert, expiry, usage_spike
}
```

### Message
```swift
struct Message: Codable, Identifiable {
    let id: String
    let senderId: String
    let receiverId: String
    let content: String
    let sentAt: Date
    let deliveredAt: Date?
    let readAt: Date?
    let fcmMessageId: String?
}
```

### Thread
```swift
struct Thread: Codable, Identifiable {
    let id: String
    let user1Id: String
    let user2Id: String
    let lastMessageId: String?
    let updatedAt: String
    let displayName: String?
    let displayPhoto: String?
}
```

### Notification
```swift
struct Notification: Codable, Identifiable {
    let id: String
    let userId: String
    let type: String
    let message: String
    let isRead: Bool
    let createdAt: String
}
```

### ICADelivery
```swift
struct ICADelivery: Codable, Identifiable {
    let id: String
    let date: String
    let deliveryType: DeliveryType
    let timePeriod: TimePeriod
    let quantity: Int
    let branchId: String
    let createdAt: String
}

enum DeliveryType: String, Codable, CaseIterable {
    case salmonAndRolls = "Salmon and Rolls"
    case combo = "Combo"
    case salmonAndAvocadoRolls = "Salmon and Avocado Rolls"
    case veganCombo = "Vegan Combo"
    case gomaWakame = "Goma Wakame"
}

enum TimePeriod: String, Codable, CaseIterable {
    case lunch = "Lunch"
    case dinner = "Dinner"
}
```

---

## 17. API Reference

### Base URL
```
Production: https://your-supabase-project.supabase.co
```

### Authentication Headers
```
Authorization: Bearer <access_token>
apikey: <supabase_anon_key>
Content-Type: application/json
```

### Endpoints

#### Authentication
```
POST /auth/v1/token?grant_type=password
Body: { "email": "", "password": "" }

POST /auth/v1/signup
Body: { "email": "", "password": "", "data": { "name": "" } }

POST /auth/v1/recover
Body: { "email": "" }
```

#### Items
```
GET /rest/v1/items?select=*
POST /rest/v1/items
PATCH /rest/v1/items?id=eq.<id>
DELETE /rest/v1/items?id=eq.<id>
```

#### Stock
```
GET /rest/v1/stock?select=*,items(*)&branch_id=eq.<branchId>
POST /rest/v1/rpc/update_stock
Body: { "p_item_id": "", "p_branch_id": "", "p_quantity_change": 0, "p_type": "in|out" }
```

#### Staff
```
GET /rest/v1/users?select=*
POST /rest/v1/users
PATCH /rest/v1/users?id=eq.<id>
DELETE /rest/v1/users?id=eq.<id>
POST /rest/v1/rpc/activate_user
Body: { "user_id": "" }
```

#### Moveout Lists
```
GET /rest/v1/moveout_lists?select=*,moveout_items(*)
POST /rest/v1/moveout_lists
POST /rest/v1/rpc/complete_moveout_item
Body: { "p_item_id": "", "p_completed_quantity": 0, "p_user_id": "" }
```

#### Calendar Events
```
GET /rest/v1/calendar_events?branch_id=eq.<branchId>
POST /rest/v1/calendar_events
DELETE /rest/v1/calendar_events?id=eq.<id>
```

#### Notifications
```
GET /rest/v1/notifications?user_id=eq.<userId>&order=created_at.desc
PATCH /rest/v1/notifications?id=eq.<id>
Body: { "is_read": true }
POST /rest/v1/rpc/mark_all_notifications_read
Body: { "p_user_id": "" }
```

#### Messages
```
GET /rest/v1/messages?or=(sender_id.eq.<userId>,receiver_id.eq.<userId>)
POST /rest/v1/messages
```

#### ICA Delivery
```
GET /rest/v1/ica_delivery?branch_id=eq.<branchId>&order=date.desc
POST /rest/v1/ica_delivery
PATCH /rest/v1/ica_delivery?id=eq.<id>
DELETE /rest/v1/ica_delivery?id=eq.<id>
```

#### Activity Logs
```
GET /rest/v1/activity_logs?order=created_at.desc&limit=100
```

#### Analytics
```
GET /rest/v1/rpc/get_dashboard_stats
Body: { "p_branch_id": "" }

GET /rest/v1/rpc/get_stock_movements
Body: { "p_branch_id": "", "p_start_date": "", "p_end_date": "" }
```

---

## 18. UI/UX Guidelines

### Color Palette
```swift
extension Color {
    static let stockNexusRed = Color(hex: "#E6002A")
    static let stockNexusDark = Color(hex: "#1C1C1E")
    static let stockNexusBackground = Color(hex: "#000000")
    static let cardBackground = Color(hex: "#2D2D30")
    
    // Stock levels
    static let stockNormal = Color.green
    static let stockLow = Color.orange
    static let stockCritical = Color.red
    static let stockBelowThreshold = Color.yellow
}
```

### Typography
- Use system fonts (SF Pro)
- Large titles for screen headers
- Body text for content
- Caption for timestamps and metadata

### Common Components

**Search Bar:**
- Rounded rectangle style
- Magnifying glass icon
- Clear button when text present

**Pull-to-Refresh:**
- Native UIRefreshControl
- On all list views

**Empty States:**
- Centered icon
- Descriptive text
- Action button where applicable

**Loading States:**
- ProgressView (spinner)
- Skeleton loaders for lists

**Error States:**
- Alert dialog for errors
- Retry button
- Clear error message

### Navigation
- Tab bar NOT used (use sidebar)
- Navigation stack for drill-down
- Sheet presentation for modals
- Full screen cover for forms

### Sidebar Menu
```
┌────────────────────────────┐
│ [Logo] Stock Nexus         │
│                            │
│ [Photo] User Name          │
│ Role • Branch              │
├────────────────────────────┤
│ 🏠 Dashboard               │
│ 📦 Stock Out               │
│ 📥 Stock In                │
│ 📋 Moveout Lists           │
│ 🚛 ICA Delivery            │
│ 👥 Staff                   │
│ 📊 Analytics               │
│ 📑 Reports                 │
│ 📜 Activity Logs           │
├────────────────────────────┤
│ ⚙️ Settings                │
│ 🚪 Logout                  │
└────────────────────────────┘
```

---

## 19. Recommended iOS Framework

### Primary Recommendation: **SwiftUI + Swift**

**Why SwiftUI:**
1. **Modern & Declarative** - Similar paradigm to Jetpack Compose
2. **Native Performance** - Optimized for iOS
3. **Less Code** - Faster development
4. **Built-in Animations** - Matches Material animations
5. **State Management** - @State, @Binding, @ObservedObject
6. **Easy Navigation** - NavigationStack, NavigationLink

### Architecture: **MVVM + Repository Pattern**

```
├── App/
│   ├── StockNexusApp.swift
│   └── ContentView.swift
├── Models/
│   ├── User.swift
│   ├── Item.swift
│   ├── Stock.swift
│   └── ...
├── ViewModels/
│   ├── AuthViewModel.swift
│   ├── DashboardViewModel.swift
│   ├── InventoryViewModel.swift
│   └── ...
├── Views/
│   ├── Auth/
│   ├── Dashboard/
│   ├── Inventory/
│   ├── Moveout/
│   └── ...
├── Repositories/
│   ├── AuthRepository.swift
│   ├── InventoryRepository.swift
│   └── ...
├── Services/
│   ├── APIClient.swift
│   ├── SocketService.swift
│   ├── NotificationService.swift
│   └── ...
└── Utilities/
    ├── Extensions/
    ├── Helpers/
    └── Constants.swift
```

### Key Dependencies

```swift
// Package.swift or SPM
dependencies: [
    // Supabase SDK
    .package(url: "https://github.com/supabase/supabase-swift", from: "2.0.0"),
    
    // Socket.IO
    .package(url: "https://github.com/socketio/socket.io-client-swift", from: "16.0.0"),
    
    // Image Loading
    .package(url: "https://github.com/kean/Nuke", from: "12.0.0"),
    
    // Charts
    .package(url: "https://github.com/danielgindi/Charts", from: "5.0.0"),
    
    // Keychain
    .package(url: "https://github.com/kishikawakatsumi/KeychainAccess", from: "4.2.0"),
]
```

### Alternative: **UIKit + Swift** (if complex custom UI needed)
- More control over animations
- Better for complex gesture handling
- Can mix with SwiftUI using UIHostingController

### NOT Recommended:
- **React Native** - Performance overhead, harder to match native feel
- **Flutter** - Different design language, larger app size
- **Objective-C** - Legacy, harder maintenance

---

## Summary

This guide covers all functionality needed to build an iOS version of Stock Nexus that matches the Android app exactly. Key points:

1. **Use SwiftUI** for fastest development with native feel
2. **Follow MVVM pattern** for clean architecture
3. **Implement Socket.IO** for real-time features
4. **Use APNs** for push notifications (not FCM on iOS)
5. **Match color palette** exactly (#E6002A primary)
6. **Maintain role-based access** throughout
7. **Support offline caching** for user profile and messages

The iOS app should feel native while maintaining feature parity with Android.
