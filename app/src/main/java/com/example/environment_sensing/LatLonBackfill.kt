package com.example.environment_sensing

import android.content.Context
import com.example.environment_sensing.data.AppDatabase
import kotlinx.coroutines.delay

suspend fun backfillMissingLocationsOnce(context: Context, maxItems: Int = 200) {
    val db = AppDatabase.getInstance(context)
    val lp = LocationProvider(context)

    val normals = db.normalEnvironmentLogDao().findMissingNormal(maxItems)
    for (n in normals) {
        repeat(10) {
            val g = lp.currentOrNull()
            if (g != null) {
                db.normalEnvironmentLogDao().updateLatLon(n.id, g.lat, g.lon)
                return@repeat
            }
            delay(3000)
        }
    }

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