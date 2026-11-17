package com.clinica.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ItemCategory { medicamento, insumo }

@Serializable
data class InventoryItem(
    val id: Int,
    val sku: String,
    val name: String,
    val category: ItemCategory,
    val unit: String,
    val stock: Int,
    @SerialName("min_stock") val minStock: Int,
    @SerialName("low_stock") val lowStock: Boolean = false,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("unit_cost") val unitCost: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class InventoryItemCreate(
    val sku: String,
    val name: String,
    val category: ItemCategory = ItemCategory.insumo,
    val unit: String = "unidad",
    val stock: Int = 0,
    @SerialName("min_stock") val minStock: Int = 0,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("unit_cost") val unitCost: String = "0.00"
)

@Serializable
data class StockMovement(
    val id: Int,
    @SerialName("item_id") val itemId: Int,
    val change: Int,
    @SerialName("stock_after") val stockAfter: Int,
    val reason: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val user: UserBrief? = null
)

@Serializable
data class StockMovementIn(
    val change: Int,
    val reason: String? = null
)

typealias InventoryItemInput = InventoryItemCreate
typealias StockMovementInput = StockMovementIn
