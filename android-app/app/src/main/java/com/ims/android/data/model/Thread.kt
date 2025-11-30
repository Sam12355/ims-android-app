package com.ims.android.data.model

import java.util.UUID
import java.time.Instant

// Thread model for grouping conversations
 data class Thread(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val lastMessageId: String?,
    val updatedAt: Instant,
    val displayName: String? = null,
    val displayPhoto: String? = null
)