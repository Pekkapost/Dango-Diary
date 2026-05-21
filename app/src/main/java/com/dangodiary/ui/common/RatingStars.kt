package com.dangodiary.ui.common

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// Muted gold. Deliberately fixed rather than themed: users expect star ratings to read as warm
// gold across apps, and dynamic-color tints can land on anything from pink to teal. Desaturated
// from the conventional bright amber so the stars don't dominate a busy list row.
private val RatingAmber = Color(0xFFCBA135)

/**
 * Rating in half-star units (0..maxStars*2). For example, 7 renders as 3.5 stars.
 * Tap the left half of a star to set a half-star value, the right half for a full star.
 */
@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
    onRatingChange: ((Int) -> Unit)? = null,
    maxStars: Int = 5,
    tint: Color = RatingAmber,
) {
    Row(modifier = modifier) {
        for (i in 1..maxStars) {
            val halfValue = i * 2 - 1
            val fullValue = i * 2
            val icon = when {
                rating >= fullValue -> Icons.Filled.Star
                rating >= halfValue -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Outlined.StarBorder
            }
            if (onRatingChange != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val picked = if (offset.x < size.width / 2f) halfValue else fullValue
                                onRatingChange(picked)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = "Rate $halfValue or $fullValue half-stars", tint = tint)
                }
            } else {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
        }
    }
}
