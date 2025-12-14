#!/usr/bin/env python3
"""
Fix hardcoded dark theme colors in StyleSheets by adding inline style overrides
"""
import re
import sys

def add_card_overrides(content):
    """Add inline overrides for card backgrounds"""
    # Pattern for card-like elements with static background colors in StyleSheets
    patterns = [
        (r'style={styles\.(\w*[Cc]ard\w*)}', r'style={[styles.\1, { backgroundColor: designColors.cardBackground, ...(isDark ? {} : { shadowColor: \'#000\', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.1, shadowRadius: 4, elevation: 3 }) }]}'),
        (r'style={styles\.(modal(?!Overlay)\w*)}', r'style={[styles.\1, { backgroundColor: designColors.surfaceVariant }]}'),
        (r'style={styles\.(fieldContainer|inputContainer|input\w*)}', r'style={[styles.\1, { backgroundColor: designColors.surfaceVariant, borderColor: designColors.borderLight }]}'),
    ]
    
    for pattern, replacement in patterns:
        # Only replace if not already using array syntax with inline overrides
        matches = re.finditer(pattern, content)
        for match in matches:
            original = match.group(0)
            if 'designColors' not in original and '[' not in original:
                content = content.replace(original, re.sub(pattern, replacement, original))
    
    return content

# Read file
with open(sys.argv[1], 'r') as f:
    content = f.read()

# Apply fixes
content = add_card_overrides(content)

# Write back
with open(sys.argv[1], 'w') as f:
    f.write(content)

print(f"Fixed {sys.argv[1]}")
