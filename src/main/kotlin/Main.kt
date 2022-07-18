// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.PlotOrientation
import javax.swing.BoxLayout
import javax.swing.JPanel


private lateinit var listener: SerialPortDataListener

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview
fun App(window: ComposeWindow) {
    var serialPort by remember { mutableStateOf<SerialPort?>(null) }
    var serialPorts = getSerialPorts().map { it.systemPortName }
    var selectedPortIndex by remember { mutableStateOf(0) }
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    var sliderPosition by remember { mutableStateOf(0f) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    val bufferList by remember { mutableStateOf(mutableListOf<String>()) }
    val dataBufferList by remember { mutableStateOf(mutableListOf<String>()) }
    val usableDataList by remember { mutableStateOf(mutableListOf<SensorData>()) }

    val irChart by remember {
        mutableStateOf(
            ChartFactory.createXYLineChart(
                Sensor.IR.chartTitle,
                Sensor.IR.chartXLabel,
                Sensor.IR.chartYLabel,
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            )
        )
    }

    val lcChart by remember {
        mutableStateOf(
            ChartFactory.createXYLineChart(
                Sensor.LC.chartTitle,
                Sensor.LC.chartXLabel,
                Sensor.LC.chartYLabel,
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            )
        )
    }

    val vChart by remember {
        mutableStateOf(
            ChartFactory.createXYLineChart(
                Sensor.Voltage.chartTitle,
                Sensor.Voltage.chartXLabel,
                Sensor.Voltage.chartYLabel,
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            )
        )
    }

    val cChart by remember {
        mutableStateOf(
            ChartFactory.createXYLineChart(
                Sensor.Current.chartTitle,
                Sensor.Current.chartXLabel,
                Sensor.Current.chartYLabel,
                null,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
            )
        )
    }

    val sensorChartList = remember {
        mutableListOf(irChart, lcChart, vChart, cChart)
    }

    LaunchedEffect(key1 = Unit) {
        sensorChartList.forEach {
            val sensor: Sensor = when (it.title.text) {
                Sensor.IR.chartTitle -> Sensor.IR
                Sensor.LC.chartTitle -> Sensor.LC
                Sensor.Voltage.chartTitle -> Sensor.Voltage
                Sensor.Current.chartTitle -> Sensor.Current
                else -> Sensor.IR
            }

            val rangeAxis = it.xyPlot.rangeAxis as NumberAxis
            rangeAxis.setRange(sensor.lowerRange, sensor.upperRange)
        }

        listener = object : SerialPortDataListener {
            override fun getListeningEvents(): Int {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED or SerialPort.LISTENING_EVENT_DATA_WRITTEN or SerialPort.LISTENING_EVENT_PORT_DISCONNECTED
            }

            override fun serialEvent(event: SerialPortEvent?) {
                if (event == null) return
                when (event.eventType) {
                    SerialPort.LISTENING_EVENT_DATA_RECEIVED -> {
                        val receivedData: ByteArray = event.receivedData
                        val msg = String(receivedData)

                        if (msg.contains("q")) {
                            println(msg)
                            return
                        }

                        // This scope should only be used for appending new data to bufferList
                        // bufferList.size should not increase over time
                        CoroutineScope(Dispatchers.IO).launch {
                            bufferList.add(msg)
                        }
                    }
                    SerialPort.LISTENING_EVENT_DATA_WRITTEN -> {
                        // TODO callback for successful write event
                        println("Write event")

                    }
                    SerialPort.LISTENING_EVENT_PORT_DISCONNECTED -> {
                        if (event.serialPort.systemPortName == serialPort?.systemPortName) {
                            serialPort?.closeSerialPortAndStopListening()
                            serialPort = null
                        }
                    }
                }
            }
        }

        // Mock loop
//        launch(Dispatchers.IO) {
//            var lcBase = 0
//            while (isActive) {
//                try {
//                    val irData = 200 + lcBase + (-5..5).random()
//                    val lcData = 0 + lcBase + (-2..5).random()
//                    val vData = lcData * 0.4 + (-2..2).random()
//                    val cData = lcData * 0.3 + (-2..2).random()
//                    dataBufferList.add("<IR:${irData},LC:${lcData},V:${vData},C:${cData}>")
//                } catch (e: Exception) {
//                    println(e)
//                }
//                lcBase += 10
//                delay(1000)
//            }
//        }

        // Iterates through the Arduino bufferList and appends the raw valid data to dataBufferList
        launch(Dispatchers.IO) {
            while (isActive) {
//                delay(100)
                try {
                    processArduinoBuffer(bufferList, dataBufferList)
                } catch (e: Exception) {
                    // TODO concurrent list modification error is thrown
                    println("Error1: ${e.message}")
                }
            }
        }

        // Iterates through the dataBufferList and parses the raw valid data, appending every data to respective lists
        launch(Dispatchers.IO) {
            while (isActive) {
                delay(10)
                try {
                    processDataBuffer(
                        dataBufferList,
                        usableDataList,
                        irChart,
                        lcChart,
                        vChart,
                        cChart,
                        (sliderPosition * 1023).toInt()
                    )
                } catch (e: Exception) {
                    println("Error2: ${e.message}")
                }
            }
        }

        // Checks for available ports and handles port open/close logic
        launch(Dispatchers.IO) {
            while (isActive) {
                delay(100)
                val ports = getSerialPorts()
                ports.let {
                    // No previous/current port
                    if (serialPort == null) {
                        // New port list is not empty
                        if (it.isNotEmpty()) {
                            serialPort = it.first()
                        }
                    }
                    // Current/previous port exists
                    else {
                        // New port list is empty
                        if (it.isEmpty()) {
                            serialPort?.closeSerialPortAndStopListening()
                            serialPort = null
                        }
                        // New post list is not empty, look for port name change
                        else {
                            // Old port is not in new port list
                            val match = it.find { new -> new.systemPortName == serialPort?.systemPortName }
                            if (match == null) {
                                serialPort?.closeSerialPortAndStopListening()
                                serialPort = it[0]
                            }
                        }
                    }
                }

                serialPorts = ports.map { it.systemPortName }
                val foundIndex = serialPorts.indexOf(serialPort?.systemPortName)
                selectedPortIndex = if (foundIndex == -1) 0 else foundIndex

                serialPort?.openSerialPortAndStartListening(listener)
            }
            serialPort?.closeSerialPortAndStopListening()
        }
    }

    LaunchedEffect(key1 = serialPort) {
        // TODO callback for serialPort change
        println("SerialPort: $serialPort")
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(selected = selectedTab, onSelect = { selectedTab = it })
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart
        ) {
            // TODO split to different functions for pages
            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 80.dp, start = 8.dp, end = 8.dp)
                            .fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Column {
                                    Text(text = "Sensors:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "Voltage: ")
                                    Text(text = "Current: ")
                                    Text(text = "Power: ")
                                    Text(text = "Torque: ")
                                    Text(text = "Thrust: ")
                                    Text(text = "RPM: ")
                                }
                                Column {
                                    Text(text = "Units:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "Torque: Newton meter")
                                    Text(text = "Thrust: Kilogram force")
                                    Text(text = "Weight: Kilogram")
                                    Text(text = "Motor Speed: Rev per minute")
                                }
                                Column {
                                    Text(text = "Limits:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(text = "Maximum Torque: 30Nm")
                                    Text(text = "Maximum Voltage: 55V")
                                    Text(text = "Maximum Ampere: 100A")
                                    Text(text = "Maximum RPM: 25000 RPM")
                                    Text(text = "Maximum Diameter: 80cm/32\"")
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .border(BorderStroke((0.1).dp, Color.DarkGray))
                                        .padding(4.dp)
                                        .height(64.dp)
                                        .width(256.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Card(
//                                                modifier = Modifier.size(40.dp),
                                        shape = RoundedCornerShape(0.dp),
                                        backgroundColor = Color.White,
                                        onClick = {
                                            val message = "<q:0>"
                                            serialPort?.writeBytes(
                                                message.toByteArray(),
                                                message.toByteArray().size.toLong()
                                            )
                                        }
                                    ) {
                                        Card(
                                            modifier = Modifier.size(40.dp),
                                            shape = RoundedCornerShape(40.dp),
                                            backgroundColor = Color.Red
                                        ) {

                                        }
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text(
                                                text = "Red Button",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Disconnect",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                                Button(onClick = {
                                    saveFileDialog(window, "Save Data", usableDataList)
                                }) {
                                    Text(text = "Save CSV")
                                }

                                Row(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .clickable(onClick = { expanded = true })
                                ) {
                                    Text(text = serialPort?.systemPortName ?: "COM3")
//                                    Text(text = serialPort?.systemPortName ?: "No active ports")
                                    Box(modifier = Modifier.width(160.dp).wrapContentSize(Alignment.TopStart)) {
//                                        Text(
//                                            dropDownItems[selectedIndex],
//                                            modifier = Modifier.fillMaxWidth().clickable(onClick = { expanded = true })
//                                                .background(Color.Gray)
//                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            offset = DpOffset(4.dp, 4.dp),
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.width(180.dp)
                                        ) {
                                            serialPorts.forEachIndexed { index, s ->
                                                DropdownMenuItem(onClick = {
                                                    selectedPortIndex = index
                                                    expanded = false
                                                }) {
                                                    Text(text = s)
                                                }
                                            }
                                            if (serialPorts.isEmpty()) {
                                                DropdownMenuItem(onClick = {
                                                    expanded = false
                                                }) {
                                                    Text(text = "No Active Ports")
                                                }
                                            }
                                        }
                                    }
                                }


                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                            ) {
//                            Text(text = (sliderPosition * 1023).toInt().toString())
                                Text(
                                    text = "%${(sliderPosition * 100).toInt()}",
                                    modifier = Modifier.width(64.dp)
                                )
                                Row(modifier = Modifier.width(320.dp)) {
                                    Slider(
                                        value = sliderPosition,
                                        onValueChange = {
                                            sliderPosition = it

                                            debounceJob?.cancel()
                                            debounceJob = CoroutineScope(Dispatchers.IO).launch {
                                                delay(50)
                                                val message = "<q:${(it * 1023).toInt()}>"
                                                serialPort?.writeBytes(
                                                    message.toByteArray(),
                                                    message.toByteArray().size.toLong()
                                                )
                                            }
                                        })
                                }
                            }
                        }

                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    SwingPanel(
                                        background = Color.White,
                                        modifier = Modifier.width(400.dp).height(200.dp),
                                        factory = {
                                            JPanel().apply {
                                                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                                                val cp = ChartPanel(sensorChartList[0])
                                                cp.setMouseZoomable(false)
                                                add(cp)
                                            }
                                        }
                                    )
                                    SwingPanel(
                                        background = Color.White,
                                        modifier = Modifier.width(400.dp).height(200.dp),
                                        factory = {
                                            JPanel().apply {
                                                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                                                val cp = ChartPanel(sensorChartList[1])
                                                cp.setMouseZoomable(false)
                                                add(cp)
                                            }
                                        }
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    SwingPanel(
                                        background = Color.White,
                                        modifier = Modifier.width(400.dp).height(200.dp),
                                        factory = {
                                            JPanel().apply {
                                                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                                                val cp = ChartPanel(sensorChartList[2])
                                                cp.setMouseZoomable(false)
                                                add(cp)
                                            }
                                        }
                                    )
                                    SwingPanel(
                                        background = Color.White,
                                        modifier = Modifier.width(400.dp).height(200.dp),
                                        factory = {
                                            JPanel().apply {
                                                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                                                val cp = ChartPanel(sensorChartList[3])
                                                cp.setMouseZoomable(false)
                                                add(cp)
                                            }
                                        }
                                    )
                                }
                            }

                        }

//                        sensorChartList.forEach {
//                            item {
//                                SwingPanel(
//                                    background = Color.White,
//                                    modifier = Modifier.width(400.dp).height(200.dp),
//                                    factory = {
//                                        JPanel().apply {
//                                            layout = BoxLayout(this, BoxLayout.Y_AXIS)
//                                            val cp = ChartPanel(it)
//                                            cp.setMouseZoomable(false)
//                                            add(cp)
//                                        }
//                                    }
//                                )
//                            }
//                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(title = "Manualis Benchmark", onCloseRequest = ::exitApplication) {
        App(window)
    }
}
