package com.example.environment_sensing

data class NormalEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean
)

object NormalEnvironmentChecker {

    private val environments = listOf(
        NormalEnvironment("静かめ快適環境") {
            it.temperature in 22.0..26.0 &&
                    it.noise < 60.0
        },
        NormalEnvironment("涼しめ明るい環境") {
            it.temperature in 15.0..20.0 &&
                    it.light > 300 &&
                    it.noise > 60.0
        },
        NormalEnvironment("ざわざわ環境") {
            it.noise > 120.0 &&
                    it.light > 200.0 && it.tvoc > 100
        } ,
        NormalEnvironment("暗い静か環境") {
            it.light < 80 &&
                    it.noise < 70.0
        },
        /*NormalEnvironment("チェック用") {
                    it.humidity > 70.0
        },*/
    )

    fun check(data: SensorData): String? {
        return environments.firstOrNull { it.condition(data) }?.name
    }
}