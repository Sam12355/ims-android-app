# Backend Integration Guide for FCM Push Notifications

## Overview

The Stock Nexus Android app now supports Firebase Cloud Messaging (FCM) for instant push notifications. This eliminates the 1-minute polling delay and provides battery-efficient, real-time notifications.

## What Changed?

**Before (Polling):**

1. Mobile app polls `/api/notifications` every 60 seconds
2. Up to 60-second delay
3. Battery drain from constant polling
4. Notifications stored in database

**After (FCM):**

1. Backend sends push notification directly to device
2. Instant delivery (0-2 seconds)
3. Battery efficient
4. No polling needed (kept as fallback)

## Backend Requirements

### 1. Get Firebase Server Key

From Firebase Console (Project Settings → Cloud Messaging):

- **Server Key** (also called "Legacy Server Key")
- **Sender ID**

Store these securely in your environment variables:

```env
FCM_SERVER_KEY=your_server_key_here
FCM_SENDER_ID=your_sender_id_here
```

### 2. Install Firebase Admin SDK

**Node.js:**

```bash
npm install firebase-admin
```

**Python:**

```bash
pip install firebase-admin
```

### 3. Store FCM Tokens

Add a new column to your `users` table:

```sql
ALTER TABLE users ADD COLUMN fcm_token TEXT;
```

Or create a separate table for device tokens:

```sql
CREATE TABLE fcm_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    device_info TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
```

### 4. Create API Endpoint to Receive FCM Tokens

**POST /api/fcm/token**

Request body:

```json
{
  "token": "fcm_token_from_mobile_app",
  "device_info": "Samsung SM-S936B" // optional
}
```

Response:

```json
{
  "success": true,
  "message": "FCM token registered"
}
```

Example implementation (Node.js):

```javascript
app.post("/api/fcm/token", authenticateUser, async (req, res) => {
  const { token, device_info } = req.body;
  const user_id = req.user.id;

  await db.query(
    "INSERT INTO fcm_tokens (user_id, token, device_info) VALUES ($1, $2, $3) ON CONFLICT (token) DO UPDATE SET updated_at = NOW()",
    [user_id, token, device_info]
  );

  res.json({ success: true, message: "FCM token registered" });
});
```

### 5. Send Push Notifications

When creating stock alerts or event reminders, send FCM push instead of (or in addition to) database records.

**Node.js Example:**

```javascript
const admin = require("firebase-admin");

// Initialize Firebase Admin (do once at startup)
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n"),
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
  }),
});

// Function to send notification
async function sendStockAlert(userId, itemName, currentQty, threshold) {
  // Get user's FCM token
  const result = await db.query("SELECT fcm_token FROM users WHERE id = $1", [
    userId,
  ]);
  const fcmToken = result.rows[0]?.fcm_token;

  if (!fcmToken) {
    console.log("No FCM token for user", userId);
    return;
  }

  // Create notification
  const message = {
    token: fcmToken,
    notification: {
      title: "Low Stock Alert",
      body: `${itemName} is running low. Current: ${currentQty}, Threshold: ${threshold}`,
    },
    data: {
      type: "stock_alert",
      notification_id: "some_unique_id",
      item_name: itemName,
      current_qty: currentQty.toString(),
      threshold: threshold.toString(),
    },
    android: {
      priority: "high",
      notification: {
        sound: "default",
        channelId: "stock_nexus_notifications",
      },
    },
  };

  try {
    const response = await admin.messaging().send(message);
    console.log("✅ Notification sent:", response);
  } catch (error) {
    console.error("❌ Error sending notification:", error);
  }
}
```

**Python Example:**

```python
import firebase_admin
from firebase_admin import credentials, messaging

# Initialize Firebase Admin (do once at startup)
cred = credentials.Certificate('path/to/serviceAccountKey.json')
firebase_admin.initialize_app(cred)

# Function to send notification
def send_stock_alert(user_id, item_name, current_qty, threshold):
    # Get user's FCM token from database
    fcm_token = get_user_fcm_token(user_id)

    if not fcm_token:
        print(f'No FCM token for user {user_id}')
        return

    # Create notification
    message = messaging.Message(
        notification=messaging.Notification(
            title='Low Stock Alert',
            body=f'{item_name} is running low. Current: {current_qty}, Threshold: {threshold}'
        ),
        data={
            'type': 'stock_alert',
            'notification_id': 'some_unique_id',
            'item_name': item_name,
            'current_qty': str(current_qty),
            'threshold': str(threshold)
        },
        android=messaging.AndroidConfig(
            priority='high',
            notification=messaging.AndroidNotification(
                sound='default',
                channel_id='stock_nexus_notifications'
            )
        ),
        token=fcm_token
    )

    try:
        response = messaging.send(message)
        print(f'✅ Notification sent: {response}')
    except Exception as error:
        print(f'❌ Error sending notification: {error}')
```

### 6. Update Existing Notification Logic

Find where you currently create notifications in the database and add FCM sending:

```javascript
// Example: When stock goes below threshold
async function recordStockOut(itemId, quantity, userId) {
  // ... existing logic to update stock ...

  // Check if below threshold
  if (newQuantity < item.threshold) {
    // Option 1: Send FCM AND create database record
    await sendStockAlert(userId, item.name, newQuantity, item.threshold);
    await createNotificationRecord(userId, "stock_alert", message);

    // Option 2: Send FCM ONLY (recommended for instant notifications)
    await sendStockAlert(userId, item.name, newQuantity, item.threshold);
  }
}
```

## Testing

1. Run the mobile app and check logs for FCM token:

   ```
   🔑 FCM Token: eXaMpLe_ToKeN_123...
   ```

2. Copy this token

3. Use Firebase Console to test:

   - Go to Cloud Messaging → "Send test message"
   - Paste the FCM token
   - Send notification
   - Should appear on device instantly!

4. Test from your backend:
   - Call your notification sending function
   - Should receive notification in 1-2 seconds

## Notification Payload Format

For consistency with the mobile app, use this structure:

**notification** (appears in status bar):

- `title`: Main heading
- `body`: Message text

**data** (custom payload):

- `type`: "stock_alert" | "event" | "general"
- `notification_id`: Unique ID (for Read button)
- Additional fields as needed

## Benefits

✅ **Instant delivery**: 0-2 seconds vs 60 seconds
✅ **Battery efficient**: No polling needed
✅ **Reliable**: Works even if app is closed
✅ **Free**: Unlimited messages
✅ **Scalable**: Handles millions of devices

## Fallback Strategy

The mobile app still polls every 60 seconds as a fallback. Once FCM is working reliably, polling can be disabled.

## Need Help?

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Send messages to multiple devices](https://firebase.google.com/docs/cloud-messaging/send-message)
- [Node.js Admin SDK](https://firebase.google.com/docs/admin/setup)
