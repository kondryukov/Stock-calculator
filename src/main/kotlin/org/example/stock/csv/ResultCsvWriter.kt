package org.example.stock.csv

import org.example.stock.model.StockMovement
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createParentDirectories

class ResultCsvWriter {

    private val log = LoggerFactory.getLogger(javaClass)

    fun writeMovements(path: Path, movements: List<StockMovement>) {
        path.toAbsolutePath().createParentDirectories()
        path.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.appendLine(HEADER)
            for (movement in movements) {
                val fields = listOf(
                    movement.operationNumber.toString(),
                    movement.type.name,
                    movement.groupId,
                    movement.productId,
                    movement.quantity.toString(),
                    movement.balance.toString(),
                )
                writer.appendLine(fields.joinToString(DELIMITER.toString()) { escape(it) })
            }
        }
        log.info("Записан файл движений: {} ({} строк)", path, movements.size)
    }

    private fun escape(value: String): String =
        if (value.any { it == DELIMITER || it == '"' || it == '\n' || it == '\r' }) {
            '"' + value.replace("\"", "\"\"") + '"'
        } else {
            value
        }

    private companion object {
        const val DELIMITER = ';'
        val HEADER = listOf("operation_number", "type", "group_id", "product_id", "quantity", "balance")
            .joinToString(DELIMITER.toString())
    }
}
