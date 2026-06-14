/*
 * WinDroid - Home Screen
 * Created by Raunak Singh
 * Updated: Windows version picker card added
 */

package com.raunaksingh.windroid.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.raunaksingh.windroid.core.*
import com.raunaksingh.windroid.tweaks.WindowsVersion
import com.raunaksingh.windroid.ui.theme.*

@Composable
fun HomeScreen(state: FlashState, vm: WinDroidViewModel) {
    val context = LocalContext.current

    val isoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, uri) ?: "Windows.iso"
            val size = getFileSize(context, uri) / (1024 * 1024)
            vm.selectIso(uri, name, size)
        }
    }

    val usbLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            vm.selectUsb(uri, "USB Drive")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ElectricBlue.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.15f, size.height * 0.1f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyanAccent.copy(alpha = 0.05f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.7f),
                    radius = size.width * 0.5f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            WinDroidLogo()

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Windows USB Creator",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(Modifier.height(4.dp))

            AuthorBadge()

            Spacer(Modifier.height(36.dp))

            // ── Step 1: Windows Version ──────────────────────────────────────
            WindowsVersionCard(
                selected = state.tweakConfig.windowsVersion,
                onSelect = { version ->
                    vm.updateTweaks(state.tweakConfig.copy(windowsVersion = version))
                }
            )

            Spacer(Modifier.height(12.dp))

            // ── Step 2: ISO ──────────────────────────────────────────────────
            StepCard(
                step       = 2,
                title      = "Select Windows ISO",
                subtitle   = if (state.isoUri != null) state.isoName
                             else "Tap to browse for .iso file",
                icon       = Icons.Rounded.FolderOpen,
                isSelected = state.isoUri != null,
                badge      = if (state.isoSizeMb > 0) "${state.isoSizeMb} MB" else null,
                onClick    = { isoLauncher.launch(arrayOf("application/octet-stream", "application/x-iso9660-image")) }
            )

            Spacer(Modifier.height(12.dp))

            // ── Step 3: USB ──────────────────────────────────────────────────
            StepCard(
                step       = 3,
                title      = "Select USB Drive",
                subtitle   = if (state.usbUri != null) state.usbLabel
                             else "Plug in USB OTG → tap to select",
                icon       = Icons.Rounded.Usb,
                isSelected = state.usbUri != null,
                badge      = if (state.usbUri != null) "Ready" else null,
                onClick    = { usbLauncher.launch(null) }
            )

            Spacer(Modifier.height(12.dp))

            // ── Step 4: Tweaks ───────────────────────────────────────────────
            TweaksPreviewCard(
                config  = state.tweakConfig,
                onClick = { vm.navigateTo(Screen.Tweaks) }
            )

            Spacer(Modifier.height(28.dp))

            // ── Flash button ─────────────────────────────────────────────────
            val canFlash = state.isoUri != null && state.usbUri != null

            Button(
                onClick  = { vm.startFlash() },
                enabled  = canFlash,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = ElectricBlue,
                    disabledContainerColor = NavyBorder
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text  = if (canFlash) "Flash Windows to USB" else "Select ISO + USB to continue",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningAmber.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = "All data on the USB drive will be erased. Back up first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarningAmber
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Windows Version Picker Card ───────────────────────────────────────────────

@Composable
fun WindowsVersionCard(
    selected: WindowsVersion,
    onSelect: (WindowsVersion) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Map each version to a display emoji + color
    val versionMeta = mapOf(
        WindowsVersion.AUTO_DETECT  to Triple("🔍", "Auto-Detect", CyanAccent),
        WindowsVersion.WINDOWS_11   to Triple("🪟", "Windows 11",  ElectricBlue),
        WindowsVersion.WINDOWS_10   to Triple("🪟", "Windows 10",  ElectricBlue),
        WindowsVersion.WINDOWS_8    to Triple("🪟", "Windows 8 / 8.1", PurpleAccent),
        WindowsVersion.WINDOWS_7    to Triple("🪟", "Windows 7",   WarningAmber),
        WindowsVersion.WINDOWS_XP   to Triple("🖥️", "Windows XP",  SuccessGreen),
    )

    val (emoji, label, color) = versionMeta[selected]!!

    Column(modifier = Modifier.fillMaxWidth()) {
        // Step label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("1", style = MaterialTheme.typography.labelSmall,
                    color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "Windows Version",
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }

        Card(
            onClick   = { expanded = !expanded },
            modifier  = Modifier
                .fillMaxWidth()
                .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column {
                // Selected row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            versionSubtitle(selected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = color
                        )
                    }
                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowUp
                        else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted
                    )
                }

                // Expanded version options
                if (expanded) {
                    HorizontalDivider(color = NavyBorder, thickness = 1.dp)
                    Spacer(Modifier.height(4.dp))

                    WindowsVersion.values().forEach { version ->
                        val (vEmoji, vLabel, vColor) = versionMeta[version]!!
                        val isSelected = version == selected

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(version)
                                    expanded = false
                                }
                                .background(
                                    if (isSelected) vColor.copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vEmoji, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    vLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) vColor else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                                Text(
                                    versionSubtitle(version),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    null,
                                    tint = vColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

private fun versionSubtitle(version: WindowsVersion): String = when (version) {
    WindowsVersion.AUTO_DETECT -> "Detects from ISO automatically"
    WindowsVersion.WINDOWS_11  -> "autounattend.xml · TPM/SecureBoot bypass"
    WindowsVersion.WINDOWS_10  -> "autounattend.xml · Telemetry & Cortana off"
    WindowsVersion.WINDOWS_8   -> "autounattend.xml · ei.cfg · online account skip"
    WindowsVersion.WINDOWS_7   -> "autounattend.xml · ei.cfg · activation skip"
    WindowsVersion.WINDOWS_XP  -> "winnt.sif · legacy unattended setup"
}

// ── Existing composables ──────────────────────────────────────────────────────

@Composable
fun WinDroidLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = Brush.linearGradient(listOf(ElectricBlue, CyanAccent)),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "W",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text  = "WinDroid",
            style = MaterialTheme.typography.displayLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun AuthorBadge() {
    Row(
        modifier = Modifier
            .background(
                brush = Brush.horizontalGradient(
                    listOf(ElectricBlue.copy(alpha = 0.15f), CyanAccent.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(1.dp, ElectricBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Code, null, tint = CyanAccent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text  = "Created by Raunak Singh",
            style = MaterialTheme.typography.labelSmall,
            color = CyanAccent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StepCard(
    step: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) ElectricBlue.copy(alpha = 0.6f) else NavyBorder

    Card(
        onClick   = onClick,
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) ElectricBlue else NavyBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text  = "$step",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) CyanAccent else TextSecondary,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) ElectricBlue else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
                if (badge != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                }
            }
        }
    }
}

@Composable
fun TweaksPreviewCard(config: com.raunaksingh.windroid.tweaks.TweakConfig, onClick: () -> Unit) {
    val activeTweaks = listOf(
        config.skipMicrosoftAccount,
        config.bypassTPM,
        config.disableTelemetry,
        config.skipOOBE,
        config.disableCortana,
        config.injectEiCfg,
        config.skipActivationNag,
    ).count { it }

    Card(
        onClick  = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = NavyCard)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PurpleAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Tune, null, tint = PurpleAccent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Windows Tweaks", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(
                    "$activeTweaks tweaks active · tap to configure",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PurpleAccent
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextMuted)
        }
    }
}

// Helper functions
fun getFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
    }
    return name
}

fun getFileSize(context: android.content.Context, uri: Uri): Long {
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst() && idx >= 0) size = cursor.getLong(idx)
    }
    return size
}
