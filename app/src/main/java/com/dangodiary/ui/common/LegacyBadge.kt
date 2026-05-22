package com.dangodiary.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dangodiary.R

/** Brown shared by the edit-screen FilterChip (when selected) and the list-row badge so the
 *  two read as the same tag regardless of the active app theme. Medium brown — readable
 *  against any of the in-app theme backgrounds. */
val LegacyBadgeBg: Color = Color(0xFF8D6E63)

/** Small rounded brown pill labelled "Legacy". Shown on a list row under the date when the
 *  entry's [com.dangodiary.data.Entry.isLegacy] flag is on. */
@Composable
fun LegacyBadge(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .background(LegacyBadgeBg, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.list_legacy_badge),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
