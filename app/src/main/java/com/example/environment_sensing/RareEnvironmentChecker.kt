package com.example.environment_sensing

data class RareEnvironment(
    val name: String,
    val predicate: (SensorData) -> Boolean,
    val requireDurationMillis: Long = 60_000,
    val tier: Int = 1
)

object RareEnvironmentChecker {

    data class RareEnvironment(
        val name: String,
        val predicate: (SensorData) -> Boolean,
        val requireDurationMillis: Long = 60_000,
        val tier: Int = 1
    )

    private val startedAt = mutableMapOf<String, Long>()

    val environments = listOf(
        RareEnvironment("熱帯低気圧レア環境", {
            it.pressure <= 990 && it.temperature >= 30
        }, requireDurationMillis = 30_000, tier = 2),

        RareEnvironment("クラブわいわいレア環境", {
            it.light <= 30 && it.noise >= 85
        }, requireDurationMillis = 15_000, tier = 1),

        RareEnvironment("工事現場みたいなレア環境", {
            it.noise >= 100 && it.tvoc >= 250
        }, requireDurationMillis = 30_000, tier = 1),

        RareEnvironment("真夏の密室レア環境", {
            it.temperature >= 32 && it.co2 >= 1200
        }, requireDurationMillis = 30_000, tier = 1),

        RareEnvironment("南国リゾートレア環境", {
            it.temperature in 28.0..32.0 && it.light >= 500 && it.noise in 50.0..65.0
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("星空キャンプレア環境", {
            it.light < 20 && it.noise < 40.0 && it.temperature in 10.0..20.0
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("勉強はかどる集中レア環境", {
            it.noise in 40.0..55.0 && it.light in 300..600
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("カラオケ大会レア環境", {
            it.noise in 90.0..120.0 && it.co2 >= 1000
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("焚き火レア環境", {
            it.light in 50..150 && it.tvoc >= 180 && it.co2 >= 1000
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("電車ラッシュレア環境", {
            it.tvoc >= 150 && it.co2 >= 1500 && it.temperature >= 28
        }, requireDurationMillis = 90_000, tier = 1),

        RareEnvironment("映画館レア環境", {
            it.light < 20 && it.noise in 40.0..55.0 && it.co2 in 800..1500
        }, requireDurationMillis = 120_000, tier = 1),

        RareEnvironment("ととのいサウナっぽいレア環境", {
            it.temperature >= 40 && it.co2 >= 1500 && it.noise < 60.0
        }, requireDurationMillis = 30_000, tier = 1),

        RareEnvironment("めっちゃ静かレア環境", {
            it.noise < 10 && it.co2 in 300..500
        }, requireDurationMillis = 90_000, tier = 2),

        RareEnvironment("焼肉屋っぽいレア環境", {
            it.tvoc >= 300 && it.co2 >= 1200 && it.noise in 60.0..85.0 && it.temperature >= 25
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("山頂絶景レア環境", {
            it.pressure <= 900 && it.temperature in -2.0..18.0 && it.co2 <= 700
        }, requireDurationMillis = 90_000, tier = 2),

        RareEnvironment("高原さわやかレア環境", {
            it.pressure in 900.0..940.0 && it.temperature in 12.0..22.0 && it.co2 <= 800 && it.light >= 500
        }, requireDurationMillis = 90_000, tier = 1),

        RareEnvironment("まるで北極レア環境", {
            it.temperature <= 5.0 && it.noise < 45.0
        }, requireDurationMillis = 60_000, tier = 2),

        RareEnvironment("お昼の公園っぽいレア環境", {
            it.light in 500..900 && it.noise in 50.0..65.0 && it.co2 in 400..700
        }, requireDurationMillis = 90_000, tier = 1),

        RareEnvironment("放課後教室っぽいレア環境", {
            it.light in 300..700 && it.noise in 45.0..60.0 && it.co2 in 800..1200
        }, requireDurationMillis = 60_000, tier = 1),

        RareEnvironment("地下鉄ホームレア環境", {
            it.noise in 80.0..100.0 && it.light in 100..400 && it.co2 in 800..2000
        }, requireDurationMillis = 90_000, tier = 2),

        RareEnvironment("無響室レア環境", {
            it.noise < 20.0 && it.co2 in 400..700 && it.light in 100..400
        }, requireDurationMillis = 90_000, tier = 3),

        RareEnvironment("厳冬オーロラレア環境", {
            it.temperature in -20.0..0.0 && it.noise < 35.0 && it.light in 150..350 && it.co2 in 400..800
        }, requireDurationMillis = 90_000, tier = 3),

        RareEnvironment("真空スーパーレア環境", {
            it.pressure <= 100
        }, requireDurationMillis = 10_000, tier = 99),

        RareEnvironment("ブラックホール直前環境", {
            it.pressure <= 50 && it.light < 5
        }, requireDurationMillis = 10_000, tier = 99),

        RareEnvironment("火星コロニーレア環境", {
            it.pressure in 550.0..700.0 && it.temperature in -60.0..-10.0 && it.co2 >= 5000
        }, requireDurationMillis = 90_000, tier = 99)
    )

    //レア度が高い方を返す
    fun check(data: SensorData, now: Long = System.currentTimeMillis()): String? {
        val completed = mutableListOf<RareEnvironment>()

        for (env in environments) {
            if (env.predicate(data)) {
                val t0 = startedAt.getOrPut(env.name) { now }
                if (now - t0 >= env.requireDurationMillis) {
                    completed += env
                }
            } else {
                startedAt.remove(env.name)
            }
        }
        return completed.maxByOrNull { it.tier }?.name
    }
}