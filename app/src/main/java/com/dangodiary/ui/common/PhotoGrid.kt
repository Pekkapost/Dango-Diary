package com.dangodiary.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

/**
 * Grid of photo thumbnails. Read-only (no remove buttons) by default; pass [onRemove] to add a
 * × overlay per cell. [captionFor], when non-null, renders the photo's caption as a small text
 * line below each thumbnail — empty captions are skipped so unlabelled photos don't get a blank
 * gap.
 */
@Composable
fun PhotoGrid(
    paths: List<String>,
    modifier: Modifier = Modifier,
    onRemove: ((String) -> Unit)? = null,
    captionFor: ((String) -> String)? = null,
) {
    if (paths.isEmpty()) return
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(paths, key = { it }) { path ->
            Column(modifier = Modifier.padding(4.dp)) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (onRemove != null) {
                        IconButton(
                            onClick = { onRemove(path) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
                val caption = captionFor?.invoke(path).orEmpty()
                if (caption.isNotBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
