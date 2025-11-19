# Notification Toggle Fix

## Problem

The notification toggles in the Settings screen were successfully updating the database but then failing with a "host resolution error" when trying to refresh the profile from the backend API.

## Root Cause

The `updateNotificationSettings()` function in `ApiClient.kt` was:

1. Successfully updating the Supabase database with new notification settings
2. Then calling `getProfile()` to refresh the profile data
3. `getProfile()` makes an HTTP request to the backend API at `https://stock-nexus-84-main-2-1.onrender.com/api`
4. If the backend API is unreachable or slow, this fails with a host resolution error
5. Even though the database was updated successfully, the user sees an error message

## Solution

Modified `updateNotificationSettings()` in `/app/src/main/java/com/stocknexus/data/api/ApiClient.kt` to:

1. Update the Supabase database (as before)
2. Instead of calling the backend API, create an updated Profile object locally using the stored profile data
3. Update the notification settings in the cached profile
4. Save the updated profile to DataStore cache
5. Return the updated profile immediately

## Changes Made

### File: `/app/src/main/java/com/stocknexus/data/api/ApiClient.kt`

**Before:**

```kotlin
suspend fun updateNotificationSettings(notificationSettings: Map<String, Boolean>): Profile = withContext(Dispatchers.IO) {
    // ... update Supabase ...

    // Refresh profile data after update
    getProfile()  // ❌ This depends on backend API
}
```

**After:**

```kotlin
suspend fun updateNotificationSettings(notificationSettings: Map<String, Boolean>): Profile = withContext(Dispatchers.IO) {
    // ... update Supabase ...

    // Create updated profile with new notification settings
    // Instead of calling getProfile() which depends on backend API
    val updatedProfile = currentProfile.copy(
        notificationSettings = notificationSettings,
        updatedAt = java.time.Instant.now().toString()
    )

    // Cache the updated profile
    context.dataStore.edit { preferences ->
        preferences[PROFILE_DATA_KEY] = json.encodeToString(Profile.serializer(), updatedProfile)
    }

    return@withContext updatedProfile  // ✅ Returns immediately without API call
}
```

## Benefits

1. **No dependency on backend API**: The notification settings update works even when the backend is unreachable
2. **Faster response**: No need to wait for an HTTP round trip to get the updated profile
3. **Better user experience**: Users see immediate success feedback instead of errors
4. **Database still updated**: The Supabase database is updated correctly, which is the source of truth
5. **Cache consistency**: The local cache is immediately updated with the new settings

## Testing

To verify the fix:

1. Open the app and go to Settings
2. Toggle any notification setting (email, SMS, WhatsApp, etc.)
3. The toggle should update immediately without errors
4. The database should be updated (verify in Supabase dashboard)
5. Refresh the profile (restart app or navigate away and back) - settings should persist

## Technical Notes

- The function still updates Supabase directly, bypassing the backend API (this was already working)
- The only change is removing the dependency on `getProfile()` which calls the backend API
- The cached profile is used as the base and updated with the new notification settings
- Next time `refreshProfile()` is called, it will fetch from the backend API and sync any differences
