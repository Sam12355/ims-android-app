# 🚀 Quick Start: Run App with Emulator in VS Code

## One-Time Setup (Do this first)

### Step 1: Launch Emulator from VS Code

1. Press `Cmd + Shift + P` (opens Command Palette)
2. Type: **"Emulator"** or **"Android"**
3. You'll see options like:
   - "Emulate"
   - "Android: Launch Emulator"
   - "Android Emulator Launcher"
4. Select one and choose an emulator from the list

**Alternative:** Use the bottom status bar - look for emulator icon/button

### Step 2: Wait for Emulator to Boot

- First boot takes 1-2 minutes
- You'll see the Android home screen when ready
- The emulator runs in a separate window

---

## Every Time You Want to Debug

### Option A: Use the Script (Easiest)

```bash
./run-debug.sh
```

This automatically:

1. Builds the APK
2. Installs to emulator
3. Launches the app
4. Shows real-time logs

### Option B: Use VS Code Tasks

1. Press `Cmd + Shift + P`
2. Type: **"Tasks: Run Task"**
3. Select: **"Build, Install & Launch with Logs"**

---

## What You'll See

When you run the app, logs appear in the VS Code terminal:

```
🚀 Stock Nexus Debug Runner
==========================

📱 Checking for connected devices...
✅ Found 1 device(s)

🔨 Building debug APK...
✅ Build successful

📦 Installing APK...
✅ Installation successful

🚀 Launching app...
✅ App launched

📋 Showing logs...
==========================

11-07 10:30:45.123 D/AuthViewModel: Sign in started for email: test@example.com
11-07 10:30:45.456 D/EnhancedAuthRepo: Calling API signIn for: test@example.com
11-07 10:30:45.789 D/ApiClient: signIn called for: test@example.com
11-07 10:30:46.012 D/ApiClient: login result isSuccess: true
...
```

---

## Testing Sign In

1. **Start the emulator** (use Command Palette)
2. **Run the app** (`./run-debug.sh` or use VS Code task)
3. **In the emulator window:**
   - Enter any email (e.g., `test@example.com`)
   - Enter any password
   - Click "Sign In"
4. **Watch the VS Code terminal** - you'll see:
   - Each step of the sign-in process
   - Any errors with full details
   - Exactly where it's stuck (if it gets stuck)

---

## If Sign In Keeps Loading Forever

Look for these patterns in the logs:

### ✅ Normal (Working):

```
D/AuthViewModel: Sign in started for email: test@example.com
D/EnhancedAuthRepo: Calling API signIn for: test@example.com
D/ApiClient: signIn called for: test@example.com
D/ApiClient: login result isSuccess: true
D/ApiClient: Creating enhanced AuthResponse
D/EnhancedAuthRepo: API signIn successful, saving session
D/EnhancedAuthRepo: Session saved successfully
D/AuthViewModel: Sign in result: true
D/AuthViewModel: Sign in successful, user: test@example.com
```

### ❌ Problem (Something Wrong):

```
D/AuthViewModel: Sign in started for email: test@example.com
D/EnhancedAuthRepo: Calling API signIn for: test@example.com
D/ApiClient: signIn called for: test@example.com
E/ApiClient: login failed: NullPointerException
E/EnhancedAuthRepo: Sign in failed
    at com.stocknexus.data.api.ApiClient.signIn(ApiClient.kt:115)
    ...
```

The logs will tell you **exactly** what went wrong!

---

## Quick Commands

```bash
# Check if emulator is running
adb devices

# Restart app (if already installed)
adb shell am start -n com.stocknexus/.android.presentation.MainActivity

# View only errors
adb logcat -v time AndroidRuntime:E StockNexus:V *:S

# Clear all app data (reset to fresh state)
adb shell pm clear com.stocknexus

# Uninstall app
adb uninstall com.stocknexus

# Stop the app
adb shell am force-stop com.stocknexus
```

---

## Tips

1. **Keep the terminal visible** while testing - you'll immediately see any errors
2. **Use Cmd+F** in the terminal to search for "Error" or "Exception"
3. **If stuck loading**, check the last log message to see where it stopped
4. **To test again**, just close and reopen the app in the emulator - logs continue

---

## Need Help?

If you see errors in the logs, just share them - the logs show:

- Exact line numbers where problems occur
- Full error messages
- Stack traces for debugging

Much better than testing on a real device where you can't see what's happening! 🎉
