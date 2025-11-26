package com.ims.android.ui.toast

import com.ims.android.data.model.UserOnlineEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object UserOnlineToastManager {
    private val _event = MutableStateFlow<UserOnlineEvent?>(null)
    val event: StateFlow<UserOnlineEvent?> = _event

    fun post(event: UserOnlineEvent?) {
        _event.value = event
    }
}
