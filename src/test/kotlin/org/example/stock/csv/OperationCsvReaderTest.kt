package org.example.stock.csv

import org.example.stock.model.Operation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OperationCsvReaderTest {

    private val reader = OperationCsvReader()

    private fun read(vararg lines: String) = reader.read(lines.asSequence())

    @Test
    fun `три колонки читаются как поступление, две — как продажа`() {
        val result = read("g1;A;10", "g1;4")

        assertEquals(
            listOf(
                Operation.Receipt(lineNumber = 1, groupId = "g1", productId = "A", quantity = 10),
                Operation.Sale(lineNumber = 2, groupId = "g1", quantity = 4),
            ),
            result.operations,
        )
        assertTrue(result.invalidLines.isEmpty())
    }

    @Test
    fun `пробелы вокруг значений игнорируются`() {
        val result = read("  g1 ; A ; 10  ")

        assertEquals(
            listOf(Operation.Receipt(lineNumber = 1, groupId = "g1", productId = "A", quantity = 10)),
            result.operations,
        )
    }

    @Test
    fun `заголовок, пустые строки и комментарии пропускаются`() {
        val result = read("group_id;product_id;quantity", "", "# комментарий", "g1;A;10", "   ")

        assertEquals(1, result.operations.size)
        assertTrue(result.invalidLines.isEmpty())
    }

    @Test
    fun `первая строка с числом в конце — это операция, а не заголовок`() {
        val result = read("g1;A;10")

        assertEquals(1, result.operations.size)
    }

    @Test
    fun `некорректные строки пропускаются и попадают в отчёт`() {
        val result = read(
            "g1;A;10",
            "g1;A;не-число",
            "g1;A;B;C;5",
            "g1;-5",
            "g1;0",
            ";A;10",
            "g1;;10",
            "g1;A;7",
        )

        assertEquals(2, result.operations.size)
        assertEquals(listOf(2, 3, 4, 5, 6, 7), result.invalidLines.map { it.lineNumber })
    }

    @Test
    fun `BOM в начале файла не ломает разбор`() {
        val result = read("﻿g1;A;10")

        assertEquals(
            listOf(Operation.Receipt(lineNumber = 1, groupId = "g1", productId = "A", quantity = 10)),
            result.operations,
        )
    }
}
