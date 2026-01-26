package com.h2.wellspend.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import android.content.Context
import android.widget.Toast

suspend fun performWiggle(offsetX: Animatable<Float, *>, width: Float, context: Context, message: String = "Swipe left/right for options") {
    val wiggleDistance = width / 4f // Move 25% of the action width
    
    // Wiggle Right
    offsetX.animateTo(wiggleDistance, tween(100))
    // Wiggle Left
    offsetX.animateTo(-wiggleDistance, tween(100))
    // Back to Center
    offsetX.animateTo(0f, tween(100))
    
    // Toast
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
