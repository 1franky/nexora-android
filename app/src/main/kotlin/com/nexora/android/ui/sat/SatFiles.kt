package com.nexora.android.ui.sat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File

/**
 * Nombre "legible" de un documento elegido con el selector del sistema (SAF) —
 * usado tanto para mostrarlo en la UI como para el nombre de archivo que se
 * manda en el multipart al conectar la e.firma. Distintos proveedores de
 * documentos pueden no soportar la columna DISPLAY_NAME; en ese caso se cae al
 * último segmento de la ruta del Uri, y si tampoco hay eso, a null (el
 * llamador decide un nombre por defecto).
 */
fun queryDisplayName(context: Context, uri: Uri): String? {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
    }
    return uri.lastPathSegment
}

/** Lee el contenido completo de un documento elegido con el selector del sistema. Null si el proveedor no pudo abrirlo. */
fun readBytes(context: Context, uri: Uri): ByteArray? =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }

/**
 * Escribe el XML a `cache/sat-xml/` (ver res/xml/file_paths.xml) y arma un
 * Intent.ACTION_SEND con un content:// de FileProvider — nunca un Uri
 * file:// directo, que revienta con FileUriExposedException desde Android 7.
 */
fun shareXmlIntent(context: Context, fileName: String, bytes: ByteArray): Intent {
    val dir = File(context.cacheDir, "sat-xml").apply { mkdirs() }
    val file = File(dir, fileName)
    file.writeBytes(bytes)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = "application/xml"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
