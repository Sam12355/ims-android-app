package com.ims.android.service

import java.util.LinkedHashSet

object NotificationDeduper {
    private val recent = LinkedHashSet<String>()
    private const val MAX_SIZE = 200

    @Synchronized
    fun has(id: String): Boolean {
        return recent.contains(id)
    }

    @Synchronized
    fun record(id: String) {
        if (recent.size >= MAX_SIZE) {
            val it = recent.iterator()
            if (it.hasNext()) it.next(); it.remove()
        }
        recent.add(id)
    }

    fun makeId(notificationId: String?, title: String, message: String): String {
        return notificationId ?: ((title + "|" + message).hashCode().toString())
    }
}
