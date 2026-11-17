package com.clinica.data.repository

import com.clinica.data.remote.api.InventoryApi
import com.clinica.domain.model.InventoryItem
import com.clinica.domain.model.InventoryItemCreate
import com.clinica.domain.model.StockMovement
import com.clinica.domain.model.StockMovementIn
import com.clinica.domain.repository.InventoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val api: InventoryApi
) : InventoryRepository {

    override suspend fun list(query: String?): List<InventoryItem> = api.list(query).items

    override suspend fun create(item: InventoryItemCreate): InventoryItem = api.create(item)

    override suspend fun move(itemId: Int, movement: StockMovementIn): StockMovement =
        api.move(itemId, movement)

    override suspend fun movements(itemId: Int): List<StockMovement> =
        api.movements(itemId).items
}
