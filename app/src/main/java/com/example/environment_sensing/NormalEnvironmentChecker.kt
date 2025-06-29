package com.example.environment_sensing

data class NormalEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean
)

object NormalEnvironmentChecker {

    private val environments = listOf(
        NormalEnvironment("静かめ快適環境") {
            it.temperature in 20.0..26.0 &&
                    it.noise < 60.0
        },
        NormalEnvironment("涼しめ明るい環境") {
            it.temperature in 15.0..25.0 &&
                    it.light > 1000 &&
                    it.noise > 60.0
        },
        NormalEnvironment("高湿度×涼しい環境") {
            it.temperature < 26.0 &&
                    it.humidity > 70.0
        } ,
        NormalEnvironment("高温度×高湿度環境") {
            it.temperature > 28 &&
                    it.humidity > 70.0
        },
    )

    fun check(data: SensorData): String? {
        return environments.firstOrNull { it.condition(data) }?.name
    }
}