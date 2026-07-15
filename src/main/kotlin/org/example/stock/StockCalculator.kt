package org.example.stock

import org.example.stock.model.Operation
import org.example.stock.model.OperationType
import org.example.stock.model.ProductBalance
import org.example.stock.model.StockMovement
import org.slf4j.LoggerFactory
import java.util.TreeMap

/**
 * Считает остатки по товарам, последовательно применяя операции.
 *
 * Правила:
 *  - поступление увеличивает остаток конкретного товара;
 *  - продажа списывает товары группы по рангу: сначала товар с наивысшим рангом
 *    (лексикографически первый `productId`), затем следующий и так далее;
 *  - если продать нужно больше, чем есть в группе, продаётся весь возможный остаток,
 *    а недостача уходит в минус по последнему (низшему) рангу; если в группе не было
 *    ни одного поступления — по псевдо-товару с пустым id.
 */
class StockCalculator {

    private val log = LoggerFactory.getLogger(javaClass)

    /** groupId -> (productId -> остаток). TreeMap даёт обход товаров в порядке ранга. */
    private val stock: MutableMap<String, TreeMap<String, Long>> = LinkedHashMap()

    private var operationNumber = 0

    /** Применяет операцию и возвращает движения, которые она породила. */
    fun apply(operation: Operation): List<StockMovement> {
        operationNumber++
        return when (operation) {
            is Operation.Receipt -> receive(operation)
            is Operation.Sale -> sell(operation)
        }
    }

    fun applyAll(operations: Iterable<Operation>): List<StockMovement> =
        operations.flatMap { apply(it) }

    /** Итоговые остатки: группы — в порядке первого появления, товары — в порядке ранга. */
    fun balances(): List<ProductBalance> =
        stock.flatMap { (groupId, products) ->
            products.map { (productId, balance) -> ProductBalance(groupId, productId, balance) }
        }

    private fun receive(op: Operation.Receipt): List<StockMovement> {
        val products = productsOf(op.groupId)
        val balance = (products[op.productId] ?: 0L) + op.quantity
        products[op.productId] = balance

        log.debug(
            "Операция #{}: поступление {} шт. товара '{}' группы '{}', остаток {}",
            operationNumber, op.quantity, op.productId, op.groupId, balance,
        )
        return listOf(
            StockMovement(operationNumber, OperationType.RECEIPT, op.groupId, op.productId, op.quantity, balance),
        )
    }

    private fun sell(op: Operation.Sale): List<StockMovement> {
        val products = productsOf(op.groupId)

        val soldByProduct = LinkedHashMap<String, Long>()
        var remaining = op.quantity

        for (productId in products.keys.toList()) {
            if (remaining == 0L) break
            val available = products.getValue(productId)
            if (available <= 0L) continue

            val taken = minOf(available, remaining)
            products[productId] = available - taken
            soldByProduct[productId] = taken
            remaining -= taken
        }

        if (remaining > 0L) {
            val debtorId = products.lastEntry()?.key ?: GROUP_LEVEL_PRODUCT_ID
            products[debtorId] = (products[debtorId] ?: 0L) - remaining
            soldByProduct.merge(debtorId, remaining, Long::plus)

            log.warn(
                "Операция #{} (строка {}): продажа {} шт. из группы '{}' превышает остаток, " +
                    "недостача {} шт. записана в минус по товару '{}'",
                operationNumber, op.lineNumber, op.quantity, op.groupId, remaining, debtorId,
            )
        }

        log.debug(
            "Операция #{}: продажа {} шт. из группы '{}' списана с товаров {}",
            operationNumber, op.quantity, op.groupId, soldByProduct.keys,
        )
        return soldByProduct.map { (productId, sold) ->
            StockMovement(
                operationNumber = operationNumber,
                type = OperationType.SALE,
                groupId = op.groupId,
                productId = productId,
                quantity = -sold,
                balance = products.getValue(productId),
            )
        }
    }

    private fun productsOf(groupId: String): TreeMap<String, Long> =
        stock.getOrPut(groupId) { TreeMap() }

    companion object {
        /** id «товара», на который списывается недостача группы без поступлений. */
        const val GROUP_LEVEL_PRODUCT_ID = ""
    }
}
