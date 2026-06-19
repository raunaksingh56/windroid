/*
 * WinDroid - ISO 9660 / UDF Extractor
 * Created by Raunak Singh
 *
 * Fully implements ISO 9660 (Primary Volume Descriptor) parsing with
 * Joliet extension support (long Unicode filenames).
 * Supports: Windows XP, 7, 8, 8.1, 10, 11 ISOs
 * Auto-splits install.wim / install.esd > 4 GB for FAT32.
 */

package com.raunaksingh.windroid.core

import android.content.Context
import android.net.Uri
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset

class IsoExtractor(private val context: Context, private val isoUri: Uri) {

    companion object {
        const val SECTOR_SIZE    = 2048
        const val PVD_SECTOR     = 16        // Primary Volume Descriptor starts here
        const val VD_TYPE_PRIMARY  = 1.toByte()
        const val VD_TYPE_JOLIET   = 2.toByte()
        const val VD_TYPE_TERMINATOR = 255.toByte()
        const val WIM_SPLIT_SIZE = 3_800_000_000L  // 3.8 GB per chunk (safe for FAT32)

        // File flags in directory record
        const val FLAG_DIRECTORY = 0x02
    }

    // ── Public entry point ─────────────────────────────────────────────────────

    fun extractAll(
        destination: SafUsbWriter,
        onProgress: (Int, String) -> Unit,
        onLog: (String) -> Unit
    ) {
        val isoStream = openIso()
        isoStream.use { stream ->
            onLog("Scanning ISO volume descriptors...")
            val descriptor = findBestVolumeDescriptor(stream, onLog)
            onLog("Using ${descriptor.type} volume descriptor (root at sector ${descriptor.rootDirSector})")

            onLog("Parsing directory tree...")
            val entries = walkDirectory(
                sector     = descriptor.rootDirSector,
                size       = descriptor.rootDirSize,
                parentPath = "",
                useJoliet  = descriptor.type == VdType.JOLIET
            )
            onLog("Found ${entries.size} files in ISO")

            var processed = 0
            entries.forEachIndexed { idx, entry ->
                val pct = ((idx.toFloat() / entries.size) * 100).toInt()
                onProgress(pct, "Copying: ${entry.name}")
                onLog("→ ${entry.fullPath} (${entry.size.toHumanSize()})")

                val entryStream = readRawEntry(entry)

                if (isInstallImage(entry) && entry.size > UsbWriter.MAX_FAT32_FILE_SIZE) {
                    val ext = if (entry.name.endsWith(".esd", ignoreCase = true)) "esd" else "wim"
                    onLog("${entry.name} is ${entry.size.toHumanSize()} — splitting for FAT32...")
                    entryStream.use { splitAndWrite(it, entry.dirPath, ext, destination, onLog) }
                } else {
                    entryStream.use { destination.writeStream(entry.fullPath, it, entry.size) {} }
                }
                processed++
            }
            onProgress(100, "Extraction complete")
        }
    }

    // ── Volume Descriptor ──────────────────────────────────────────────────────

    enum class VdType { PRIMARY, JOLIET }

    data class VolumeDescriptor(
        val type: VdType,
        val rootDirSector: Long,
        val rootDirSize:   Long
    )

    private fun findBestVolumeDescriptor(stream: InputStream, onLog: (String) -> Unit): VolumeDescriptor {
        var primary: VolumeDescriptor? = null
        var joliet:  VolumeDescriptor? = null

        var sectorIdx = PVD_SECTOR
        stream.skip(PVD_SECTOR.toLong() * SECTOR_SIZE)

        while (true) {
            val sector = ByteArray(SECTOR_SIZE)
            if (stream.read(sector) != SECTOR_SIZE) break
            sectorIdx++

            val type = sector[0]
            val id   = String(sector, 1, 5, Charsets.US_ASCII)
            if (id != "CD001") break

            when (type) {
                VD_TYPE_TERMINATOR -> break

                VD_TYPE_PRIMARY -> {
                    val rootSector = readLE32(sector, 156 + 2).toLong()
                    val rootSize   = readLE32(sector, 156 + 10).toLong()
                    primary = VolumeDescriptor(VdType.PRIMARY, rootSector, rootSize)
                    onLog("  Found Primary Volume Descriptor")
                }

                VD_TYPE_JOLIET -> {
                    // Joliet: escape sequences at offset 88, length 3
                    val esc = String(sector, 88, 3, Charsets.US_ASCII)
                    if (esc.startsWith("%/")) {
                        val rootSector = readLE32(sector, 156 + 2).toLong()
                        val rootSize   = readLE32(sector, 156 + 10).toLong()
                        joliet = VolumeDescriptor(VdType.JOLIET, rootSector, rootSize)
                        onLog("  Found Joliet Volume Descriptor (Unicode filenames)")
                    }
                }
            }
        }

        return joliet ?: primary
            ?: throw IOException("No valid ISO 9660 volume descriptor found — is this a valid Windows ISO?")
    }

    // ── Directory Walker ───────────────────────────────────────────────────────

    data class IsoEntry(
        val name:     String,
        val dirPath:  String,
        val fullPath: String,
        val sector:   Long,
        val size:     Long
    )

    private fun walkDirectory(
        sector:     Long,
        size:       Long,
        parentPath: String,
        useJoliet:  Boolean
    ): List<IsoEntry> {
        val entries  = mutableListOf<IsoEntry>()
        val dirData  = readSectors(sector, size)
        var offset   = 0

        while (offset < dirData.size) {
            val recordLen = dirData[offset].toInt() and 0xFF
            if (recordLen == 0) {
                // Move to next sector boundary
                offset = ((offset / SECTOR_SIZE) + 1) * SECTOR_SIZE
                if (offset >= dirData.size) break
                continue
            }

            val fileFlags   = dirData[offset + 25].toInt()
            val isDir       = (fileFlags and FLAG_DIRECTORY) != 0
            val fileSector  = readLE32(dirData, offset + 2).toLong()
            val fileSize    = readLE32(dirData, offset + 10).toLong()
            val nameLen     = dirData[offset + 32].toInt() and 0xFF
            val rawName     = dirData.copyOfRange(offset + 33, offset + 33 + nameLen)

            val name = when {
                nameLen == 1 && rawName[0] == 0.toByte() -> "."   // current dir
                nameLen == 1 && rawName[0] == 1.toByte() -> ".."  // parent dir
                useJoliet -> stripVersionSuffix(String(rawName, Charsets.UTF_16BE))
                else -> stripVersionSuffix(String(rawName, Charsets.US_ASCII))
            }

            if (name != "." && name != "..") {
                val fullPath = if (parentPath.isEmpty()) name else "$parentPath/$name"
                if (isDir) {
                    // Recurse
                    entries += walkDirectory(fileSector, fileSize, fullPath, useJoliet)
                } else {
                    entries += IsoEntry(
                        name     = name,
                        dirPath  = parentPath,
                        fullPath = fullPath,
                        sector   = fileSector,
                        size     = fileSize
                    )
                }
            }

            offset += recordLen
        }

        return entries
    }

    // ── Raw I/O ────────────────────────────────────────────────────────────────

    private fun readSectors(startSector: Long, byteCount: Long): ByteArray {
        val fd = context.contentResolver.openFileDescriptor(isoUri, "r")
            ?: throw IOException("Cannot open ISO")
        return fd.use {
            FileInputStream(it.fileDescriptor).use { fis ->
                fis.skip(startSector * SECTOR_SIZE)
                val buf = ByteArray(byteCount.toInt())
                var read = 0
                while (read < buf.size) {
                    val r = fis.read(buf, read, buf.size - read)
                    if (r == -1) break
                    read += r
                }
                buf
            }
        }
    }

    private fun readRawEntry(entry: IsoEntry): InputStream {
        val fd = context.contentResolver.openFileDescriptor(isoUri, "r")
            ?: throw IOException("Cannot re-open ISO")
        val fis = FileInputStream(fd.fileDescriptor)
        fis.skip(entry.sector * SECTOR_SIZE)
        return object : FilterInputStream(fis) {
            var remaining = entry.size
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (remaining <= 0) return -1
                val toRead = minOf(len.toLong(), remaining).toInt()
                val r = super.read(b, off, toRead)
                if (r > 0) remaining -= r
                return r
            }
            override fun close() {
                super.close()
                fd.close()
            }
        }
    }

    private fun openIso(): InputStream =
        context.contentResolver.openInputStream(isoUri)
            ?: throw IOException("Cannot open ISO URI")

    // ── WIM / ESD Splitter ─────────────────────────────────────────────────────

    private fun splitAndWrite(
        input:       InputStream,
        dirPath:     String,
        ext:         String,   // "wim" or "esd"
        writer:      SafUsbWriter,
        onLog:       (String) -> Unit
    ) {
        var chunkIdx = 1
        while (true) {
            val fileName = if (chunkIdx == 1) "install.$ext" else "install$chunkIdx.$ext"
            val path     = if (dirPath.isEmpty()) fileName else "$dirPath/$fileName"

            onLog("  Writing chunk $chunkIdx → $fileName (streaming, up to ${WIM_SPLIT_SIZE.toHumanSize()})...")
            val written = writer.writeStreamChunk(path, input, WIM_SPLIT_SIZE)
            onLog("  Chunk $chunkIdx complete (${written.toHumanSize()})")

            if (written < WIM_SPLIT_SIZE) break  // hit EOF before filling this chunk
            chunkIdx++
        }
        onLog("  Split complete → $chunkIdx chunk(s)")
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * ISO 9660 filenames carry a trailing ";N" version number (e.g. "SETUP.EXE;1").
     * Strip exactly that suffix — not any trailing digit/space/dot — so filenames
     * that legitimately end in characters like '1' (e.g. "boot1.txt") survive intact.
     */
    private val versionSuffixRegex = Regex(";\\d+$")
    private fun stripVersionSuffix(raw: String): String {
        val noVersion = raw.replace(versionSuffixRegex, "")
        // A single trailing dot with no extension after it (e.g. "FILENAME.") is
        // a real ISO 9660 artifact for extensionless files — safe to drop only
        // when it's the very last character.
        return if (noVersion.endsWith(".") && !noVersion.dropLast(1).contains("."))
            noVersion.dropLast(1)
        else noVersion
    }

    private fun isInstallImage(entry: IsoEntry) =
        entry.name.equals("install.wim", ignoreCase = true) ||
        entry.name.equals("install.esd", ignoreCase = true)

    /** Little-endian 32-bit read */
    private fun readLE32(buf: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun Long.toHumanSize(): String = when {
        this >= 1_073_741_824L -> "%.1f GB".format(this / 1_073_741_824.0)
        this >= 1_048_576L     -> "%.1f MB".format(this / 1_048_576.0)
        this >= 1_024L         -> "%.1f KB".format(this / 1_024.0)
        else                   -> "$this B"
    }
}
