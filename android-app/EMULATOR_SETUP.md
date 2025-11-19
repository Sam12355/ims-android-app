# Running Android App with Emulator (No Android Studio Required)

## Quick Setup Guide

You have two options to run the app with full error visibility without using a physical device:

---

## Option 1: Use VS Code Extension with Existing Emulator (Recommended)

If you have Android Studio installed (even if you don't want to use it), you can launch emulators directly from VS Code.

### Setup:

1. **You already have these extensions installed:**

   - ✅ Android iOS Emulator (diemasmichiels.emulate)
   - ✅ Android (adelphes.android-dev-ext)
   - ✅ Android Emulator Launcher (343max.android-emulator-launcher)

2. **Launch Emulator from VS Code:**

   - Press `Cmd+Shift+P` (Command Palette)
   - Type "Android" or "Emulator"
   - Select one of:
     - `Android: Launch Emulator` or
     - `Emulate` or
     - `Android Emulator Launcher: Launch`

3. **Once the emulator is running:**
   - Press `Cmd+Shift+P`
   - Run task: `Tasks: Run Task`
   - Select: `Build, Install & Launch with Logs`

---

## Option 2: Install Command-Line Android Tools (Lightweight)

If you don't have Android Studio, install just the command-line tools:

### Step 1: Install Android Command-Line Tools

```bash
# Download the command-line tools
cd ~/Downloads
curl -O https://dl.google.com/android/repository/commandlinetools-mac-11076708_latest.zip

# Create SDK directory
mkdir -p ~/Library/Android/sdk/cmdline-tools

# Extract tools
unzip commandlinetools-mac-11076708_latest.zip -d ~/Library/Android/sdk/cmdline-tools
mv ~/Library/Android/sdk/cmdline-tools/cmdline-tools ~/Library/Android/sdk/cmdline-tools/latest
```

### Step 2: Add to PATH

Add to your `~/.zshrc`:

```bash
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.zshrc
source ~/.zshrc
```

### Step 3: Install Required Components

```bash
# Accept licenses
sdkmanager --licenses

# Install platform tools and emulator
sdkmanager "platform-tools" "platforms;android-34" "emulator"

# Install system image (choose one based on your Mac chip)
# For Apple Silicon (M1/M2/M3):
sdkmanager "system-images;android-34;google_apis;arm64-v8a"

# For Intel Macs:
sdkmanager "system-images;android-34;google_apis;x86_64"
```

### Step 4: Create AVD (Android Virtual Device)

```bash
# For Apple Silicon:
avdmanager create avd -n StockNexus_Emulator -k "system-images;android-34;google_apis;arm64-v8a" -d pixel_5

# For Intel:
avdmanager create avd -n StockNexus_Emulator -k "system-images;android-34;google_apis;x86_64" -d pixel_5
```

### Step 5: Launch Emulator

```bash
emulator -avd StockNexus_Emulator
```

---

## Option 3: Use Existing Emulator from Android Studio

If you have Android Studio installed but don't want to use it:

### Step 1: Find Android SDK Location

```bash
# Check if Android Studio is installed
ls ~/Library/Android/sdk

# If found, add to PATH
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/emulator' >> ~/.zshrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.zshrc
source ~/.zshrc
```

### Step 2: List Available Emulators

```bash
emulator -list-avds
```

### Step 3: Launch Emulator

```bash
# Replace with your AVD name from the list
emulator -avd Pixel_5_API_34
```

Or use the VS Code extension (see Option 1).

---

## Running the App with Emulator

Once you have an emulator running (from any option above):

### Method 1: Use the Debug Script

```bash
./run-debug.sh
```

This will automatically:

- ✅ Build the APK
- ✅ Install to emulator
- ✅ Launch the app
- ✅ Show real-time logs with errors

### Method 2: Use VS Code Tasks

1. Press `Cmd+Shift+P`
2. Type "Tasks: Run Task"
3. Select "Build, Install & Launch with Logs"

### Method 3: Manual Commands

```bash
# 1. Build
./gradlew assembleDebug

# 2. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. Launch
adb shell am start -n com.stocknexus/.android.presentation.MainActivity

# 4. View logs
adb logcat -v time StockNexus:V AndroidRuntime:E *:S
```

---

## Checking Emulator Status

```bash
# Check if emulator is running
adb devices

# You should see something like:
# emulator-5554   device
```

---

## Troubleshooting

### "emulator: command not found"

- Android SDK is not installed or not in PATH
- Follow Option 2 above to install command-line tools

### "adb: command not found"

- Platform tools not installed or not in PATH
- Install with: `sdkmanager "platform-tools"`

### No emulator in the list

- Create one with `avdmanager create avd` (see Option 2, Step 4)
- Or launch Android Studio once to create an emulator through Device Manager

### Emulator is slow

- Make sure you're using the correct system image for your CPU:
  - Apple Silicon (M1/M2/M3): arm64-v8a
  - Intel: x86_64
- Allocate more RAM to the emulator (edit AVD config)

### App installs but doesn't launch

- Check logs: `adb logcat -v time AndroidRuntime:E *:S`
- Make sure you built the debug variant: `./gradlew assembleDebug`

---

## Recommended: Quick Setup for Your System

Since you have the VS Code extensions already installed, the fastest way is:

1. **If you have Android Studio installed** (even if you never use it):

   - Press `Cmd+Shift+P` in VS Code
   - Type "Emulate" or "Android Emulator"
   - Select and launch an emulator
   - Run `./run-debug.sh` in terminal

2. **If you don't have Android Studio**:
   - Follow Option 2 to install command-line tools (takes ~10 minutes)
   - Create an emulator
   - Use the VS Code extensions or `./run-debug.sh`

---

## Viewing Logs in Real-Time

Once your emulator is running and app is installed, logs will appear in VS Code terminal showing:

```
D/AuthViewModel: Sign in started for email: test@example.com
D/EnhancedAuthRepo: Calling API signIn for: test@example.com
D/ApiClient: signIn called for: test@example.com
D/ApiClient: login result isSuccess: true
...
```

Any errors will be clearly visible with full stack traces!

---

## Next Steps

1. Choose your preferred option above
2. Get an emulator running
3. Run `./run-debug.sh`
4. Try signing in and watch the logs to see exactly what happens

Need help? Let me know which option you chose and any errors you see!
