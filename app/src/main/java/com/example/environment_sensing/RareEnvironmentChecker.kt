package com.example.environment_sensing

data class RareEnvironment(
    val name: String,
    val condition: (SensorData) -> Boolean,
    val requireDurationMillis: Long // 継続必要時間（ms）
)

object RareEnvironmentChecker {
    val environments = listOf(
        // --- 悪環境系 ---
        RareEnvironment("低気圧×高温レア環境", {
            it.pressure <= 990 && it.temperature >= 30
        }, requireDurationMillis = 120_000), // 台風・熱帯夜

        RareEnvironment("暗い×うるさいレア環境", {
            it.light <= 30 && it.noise >= 85
        }, requireDurationMillis = 10_000), // クラブ・ライブハウス

        RareEnvironment("うるさい×汚いレア環境", {
            it.noise >= 100 && it.tvoc >= 250
        }, requireDurationMillis = 90_000), // 工事現場

        RareEnvironment("高温×息苦しさレア環境", {
            it.temperature >= 32 && it.co2 >= 1200
        }, requireDurationMillis = 90_000), // 真夏の密室

        // --- 良い環境系 ---
        RareEnvironment("南国リゾートレア環境", {
            it.temperature in 28.0..32.0 && it.light >= 500 && it.noise in 50.0..65.0
        }, requireDurationMillis = 60_000), // ビーチリゾート

        RareEnvironment("星空キャンプレア環境", {
            it.light < 20 && it.noise < 40.0 && it.temperature in 10.0..20.0
        }, requireDurationMillis = 90_000), // 静かな夜のキャンプ場

        RareEnvironment("勉強はかどる集中レア環境", {
            it.noise in 40.0..55.0 && it.light in 300..600
        }, requireDurationMillis = 60_000), // 図書館・自習室

        // --- ユーモア枠 ---
        RareEnvironment("カラオケ大会レア環境", {
            it.noise in 90.0..120.0 && it.co2 >= 1000
        }, requireDurationMillis = 60_000), // 酸欠気味の大騒ぎ

        RareEnvironment("焚き火レア環境", {
            it.light in 50..150 && it.noise in 40.0..55.0 && it.tvoc >= 180
        }, requireDurationMillis = 120_000), // パチパチ音＋煙イメージ

        RareEnvironment("電車ラッシュレア環境", {
            it.noise >= 85 && it.co2 >= 1500 && it.temperature >= 28
        }, requireDurationMillis = 90_000), // 満員電車地獄

        RareEnvironment("映画館レア環境", {
            it.light < 20 && it.noise in 40.0..55.0 && it.co2 in 800..1500
        }, requireDurationMillis = 120_000), // 暗くて静か、ちょっと二酸化炭素多め

        // 実験用レア環境
        RareEnvironment("めちゃ明るいレア環境（実験用）", {
            it.light >= 800
        }, requireDurationMillis = 5_000)

    )

    private val startedAt = mutableMapOf<String, Long>()

    fun check(data: SensorData, now: Long = System.currentTimeMillis()): String? {
        var matched: String? = null
        environments.forEach { env ->
            if (env.condition(data)) {
                val t0 = startedAt.getOrPut(env.name) { now }
                if (now - t0 >= env.requireDurationMillis) {
                    matched = env.name
                }
            } else {
                startedAt.remove(env.name)
            }
        }
        return matched
    }
}