package com.example.environment_sensing

data class RareEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean
)

object RareEnvironmentChecker {

    private val environments = listOf(
        RareEnvironment("低気圧×高温レア環境") { /*台風を想定*/
            it.pressure <= 990 && it.temperature >= 30
        },
        RareEnvironment("暗い×うるさいレア環境") {/*クラブを想定*/
            it.light <= 30 && it.noise >= 80
        },
        RareEnvironment("うるさい×汚いレア環境") {/*工事現場のような環境を想定*/
            it.noise <= 150 && it.tvoc >= 250
        },
        RareEnvironment("薄暗い×有機ガスレア環境") {/*シーシャバーを想定*/
            it.light <= 30 && it.tvoc >= 200
        },
        RareEnvironment("高温×息苦しさレア環境") {/*真夏の密室を想定*/
            it.temperature >= 30 && it.co2 >= 600
        },
    )

    fun check(data: SensorData): String? {
        return environments.firstOrNull { it.condition(data) }?.name
    }
}