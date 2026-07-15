package org.example.stock.model

sealed interface Operation {
    val lineNumber: Int
    val groupId: String

    /** Поступление товара: `<id группы>;<id товара>;<количество>`. */
    data class Receipt(
        override val lineNumber: Int,
        override val groupId: String,
        val productId: String,
        val quantity: Long,
    ) : Operation

    /** Продажа товара из группы: `<id группы>;<количество>`. */
    data class Sale(
        override val lineNumber: Int,
        override val groupId: String,
        val quantity: Long,
    ) : Operation
}

enum class OperationType { RECEIPT, SALE }
