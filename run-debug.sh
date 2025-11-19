#!/bin/bash

# Source environment
source ./setup-env.sh > /dev/null 2>&1

echo "🚀 Stock Nexus Debug Runner"
echo "=========================="
echo ""

# Check if device is connected
echo "📱 Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No Android devices found!"
    echo "   Please connect your device or start an emulator"
    echo ""
    echo "   To start an emulator:"
    echo "   - Open Android Studio"
    echo "   - Go to Tools → Device Manager"
    echo "   - Start an emulator"
    exit 1
fi

echo "✅ Found $DEVICES device(s)"
echo ""

# Build the APK
echo "🔨 Building debug APK..."
./gradlew assembleDebug --quiet

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build successful"
echo ""

# Install the APK
echo "📦 Installing APK..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "❌ Installation failed!"
    exit 1
fi

echo "✅ Installation successful"
echo ""

# Clear old logs
echo "🧹 Clearing old logs..."
adb logcat -c
echo "✅ Logs cleared"
echo ""

# Launch the app
echo "🚀 Launching app..."
adb shell am start -n com.stocknexus/.android.presentation.MainActivity

if [ $? -ne 0 ]; then
    echo "❌ Failed to launch app!"
    exit 1
fi

echo "✅ App launched"
echo ""

# Start showing logs
echo "📋 Showing logs (Ctrl+C to stop)..."
echo "=================================="
echo ""

# Show logs with colors if possible
if command -v ccze &> /dev/null; then
    adb logcat -v time StockNexus:V AndroidRuntime:E System.err:V *:S | ccze -A
else
    adb logcat -v time StockNexus:V AndroidRuntime:E System.err:V *:S
fi
