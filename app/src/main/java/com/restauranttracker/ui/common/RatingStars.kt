package com.restauranttracker.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun RatingStars(
    rating: Int,
    onRatingChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
    max: Int = 5,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Row(modifier = modifier) {
        for (i in 1..max) {
            val filled = i <= rating
            if (onRatingChange != null) {
                IconButton(onClick = { onRatingChange(i) }) {
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Rate $i",
                        tint = tint,
                    )
                }
            } else {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = tint,
                )
            }
        }
    }
}
