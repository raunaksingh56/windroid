/*
 * WinDroid - USB Writer
 * Created by Raunak Singh
 *
 * Uses Android Storage Access Framework (no root required).
 * Writes Windows setup files + autounattend.xml to USB OTG drive.
 * Handles FAT32 4GB split for install.wim automatically.
 * Delegates boot sector setup to HybridBootManager (3-level fallback).
 */

package com.raunaksingh.windroid.core

import android.content.Context
import android.net.Uri
import java.io.*

sealed class FlashEvent {
    data class Progress(val percent: Int, val message: String) : FlashEvent()
    data class Log(val message: String) : FlashEvent()
    data class BootLevelDetected(val level: BootLevel, val message: String) : FlashEvent()
    object Success : FlashEvent()
    data class Error(val message: String) : FlashEvent()
}

class UsbWriter(private val context: Context) {

    companion object {
        const val MAX_FAT32_FILE_SIZE = 4_000_000_000L
        const val BUFFER_SIZE         = 8 * 1024 * 1024
    }

    private val bootManager = HybridBootManager(context)

    /**
     * Main flash pipeline:
     *  1. Open ISO
     *  2. Extract all files to USB via SAF
     *  3. Split install.wim > 4 GB automatically
     *  4. Inject autounattend.xml tweaks
     *  5. Apply best available hybrid boot (auto-detects root)
     */
    fun flashToUsb(
        isoUri: Uri,
        usbUri: Uri,
        autounattendXml: String,
        onEvent: (FlashEvent) -> Unit
    ) {
        try {
            onEvent(FlashEvent.Log("WinDroid flash started — Created by Raunak Singh"))
            onEvent(FlashEvent.Progress(2, "Opening ISO..."))

            context.contentResolver.openInputStream(isoUri)
                ?: throw IOException("Cannot open ISO file")

            onEvent(FlashEvent.Progress(5, "Parsing ISO structure..."))
            val isoParser = IsoExtractor(context, isoUri)

            onEvent(FlashEvent.Progress(10, "Preparing USB drive..."))
            val safWriter = SafUsbWriter(context, usbUri)

            // ── Step 1: Extract ISO files ──────────────────────────────────
            isoParser.extractAll(
                destination = safWriter,
                onProgress  = { pct, msg ->
                    onEvent(FlashEvent.Progress(10 + (pct * 0.72).toInt(), msg))
                },
                onLog = { onEvent(FlashEvent.Log(it)) }
            )

            // ── Step 2: Inject autounattend.xml ────────────────────────────
            onEvent(FlashEvent.Progress(83, "Injecting Windows tweaks..."))
            onEvent(FlashEvent.Log("Writing autounattend.xml..."))
            safWriter.writeFile("autounattend.xml", autounattendXml.toByteArray())
            onEvent(FlashEvent.Log("✓ autounattend.xml written"))

            // ── Step 3: Hybrid boot setup (3-level, auto-detects root) ─────
            onEvent(FlashEvent.Progress(88, "Setting up hybrid boot..."))

            val blockDevice = if (bootManager.checkRoot()) {
                bootManager.findUsbBlockDevice { onEvent(FlashEvent.Log(it)) }
            } else null

            val bootResult = bootManager.applyBestBootMethod(
                usbBlockDevice = blockDevice,
                usbWriter      = safWriter,
                onLog          = { onEvent(FlashEvent.Log(it)) }
            )

            onEvent(FlashEvent.BootLevelDetected(bootResult.level, bootResult.message))
            onEvent(FlashEvent.Log("✓ ${bootResult.message}"))

            // ── Done ───────────────────────────────────────────────────────
            onEvent(FlashEvent.Progress(98, "Finalising..."))
            safWriter.flush()
            onEvent(FlashEvent.Progress(100, "Complete!"))
            onEvent(FlashEvent.Success)

        } catch (e: Exception) {
            onEvent(FlashEvent.Error(e.message ?: "Unknown error"))
        }
    }
}

/**
 * Storage Access Framework based USB writer
 * Works completely without root using Android's DocumentFile API
 */
class SafUsbWriter(private val context: Context, private val rootUri: Uri) {

    private val resolver = context.contentResolver

    fun writeFile(relativePath: String, data: ByteArray) {
        val parts  = relativePath.replace("\\", "/").split("/")
        var dirUri = rootUri

        // Create nested directories
        for (i in 0 until parts.size - 1) {
            dirUri = getOrCreateDir(dirUri, parts[i])
        }

        val fileName = parts.last()
        val mimeType = guessMime(fileName)
        val fileUri  = createFile(dirUri, mimeType, fileName)

        resolver.openOutputStream(fileUri)?.use { out ->
            out.write(data)
        }
    }

    fun writeStream(relativePath: String, stream: InputStream, totalBytes: Long,
                    onProgress: (Long) -> Unit) {
        val parts  = relativePath.replace("\\", "/").split("/")
        var dirUri = rootUri

        for (i in 0 until parts.size - 1) {
            dirUri = getOrCreateDir(dirUri, parts[i])
        }

        val fileName = parts.last()
        val fileUri  = createFile(dirUri, guessMime(fileName), fileName)

        resolver.openOutputStream(fileUri)?.use { out ->
            val buf     = ByteArray(UsbWriter.BUFFER_SIZE)
            var written = 0L
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                out.write(buf, 0, read)
                written += read
                onProgress(written)
            }
        }
    }

    fun flush() { /* SAF auto-flushes */ }

    private fun getOrCreateDir(parent: Uri, name: String): Uri {
        // Try to find existing dir first
        // Simplified: use DocumentFile API
        return android.provider.DocumentsContract.createDocument(
            resolver, parent,
            android.provider.DocumentsContract.Document.MIME_TYPE_DIR,
            name
        ) ?: parent
    }

    private fun createFile(parent: Uri, mime: String, name: String): Uri {
        return android.provider.DocumentsContract.createDocument(
            resolver, parent, mime, name
        ) ?: throw IOException("Failed to create file: $name")
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".xml")  -> "text/xml"
        name.endsWith(".efi")  -> "application/octet-stream"
        name.endsWith(".wim")  -> "application/octet-stream"
        name.endsWith(".swm")  -> "application/octet-stream"
        name.endsWith(".dll")  -> "application/octet-stream"
        name.endsWith(".ini")  -> "text/plain"
        name.endsWith(".cfg")  -> "text/plain"
        else                   -> "application/octet-stream"
    }
}
