package com.example.environment_sensing

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object CooldownGate {
    private val last = ConcurrentHashMap<String, Long>()
//
//    var normalCooldownMs: Long = 10 * 60_000L
//    var rareCooldownMs: Long = 15 * 60_000L
//
//    fun configureForExperiment(enabled: Boolean) {
//        if (enabled) {
//            normalCooldownMs = 2 * 60_000L
//            rareCooldownMs = 5 * 60_000L
//        } else {
//            normalCooldownMs = 10 * 60_000L
//            rareCooldownMs = 15 * 60_000L
//        }
//        last.clear() // モード切替時にクールダウン状態をリセット
//    }
//
//    fun allow(name: String, isRare: Boolean, now: Long = System.currentTimeMillis()): Boolean {
//        val cd = if (isRare) rareCooldownMs else normalCooldownMs
//        val lastTs = last[name] ?: Long.MIN_VALUE
//        return if (now - lastTs >= cd) {
//            last[name] = now
//            true
//        } else {
//            false
//        }
//    }
}

//通常モード
//ノーマル環境: 10分（600,000ms）
//レア環境: 15分（900,000ms）
//
//実験モード
//ノーマル環境: 2分（120,000ms）
//レア環境: 5分（300,000ms）