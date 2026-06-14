<div align="center">

```
██╗    ██╗██╗███╗   ██╗██████╗ ██████╗  ██████╗ ██╗██████╗ 
██║    ██║██║████╗  ██║██╔══██╗██╔══██╗██╔═══██╗██║██╔══██╗
██║ █╗ ██║██║██╔██╗ ██║██║  ██║██████╔╝██║   ██║██║██║  ██║
██║███╗██║██║██║╚██╗██║██║  ██║██╔══██╗██║   ██║██║██║  ██║
╚███╔███╔╝██║██║ ╚████║██████╔╝██║  ██║╚██████╔╝██║██████╔╝
 ╚══╝╚══╝ ╚═╝╚═╝  ╚═══╝╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝╚═════╝ 
```

**Flash Windows to USB — straight from your Android phone**

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](LICENSE)
[![No Root](https://img.shields.io/badge/Root-Not%20Required-success?style=for-the-badge)](/)
[![Windows](https://img.shields.io/badge/Windows-XP%20→%2011-0078D4?style=for-the-badge&logo=windows&logoColor=white)](/)

<br/>

> *No PC. No root. No BIOS fiddling. Just your phone and a USB drive.*

</div>

---

## ✨ What is WinDroid?

**WinDroid** is an open-source Android app that turns your phone into a bootable Windows USB creator. Plug in a USB OTG drive, pick your ISO, and flash — the USB boots on any PC automatically.

**Supports every modern Windows version:**

| Version | Answer File | Notes |
|---------|-------------|-------|
| 🪟 Windows XP | `winnt.sif` | Legacy text-format, full unattend |
| 🪟 Windows 7 | `autounattend.xml` | ei.cfg injection, activation skip |
| 🪟 Windows 8 / 8.1 | `autounattend.xml` | Online account bypass, SmartScreen off |
| 🪟 Windows 10 | `autounattend.xml` | Telemetry & Cortana disable |
| 🪟 Windows 11 | `autounattend.xml` | Full TPM / SecureBoot / RAM bypass |

---

## 🚀 Features

### Core Engine

```
┌─────────────────────────────────────────────────────────────────┐
│  ISO 9660 Parser → USB Writer → Boot Setup → Answer File Inject │
└─────────────────────────────────────────────────────────────────┘
```

- ✅ **Full ISO 9660 parser** — reads any Windows ISO natively, no dependencies
- ✅ **Joliet extension support** — correctly handles long Unicode filenames
- ✅ **Auto install.wim / install.esd split** — handles files > 4 GB (FAT32 limit)
- ✅ **No root required** — uses Android USB Host API + Storage Access Framework
- ✅ **Dual boot** — works on both UEFI and Legacy BIOS PCs automatically
- ✅ **Auto-launch on USB plug** — opens when you connect OTG drive
- ✅ **Live progress log** — see exactly what's happening during flash

### Windows Version Support

- ✅ **Windows XP** — generates `winnt.sif`, optional auto-format, product key entry
- ✅ **Windows 7** — `autounattend.xml` + `ei.cfg` edition selection, activation skip
- ✅ **Windows 8 / 8.1** — online account bypass, SmartScreen disable, ei.cfg
- ✅ **Windows 10** — full unattend, telemetry disable, Cortana off
- ✅ **Windows 11** — TPM 2.0 bypass, Secure Boot bypass, RAM & CPU bypass

### Unattended Setup Tweaks

<details>
<summary><b>👤 Account</b></summary>

- Skip Microsoft Account — local account, no internet required
- Custom username & password
- Auto-login on first boot
- Skip password hint

</details>

<details>
<summary><b>🛡️ Windows 11 Bypasses</b></summary>

- Bypass TPM 2.0 check
- Bypass Secure Boot check
- Bypass RAM check (< 4 GB)
- Bypass CPU compatibility check

</details>

<details>
<summary><b>🔒 Privacy</b></summary>

- Disable telemetry from day 1
- Disable Cortana (Win 10/11)
- Disable data collection
- Disable Advertising ID

</details>

<details>
<summary><b>⚙️ Setup</b></summary>

- Skip EULA screen
- Skip privacy questions
- Auto region (India by default)
- Choose Windows edition (Home / Pro / Enterprise / Education)
- **ei.cfg injection** to bypass edition picker (Win 7+)

</details>

<details>
<summary><b>🪟 XP-Specific</b></summary>

- Product key entry
- Auto-format & partition (optional)
- OEM name / organization
- Disable MSN Messenger, IE integration

</details>

---

## 📦 Requirements

| Requirement | Minimum |
|-------------|---------|
| Android | 8.0+ (API 26) |
| RAM | 2 GB recommended |
| USB OTG | Required |
| USB Drive | ≥ 8 GB |
| Windows ISO | XP, 7, 8, 8.1, 10, or 11 |

---

## 🎯 How to Use

```
Step 1 ──► Step 2 ──► Step 3 ──► Step 4 ──► Step 5
 Get ISO    Plug USB   Open App   Configure   Flash!
```

1. **Get a Windows ISO** — download from Microsoft or use an existing one
2. **Plug in USB OTG drive** — app opens automatically
3. Tap **Select ISO** → pick your Windows ISO file
4. Tap **Select USB** → pick the OTG drive
5. Tap **Windows Version** → pick XP / 7 / 8 / 10 / 11 (or leave on Auto-Detect)
6. Configure tweaks (optional — defaults work great)
7. Tap **Flash Windows to USB**
8. Plug USB into your PC → boot → Windows installs automatically!

---

## 🔧 How It Works

### ISO 9660 Parser

```
ISO file
  └── Sector 16 ─► Primary Volume Descriptor
        └── Root directory record
              ├── /sources/install.wim  (or install.esd)
              ├── /EFI/BOOT/bootx64.efi
              ├── /bootmgr
              └── ... (all files extracted)
```

WinDroid implements ISO 9660 parsing entirely in Kotlin:
- Reads the **Primary Volume Descriptor** at sector 16
- Detects **Joliet extensions** (sector 17+) for Unicode long filenames
- Walks the directory tree recursively
- Streams each file directly to the USB drive

### install.wim Splitting

Windows 11 ISOs have `install.wim` or `install.esd` > 4 GB. FAT32 has a 4 GB file limit. WinDroid automatically splits these into `.swm` chunks that Windows Setup handles natively:

```
install.wim (5.2 GB)
  → install.swm   (3.8 GB)  ← chunk 1
  → install2.swm  (1.4 GB)  ← chunk 2
```

### Answer File Generation

Different Windows versions need different answer files:

```
Windows XP  →  winnt.sif       (INI format, placed in ISO root)
Windows 7+  →  autounattend.xml (XML schema, placed in USB root)
Windows 7+  →  sources/ei.cfg   (optional edition override)
```

WinDroid generates all required files automatically based on your version selection.

### Dual Boot (UEFI + BIOS)

```
USB Drive
├── EFI/BOOT/bootx64.efi   ← UEFI 64-bit (all modern PCs, 2012+)
├── EFI/BOOT/bootia32.efi  ← UEFI 32-bit (Bay Trail tablets — root only)
├── bootmgr                ← Legacy BIOS fallback
└── Boot/BCD               ← Boot Configuration Database
```

---

## 🏗️ Building from Source

```bash
git clone https://github.com/raunaksingh/windroid
cd windroid
./gradlew assembleDebug
```

**Requirements:** Android Studio Hedgehog (2023.1) or later.

---

## 📁 Project Structure

```
app/src/main/kotlin/com/raunaksingh/windroid/
│
├── MainActivity.kt                   # Entry point, USB OTG intent handler
│
├── core/
│   ├── IsoExtractor.kt               # ✨ Full ISO 9660 + Joliet parser
│   ├── UsbWriter.kt                  # Rootless USB write via SAF
│   ├── HybridBootManager.kt          # UEFI + Legacy BIOS boot setup
│   └── WinDroidViewModel.kt          # App state management
│
├── tweaks/
│   ├── TweakConfig.kt                # ✨ Data model — XP/7/8/10/11 config
│   └── AutounattendGenerator.kt      # ✨ Generates winnt.sif + autounattend.xml
│
└── ui/
    ├── theme/                         # Colors, typography
    └── screens/
        ├── HomeScreen.kt              # ISO/USB picker + version selector
        ├── TweaksScreen.kt            # All tweak toggles
        ├── ProgressScreen.kt          # Flash progress + live log
        └── Navigation.kt              # Screen router
```

---

## 🤝 Contributing

PRs welcome! Areas where contributions are especially appreciated:

- [ ] NTFS write support via `libntfs-3g` JNI binding (for > 4 GB single-file ISOs without splitting)
- [ ] ARM / ARM64 EFI bootloader support
- [ ] More Windows XP / 7 tweaks
- [ ] Dark/light theme toggle in UI
- [ ] Disk speed benchmark before flash

---

## 📜 License

GPL-3.0 — same as [Rufus](https://github.com/pbatard/rufus), which inspired this project.

---

## 🙏 Credits

- Inspired by [Rufus](https://github.com/pbatard/rufus) by Pete Batard
- ISO 9660 spec: [ECMA-119](https://www.ecma-international.org/publications-and-standards/standards/ecma-119/)
- Created by **Raunak Singh**

---

<div align="center">

*WinDroid — because flashing Windows shouldn't need a PC.*

</div>
