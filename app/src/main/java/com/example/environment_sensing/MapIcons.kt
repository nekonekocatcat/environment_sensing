package com.example.environment_sensing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun bitmapDescriptorFromVector(
    context: android.content.Context,
    @DrawableRes vectorResId: Int,
    @ColorInt tint: Int,
    sizeDp: Float = 48f
): BitmapDescriptor {
    val d = AppCompatResources.getDrawable(context, vectorResId)
        ?: error("Drawable not found: $vectorResId")

    val density = context.resources.displayMetrics.density
    val w = (sizeDp * density).toInt().coerceAtLeast(1)
    val h = (sizeDp * density).toInt().coerceAtLeast(1)

    // 色付け
    val tinted = d.mutate()
    DrawableCompat.setTint(tinted, tint)

    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    canvas.setDrawFilter(android.graphics.PaintFlagsDrawFilter(0,
        Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

    tinted.setBounds(0, 0, w, h)
    tinted.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bmp)
}