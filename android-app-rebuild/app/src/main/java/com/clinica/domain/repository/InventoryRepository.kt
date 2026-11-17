package com.clinica.domain.repository

import com.clinica.domain.model.InventoryItem
import com.clinica.domain.model.InventoryItemCreate
import com.clinica.domain.model.StockMovement
import com.clinica.domain.model.StockMovementIn

interface InventoryRepository {
    suspend fun list(query: String? = null): List<InventoryItem>
    suspend fun create(item: InventoryItemCreate): InventoryItem
    suspend fun move(itemId: Int, movement: StockMovementIn): StockMovement
    suspend fun movements(itemId: Int): List<StockMovement>
}
