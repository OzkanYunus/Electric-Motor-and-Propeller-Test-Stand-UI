interface SensorProperties {
    val chartTitle: String
    val chartXLabel: String
    val chartYLabel: String
    val lowerRange: Double
    val upperRange: Double
    val prefix: String
}

sealed class Sensor: SensorProperties {
    object IR : Sensor() {
        override val chartTitle = "IR Chart"
        override val chartXLabel = "x"
        override val chartYLabel = "y"
        override val lowerRange = 0.0
        override val upperRange = 1.0
        override val prefix = "IR:"
    }
    object LC : Sensor() {
        override val chartTitle = "LoadCell Chart"
        override val chartXLabel = "x"
        override val chartYLabel = "y"
        override val lowerRange = 0.0
        override val upperRange = 2000.0
        override val prefix = "LC:"
    }
    object Voltage : Sensor() {
        override val chartTitle = "Voltage Chart"
        override val chartXLabel = "x"
        override val chartYLabel = "y"
        override val lowerRange = 0.0
        override val upperRange = 2.0
        override val prefix = "V:"
    }
    object Current : Sensor() {
        override val chartTitle = "Current Chart"
        override val chartXLabel = "x"
        override val chartYLabel = "y"
        override val lowerRange = 0.0
        override val upperRange = 5.0
        override val prefix = "C:"
    }
}
