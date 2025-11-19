# Stock Nexus Android - Debug Guide

## Running the App with Full Error Visibility

### Option 1: Using VS Code Tasks (Recommended)

1. **Connect your Android device via USB** or start an emulator
2. **Enable USB Debugging** on your device (Settings → Developer Options → USB Debugging)
3. **Open Command Palette** (Cmd+Shift+P on Mac, Ctrl+Shift+P on Windows/Linux)
4. **Run Task**: Type "Tasks: Run Task" and select it
5. **Choose Task**: Select "Build, Install & Launch with Logs"

This will:

- Build the debug APK
- Clear old logs
- Install the app on your device
- Start showing real-time logs in VS Code terminal

### Option 2: Manual Steps

#### Step 1: Check Device Connection

```bash
adb devices
```

You should see your device listed.

#### Step 2: Build the APK

```bash
./gradlew assembleDebug
```

#### Step 3: Install the APK

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Step 4: Clear Old Logs

```bash
adb logcat -c
```

#### Step 5: Start Logcat (View Logs)

Run one of these commands in a separate terminal:

**Option A: See All App Logs**

```bash
adb logcat -v time StockNexus:V AndroidRuntime:E *:S
```

**Option B: See Only Errors**

```bash
adb logcat -v time AndroidRuntime:E StockNexus:V System.err:V *:S
```

**Option C: See Everything (Verbose)**

```bash
adb logcat -v time
```

#### Step 6: Launch the App

Manually open the app on your device, or use:

```bash
adb shell am start -n com.stocknexus/.android.presentation.MainActivity
```

## Understanding Log Tags

The app uses these log tags for debugging:

- `AuthViewModel` - Authentication view model logs
- `EnhancedAuthRepo` - Repository layer authentication logs
- `ApiClient` - API client and network logs
- `AndroidRuntime` - System errors and crashes

## Common Issues & Solutions

### Issue: "Sign In" Button Just Keeps Loading

**What to look for in logs:**

```
D/AuthViewModel: Sign in started for email: user@example.com
D/EnhancedAuthRepo: Calling API signIn for: user@example.com
D/ApiClient: signIn called for: user@example.com
```

If you see these but nothing after, check for:

- Exceptions or errors
- Timeout issues
- Missing network permissions

### Issue: App Crashes on Launch

**Look for:**

```
E/AndroidRuntime: FATAL EXCEPTION
```

This will show the complete stack trace of what went wrong.

### Issue: No Logs Appearing

1. Make sure USB debugging is enabled
2. Check device connection: `adb devices`
3. Restart adb: `adb kill-server && adb start-server`
4. Check if logs are being cleared: Run `adb logcat` without filters

## VS Code Logcat Panel

When using the "View Logcat" tasks, logs will appear in a dedicated terminal panel at the bottom of VS Code. You can:

- **Clear logs**: Run task "Clear Logcat"
- **Filter logs**: The task already filters to show only relevant logs
- **Search logs**: Use Cmd+F (Mac) or Ctrl+F (Windows/Linux) in the terminal

## Debugging Sign-In Issues

When you try to sign in, you should see this sequence in logs:

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

If any step fails, you'll see an error log indicating where the problem is.

## Quick Commands Reference

```bash
# Check connected devices
adb devices

# Install APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uninstall app
adb uninstall com.stocknexus

# Clear app data (reset app to fresh state)
adb shell pm clear com.stocknexus

# View real-time logs
adb logcat -v time StockNexus:V AndroidRuntime:E *:S

# Filter for specific errors
adb logcat -v time | grep -i "error\|exception"

# Save logs to file
adb logcat -v time > app_logs.txt

# Launch app
adb shell am start -n com.stocknexus/.android.presentation.MainActivity

# Stop app
adb shell am force-stop com.stocknexus
```

## Testing Sign In with Logs

1. Run the "View Logcat (All App Logs)" task
2. Open the app on your device
3. Try to sign in with any email/password (demo mode)
4. Watch the VS Code terminal for log output
5. If it fails, the logs will show exactly where and why

The logs are tagged and timestamped, making it easy to trace the flow of execution.
