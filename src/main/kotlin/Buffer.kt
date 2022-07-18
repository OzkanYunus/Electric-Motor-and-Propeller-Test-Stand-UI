import org.jfree.chart.JFreeChart
import org.jfree.data.xy.DefaultXYDataset
import org.jfree.data.xy.XYDataset
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// FIXME does not process data right
fun processArduinoBuffer(bufferList: MutableList<String>, dataBufferList: MutableList<String>) {
    if (bufferList.isEmpty()) return

    val startIndex = bufferList.indexOfFirst { it.contains("<") }
    if (startIndex == -1) return

    val endIndex = bufferList.indexOfFirst { it.contains(">") }
    if (endIndex == -1) return

    var serialData = ""
    val removeCount: Int

    if (endIndex > startIndex) {
        val dataList = bufferList.subList(startIndex, endIndex + 1)
        val newData = dataList.joinToString(separator = "")

        val dataStart = newData.indexOf('<')
        val dataEnd = newData.indexOf('>')

        serialData = newData.substring(dataStart, dataEnd + 1)

        val left = newData.substring(dataEnd + 1)
        if (left.contains("<")) {
            bufferList[endIndex] = left
        }

        removeCount = endIndex
    } else if (endIndex == startIndex) {
        val item = bufferList[endIndex]
        val start = item.indexOf('<')
        val end = item.indexOf('>')
        if (start > end) {
            serialData = item.substring(start, end + 1)
            bufferList[endIndex] = item.substring(end + 1)
        } else {
            bufferList[endIndex] = item.substringAfter(">")
        }
        removeCount = endIndex
    } else {
        removeCount = startIndex + 1
    }

    repeat(removeCount) { bufferList.removeFirst() }

    if (serialData.isBlank()) return
    dataBufferList.add(serialData)

//    println("Buffer: $bufferList")
//    println("Buffer size: ${bufferList.size}")
//    println("Data: $serialData")
//    println("Datalist: ${dataBufferList.size}")
//    println()
}

fun processDataBuffer(
    dataBufferList: MutableList<String>,
    usableDataList: MutableList<SensorData>,
    irChart: JFreeChart,
    lcChart: JFreeChart,
    vChart: JFreeChart,
    cChart: JFreeChart,
    potData: Int,
) {
    val irList = mutableListOf<Int>()
    val lcList = mutableListOf<Double>()
    val vList = mutableListOf<Double>()
    val cList = mutableListOf<Double>()

    dataBufferList
        .takeLast(500)
        .forEach { s ->
            val value = s.trimStart('<').trimEnd('>').split(",")

            // TODO there is a parsing problem in the first buffer loop
            if (value.size != 4) {
//                println(s)
                return@forEach
            }
            val irValue = value[0].substringAfter(Sensor.IR.prefix).toIntOrNull()
            val lcValue = value[1].substringAfter(Sensor.LC.prefix).toDoubleOrNull()
            val vValue = value[2].substringAfter(Sensor.Voltage.prefix).toDoubleOrNull()
            val cValue = value[3].substringAfter(Sensor.Current.prefix).toDoubleOrNull()

            if (irValue == null) return
            if (lcValue == null) return
            if (vValue == null) return
            if (cValue == null) return
            irList.add(irValue)
            lcList.add(lcValue)
            vList.add(vValue)
            cList.add(cValue)

            val myDateObj = LocalDateTime.now()
            val myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            val formattedDate = myDateObj.format(myFormatObj)
            usableDataList.add(SensorData(formattedDate, irValue, lcValue, lcValue, cValue))
        }

    val newIrData = createDataset(irList.map { it.toDouble() }.toDoubleArray())
    irChart.xyPlot.dataset = newIrData

    val newLcData = createDataset(lcList.map { it }.toDoubleArray())
    lcChart.xyPlot.dataset = newLcData

    val newVData = createDataset(vList.map { it }.toDoubleArray())
    vChart.xyPlot.dataset = newVData

    val newCData = createDataset(cList.map { it }.toDoubleArray())
    cChart.xyPlot.dataset = newCData
}

fun createDataset(list: DoubleArray): XYDataset {
    val ds = DefaultXYDataset()
    val data = arrayOf(list.mapIndexed { index, _ -> index.toDouble() }.toDoubleArray(), list)
    ds.addSeries("Series 1", data)
    return ds
}
