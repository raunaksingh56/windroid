/*
 * WinDroid - Answer File Generator
 * Created by Raunak Singh
 *
 * Generates the correct answer file for each Windows version:
 *   XP          → winnt.sif  (legacy text-format answer file)
 *   7 / 8 / 10 / 11 → autounattend.xml  (modern unattend schema)
 *
 * Windows 7 / 8 differences vs 10/11:
 *   - No TPM/SecureBoot/RAM bypass needed
 *   - ei.cfg used to pick edition and channel
 *   - OOBE component names differ slightly
 *   - Cortana/telemetry components only exist on 10+
 */

package com.raunaksingh.windroid.tweaks

object AutounattendGenerator {

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns a map of filename → content for all files to inject into the USB root.
     * Callers should write every entry to the USB.
     */
    fun generateAll(cfg: TweakConfig): Map<String, String> {
        val files = mutableMapOf<String, String>()

        when (cfg.windowsVersion) {
            WindowsVersion.WINDOWS_XP -> {
                files["winnt.sif"] = generateWinntSif(cfg)
            }
            WindowsVersion.WINDOWS_7,
            WindowsVersion.WINDOWS_8 -> {
                files["autounattend.xml"] = generateModernXml(cfg)
                if (cfg.injectEiCfg && cfg.windowsEdition.eiCfgValue.isNotEmpty()) {
                    files["sources/ei.cfg"] = generateEiCfg(cfg)
                }
            }
            else -> {
                // Windows 10, 11, Auto-Detect
                files["autounattend.xml"] = generateModernXml(cfg)
                if (cfg.injectEiCfg && cfg.windowsEdition.eiCfgValue.isNotEmpty()) {
                    files["sources/ei.cfg"] = generateEiCfg(cfg)
                }
            }
        }

        return files
    }

    /** Backwards-compat helper — returns autounattend.xml content for Win 10/11 */
    fun generate(cfg: TweakConfig): String = generateAll(cfg)["autounattend.xml"]
        ?: generateAll(cfg).values.first()

    // ── Windows XP — winnt.sif ─────────────────────────────────────────────────

    private fun generateWinntSif(cfg: TweakConfig): String = buildString {
        appendLine("; WinDroid - Windows XP Unattended Answer File")
        appendLine("; Generated automatically — do not edit")
        appendLine()
        appendLine("[Data]")
        appendLine("AutoPartition=${if (cfg.xpAutoFormat) "1" else "0"}")
        appendLine("MsDosInitiated=\"0\"")
        appendLine("UnattendedInstall=\"Yes\"")
        appendLine()
        appendLine("[Unattended]")
        appendLine("UnattendMode=FullUnattended")
        appendLine("OemSkipEula=Yes")
        appendLine("OemPreinstall=No")
        appendLine("TargetPath=\\WINDOWS")
        if (cfg.xpAutoFormat) {
            appendLine("FileSystem=NTFS")
            appendLine("NtUpgrade=No")
            appendLine("OverwriteOemFilesOnUpgrade=No")
        }
        appendLine()
        appendLine("[GuiUnattended]")
        appendLine("AdminPassword=${cfg.localPassword.ifEmpty { "*" }}")
        appendLine("EncryptedAdminPassword=No")
        appendLine("OEMSkipRegional=1")
        appendLine("TimeZone=330")   // India Standard Time (IST)
        appendLine("OemSkipWelcome=1")
        appendLine()
        appendLine("[UserData]")
        appendLine("ProductKey=${cfg.xpProductKey.ifEmpty { "XXXXX-XXXXX-XXXXX-XXXXX-XXXXX" }}")
        appendLine("FullName=\"${cfg.localUsername}\"")
        appendLine("OrgName=\"${cfg.xpOemName}\"")
        appendLine("ComputerName=${cfg.localUsername}PC")
        appendLine()
        appendLine("[Identification]")
        appendLine("JoinWorkgroup=WORKGROUP")
        appendLine()
        appendLine("[Networking]")
        appendLine("InstallDefaultComponents=Yes")
        appendLine()
        appendLine("[Components]")
        // Disable bloat
        appendLine("msmsgs=Off")
        appendLine("msnexplr=Off")
        appendLine()
        appendLine("[Shell]")
        appendLine("DefaultStartPanelOff=No")
    }

    // ── ei.cfg — edition + channel override ───────────────────────────────────

    private fun generateEiCfg(cfg: TweakConfig): String = buildString {
        appendLine("[CHANNEL]")
        appendLine("Retail")
        appendLine("[VL]")
        appendLine("0")
        if (cfg.windowsEdition.eiCfgValue.isNotEmpty()) {
            appendLine("[EditionID]")
            appendLine(cfg.windowsEdition.eiCfgValue)
        }
    }

    // ── Modern XML (Win 7 / 8 / 8.1 / 10 / 11) ────────────────────────────────

    private fun generateModernXml(cfg: TweakConfig): String {
        val isWin11   = cfg.windowsVersion == WindowsVersion.WINDOWS_11
                     || cfg.windowsVersion == WindowsVersion.AUTO_DETECT
        val isWin8x   = cfg.windowsVersion == WindowsVersion.WINDOWS_8
        val isWin7    = cfg.windowsVersion == WindowsVersion.WINDOWS_7
        val isLegacy  = isWin7 || isWin8x  // 7 or 8.x
        val arch      = "amd64"
        val pkt       = "31bf3856ad364e35"

        return buildString {
            appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
            appendLine("""<unattend xmlns="urn:schemas-microsoft-com:unattend">""")

            // ── windowsPE ────────────────────────────────────────────────────────
            appendLine("""  <settings pass="windowsPE">""")

            // Regional settings
            appendLine("""    <component name="Microsoft-Windows-International-Core-WinPE" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS" xmlns:wcm="http://schemas.microsoft.com/WMIConfig/2002/State">""")
            appendLine("""      <SetupUILanguage><UILanguage>${cfg.autoLocale}</UILanguage></SetupUILanguage>""")
            appendLine("""      <InputLocale>${cfg.autoKeyboard}</InputLocale>""")
            appendLine("""      <SystemLocale>${cfg.autoLocale}</SystemLocale>""")
            appendLine("""      <UILanguage>${cfg.autoLocale}</UILanguage>""")
            appendLine("""      <UserLocale>${cfg.autoLocale}</UserLocale>""")
            appendLine("""    </component>""")

            // Setup component
            appendLine("""    <component name="Microsoft-Windows-Setup" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS" xmlns:wcm="http://schemas.microsoft.com/WMIConfig/2002/State">""")

            // Win11 hardware bypasses (registry in WinPE)
            val needsLabConfigBypass = isWin11 && (cfg.bypassTPM || cfg.bypassSecureBoot || cfg.bypassRAMCheck || cfg.bypassCPUCheck)
            // Online account / network requirement bypass — Win 10 (2004+) and Win 11
            val needsNroBypass = !isLegacy && cfg.bypassOnlineAccountRequirement

            if (needsLabConfigBypass || needsNroBypass) {
                appendLine("""      <RunSynchronous>""")
                var order = 1
                fun reg(path: String, value: String) {
                    appendLine("""        <RunSynchronousCommand wcm:action="add">""")
                    appendLine("""          <Order>${order++}</Order>""")
                    appendLine("""          <Path>reg add "HKLM\SYSTEM\Setup\LabConfig" /v $path /t REG_DWORD /d $value /f</Path>""")
                    appendLine("""        </RunSynchronousCommand>""")
                }
                if (needsLabConfigBypass) {
                    if (cfg.bypassTPM)        reg("BypassTPMCheck", "1")
                    if (cfg.bypassSecureBoot) reg("BypassSecureBootCheck", "1")
                    if (cfg.bypassRAMCheck)   reg("BypassRAMCheck", "1")
                    if (cfg.bypassCPUCheck)   reg("BypassCPUCheck", "1")
                }
                if (needsNroBypass) {
                    // Forces Setup to offer a local account instead of requiring
                    // an internet connection / Microsoft account during OOBE.
                    appendLine("""        <RunSynchronousCommand wcm:action="add">""")
                    appendLine("""          <Order>${order++}</Order>""")
                    appendLine("""          <Path>reg add "HKLM\SYSTEM\Setup\LabConfig" /v BypassNRO /t REG_DWORD /d 1 /f</Path>""")
                    appendLine("""        </RunSynchronousCommand>""")
                    appendLine("""        <RunSynchronousCommand wcm:action="add">""")
                    appendLine("""          <Order>${order++}</Order>""")
                    appendLine("""          <Path>reg add "HKLM\SYSTEM\Setup\LabConfig" /v BypassNROCheck /t REG_DWORD /d 1 /f</Path>""")
                    appendLine("""        </RunSynchronousCommand>""")
                }
                appendLine("""      </RunSynchronous>""")
            }

            // Disable activation nag (Win 7 / 8 — skip license key screen)
            if (isLegacy && cfg.skipActivationNag) {
                appendLine("""      <UserData>""")
                appendLine("""        <ProductKey><WillShowUI>OnError</WillShowUI></ProductKey>""")
                if (cfg.skipLicenseScreen) {
                    appendLine("""        <AcceptEula>true</AcceptEula>""")
                }
                appendLine("""      </UserData>""")
            } else if (cfg.skipLicenseScreen) {
                appendLine("""      <UserData><AcceptEula>true</AcceptEula></UserData>""")
            }

            appendLine("""    </component>""")
            appendLine("""  </settings>""")

            // ── specialize ───────────────────────────────────────────────────────
            appendLine("""  <settings pass="specialize">""")
            appendLine("""    <component name="Microsoft-Windows-Shell-Setup" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS" xmlns:wcm="http://schemas.microsoft.com/WMIConfig/2002/State">""")
            appendLine("""      <ComputerName>${cfg.localUsername}PC</ComputerName>""")
            if (!cfg.skipRegionScreen) {
                appendLine("""      <TimeZone>India Standard Time</TimeZone>""")
            }
            appendLine("""    </component>""")

            // Win 8.x — disable SmartScreen
            if (isWin8x && cfg.disableSmartScreen) {
                appendLine("""    <component name="Microsoft-Windows-Security-SPP-UX" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS">""")
                appendLine("""      <SkipAutoActivation>true</SkipAutoActivation>""")
                appendLine("""    </component>""")
            }

            // Telemetry / Cortana — Win 10+ only
            if (!isLegacy && cfg.disableTelemetry) {
                appendLine("""    <component name="Microsoft-Windows-SQMAPI" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS">""")
                appendLine("""      <CEIPEnabled>0</CEIPEnabled>""")
                appendLine("""    </component>""")
            }

            appendLine("""  </settings>""")

            // ── oobeSystem ───────────────────────────────────────────────────────
            appendLine("""  <settings pass="oobeSystem">""")
            appendLine("""    <component name="Microsoft-Windows-Shell-Setup" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS" xmlns:wcm="http://schemas.microsoft.com/WMIConfig/2002/State">""")

            // OOBE settings
            appendLine("""      <OOBE>""")
            if (cfg.skipOOBE) {
                if (isLegacy) {
                    // Win 7/8 OOBE tags
                    appendLine("""        <HideEULAPage>true</HideEULAPage>""")
                    appendLine("""        <HideLocalAccountScreen>false</HideLocalAccountScreen>""")
                    appendLine("""        <HideWirelessSetupInOOBE>true</HideWirelessSetupInOOBE>""")
                    if (!isWin7) {
                        // Win 8 only
                        appendLine("""        <HideOnlineAccountScreens>true</HideOnlineAccountScreens>""")
                    }
                } else {
                    // Win 10/11
                    appendLine("""        <HideEULAPage>true</HideEULAPage>""")
                    appendLine("""        <HideLocalAccountScreen>false</HideLocalAccountScreen>""")
                    appendLine("""        <HideOnlineAccountScreens>true</HideOnlineAccountScreens>""")
                    appendLine("""        <HideWirelessSetupInOOBE>true</HideWirelessSetupInOOBE>""")
                    appendLine("""        <SkipMachineOOBE>true</SkipMachineOOBE>""")
                    appendLine("""        <SkipUserOOBE>true</SkipUserOOBE>""")
                    appendLine("""        <ProtectYourPC>3</ProtectYourPC>""")
                    if (cfg.bypassOnlineAccountRequirement) {
                        // Pins setup to a wired/offline path so it doesn't loop
                        // back into "Let's connect you to a network".
                        appendLine("""        <NetworkLocation>Home</NetworkLocation>""")
                    }
                }
            }
            appendLine("""      </OOBE>""")

            // Local user account
            if (cfg.skipMicrosoftAccount || isLegacy) {
                appendLine("""      <UserAccounts>""")
                appendLine("""        <LocalAccounts>""")
                appendLine("""          <LocalAccount wcm:action="add">""")
                appendLine("""            <Name>${cfg.localUsername}</Name>""")
                appendLine("""            <Group>Administrators</Group>""")
                appendLine("""            <Password><Value>${cfg.localPassword}</Value><PlainText>true</PlainText></Password>""")
                appendLine("""          </LocalAccount>""")
                appendLine("""        </LocalAccounts>""")
                appendLine("""      </UserAccounts>""")
                appendLine("""      <AutoLogon>""")
                appendLine("""        <Username>${cfg.localUsername}</Username>""")
                appendLine("""        <Password><Value>${cfg.localPassword}</Value><PlainText>true</PlainText></Password>""")
                appendLine("""        <LogonCount>1</LogonCount>""")
                appendLine("""        <Enabled>true</Enabled>""")
                appendLine("""      </AutoLogon>""")
            }

            appendLine("""    </component>""")

            // Cortana (Win 10+ only)
            if (!isLegacy && cfg.disableCortana) {
                appendLine("""    <component name="Microsoft-Windows-Shell-Setup" processorArchitecture="$arch" publicKeyToken="$pkt" language="neutral" versionScope="nonSxS">""")
                appendLine("""      <DisableAutoDaylightTimeSet>false</DisableAutoDaylightTimeSet>""")
                appendLine("""    </component>""")
            }

            appendLine("""  </settings>""")
            appendLine("""</unattend>""")
        }
    }
}
