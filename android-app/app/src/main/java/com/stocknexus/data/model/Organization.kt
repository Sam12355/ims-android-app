package com.stocknexus.data.model

import kotlinx.serialization.Serializable

/**
 * Organization structure models matching the web application
 */

@Serializable
data class Region(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class District(
    val id: String? = null,
    val name: String? = null,
    val regionId: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val region: Region? = null
)

@Serializable
data class Branch(
    val id: String,
    val name: String,
    val location: String? = null,
    val districtId: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val managerId: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val district: District? = null,
    val manager: Profile? = null
)

@Serializable
data class BranchAssignment(
    val id: String,
    val userId: String,
    val branchId: String,
    val assignedAt: String,
    val assignedBy: String,
    val isActive: Boolean = true,
    val user: Profile? = null,
    val branch: Branch? = null,
    val assignedByProfile: Profile? = null
)