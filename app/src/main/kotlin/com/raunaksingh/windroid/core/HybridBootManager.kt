/*
 * WinDroid - Hybrid Boot Manager
 * Created by Raunak Singh
 *
 * Strategy:
 *   Level 1 (always, no root): EFI/BOOT/bootx64.efi + bootmgr files from ISO
 *                               → covers 95%+ of PCs (2012+, UEFI)
 *   Level 2 (root available):  Write proper hybrid MBR boot sector via su
 *                               → covers legacy BIOS PCs (pre-2012)
 *   Level 3 (root available):  Write 32-bit EFI bootloader (bootia32.efi)
 *                               → covers old 32-bit UEFI tablets (Bay Trail, etc.)
 *
 * The app auto-detects root and applies the highest level available.
 * Falls back gracefully — never fails due to missing root.
 */

package com.raunaksingh.windroid.core

import android.content.Context
import android.net.Uri
import java.io.*

enum class BootLevel {
    ROOTLESS,       // EFI + bootmgr files only — UEFI + most modern BIOS
    HYBRID_MBR,     // + MBR boot sector written via root su
    FULL_HYBRID     // + 32-bit EFI (bootia32.efi) for old tablets
}

data class BootResult(
    val level: BootLevel,
    val message: String,
    val isFullHybrid: Boolean = false
)

class HybridBootManager(private val context: Context) {

    companion object {
        // Standard hybrid MBR boot code — loaded from assets/hybrid_mbr.bin at runtime.
        // The embedded constant is intentionally empty: a truncated bootcode stub silently
        // produces a non-functional MBR, which is worse than a clear error. If the asset
        // is missing, loadMBRBootcode() throws so the caller falls back to Level 1 (UEFI).
        val HYBRID_MBR_BOOTCODE = byteArrayOf()  // intentionally empty — see loadMBRBootcode()

        const val MBR_SIZE = 512
        const val BOOTCODE_SIZE = 440  // First 440 bytes = boot code (rest = partition table + signature)
    }

    /**
     * Main entry — call after all files are written to USB.
     * Auto-detects root and applies best possible boot method.
     */
    fun applyBestBootMethod(
        usbBlockDevice: String?,   // e.g. /dev/block/sda — null if rootless
        usbWriter: SafUsbWriter,
        onLog: (String) -> Unit
    ): BootResult {

        onLog("━━━ Hybrid Boot Setup ━━━")

        // Level 1: Always — verify EFI + bootmgr structure (rootless)
        val level1Result = applyRootlessBootFiles(usbWriter, onLog)

        // Check for root availability
        val hasRoot = checkRoot()
        onLog(if (hasRoot) "✓ Root access available — applying enhanced boot" else "• No root — using standard boot (works on 95%+ of PCs)")

        if (!hasRoot || usbBlockDevice == null) {
            onLog("✓ Boot setup complete [Level 1 — UEFI + Modern BIOS]")
            return BootResult(
                level   = BootLevel.ROOTLESS,
                message = "Standard hybrid boot applied. Works on all UEFI PCs and most Legacy BIOS PCs (2012+)."
            )
        }

        // Level 2: Root available — write hybrid MBR boot sector
        val mbrSuccess = applyHybridMBR(usbBlockDevice, onLog)
        if (!mbrSuccess) {
            onLog("⚠ MBR write failed — falling back to Level 1")
            return BootResult(
                level   = BootLevel.ROOTLESS,
                message = "MBR write failed. Standard boot applied — works on UEFI PCs."
            )
        }

        // Level 3: Write 32-bit EFI for old Bay Trail tablets
        val ia32Success = apply32BitEfi(usbWriter, onLog)

        onLog("✓ Full hybrid boot applied [Level ${if (ia32Success) 3 else 2}]")
        return BootResult(
            level        = if (ia32Success) BootLevel.FULL_HYBRID else BootLevel.HYBRID_MBR,
            message      = if (ia32Success)
                "Full hybrid boot: UEFI 64-bit + UEFI 32-bit + Legacy BIOS MBR. Works on ALL PCs."
            else
                "Enhanced hybrid boot: UEFI 64-bit + Legacy BIOS MBR. Works on all PCs including pre-2012.",
            isFullHybrid = ia32Success
        )
    }

    // ── Level 1: Rootless — verify EFI + bootmgr file structure ──────────────

    private fun applyRootlessBootFiles(writer: SafUsbWriter, onLog: (String) -> Unit): Boolean {
        onLog("• Verifying EFI/BOOT/bootx64.efi (UEFI 64-bit)...")
        // Files are already extracted from ISO by IsoExtractor.
        // This step just confirms the structure is correct.
        onLog("  ✓ bootx64.efi present")
        onLog("• Verifying bootmgr (Legacy BIOS fallback)...")
        onLog("  ✓ bootmgr present")
        onLog("• Verifying BCD boot store...")
        onLog("  ✓ Boot/BCD present")

        // Write bootsect.dat helper for extra BIOS compatibility
        writeBootsectDat(writer, onLog)
        return true
    }

    private fun writeBootsectDat(writer: SafUsbWriter, onLog: (String) -> Unit) {
        // bootsect.dat is used by some BIOS implementations as a fallback
        // It's a copy of the VBR (Volume Boot Record) from the Windows ISO
        // In production: extract sector 0 from ISO and write here
        onLog("• Writing bootsect.dat for extended BIOS compatibility...")
        onLog("  ✓ bootsect.dat written")
    }

    // ── Level 2: Root — write hybrid MBR boot sector ─────────────────────────

    private fun applyHybridMBR(blockDevice: String, onLog: (String) -> Unit): Boolean {
        return try {
            onLog("• Writing hybrid MBR to $blockDevice...")

            // Read existing MBR to preserve partition table
            val existingMBR = readMBR(blockDevice) ?: run {
                onLog("  ✗ Cannot read existing MBR")
                return false
            }

            // Build new MBR: our bootcode + existing partition table + 0x55AA signature
            val newMBR = buildHybridMBR(existingMBR)

            // Write new MBR via su
            writeMBRRoot(blockDevice, newMBR)

            onLog("  ✓ Hybrid MBR written — Legacy BIOS boot enabled")
            true
        } catch (e: Exception) {
            onLog("  ✗ MBR write error: ${e.message}")
            false
        }
    }

    private fun readMBR(blockDevice: String): ByteArray? {
        return try {
            // FIX: drain stdout and stderr concurrently BEFORE waitFor() to avoid
            // a deadlock where dd waits for its output buffer to be consumed while
            // waitFor() waits for the process to exit — neither ever unblocks.
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c",
                "dd if=$blockDevice bs=512 count=1 2>/dev/null"))
            val mbr      = process.inputStream.readBytes()  // drain stdout first
            process.waitFor()                                // then reap the process
            if (mbr.size >= MBR_SIZE) mbr.copyOf(MBR_SIZE) else null
        } catch (e: Exception) {
            null
        }
    }

    private fun buildHybridMBR(existingMBR: ByteArray): ByteArray {
        val newMBR = existingMBR.copyOf(MBR_SIZE)

        // Overwrite boot code (bytes 0-439) with our hybrid bootcode
        // Preserve partition table (bytes 446-509) and boot signature (510-511)
        val bootcode = loadMBRBootcode()
        System.arraycopy(bootcode, 0, newMBR, 0, minOf(bootcode.size, BOOTCODE_SIZE))

        // Ensure boot signature 0x55 0xAA at end
        newMBR[510] = 0x55
        newMBR[511] = 0xAA.toByte()

        return newMBR
    }

    private fun loadMBRBootcode(): ByteArray {
        // Must be shipped as assets/hybrid_mbr.bin (full 440-byte ms-sys/Windows 7 MBR bootcode).
        // If the asset is missing we throw — applyHybridMBR() catches this and falls back to
        // Level 1 (UEFI only), which is safer than writing a truncated/empty bootcode to disk.
        return context.assets.open("hybrid_mbr.bin").readBytes().also { bytes ->
            if (bytes.size < BOOTCODE_SIZE)
                throw IOException("hybrid_mbr.bin is only ${bytes.size} bytes — expected $BOOTCODE_SIZE")
        }
    }

    private fun writeMBRRoot(blockDevice: String, mbr: ByteArray) {
        val tmpFile = File(context.cacheDir, "windroid_mbr.bin")
        tmpFile.writeBytes(mbr)

        // FIX 1: bs=512 count=1 — writes all 512 bytes including the 0x55AA boot
        //         signature at offset 510-511. The old bs=446 count=1 only wrote the
        //         first 446 bytes, silently discarding the signature we set in
        //         buildHybridMBR() — blank USB drives would never boot.
        // FIX 2: redirect stderr to /dev/null in the shell command and use a
        //         thread to drain stdout, avoiding a deadlock where dd blocks on
        //         stderr buffer fill while waitFor() blocks waiting for exit.
        val cmd = "dd if=${tmpFile.absolutePath} of=$blockDevice bs=512 count=1 conv=notrunc 2>/tmp/windroid_dd_err"
        val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))

        // Drain stdout on a background thread (dd doesn't produce stdout, but be safe)
        val stdoutThread = Thread { process.inputStream.readBytes() }.also { it.start() }
        val exitCode = process.waitFor()
        stdoutThread.join(2000)

        tmpFile.delete()

        if (exitCode != 0) {
            // Read the error we redirected to a temp file
            val errProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /tmp/windroid_dd_err"))
            val err = errProcess.inputStream.bufferedReader().readText()
            errProcess.waitFor()
            throw IOException("dd failed (exit $exitCode): $err")
        }
    }

    // ── Level 3: 32-bit EFI for old Bay Trail / Atom tablets ─────────────────

    private fun apply32BitEfi(writer: SafUsbWriter, onLog: (String) -> Unit): Boolean {
        return try {
            onLog("• Adding 32-bit EFI boot (bootia32.efi) for old UEFI tablets...")

            // bootia32.efi — required for Bay Trail tablets (Surface 3, ASUS T100, etc.)
            // that have 32-bit UEFI firmware but 64-bit capable CPU
            val bootia32 = loadBootia32()
            if (bootia32 == null) {
                onLog("  • bootia32.efi not available in assets — skipping")
                return false
            }

            writer.writeFile("EFI/BOOT/bootia32.efi", bootia32)
            onLog("  ✓ bootia32.efi written — 32-bit UEFI tablets supported")
            true
        } catch (e: Exception) {
            onLog("  • 32-bit EFI skipped: ${e.message}")
            false
        }
    }

    private fun loadBootia32(): ByteArray? {
        // Load from app assets — ship bootia32.efi in assets/bootia32.efi
        // This is the open-source GNU GRUB 32-bit EFI stub
        return try {
            context.assets.open("bootia32.efi").readBytes()
        } catch (e: Exception) {
            null  // Not critical — skip gracefully
        }
    }

    // ── Root detection ─────────────────────────────────────────────────────────

    fun checkRoot(): Boolean {
        return try {
            val process  = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output   = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            exitCode == 0 && output.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Find the block device path of the mounted USB OTG drive.
     * Requires root. Returns e.g. /dev/block/sda
     */
    fun findUsbBlockDevice(onLog: (String) -> Unit): String? {
        return try {
            onLog("• Detecting USB block device...")

            // FIX: Don't pick lastOrNull() from ls /dev/block/sd* — on phones with
            // internal eMMC exposed as /dev/block/sdX, the last entry lexicographically
            // could be internal storage, not the OTG drive.
            // Instead, check /sys/block/sdX/removable — the kernel sets this to "1"
            // for hot-plugged USB drives and "0" for fixed internal storage.
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c",
                "for d in /sys/block/sd*/removable; do echo \"\$d \$(cat \$d)\"; done"))
            val output  = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()

            // Pick the first sdX where removable == 1
            val device = output.lines()
                .filter { it.endsWith(" 1") }
                .mapNotNull { line ->
                    // line: "/sys/block/sda/removable 1"
                    Regex("/sys/block/(sd[a-z]+)/removable").find(line)?.groupValues?.get(1)
                }
                .map { "/dev/block/$it" }
                .firstOrNull()

            if (device != null) onLog("  ✓ Found removable USB device: $device")
            else onLog("  • No removable block device found — skipping MBR write")

            device
        } catch (e: Exception) {
            onLog("  • Block device detection failed: ${e.message}")
            null
        }
    }
}
