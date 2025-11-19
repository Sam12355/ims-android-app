package com.stocknexus.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Inventory and stock models matching the web application
 */

@Serializable
data class Item(
    val id: String,
    val name: String,
    val category: String,
    val description: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    @SerialName("unit_of_measure") val unitOfMeasure: String = "pieces",
    @SerialName("threshold_level") val thresholdLevel: Int = 10,
    @SerialName("low_level") val lowLevel: Int? = 5,
    @SerialName("critical_level") val criticalLevel: Int? = 2,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("storage_temperature") val storageTemperature: Double? = null,
    @SerialName("base_unit") val baseUnit: String? = "piece",
    @SerialName("enable_packaging") val enablePackaging: Boolean? = false,
    @SerialName("packaging_unit") val packagingUnit: String? = null,
    @SerialName("units_per_package") val unitsPerPackage: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val branch: Branch? = null
)

@Serializable
data class Stock(
    val id: String,
    val itemId: String,
    val branchId: String,
    val currentQuantity: Int,
    val reservedQuantity: Int = 0,
    val availableQuantity: Int,
    val lastUpdated: String,
    val item: Item? = null,
    val branch: Branch? = null
)

@Serializable
data class StockItem(
    val id: String,
    @SerialName("item_id") val item_id: String,
    @SerialName("current_quantity") val current_quantity: Int,
    @SerialName("last_updated") val last_updated: String,
    val items: ItemDetails
)

@Serializable
data class ItemDetails(
    val name: String,
    val category: String,
    @SerialName("threshold_level") val threshold_level: Int,
    @SerialName("low_level") val low_level: Int? = null,
    @SerialName("critical_level") val critical_level: Int? = null,
    @SerialName("image_url") val image_url: String? = null,
    @SerialName("branch_id") val branch_id: String,
    @SerialName("base_unit") val base_unit: String? = "piece",
    @SerialName("enable_packaging") val enable_packaging: Boolean? = false,
    @SerialName("packaging_unit") val packaging_unit: String? = null,
    @SerialName("units_per_package") val units_per_package: Int? = null
)

@Serializable
data class StockMovement(
    val id: String,
    val itemId: String,
    val branchId: String,
    val movementType: String, // "IN", "OUT", "ADJUSTMENT", "TRANSFER"
    val quantity: Int,
    val previousQuantity: Int,
    val newQuantity: Int,
    val reason: String? = null,
    val reference: String? = null,
    val notes: String? = null,
    val userId: String,
    val createdAt: String,
    val item: Item? = null,
    val branch: Branch? = null,
    val user: Profile? = null
)

@Serializable
data class StockIn(
    val id: String,
    val branchId: String,
    val reference: String,
    val supplier: String? = null,
    val notes: String? = null,
    val status: String = "pending", // "pending", "approved", "rejected"
    val totalItems: Int,
    val userId: String,
    val approvedBy: String? = null,
    val approvedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val items: List<StockInItem> = emptyList(),
    val branch: Branch? = null,
    val user: Profile? = null,
    val approver: Profile? = null
)

@Serializable
data class StockInItem(
    val id: String,
    val stockInId: String,
    val itemId: String,
    val quantity: Int,
    val unitCost: Double? = null,
    val expiryDate: String? = null,
    val batchNumber: String? = null,
    val notes: String? = null,
    val item: Item? = null
)

@Serializable
data class MoveoutList(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("branch_id")
    val branchId: String? = null,
    val status: String = "active", // "active", "completed", "cancelled"
    @SerialName("created_by")
    val createdBy: String? = null,
    @SerialName("completed_by")
    val completedBy: String? = null,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val items: List<MoveoutItem> = emptyList(),
    val branch: Branch? = null,
    val creator: Profile? = null,
    val completer: Profile? = null
)

@Serializable
data class MoveoutItem(
    val id: String? = null,
    @SerialName("moveout_list_id")
    val moveoutListId: String? = null,
    @SerialName("item_id")
    val itemId: String,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("available_amount")
    val availableAmount: Int,
    @SerialName("request_amount")
    val requestAmount: Int,
    val status: String? = null, // "pending", "completed"
    val completed: Boolean = false,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("completed_by")
    val completedBy: String? = null,
    @SerialName("completed_by_name")
    val completedByName: String? = null,
    @SerialName("processed_by")
    val processedBy: String? = null,
    @SerialName("processed_at")
    val processedAt: String? = null,
    val notes: String? = null,
    val item: Item? = null,
    val completer: Profile? = null
)

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("event_date") val eventDate: String,
    @SerialName("event_type") val eventType: String = "reorder", // reorder, delivery, alert, expiry, usage_spike
    @SerialName("branch_id") val branchId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)