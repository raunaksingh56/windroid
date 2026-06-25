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
[![License](https://img.shields.io/badge/License-GPL--3.0-blue?style=for-the-badge)](https://github.com/raunaksingh56/windroid/blob/main/LICENSE)
[![No Root](https://img.shields.io/badge/Root-Not%20Required-success?style=for-the-badge)](https://github.com/raunaksingh56/windroid)
[![Windows](https://img.shields.io/badge/Windows-XP%20→%2011-0078D4?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/raunaksingh56/windroid)
[![Build](https://img.shields.io/github/actions/workflow/status/raunaksingh56/windroid/build.yml?style=for-the-badge&label=CI)](https://github.com/raunaksingh56/windroid/actions)

> *No PC. No root. No BIOS fiddling. Just your phone and a USB drive.*

---

## ✨ What is WinDroid?

**WinDroid** is an open-source Android app that turns your phone into a bootable Windows USB creator. Plug in a USB OTG drive, pick your ISO, and flash — the USB boots on any PC automatically. No PC required, no root, no BIOS changes.

| Version | Answer File | Notes |
|---------|-------------|-------|
| 🪟 Windows XP | `winnt.sif` | Legacy text-format, full unattend |
| 🪟 Windows 7 | `autounattend.xml` | ei.cfg injection, activation skip |
| 🪟 Windows 8 / 8.1 | `autounattend.xml` | Online account bypass, SmartScreen off |
| 🪟 Windows 10 | `autounattend.xml` | Telemetry & Cortana off, offline setup |
| 🪟 Windows 11 | `autounattend.xml` | TPM / SecureBoot / RAM bypass, offline setup |

---

## 🆕 Changelog

### v1.1.0 — Bug Fix + Feature Release

#### 🐛 Critical Fixes

| # | File | Bug | Impact |
|---|------|-----|--------|
| 1 | `IsoExtractor.kt` | `splitAndWrite` buffered full WIM/ESD chunk (up to 3.8 GB) in a `ByteArrayOutputStream` before writing | **OOM crash** on every Windows 11 flash — install.wim routinely exceeds 4–5 GB |
| 2 | `IsoExtractor.kt` | `InputStream` never closed after each file — one FD leaked per file | **FD exhaustion crash** on large ISOs (thousands of files) |
| 3 | `HybridBootManager.kt` | `dd bs=446 count=1` only wrote 446 bytes — the `0x55AA` boot signature was silently never reaching the USB | USB **never bootable** on blank/corrupted drives even after MBR write "succeeded" |
| 4 | `HybridBootManager.kt` | `lastOrNull()` on `ls /dev/block/sd*` could pick internal eMMC as "USB drive" | Risk of writing MBR directly to **internal storage** |

#### 🐛 High Severity Fixes

| # | File | Bug | Impact |
|---|------|-----|--------|
| 5 | `HybridBootManager.kt` | `readMBR` called `waitFor()` after `readBytes()` — deadlock on some root environments | App **hangs forever** mid-flash |
| 6 | `HybridBootManager.kt` | `writeMBRRoot` read stderr after `waitFor()` — deadlock if stderr buffer fills | App **hangs** on slow USB writes |
| 7 | `AutounattendGenerator.kt` | `disableCortana` emitted `<DisableAutoDaylightTimeSet>` — completely wrong XML element | Cortana **never actually disabled** despite toggle being on |
| 8 | `UsbWriter.kt` | `ei.cfg` generated but never written — only `autounattend.xml` was ever flashed | Windows 7/8 **edition selection broken** entirely |

#### 🐛 Medium Severity Fixes

| # | File | Bug | Impact |
|---|------|-----|--------|
| 9 | `IsoExtractor.kt` | Joliet `trimEnd(';','1',' ')` corrupted filenames ending in digits | `setup1` → `setup`, `boot1.txt` → `boot.txt` |
| 10 | `UsbWriter.kt` | `getOrCreateDir` called `createDocument` unconditionally — duplicated folders on re-flash | `sources/` created twice, `ei.cfg` injection fails |
| 11 | `WinDroidViewModel.kt` | `isFlashing` stayed `true` on error — progress spinner kept spinning after failure | UI **stuck** showing "Flashing..." forever on error |
| 12 | `WinDroidViewModel.kt` | `generateAll()` called on main thread before IO coroutine | Potential **ANR** when version detection is added |
| 13 | `HomeScreen.kt` | `activeTweaks` count missing 5 toggles (BypassNRO, SecureBoot, RAM, CPU, etc.) | Badge always showed **wrong number** |
| 14 | `HybridBootManager.kt` | Truncated 50-byte fallback MBR stub silently written instead of failing clearly | Legacy BIOS boot appeared to succeed but PC **never booted** |

#### ✨ New Features

- **Windows 10 / 11 Offline Account Setup** (`BypassNRO`) — previously "Skip Microsoft Account" only hid the account UI; Setup still blocked until internet connected. Now injects `BypassNRO` + `BypassNROCheck` into `LabConfig` during WinPE — the actual network gate is removed, not just hidden. Toggle in Tweaks screen, on by default.
- **USB block device detection via `/sys/block/sdX/removable`** — replaces the dangerous `lastOrNull()` heuristic with kernel-reported removability flags.
- **Streaming WIM/ESD chunk writes** — replaced `ByteArrayOutputStream` buffering with direct 8 MB incremental USB writes. Windows 11 flashes no longer risk OOM.
- **Cortana disable via `FirstLogonCommands`** — correct registry key (`AllowCortana=0`) instead of the wrong `DisableAutoDaylightTimeSet` element.
- **CI fixed** — added `permissions: contents: write` to `build.yml` so GitHub Actions can create releases.

---

## 🚀 Features

### Core Engine

```
┌──────────────────────────────────────────────────────────────────┐
│  ISO 9660 Parser → USB Writer → Boot Setup → Answer File Inject  │
└──────────────────────────────────────────────────────────────────┘
```

- ✅ **Full ISO 9660 + Joliet parser** — reads any Windows ISO natively, no dependencies
- ✅ **Streaming WIM/ESD split** — handles install.wim > 4 GB without buffering in RAM
- ✅ **No root required** — uses Android USB Host API + Storage Access Framework
- ✅ **Dual boot** — UEFI 64-bit + Legacy BIOS MBR automatically
- ✅ **32-bit UEFI** — bootia32.efi for Bay Trail tablets (root only)
- ✅ **Auto-launch on USB plug** — opens when you connect OTG drive
- ✅ **Live progress log** — see exactly what's happening during flash
- ✅ **Safe USB detection** — uses `/sys/block/removable` to avoid writing to internal storage

### Unattended Setup Tweaks

**👤 Account**
- Skip Microsoft Account — local account, no internet required
- Custom username & password
- Auto-login on first boot

**🌐 Online Requirement Bypass (Win 10 / 11)**
- `BypassNRO` + `BypassNROCheck` injected at WinPE stage
- Removes the "you must connect to the internet" gate — not just the UI
- Works on Windows 10 (2004+) and Windows 11

**🛡️ Windows 11 Hardware Bypasses**
- TPM 2.0, Secure Boot, RAM, CPU compatibility checks

**🔒 Privacy**
- Disable telemetry, Cortana, data collection, Advertising ID

**⚙️ Setup**
- Skip EULA, privacy questions, auto region
- `ei.cfg` edition injection (Home / Pro / Enterprise / Education)

**🪟 XP-Specific**
- Product key, auto-format, OEM name, disable MSN Messenger

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
3. Tap **Select ISO** → pick your `.iso` file
4. Tap **Select USB** → pick the OTG drive
5. Pick **Windows Version** or leave on Auto-Detect
6. Configure tweaks — defaults work great, BypassNRO is on by default for Win 10/11
7. Tap **Flash Windows to USB**
8. Plug USB into your PC → boot → Windows installs automatically!

---

## 🔧 How It Works

### ISO 9660 Parser

```
ISO file
  └── Sector 16 ─► Primary Volume Descriptor
        └── Root directory record
              ├── /sources/install.wim  ← split if > 4 GB (streamed, no RAM buffering)
              ├── /EFI/BOOT/bootx64.efi
              ├── /bootmgr
              └── ... (all files, FDs closed after each)
```

### Answer File Generation

```
Windows XP  →  winnt.sif              (root of USB)
Windows 7+  →  autounattend.xml       (root of USB)
Windows 7+  →  sources/ei.cfg         (edition override — now actually written)
```

### Online Account Bypass

```
windowsPE pass
  └── RunSynchronousCommand
        ├── reg add LabConfig /v BypassNRO      /d 1
        └── reg add LabConfig /v BypassNROCheck /d 1

oobeSystem pass
  └── NetworkLocation = Home
```

### Hybrid Boot

```
USB Drive
├── EFI/BOOT/bootx64.efi   ← UEFI 64-bit  (Level 1 — always, no root)
├── EFI/BOOT/bootia32.efi  ← UEFI 32-bit  (Level 3 — root only)
├── bootmgr                ← Legacy BIOS   (Level 1 — always)
└── Boot/BCD               ← Boot store
    MBR boot sector        ← Written via dd bs=512 (Level 2 — root only)
```

---

## 🏗️ Building from Source

```bash
git clone https://github.com/raunaksingh56/windroid
cd windroid
./gradlew assembleRelease
```

**Requirements:** Android Studio Hedgehog (2023.1) or later, JDK 17.

---

## 📁 Project Structure

```
app/src/main/kotlin/com/raunaksingh/windroid/
│
├── MainActivity.kt
│
├── core/
│   ├── IsoExtractor.kt        # ISO 9660 + Joliet parser, streaming WIM split
│   ├── UsbWriter.kt           # SAF USB write, all answer files, safe dir creation
│   ├── HybridBootManager.kt   # UEFI + BIOS boot, safe block device detection
│   └── WinDroidViewModel.kt   # State management, IO-dispatched flash
│
├── tweaks/
│   ├── TweakConfig.kt         # Config model incl. BypassNRO
│   └── AutounattendGenerator.kt  # winnt.sif + autounattend.xml + ei.cfg
│
└── ui/screens/
    ├── HomeScreen.kt          # ISO/USB picker, correct tweak count badge
    ├── TweaksScreen.kt        # All toggles incl. online-requirement bypass
    ├── ProgressScreen.kt      # Progress + error state (isFlashing fixed)
    └── Navigation.kt
```

---

## 🤝 Contributing

PRs welcome!

- [ ] NTFS write support (for > 4 GB single-file ISOs without WIM splitting)
- [ ] ARM / ARM64 EFI bootloader
- [ ] More Windows XP / 7 tweaks
- [ ] Disk speed benchmark before flash

---

## 📜 License

GPL-3.0 — same as [Rufus](https://github.com/pbatard/rufus), which inspired this project.

---

## 🙏 Credits

- Inspired by [Rufus](https://github.com/pbatard/rufus) by Pete Batard
- ISO 9660 spec: [ECMA-119](https://www.ecma-international.org/publications-and-standards/standards/ecma-119/)
- Created by **Raunak Singh** · [@raunaksingh56](https://github.com/raunaksingh56)

---

*WinDroid — because flashing Windows shouldn't need a PC.*
