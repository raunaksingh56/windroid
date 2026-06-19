/*
 * WinDroid - Tweaks Screen
 * Created by Raunak Singh
 * Updated: Version-aware sections for XP / 7 / 8 / 10 / 11
 */

package com.raunaksingh.windroid.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.raunaksingh.windroid.core.*
import com.raunaksingh.windroid.tweaks.*
import com.raunaksingh.windroid.ui.theme.*

@Composable
fun TweaksScreen(state: FlashState, vm: WinDroidViewModel) {
    var cfg by remember { mutableStateOf(state.tweakConfig) }

    // Resolve effective version for showing/hiding sections
    val version = cfg.windowsVersion
    val isXP     = version == WindowsVersion.WINDOWS_XP
    val isWin7   = version == WindowsVersion.WINDOWS_7
    val isWin8   = version == WindowsVersion.WINDOWS_8
    val isWin10  = version == WindowsVersion.WINDOWS_10
    val isWin11  = version == WindowsVersion.WINDOWS_11 || version == WindowsVersion.AUTO_DETECT
    val isLegacy = isXP || isWin7 || isWin8
    val isModern = isWin10 || isWin11

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top bar ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 52.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    vm.updateTweaks(cfg)
                    vm.navigateTo(Screen.Home)
                }) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = TextPrimary)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        "Tweaks",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        versionTweakSubtitle(version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Version badge ──────────────────────────────────────────────
            VersionBadge(version)

            Spacer(Modifier.height(12.dp))

            // ═══════════════════════════════════════════════════════════════
            // WINDOWS XP SECTIONS
            // ═══════════════════════════════════════════════════════════════
            if (isXP) {
                TweakSection("Windows XP Setup", Icons.Rounded.Window, SuccessGreen) {
                    InfoChip("Generates winnt.sif for fully unattended XP installation")
                    Spacer(Modifier.height(8.dp))

                    TweakTextField(
                        label   = "Product Key (required for full unattend)",
                        value   = cfg.xpProductKey,
                        icon    = Icons.Rounded.Key,
                        onValue = { cfg = cfg.copy(xpProductKey = it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    TweakTextField(
                        label   = "OEM / Organisation Name",
                        value   = cfg.xpOemName,
                        icon    = Icons.Rounded.Business,
                        onValue = { cfg = cfg.copy(xpOemName = it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    TweakToggle(
                        title    = "Auto-Format & Partition",
                        subtitle = "⚠ Erases target disk — only enable if you're certain",
                        checked  = cfg.xpAutoFormat,
                        recommended = false,
                        onToggle = { cfg = cfg.copy(xpAutoFormat = it) }
                    )
                    TweakToggle(
                        title    = "Skip EULA",
                        subtitle = "Auto-accept the license agreement",
                        checked  = cfg.skipLicenseScreen,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipLicenseScreen = it) }
                    )
                }

                TweakSection("Account", Icons.Rounded.Person, CyanAccent) {
                    TweakTextField(
                        label   = "Username",
                        value   = cfg.localUsername,
                        icon    = Icons.Rounded.Person,
                        onValue = { cfg = cfg.copy(localUsername = it) }
                    )
                    Spacer(Modifier.height(8.dp))
                    TweakTextField(
                        label   = "Password (blank = no password)",
                        value   = cfg.localPassword,
                        icon    = Icons.Rounded.Lock,
                        isPassword = true,
                        onValue = { cfg = cfg.copy(localPassword = it) }
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // WINDOWS 7 SECTIONS
            // ═══════════════════════════════════════════════════════════════
            if (isWin7) {
                TweakSection("Windows 7 Setup", Icons.Rounded.Window, WarningAmber) {
                    InfoChip("Generates autounattend.xml + ei.cfg for Windows 7")
                    Spacer(Modifier.height(8.dp))
                    TweakToggle(
                        title    = "Inject ei.cfg (Edition Override)",
                        subtitle = "Skips the edition-picker screen and sets edition directly",
                        checked  = cfg.injectEiCfg,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(injectEiCfg = it) }
                    )
                    TweakToggle(
                        title    = "Skip Activation Nag",
                        subtitle = "Defer Windows activation — install first, activate later",
                        checked  = cfg.skipActivationNag,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipActivationNag = it) }
                    )
                    TweakToggle(
                        title    = "Skip EULA Screen",
                        subtitle = "Auto-accept the license agreement",
                        checked  = cfg.skipLicenseScreen,
                        recommended = false,
                        onToggle = { cfg = cfg.copy(skipLicenseScreen = it) }
                    )
                    TweakToggle(
                        title    = "Skip OOBE Screens",
                        subtitle = "Jump straight to desktop after install",
                        checked  = cfg.skipOOBE,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipOOBE = it) }
                    )
                }

                TweakSection("Account", Icons.Rounded.Person, CyanAccent) {
                    TweakToggle(
                        title    = "Create Local Account",
                        subtitle = "Set username and password during unattend",
                        checked  = cfg.skipMicrosoftAccount,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipMicrosoftAccount = it) }
                    )
                    if (cfg.skipMicrosoftAccount) {
                        Spacer(Modifier.height(8.dp))
                        TweakTextField("Username", cfg.localUsername, Icons.Rounded.Person) { cfg = cfg.copy(localUsername = it) }
                        Spacer(Modifier.height(8.dp))
                        TweakTextField("Password (blank = no password)", cfg.localPassword, Icons.Rounded.Lock, isPassword = true) { cfg = cfg.copy(localPassword = it) }
                    }
                }

                // Edition picker for Win 7
                TweakSection("Windows 7 Edition", Icons.Rounded.Window, WarningAmber) {
                    listOf(
                        WindowsEdition.WIN7_HOME,
                        WindowsEdition.WIN7_PRO,
                        WindowsEdition.WIN7_ULT
                    ).forEach { edition ->
                        EditionRow(
                            edition  = edition,
                            selected = cfg.windowsEdition == edition,
                            onSelect = { cfg = cfg.copy(windowsEdition = edition) }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // WINDOWS 8 / 8.1 SECTIONS
            // ═══════════════════════════════════════════════════════════════
            if (isWin8) {
                TweakSection("Windows 8 / 8.1 Setup", Icons.Rounded.Window, PurpleAccent) {
                    InfoChip("Generates autounattend.xml + ei.cfg for Windows 8 / 8.1")
                    Spacer(Modifier.height(8.dp))
                    TweakToggle(
                        title    = "Inject ei.cfg (Edition Override)",
                        subtitle = "Skips the edition-picker and sets edition directly",
                        checked  = cfg.injectEiCfg,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(injectEiCfg = it) }
                    )
                    TweakToggle(
                        title    = "Skip Activation Nag",
                        subtitle = "Defer Windows activation — install first, activate later",
                        checked  = cfg.skipActivationNag,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipActivationNag = it) }
                    )
                    TweakToggle(
                        title    = "Skip Online Account (Microsoft Account)",
                        subtitle = "Create a local account — no internet needed during OOBE",
                        checked  = cfg.skipMicrosoftAccount,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipMicrosoftAccount = it) }
                    )
                    TweakToggle(
                        title    = "Disable SmartScreen",
                        subtitle = "Turn off SmartScreen filter on first boot",
                        checked  = cfg.disableSmartScreen,
                        recommended = false,
                        onToggle = { cfg = cfg.copy(disableSmartScreen = it) }
                    )
                    TweakToggle(
                        title    = "Skip All OOBE Screens",
                        subtitle = "Jump straight to desktop after install",
                        checked  = cfg.skipOOBE,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipOOBE = it) }
                    )
                }

                if (cfg.skipMicrosoftAccount) {
                    TweakSection("Account", Icons.Rounded.Person, CyanAccent) {
                        TweakTextField("Username", cfg.localUsername, Icons.Rounded.Person) { cfg = cfg.copy(localUsername = it) }
                        Spacer(Modifier.height(8.dp))
                        TweakTextField("Password (blank = no password)", cfg.localPassword, Icons.Rounded.Lock, isPassword = true) { cfg = cfg.copy(localPassword = it) }
                    }
                }

                TweakSection("Windows 8 Edition", Icons.Rounded.Window, PurpleAccent) {
                    listOf(WindowsEdition.WIN8_CORE, WindowsEdition.WIN8_PRO).forEach { edition ->
                        EditionRow(
                            edition  = edition,
                            selected = cfg.windowsEdition == edition,
                            onSelect = { cfg = cfg.copy(windowsEdition = edition) }
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // WINDOWS 10 / 11 / AUTO-DETECT SECTIONS
            // ═══════════════════════════════════════════════════════════════
            if (!isXP && !isWin7 && !isWin8) {

                // Account
                TweakSection("Account Setup", Icons.Rounded.Person, CyanAccent) {
                    TweakToggle(
                        title    = "Skip Microsoft Account",
                        subtitle = "Use a local account — no internet required during setup",
                        checked  = cfg.skipMicrosoftAccount,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(skipMicrosoftAccount = it) }
                    )
                    if (cfg.skipMicrosoftAccount) {
                        Spacer(Modifier.height(12.dp))
                        TweakTextField("Local Username", cfg.localUsername, Icons.Rounded.Person) { cfg = cfg.copy(localUsername = it) }
                        Spacer(Modifier.height(8.dp))
                        TweakTextField("Password (blank = no password)", cfg.localPassword, Icons.Rounded.Lock, isPassword = true) { cfg = cfg.copy(localPassword = it) }
                    }
                    Spacer(Modifier.height(8.dp))
                    TweakToggle(
                        title    = "Bypass Internet/Network Requirement",
                        subtitle = "Removes the \"You need an internet connection\" gate during OOBE (BypassNRO) — required for offline local-account setup on Win 10 2004+ / Win 11",
                        checked  = cfg.bypassOnlineAccountRequirement,
                        recommended = true,
                        onToggle = { cfg = cfg.copy(bypassOnlineAccountRequirement = it) }
                    )
                }

                // OOBE
                TweakSection("Setup Experience", Icons.Rounded.SkipNext, ElectricBlue) {
                    TweakToggle("Skip All OOBE Screens", "Jump straight to desktop after install",
                        cfg.skipOOBE, true) { cfg = cfg.copy(skipOOBE = it) }
                    TweakToggle("Skip Privacy Questions", "Auto-accept with privacy-safe defaults",
                        cfg.skipPrivacyQuestions, true) { cfg = cfg.copy(skipPrivacyQuestions = it) }
                    TweakToggle("Skip License Agreement", "Auto-accept EULA",
                        cfg.skipLicenseScreen, false) { cfg = cfg.copy(skipLicenseScreen = it) }
                    TweakToggle("Auto Set Region to India", "Skip region/keyboard selection screen",
                        !cfg.skipRegionScreen, false) { cfg = cfg.copy(skipRegionScreen = !it) }
                }

                // Win 11 bypasses
                if (isWin11) {
                    TweakSection("Windows 11 Bypasses", Icons.Rounded.Security, PurpleAccent) {
                        InfoChip("Allows Windows 11 to install on unsupported hardware")
                        Spacer(Modifier.height(8.dp))
                        TweakToggle("Bypass TPM 2.0 Check", "Install without TPM chip",
                            cfg.bypassTPM, true) { cfg = cfg.copy(bypassTPM = it) }
                        TweakToggle("Bypass Secure Boot Check", "Install without Secure Boot",
                            cfg.bypassSecureBoot, true) { cfg = cfg.copy(bypassSecureBoot = it) }
                        TweakToggle("Bypass RAM Check", "Install with less than 4 GB RAM",
                            cfg.bypassRAMCheck, false) { cfg = cfg.copy(bypassRAMCheck = it) }
                        TweakToggle("Bypass CPU Check", "Install on unsupported CPUs",
                            cfg.bypassCPUCheck, false) { cfg = cfg.copy(bypassCPUCheck = it) }
                    }
                }

                // Privacy (Win 10/11 only — these components don't exist on 8)
                TweakSection("Privacy & Telemetry", Icons.Rounded.VisibilityOff, SuccessGreen) {
                    TweakToggle("Disable Telemetry", "Block all data sent to Microsoft",
                        cfg.disableTelemetry, true) { cfg = cfg.copy(disableTelemetry = it) }
                    TweakToggle("Disable Cortana", "Remove Cortana assistant",
                        cfg.disableCortana, true) { cfg = cfg.copy(disableCortana = it) }
                    TweakToggle("Disable Data Collection", "Block usage data collection",
                        cfg.disableDataCollection, true) { cfg = cfg.copy(disableDataCollection = it) }
                    TweakToggle("Disable Advertising ID", "Block personalised ads",
                        cfg.disableAdvertisingId, true) { cfg = cfg.copy(disableAdvertisingId = it) }
                }

                // Edition
                TweakSection("Windows Edition", Icons.Rounded.Window, WarningAmber) {
                    listOf(
                        WindowsEdition.HOME,
                        WindowsEdition.HOME_N,
                        WindowsEdition.PRO,
                        WindowsEdition.PRO_N,
                        WindowsEdition.EDUCATION,
                        WindowsEdition.ENTERPRISE
                    ).forEach { edition ->
                        EditionRow(
                            edition  = edition,
                            selected = cfg.windowsEdition == edition,
                            onSelect = { cfg = cfg.copy(windowsEdition = edition) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Save button ────────────────────────────────────────────────
            Button(
                onClick = {
                    vm.updateTweaks(cfg)
                    vm.navigateTo(Screen.Home)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
            ) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save Tweaks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Version badge shown at top of tweaks screen ───────────────────────────────

@Composable
fun VersionBadge(version: WindowsVersion) {
    val (label, color) = when (version) {
        WindowsVersion.WINDOWS_XP   -> "🖥️  Windows XP" to SuccessGreen
        WindowsVersion.WINDOWS_7    -> "🪟  Windows 7"   to WarningAmber
        WindowsVersion.WINDOWS_8    -> "🪟  Windows 8 / 8.1" to PurpleAccent
        WindowsVersion.WINDOWS_10   -> "🪟  Windows 10"  to ElectricBlue
        WindowsVersion.WINDOWS_11   -> "🪟  Windows 11"  to ElectricBlue
        WindowsVersion.AUTO_DETECT  -> "🔍  Auto-Detect" to CyanAccent
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.weight(1f))
        Text(
            "showing relevant tweaks only",
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.6f)
        )
    }
}

private fun versionTweakSubtitle(version: WindowsVersion): String = when (version) {
    WindowsVersion.WINDOWS_XP  -> "winnt.sif options"
    WindowsVersion.WINDOWS_7   -> "Windows 7 autounattend options"
    WindowsVersion.WINDOWS_8   -> "Windows 8 / 8.1 autounattend options"
    WindowsVersion.WINDOWS_10  -> "Windows 10 autounattend options"
    WindowsVersion.WINDOWS_11  -> "Windows 11 autounattend + bypass options"
    WindowsVersion.AUTO_DETECT -> "Customize your Windows setup"
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
fun TweakSection(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text  = title,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard),
            border = BorderStroke(1.dp, NavyBorder)
        ) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun TweakToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    recommended: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                if (recommended) {
                    Spacer(Modifier.width(6.dp))
                    Text("✓", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = ElectricBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = NavyBorder
            )
        )
    }
}

@Composable
fun TweakTextField(
    label: String,
    value: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    onValue: (String) -> Unit
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValue,
        label         = { Text(label, color = TextSecondary) },
        leadingIcon   = { Icon(icon, null, tint = ElectricBlue) },
        visualTransformation = if (isPassword) PasswordVisualTransformation()
                               else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Text
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = ElectricBlue,
            unfocusedBorderColor = NavyBorder,
            focusedTextColor     = TextPrimary,
            unfocusedTextColor   = TextPrimary,
            cursorColor          = ElectricBlue
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun EditionRow(edition: WindowsEdition, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick  = onSelect,
            colors   = RadioButtonDefaults.colors(
                selectedColor   = ElectricBlue,
                unselectedColor = TextMuted
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text  = edition.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) TextPrimary else TextSecondary
        )
        if (edition == WindowsEdition.PRO) {
            Spacer(Modifier.width(8.dp))
            Text("Recommended", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
        }
    }
}

@Composable
fun InfoChip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .background(ElectricBlue.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Info, null, tint = ElectricBlue, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}
