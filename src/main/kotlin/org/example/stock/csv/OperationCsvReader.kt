package org.example.stock.csv

import org.example.stock.model.Operation
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.useLines

/** Строка входного файла, которую не удалось разобрать. */
data class InvalidLine(val lineNumber: Int, val content: String, val reason: String)

data class ParseResult(
    val operations: List<Operation>,
    val invalidLines: List<InvalidLine>,
)

private class InvalidCsvException(lineNumber: Int, content: String, reason: String) :
    IllegalArgumentException("Строка $lineNumber: $reason — '$content'")

/**
 * Читает CSV с операциями.
 *
 * Формат строки определяется числом колонок:
 *  - 3 колонки `<id группы>;<id товара>;<количество>` — поступление;
 *  - 2 колонки `<id группы>;<количество>` — продажа.
 *
 * Пустые строки и строки-комментарии (`#`) пропускаются, как и строка заголовка,
 * если она есть. Некорректные строки логируются, пропускаются и попадают
 * в [ParseResult.invalidLines].
 */
class OperationCsvReader {

    private val log = LoggerFactory.getLogger(javaClass)

    fun read(path: Path): ParseResult =
        path.useLines(StandardCharsets.UTF_8) { read(it) }

    fun read(lines: Sequence<String>): ParseResult {
        val operations = mutableListOf<Operation>()
        val invalidLines = mutableListOf<InvalidLine>()
        var lineNumber = 0

        for (rawLine in lines) {
            lineNumber++
            val line = rawLine.removePrefix(BOM).trim()

            if (line.isEmpty() || line.startsWith('#')) continue
            if (lineNumber == 1 && isHeader(line)) {
                log.info("Строка 1 распознана как заголовок и пропущена: '{}'", line)
                continue
            }

            try {
                operations += parse(lineNumber, line)
            } catch (e: InvalidCsvException) {
                log.warn("Строка {} пропущена: {}", lineNumber, e.message)
                invalidLines += InvalidLine(lineNumber, line, e.message.orEmpty())
            }
        }

        log.info("Прочитано операций: {}, пропущено некорректных строк: {}", operations.size, invalidLines.size)
        return ParseResult(operations, invalidLines)
    }

    private fun parse(lineNumber: Int, line: String): Operation {
        val fields = line.split(DELIMITER).map { it.trim() }
        return when (fields.size) {
            RECEIPT_FIELDS -> {
                val (groupId, productId, quantity) = fields
                Operation.Receipt(
                    lineNumber = lineNumber,
                    groupId = requireId(lineNumber, line, groupId, "id группы"),
                    productId = requireId(lineNumber, line, productId, "id товара"),
                    quantity = requireQuantity(lineNumber, line, quantity),
                )
            }

            SALE_FIELDS -> Operation.Sale(
                lineNumber = lineNumber,
                groupId = requireId(lineNumber, line, fields[0], "id группы"),
                quantity = requireQuantity(lineNumber, line, fields[1]),
            )

            else -> throw InvalidCsvException(
                lineNumber, line,
                "ожидалось $SALE_FIELDS колонки (продажа) или $RECEIPT_FIELDS (поступление), получено ${fields.size}",
            )
        }
    }

    private fun requireId(lineNumber: Int, line: String, value: String, name: String): String =
        value.ifBlank { throw InvalidCsvException(lineNumber, line, "$name не может быть пустым") }

    private fun requireQuantity(lineNumber: Int, line: String, value: String): Long {
        val quantity = value.toLongOrNull()
            ?: throw InvalidCsvException(lineNumber, line, "количество '$value' не является целым числом")
        if (quantity <= 0) {
            throw InvalidCsvException(lineNumber, line, "количество должно быть положительным, получено $quantity")
        }
        return quantity
    }

    private fun isHeader(line: String): Boolean =
        line.split(DELIMITER).last().trim().toLongOrNull() == null

    private companion object {
        const val DELIMITER = ';'
        const val BOM = "﻿"
        const val SALE_FIELDS = 2
        const val RECEIPT_FIELDS = 3
    }
}
