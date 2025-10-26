// LatLonBackfill.kt（新規）: シンプルなユーティリティ
package com.example.environment_sensing

import android.content.Context
import com.example.environment_sensing.data.AppDatabase
import kotlinx.coroutines.delay

suspend fun backfillMissingLocationsOnce(context: Context, maxItems: Int = 200) {
    val db = AppDatabase.getInstance(context)
    val lp = LocationProvider(context)

    // 位置更新ウォームアップ（権限があれば内部で startUpdates 済みのはず。必要ならここでも軽く呼んでOK）
    // lp.startUpdates() ← 任意（呼びすぎ防止したいなら省略可）

    // まず Normal を埋める
    val normals = db.normalEnvironmentLogDao().findMissingNormal(maxItems)
    for (n in normals) {
        repeat(10) { // 最大10回（計 ~10〜30秒程度の粘り）
            val g = lp.currentOrNull()
            if (g != null) {
                db.normalEnvironmentLogDao().updateLatLon(n.id, g.lat, g.lon)
                return@repeat
            }
            delay(3000)
        }
    }

    // Rare も同様に
    val rares = db.rareEnvironmentLogDao().findMissingRare(maxItems)
    for (r in rares) {
        repeat(10) {
            val g = lp.currentOrNull()
            if (g != null) {
                db.rareEnvironmentLogDao().updateLatLon(r.id, g.lat, g.lon)
                return@repeat
            }
            delay(3000)
        }
    }
}