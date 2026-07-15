package org.example.stock.model

/** Строка итогового CSV: движение по одному товару в рамках одной операции. */
data class StockMovement(
    val operationNumber: Int,
    val type: OperationType,
    val groupId: String,
    val productId: String,
    val quantity: Long,
    val balance: Long,
)

/** Итоговый остаток по товару после обработки всех операций. */
data class ProductBalance(
    val groupId: String,
    val productId: String,
    val balance: Long,
)
