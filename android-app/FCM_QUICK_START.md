# 🚀 FCM Quick Start Guide

FCM (Firebase Cloud Messaging) has been integrated into the Stock Nexus Android app! Follow these simple steps to enable instant push notifications.

## 📋 Prerequisites

- Google account
- 10 minutes of your time

## 🔧 Setup Steps

### Step 1: Create Firebase Project (5 minutes)

1. Go to https://console.firebase.google.com/
2. Click **"Add project"**
3. Project name: **Stock Nexus**
4. Click **"Create project"**

### Step 2: Add Android App (3 minutes)

1. Click the **Android icon** 
2. Android package name: **`com.stocknexus.android`** ⚠️ MUST match exactly!
3. Click **"Register app"**
4. **Download `google-services.json`**
5. Place it in: `android-app/app/google-services.json` (replace the placeholder file)

### Step 3: Get Server Key (2 minutes)

1. In Firebase Console → **Project Settings** (gear icon)
2. Go to **"Cloud Messaging"** tab
3. Copy the **"Server key"** (under Legacy)
4. Send this to your backend team

### Step 4: Build & Test

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Check logs for FCM token:
```bash
adb logcat | grep FCM
```

You should see:
```
🔑 FCM Token: eXaMpLe_ToKeN_hErE...
```

## 🎉 That's It!

Your mobile app is now FCM-ready!

## 📤 Backend Integration

Send the **Server Key** to your backend team along with `BACKEND_FCM_INTEGRATION.md` which contains:
- How to send push notifications
- API endpoints needed
- Code examples (Node.js & Python)

## ✅ Benefits

- **Instant notifications** (0-2 seconds vs 60 seconds)
- **Battery efficient** (no polling)
- **Free forever** (unlimited messages)
- **Works when app is closed**

## 🐛 Troubleshooting

**Build fails with "google-services.json not found":**
- Make sure file is in `app/` folder (same level as `app/build.gradle`)
- File name must be exactly `google-services.json` (all lowercase)

**Package name mismatch:**
- Package name in Firebase MUST be `com.stocknexus.android`

## 📚 Full Documentation

- `FIREBASE_SETUP.md` - Detailed Firebase setup
- `BACKEND_FCM_INTEGRATION.md` - Backend integration guide

## 🔄 Current Setup

The app currently has **both FCM and 1-minute polling**:
- FCM = instant notifications (when backend is ready)
- Polling = fallback (works now, but 60-second delay)

Once backend implements FCM, you can disable polling for better battery life!
