package org.example.stock

import org.example.stock.model.Operation
import org.example.stock.model.OperationType.RECEIPT
import org.example.stock.model.OperationType.SALE
import org.example.stock.model.ProductBalance
import org.example.stock.model.StockMovement
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StockCalculatorTest {

    private val calculator = StockCalculator()

    private fun receipt(groupId: String, productId: String, quantity: Long) =
        Operation.Receipt(lineNumber = 0, groupId = groupId, productId = productId, quantity = quantity)

    private fun sale(groupId: String, quantity: Long) =
        Operation.Sale(lineNumber = 0, groupId = groupId, quantity = quantity)

    @Test
    fun `повторные поступления накапливаются`() {
        val movements = calculator.applyAll(listOf(receipt("g1", "A", 10), receipt("g1", "A", 5)))

        assertEquals(
            listOf(
                StockMovement(1, RECEIPT, "g1", "A", 10, 10),
                StockMovement(2, RECEIPT, "g1", "A", 5, 15),
            ),
            movements,
        )
    }

    @Test
    fun `продажа списывает товар с наивысшим рангом`() {
        calculator.applyAll(listOf(receipt("g1", "B", 10), receipt("g1", "A", 10)))

        val movements = calculator.apply(sale("g1", 4))

        assertEquals(listOf(StockMovement(3, SALE, "g1", "A", -4, 6)), movements)
        assertEquals(
            listOf(ProductBalance("g1", "A", 6), ProductBalance("g1", "B", 10)),
            calculator.balances(),
        )
    }

    @Test
    fun `продажа переходит на следующий ранг, когда товар закончился`() {
        calculator.applyAll(listOf(receipt("g1", "A", 10), receipt("g1", "B", 20), receipt("g1", "C", 30)))

        val movements = calculator.apply(sale("g1", 25))

        assertEquals(
            listOf(
                StockMovement(4, SALE, "g1", "A", -10, 0),
                StockMovement(4, SALE, "g1", "B", -15, 5),
            ),
            movements,
        )
        assertEquals(
            listOf(ProductBalance("g1", "A", 0), ProductBalance("g1", "B", 5), ProductBalance("g1", "C", 30)),
            calculator.balances(),
        )
    }

    @Test
    fun `продажа ровно на весь остаток обнуляет группу`() {
        calculator.applyAll(listOf(receipt("g1", "A", 5), receipt("g1", "B", 5)))

        val movements = calculator.apply(sale("g1", 10))

        assertEquals(
            listOf(
                StockMovement(3, SALE, "g1", "A", -5, 0),
                StockMovement(3, SALE, "g1", "B", -5, 0),
            ),
            movements,
        )
    }

    @Test
    fun `продажа сверх остатка уводит в минус последний по рангу товар`() {
        calculator.applyAll(listOf(receipt("g1", "A", 5), receipt("g1", "B", 5)))

        val movements = calculator.apply(sale("g1", 12))

        assertEquals(
            listOf(
                StockMovement(3, SALE, "g1", "A", -5, 0),
                StockMovement(3, SALE, "g1", "B", -7, -2),
            ),
            movements,
        )
        assertEquals(
            listOf(ProductBalance("g1", "A", 0), ProductBalance("g1", "B", -2)),
            calculator.balances(),
        )
    }

    @Test
    fun `продажа из группы без поступлений уходит в минус по псевдо-товару`() {
        val movements = calculator.apply(sale("g1", 3))

        assertEquals(listOf(StockMovement(1, SALE, "g1", "", -3, -3)), movements)
        assertEquals(listOf(ProductBalance("g1", "", -3)), calculator.balances())
    }

    @Test
    fun `товары с нулевым и отрицательным остатком в продаже пропускаются`() {
        calculator.applyAll(listOf(receipt("g1", "A", 5), receipt("g1", "B", 5), sale("g1", 12)))
        calculator.apply(receipt("g1", "C", 10))

        val movements = calculator.apply(sale("g1", 4))

        assertEquals(listOf(StockMovement(5, SALE, "g1", "C", -4, 6)), movements)
    }

    @Test
    fun `поступление гасит отрицательный остаток`() {
        calculator.applyAll(listOf(sale("g1", 10), receipt("g1", "A", 3)))

        assertEquals(
            listOf(ProductBalance("g1", "", -10), ProductBalance("g1", "A", 3)),
            calculator.balances(),
        )

        val other = StockCalculator()
        other.applyAll(listOf(receipt("g2", "X", 2), sale("g2", 5), receipt("g2", "X", 10)))
        assertEquals(listOf(ProductBalance("g2", "X", 7)), other.balances())
    }

    @Test
    fun `ранг сравнивается лексикографически, а не численно`() {
        calculator.applyAll(listOf(receipt("g1", "A9", 5), receipt("g1", "A10", 5)))

        val movements = calculator.apply(sale("g1", 5))

        assertEquals(listOf(StockMovement(3, SALE, "g1", "A10", -5, 0)), movements)
    }

    @Test
    fun `группы не влияют друг на друга`() {
        calculator.applyAll(listOf(receipt("g1", "A", 5), receipt("g2", "A", 7)))

        val movements = calculator.apply(sale("g2", 3))

        assertEquals(listOf(StockMovement(3, SALE, "g2", "A", -3, 4)), movements)
        assertEquals(
            listOf(ProductBalance("g1", "A", 5), ProductBalance("g2", "A", 4)),
            calculator.balances(),
        )
    }
}
