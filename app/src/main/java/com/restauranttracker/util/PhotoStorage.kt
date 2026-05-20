package com.restauranttracker.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

private const val TAG = "PhotoStorage"
private const val PHOTOS_DIR = "photos"

/**
 * Copies image content from external URIs into the app's private files directory and hands
 * back the resulting absolute file paths, which are what gets persisted on a [Restaurant].
 *
 * Why copy: shared/MediaStore URIs revoke access on process restart. A local file path is
 * stable and survives anything short of the user clearing app data.
 */
class PhotoStorage(private val context: Context) {

    private val baseDir: File =
        File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }

    /** Reserve a path for a new photo about to be captured by the camera. */
    fun newCameraTargetUri(): Pair<Uri, String> {
        val file = File(baseDir, "${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return uri to file.absolutePath
    }

    /** Copy a chosen photo URI (gallery / picker) into local storage; returns the new path. */
    fun importFrom(uri: Uri): String? {
        val target = File(baseDir, "${UUID.randomUUID()}.jpg")
        return try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Could not open input for $uri" }
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import photo from $uri", e)
            target.delete()
            null
        }
    }

    /** Best-effort cleanup. Missing files are fine — Coil renders a placeholder. */
    fun delete(paths: Iterable<String>) {
        paths.forEach { path ->
            runCatching { File(path).delete() }
                .onFailure { Log.w(TAG, "Failed to delete $path", it) }
        }
    }
}
