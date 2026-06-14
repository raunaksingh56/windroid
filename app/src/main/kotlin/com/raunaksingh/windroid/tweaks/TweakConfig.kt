/*
 * WinDroid - Tweak Configuration Model
 * Created by Raunak Singh
 * Updated: Added Windows XP / 7 / 8 / 8.1 / 10 / 11 support
 */

package com.raunaksingh.windroid.tweaks

/**
 * Windows version family — determines autounattend.xml schema and bypass options.
 * XP uses sysprep answer files (sysprep.inf), 7+ use the modern unattend schema.
 */
enum class WindowsVersion(val displayName: String, val supportsWin11Bypasses: Boolean) {
    WINDOWS_XP   ("Windows XP",         false),
    WINDOWS_7    ("Windows 7",           false),
    WINDOWS_8    ("Windows 8 / 8.1",     false),
    WINDOWS_10   ("Windows 10",          false),
    WINDOWS_11   ("Windows 11",          true),
    AUTO_DETECT  ("Auto-Detect",         true),   // default — detects from ISO
}

enum class WindowsEdition(val displayName: String, val eiCfgValue: String) {
    HOME        ("Windows Home",        "Core"),
    HOME_N      ("Windows Home N",      "CoreN"),
    PRO         ("Windows Pro",         "Professional"),
    PRO_N       ("Windows Pro N",       "ProfessionalN"),
    EDUCATION   ("Windows Education",   "Education"),
    ENTERPRISE  ("Windows Enterprise",  "Enterprise"),

    // Legacy editions
    XP_HOME     ("XP Home",            ""),
    XP_PRO      ("XP Professional",    ""),
    WIN7_HOME   ("7 Home Premium",     "HomePremium"),
    WIN7_PRO    ("7 Professional",     "Professional"),
    WIN7_ULT    ("7 Ultimate",         "Ultimate"),
    WIN8_CORE   ("8 / 8.1",           "Core"),
    WIN8_PRO    ("8 / 8.1 Pro",       "Professional"),
}

data class TweakConfig(
    // ── Windows Version ────────────────────────────────────────────────────────
    val windowsVersion: WindowsVersion      = WindowsVersion.AUTO_DETECT,
    val windowsEdition: WindowsEdition      = WindowsEdition.PRO,

    // ── Account ────────────────────────────────────────────────────────────────
    val skipMicrosoftAccount: Boolean       = true,
    val localUsername: String               = "User",
    val localPassword: String               = "",
    val skipPasswordHint: Boolean           = true,

    // ── OOBE ──────────────────────────────────────────────────────────────────
    val skipOOBE: Boolean                   = true,
    val skipPrivacyQuestions: Boolean       = true,
    val skipLicenseScreen: Boolean          = true,
    val skipRegionScreen: Boolean           = false,
    val autoRegion: String                  = "IN",
    val autoLocale: String                  = "en-US",
    val autoKeyboard: String                = "0409:00000409",

    // ── Windows 11 Hardware Bypasses ──────────────────────────────────────────
    val bypassTPM: Boolean                  = true,
    val bypassSecureBoot: Boolean           = true,
    val bypassRAMCheck: Boolean             = true,
    val bypassCPUCheck: Boolean             = true,
    val bypassDiskCheck: Boolean            = false,

    // ── Windows 7 / 8 specific ────────────────────────────────────────────────
    /** Inject ei.cfg to force edition selection and skip edition-picker screen */
    val injectEiCfg: Boolean                = true,
    /** Disable Windows Activation grace-period nag on first boot */
    val skipActivationNag: Boolean          = true,
    /** Enable classic shell/taskbar on Win 8 / 8.1 */
    val enableClassicShell: Boolean         = false,
    /** Disable SmartScreen on Win 8+ */
    val disableSmartScreen: Boolean         = false,

    // ── Windows XP specific ───────────────────────────────────────────────────
    /** Inject OEMID / OEMName for unattended XP setup */
    val xpOemName: String                   = "WinDroid",
    /** Auto-format and partition (XP only — Win 7+ always show disk picker) */
    val xpAutoFormat: Boolean               = false,
    /** Product key — required for fully unattended XP setup */
    val xpProductKey: String                = "",

    // ── Privacy (Win 8+) ──────────────────────────────────────────────────────
    val disableTelemetry: Boolean           = true,
    val disableCortana: Boolean             = true,
    val disableDataCollection: Boolean      = true,
    val disableAdvertisingId: Boolean       = true,

    // ── Setup ─────────────────────────────────────────────────────────────────
    val autoPartitionDisk: Boolean          = false,
    val diskIndex: Int                      = 0,
)
