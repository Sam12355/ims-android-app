#!/bin/bash

# Android SDK Environment Setup Script
# Run this once: source ./setup-env.sh

export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/emulator"
export PATH="$PATH:$ANDROID_HOME/platform-tools"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"

echo "✅ Android environment configured!"
echo "ANDROID_HOME: $ANDROID_HOME"
echo ""
echo "To make this permanent, add these lines to your ~/.zshrc:"
echo ""
echo 'export ANDROID_HOME="$HOME/Library/Android/sdk"'
echo 'export PATH="$PATH:$ANDROID_HOME/emulator"'
echo 'export PATH="$PATH:$ANDROID_HOME/platform-tools"'
echo 'export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin"'
