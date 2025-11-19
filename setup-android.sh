#!/bin/bash

echo "🚀 Android Emulator Setup for Intel Mac"
echo "========================================"
echo ""

# Source environment
source ./setup-env.sh
echo ""

# Install x86_64 system image for Intel Mac
echo "📦 Installing x86_64 system image (Intel compatible)..."
echo "This may take 5-10 minutes (~700MB download)"
echo ""

$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "system-images;android-34-ext10;google_apis_playstore;x86_64"

echo ""
echo "✅ System image installed!"
echo ""

# Delete old ARM64 AVD if exists
if [ -d "$HOME/.android/avd/Pixel_6_API_34.avd" ]; then
    echo "🗑️  Removing old ARM64 emulator..."
    rm -rf "$HOME/.android/avd/Pixel_6_API_34.avd"
    rm -f "$HOME/.android/avd/Pixel_6_API_34.ini"
fi

# Create x86_64 AVD
echo "�� Creating Android Virtual Device (x86_64)..."
avdmanager create avd \
  -n Pixel_6_API_34_x86 \
  -k "system-images;android-34-ext10;google_apis_playstore;x86_64" \
  -d "pixel_6" \
  --force

echo ""
echo "✅ AVD created!"
echo ""

# List AVDs
echo "Available emulators:"
emulator -list-avds

echo ""
echo "🎉 Setup complete!"
echo ""
echo "To launch emulator:"
echo "  emulator -avd Pixel_6_API_34_x86 &"
echo ""
echo "Or run: ./run-debug.sh"
