package com.example.environment_sensing

data class NormalEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean
)

object NormalEnvironmentChecker {
    val environments = listOf(
        NormalEnvironment("静かめ快適環境") {
            it.temperature in 22.0..26.0 && it.noise < 60.0
        },
        NormalEnvironment("涼しめ明るい環境") {
            it.temperature in 15.0..22.0 && it.light > 300 && it.noise in 55.0..75.0
        },
        NormalEnvironment("ざわざわ環境") {
            it.noise > 80.0 && it.light > 200 && it.tvoc > 100
        },
        NormalEnvironment("暗い静か環境") {
            it.light < 80 && it.noise < 45.0
        },
        NormalEnvironment("リビングまったり環境") {
            it.light in 80..250 && it.noise < 55.0
        },
        NormalEnvironment("交通量多め環境") {
            it.noise >= 70.0 && it.light >= 300
        },
        NormalEnvironment("早朝の静けさ環境") {
            it.light < 50 && it.noise < 40.0
        },
        NormalEnvironment("調理中っぽい環境") {
            it.tvoc >= 150 && it.temperature >= 24.0
        }
    )

    fun check(data: SensorData): String? =
        environments.firstOrNull { it.condition(data) }?.name
}