# 🛠️ Emulator Setup - Do This Now

## What's Happening

Your Android SDK is installed but missing the emulator. It's currently downloading in the background.

## Current Status

```bash
# Check if installation is complete
ps aux | grep sdkmanager
```

- If you see output → still installing (be patient, ~400MB download)
- If no output → installation finished!

---

## After Installation Completes

### 1. Source the Environment (Every New Terminal)

```bash
source ./setup-env.sh
```

### 2. Create Your First Emulator (AVD)

```bash
# Check if system image is ready
sdkmanager --list_installed | grep system-images

# Create emulator device
avdmanager create avd \
  -n Pixel_6_API_34 \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d "pixel_6"
```

### 3. Verify It's Created

```bash
emulator -list-avds
```

You should see: `Pixel_6_API_34`

### 4. Configure VS Code Extension

1. Open VS Code Settings (Cmd + ,)
2. Search for: **"emulator path"**
3. Set **Emulator: Emulator Path** to:
   ```
   /Users/khalifainternationalaward/Library/Android/sdk/emulator/emulator
   ```

---

## Now You Can Launch!

### Method 1: VS Code Command Palette

1. `Cmd + Shift + P`
2. Type: "Emulator"
3. Select your device

### Method 2: Terminal

```bash
emulator -avd Pixel_6_API_34 &
```

### Method 3: Use the Debug Script

```bash
# Once emulator is running
./run-debug.sh
```

---

## Troubleshooting

### "Error fetching your Android emulators"

→ VS Code extension needs the path configured (see step 4 above)

### "emulator: command not found"

→ Run `source ./setup-env.sh` first

### "No AVDs found"

→ Run the avdmanager command (step 2 above)

---

## Make Environment Permanent (Optional)

Add to `~/.zshrc`:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/emulator"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"
```

Then run: `source ~/.zshrc`

---

## What's Next?

Once the emulator is running, you can debug your sign-in issue! The logs will show exactly where it's getting stuck.
