package com.ims.android.data.model

import java.util.UUID
import java.time.Instant

// Message model for chat
 data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val sentAt: Instant,
    val deliveredAt: Instant? = null,
    val readAt: Instant? = null,
    val fcmMessageId: String? = null
)