#!/bin/bash

echo "🔍 Checking Emulator Installation Status..."
echo ""

# Check if sdkmanager is running
if ps aux | grep -q "[s]dkmanager"; then
    echo "⏳ Installation is RUNNING..."
    echo ""
    echo "Progress check:"
    ls -lh ~/Library/Android/sdk/emulator 2>/dev/null && echo "✅ Emulator installed" || echo "❌ Emulator not yet installed"
    ls -lh ~/Library/Android/sdk/system-images/android-34 2>/dev/null && echo "✅ System image installed" || echo "❌ System image not yet installed"
else
    echo "Installation process NOT running"
    echo ""
    echo "Checking what's installed:"
    ls -lh ~/Library/Android/sdk/emulator 2>/dev/null && echo "✅ Emulator installed" || echo "❌ Emulator not installed"
    ls -lh ~/Library/Android/sdk/system-images/android-34 2>/dev/null && echo "✅ System image installed" || echo "❌ System image not installed"
    echo ""
    
    # If both installed, we're done!
    if [ -d ~/Library/Android/sdk/emulator/emulator ] && [ -d ~/Library/Android/sdk/system-images/android-34 ]; then
        echo "🎉 INSTALLATION COMPLETE!"
        echo ""
        echo "Next step: Run ./setup-android.sh to create your first emulator"
    fi
fi

echo ""
echo "Disk space: $(df -h ~ | tail -1 | awk '{print $4 " available (" $5 " used)"}')"
