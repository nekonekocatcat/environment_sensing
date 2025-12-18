package com.example.environment_sensing

data class NormalEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean
)

object NormalEnvironmentChecker {

    val environments = listOf(
        NormalEnvironment("静かめ快適環境") {
            it.temperature in 22.0..26.0 && it.noise in 35.0..55.0 && it.co2 in 400..900
        },
        NormalEnvironment("リビングまったり環境") {
            it.light in 80..250 && it.noise in 35.0..55.0 && it.temperature in 20.0..27.0
        },
        NormalEnvironment("夜ふかしの薄暗い部屋") {
            it.light in 20..80 && it.noise in 30.0..50.0 && it.temperature in 18.0..26.0
        },
        NormalEnvironment("早朝の静けさ環境") {
            it.light < 50 && it.noise < 40.0 && it.co2 in 400..900
        },
        NormalEnvironment("空気こもり気味環境") {
            it.co2 in 1200..1800 && it.noise < 70.0 && it.light in 50..400
        },

        NormalEnvironment("作業はかどり環境") {
            it.light in 250..700 && it.noise in 35.0..55.0 && it.co2 in 400..1000
        },
        NormalEnvironment("集中できないザワザワ環境") {
            it.noise in 70.0..85.0 && it.light in 150..600 && it.co2 in 600..1400
        },

        NormalEnvironment("明るい屋外っぽい環境") {
            it.light >= 900 && it.noise in 45.0..80.0 && it.co2 <= 900
        },
        NormalEnvironment("交通量多め環境") {
            it.noise in 80.0..95.0 && it.light >= 300 && it.tvoc in 0..260
        },

        NormalEnvironment("カフェっぽい環境") {
            it.light in 120..350 && it.noise in 55.0..70.0 && it.co2 in 700..1300
        },
        NormalEnvironment("フードコートっぽい環境") {
            it.noise in 70.0..88.0 && it.co2 in 900..1700 && it.light in 150..600
        },


        NormalEnvironment("調理中っぽい環境") {
            it.tvoc in 150..300 && it.temperature >= 24.0 && it.noise in 35.0..75.0
        },

        NormalEnvironment("カラカラ環境") {
            it.humidity <= 30.0 && it.temperature in 18.0..26.0
        },
        NormalEnvironment("じめじめ環境") {
            it.humidity >= 65.0 && it.temperature in 20.0..28.0
        },
        NormalEnvironment("冷房つよめ環境") {
            it.temperature in 16.0..20.5 && it.light in 100..600
        },

        NormalEnvironment("暗い静か環境") {
            it.light < 80 && it.noise < 45.0
        },
        NormalEnvironment("ざわざわ環境") {
            it.noise > 80.0 && it.light > 200 && it.tvoc > 100
        },
        NormalEnvironment("涼しめ明るい環境") {
            it.temperature in 15.0..22.0 && it.light > 300 && it.noise in 55.0..75.0
        }
    )

    fun check(data: SensorData): String? =
        environments.firstOrNull { it.condition(data) }?.name
}