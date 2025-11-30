# Firebase Cloud Messaging (FCM) Setup Guide

## Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name: **Stock Nexus** (or any name you prefer)
4. Disable Google Analytics (optional, can enable later)
5. Click "Create project"

## Step 2: Add Android App to Firebase

1. In Firebase Console, click the Android icon to add Android app
2. Enter these details:
   - **Android package name:** `com.stocknexus.android` (IMPORTANT: Must match exactly!)
   - **App nickname:** Stock Nexus Android (optional)
   - **Debug signing certificate SHA-1:** (optional for now)
3. Click "Register app"

## Step 3: Download google-services.json

1. Click "Download google-services.json"
2. **IMPORTANT:** Place this file in: `android-app/app/google-services.json`
   - It should be in the same folder as `app/build.gradle`
   - Path: `/Users/khalifainternationalaward/Downloads/ims-mobile-app-android/android-app/app/google-services.json`

## Step 4: Enable Cloud Messaging

1. In Firebase Console, go to "Build" → "Cloud Messaging"
2. Click "Get started" if you see it
3. That's it! FCM is now enabled

## Step 5: Get Server Key (for Backend)

1. In Firebase Console, go to Project Settings (gear icon)
2. Go to "Cloud Messaging" tab
3. Under "Cloud Messaging API (Legacy)", you'll see:
   - **Server key** - Copy this! Your backend needs it to send notifications
   - **Sender ID** - You'll need this too

## Step 6: Share with Backend Team

Send your backend team:

- **Server Key** (from step 5)
- **Sender ID** (from step 5)

They need to update their notification sending code to use FCM instead of creating database records.

## Verification

After placing `google-services.json` in the correct location:

1. Run `./gradlew assembleDebug`
2. If build succeeds, Firebase is configured correctly!
3. Install the app and check logs for FCM token

## What Happens Next?

Once `google-services.json` is in place:

- The app will automatically get an FCM token
- This token will be sent to your backend
- Backend can use it to send instant push notifications
- No more 1-minute polling delay!

## Troubleshooting

**Build error "google-services.json not found":**

- Make sure file is in `app/` folder, not `android-app/` folder
- Check file name is exactly `google-services.json` (all lowercase)

**Firebase project already exists:**

- You can use an existing Firebase project
- Just add a new Android app to it

**Package name mismatch:**

- Package name MUST be `com.stocknexus.android`
- If different, update in Firebase Console or change app's package name
