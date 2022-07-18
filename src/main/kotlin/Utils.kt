import androidx.compose.ui.awt.ComposeWindow
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.awt.FileDialog
import java.nio.file.Files
import java.nio.file.Paths

fun saveFileDialog(window: ComposeWindow, title: String, data: MutableList<SensorData>) {
    val fileDialog = FileDialog(window, title, FileDialog.SAVE).apply {
        isMultipleMode = false
        file = "data.csv"
        isVisible = true
    }

    if (fileDialog.file != null) {
        val path = "${fileDialog.directory}${fileDialog.file}"
        val writer = Files.newBufferedWriter(Paths.get(path))
        val csvFormat = CSVFormat.Builder.create().apply {
            setHeader("Time", "IR", "LC")
        }.build()
        val csvPrinter = CSVPrinter(writer, csvFormat)

        data
            .takeLast(data.size)
            .groupBy {
                it.time.substring(11, 19)
            }
            .forEach { group ->
                val irAverage = group.value.map { it.IR }.average()
                val lcAverage = group.value.map { it.LC }.average()

                try {
                    csvPrinter.printRecord(
                        group.key,
                        String.format("%.3f", irAverage),
                        String.format("%.3f", lcAverage),
                    )
                } catch (_: Exception) {

                }
            }

        csvPrinter.flush()
        csvPrinter.close()
    }
}