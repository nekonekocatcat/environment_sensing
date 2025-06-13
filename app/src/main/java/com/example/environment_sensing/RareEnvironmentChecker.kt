package com.example.environment_sensing

data class RareEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean
)

object RareEnvironmentChecker {

    private val environments = listOf(
        RareEnvironment("台風環境") {
            it.pressure <= 990 && it.temperature >= 30
        },
        RareEnvironment("クラブ環境") {
            it.light <= 30 && it.noise >= 80
        },
        RareEnvironment("砂漠環境") {
            it.humidity <= 30 && it.temperature >= 30
        },
        RareEnvironment("密室真夏") {
            it.temperature >= 30 && it.co2 >= 600
        },
        RareEnvironment("シーシャバー") {
            it.tvoc >= 500 && it.co2 >= 600
        }
    )

    fun check(data: SensorData): String? {
        return environments.firstOrNull { it.condition(data) }?.name
    }
}