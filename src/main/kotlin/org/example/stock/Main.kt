package org.example.stock

import org.example.stock.csv.OperationCsvReader
import org.example.stock.csv.ResultCsvWriter
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("org.example.stock.Main")

private const val USAGE = """
Расчёт остатков товаров по списку операций.

Использование:
  stock-calculator <входной.csv> <итоговый.csv>

Некорректные строки входного файла логируются и пропускаются.
"""

private data class Config(
    val input: Path,
    val output: Path,
)

fun main(args: Array<String>) {
    val config = try {
        parseArgs(args)
    } catch (e: IllegalArgumentException) {
        log.error("Некорректные аргументы: {}", e.message)
        println(USAGE.trim())
        exitProcess(EXIT_BAD_USAGE)
    }

    try {
        run(config)
    } catch (e: Exception) {
        log.error("Не удалось обработать файл {}: {}", config.input, e.message, e)
        exitProcess(EXIT_FAILURE)
    }
}

private fun run(config: Config) {
    log.info("Входной файл: {}", config.input.toAbsolutePath())

    val (operations, invalidLines) = OperationCsvReader().read(config.input)

    val calculator = StockCalculator()
    val movements = calculator.applyAll(operations)
    val balances = calculator.balances()

    ResultCsvWriter().writeMovements(config.output, movements)

    log.info(
        "Готово: операций {}, движений {}, пропущено строк {}. Итоговые остатки:",
        operations.size, movements.size, invalidLines.size,
    )
    for (balance in balances) {
        log.info("  группа '{}', товар '{}': {}", balance.groupId, balance.productId, balance.balance)
    }
}

private fun parseArgs(args: Array<String>): Config {
    require(args.size == 2) {
        "ожидалось 2 аргумента: входной и итоговый файл"
    }

    val input = Path.of(args[0])
    require(input.exists() && input.isRegularFile()) { "входной файл '$input' не найден" }

    return Config(input, Path.of(args[1]))
}

private const val EXIT_FAILURE = 1
private const val EXIT_BAD_USAGE = 2
