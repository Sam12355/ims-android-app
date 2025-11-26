package com.ims.android.data.model

/**
 * Model for user online events received via Socket.IO
 * Emitted when a user comes online in the system
 */
data class UserOnlineEvent(
    val userId: String,
    val userName: String,
    val photoUrl: String? = null,
    val timestamp: Long
)
