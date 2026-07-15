package org.example.stock

import org.example.stock.csv.OperationCsvReader
import org.example.stock.csv.ResultCsvWriter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.assertEquals

class SamplesTest {

    @TempDir
    lateinit var dir: Path

    @Test
    fun `пример из README воспроизводится`() {
        assertSampleMatches("operations.csv", "result.csv")
    }

    @Test
    fun `пример с краевыми случаями воспроизводится`() {
        assertSampleMatches("operations-edge-cases.csv", "result-edge-cases.csv")
    }

    private fun assertSampleMatches(inputName: String, expectedName: String) {
        val samples = Path.of("samples")
        val output = dir.resolve(expectedName)

        val operations = OperationCsvReader().read(samples.resolve(inputName)).operations
        ResultCsvWriter().writeMovements(output, StockCalculator().applyAll(operations))

        assertEquals(samples.resolve(expectedName).readText(), output.readText())
    }
}
