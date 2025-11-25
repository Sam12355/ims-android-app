package com.ims.android.data.model

import kotlinx.serialization.Serializable

/**
 * Lightweight model for online presence displayed in the top app bar
 */
@Serializable
data class OnlineMember(
    val id: String,
    val name: String? = null,
    val photoUrl: String? = null
)
