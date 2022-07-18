import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener

//const val BAUD_RATE = 9600
const val BAUD_RATE = 57600

fun getSerialPorts(): Array<SerialPort> {
    return SerialPort.getCommPorts()
}

fun getSerialPortNames(): String {
    return SerialPort.getCommPorts().joinToString(separator = ",") {
        it.systemPortName
    }
}

fun SerialPort.openSerialPort() {
    if (!isOpen) {
        openPort()
    }
}

fun SerialPort.closeSerialPort() {
    if (isOpen) {
        closePort()
    }
}

fun SerialPort.startListening(listener: SerialPortDataListener) {
    setComPortParameters(BAUD_RATE, 8, 1, SerialPort.NO_PARITY)
    setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0)
    addDataListener(listener)
}

fun SerialPort.stopListening() {
    removeDataListener()
}

fun SerialPort.openSerialPortAndStartListening(listener: SerialPortDataListener) {
    openSerialPort()
    startListening(listener)
}

fun SerialPort.closeSerialPortAndStopListening() {
    closeSerialPort()
    stopListening()
}