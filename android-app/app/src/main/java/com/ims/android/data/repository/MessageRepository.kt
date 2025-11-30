package com.ims.android.data.repository

import com.ims.android.data.model.Message
import com.ims.android.data.model.Thread
import com.ims.android.data.api.ApiClient
import java.util.UUID

class MessageRepository(private val apiClient: ApiClient) {
    suspend fun sendMessage(senderId: UUID, receiverId: UUID, content: String): Result<Message> {
        // TODO: Implement API call to /messages/send
        return Result.failure(NotImplementedError())
    }

    suspend fun getInbox(userId: UUID): Result<List<Thread>> {
        // TODO: Implement API call to /messages/inbox
        return Result.failure(NotImplementedError())
    }

    suspend fun getThread(user1Id: UUID, user2Id: UUID): Result<List<Message>> {
        // TODO: Implement API call to /messages/thread
        return Result.failure(NotImplementedError())
    }

    suspend fun markMessageRead(messageId: UUID): Result<Unit> {
        // TODO: Implement API call to /messages/read
        return Result.failure(NotImplementedError())
    }
}