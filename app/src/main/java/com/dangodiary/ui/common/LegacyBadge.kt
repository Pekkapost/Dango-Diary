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
import androidx.compose.ui.unit.sp
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
            .background(LegacyBadgeBg, RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp),
    ) {
        Text(
            text = stringResource(R.string.list_legacy_badge),
            color = Color.White,
            // labelSmall with a tighter font size — the default labelSmall (~11sp) read a bit
            // large when overlaid on the list thumbnail; 9sp tucks neatly into the corner
            // without losing legibility.
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        )
    }
}
