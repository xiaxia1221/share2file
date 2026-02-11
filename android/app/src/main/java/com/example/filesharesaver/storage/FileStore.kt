package com.example.filesharesaver.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileStore {
    private const val SHARED_DIR = "shared_files"

    fun saveUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val fileName = getFileName(context, uri) ?: "unnamed_file_${System.currentTimeMillis()}"
        
        val targetDir = File(context.filesDir, SHARED_DIR)
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetFile = getUniqueFile(targetDir, fileName)
        
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            targetFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                name = name?.substring(cut + 1)
            }
        }
        return name
    }

    private fun getUniqueFile(directory: File, fileName: String): File {
        var file = File(directory, fileName)
        if (!file.exists()) return file

        val nameWithoutExt = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "")
        val suffix = if (extension.isNotEmpty()) ".$extension" else ""

        var counter = 1
        while (file.exists()) {
            val newName = "$nameWithoutExt ($counter)$suffix"
            file = File(directory, newName)
            counter++
        }
        return file
    }

    fun getAllFiles(context: Context): List<File> {
        val targetDir = File(context.filesDir, SHARED_DIR)
        return targetDir.listFiles()?.toList() ?: emptyList()
    }
}
