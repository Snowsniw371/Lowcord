# Lowcord
Lightweight Android WebView client for Discord built with Kotlin. Features persistent sessions, WebRTC voice support, file uploads, and dark/light theming.

<div align="center">

  <img src="app/src/main/res/drawable/app_icon.jpg" alt="Lowcord Logo" width="110" />

  # Lowcord

  **A lightweight, fast Android WebView client for Discord built with Kotlin.**

  [![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
  [![Language](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Release](https://img.shields.io/github/v/release/Snowsniw371/Lowcord?style=for-the-badge&color=5865F2)](https://github.com/Snowsniw371/Lowcord/releases)
  [![License](https://img.shields.io/github/license/Snowsniw371/Lowcord?style=for-the-badge&color=lightgrey)](LICENSE)

  [Features](#-features) • [Comparison](#-why-lowcord) • [Installation](#-installation) • [Tech Stack](#-tech-stack)

</div>

---

## ✨ Overview

**Lowcord** wraps the Discord web client in a finely tuned native Android container. It is built for devices where the official app feels heavy, lags during scrolling, or drains excessive RAM and battery—while keeping background voice calls, file sharing, AMOLED theming, and per-app proxy support intact.

> *Disclaimer: Lowcord is an unofficial, independent WebView wrapper and is not affiliated with, endorsed, or sponsored by Discord Inc.*

## ⚡ Why Lowcord?

| Metric / Feature | Official Discord App | Lowcord |
| :--- | :---: | :---: |
| **APK Size** | ~150+ MB | **~2–4 MB** |
| **RAM Footprint** | High (heavy background services) | **Low (WebView engine)** |
| **No-Lag Performance Mode** | ❌ Heavy blur & animations | ✅ **Strips blur & animations** |
| **Background Voice Calls** | ✅ Supported | ✅ **Foreground Service** |
| **Pure Black AMOLED Theme** | Limited | ✅ **True `#000000` CSS** |
| **Built-in Per-App Proxy** | ❌ System VPN required | ✅ **SOCKS5 / HTTP inside app** |
| **File Uploads & Downloads** | ✅ Supported | ✅ **Native Android picker & DM** |

## 🚀 Features

- **Background Voice Calls (`VoiceService`):** Hooks into WebRTC audio streams via a native JS bridge (`LowcordBridge`) and runs an Android Foreground Service with microphone support, keeping voice channels alive when the screen is locked or the app is minimized.
- **No-Lag Performance Mode:** Injects lightweight CSS rules (`WebTweaks`) that disable GPU-heavy `backdrop-filter` blur effects and clamp UI animations/transitions to `0.01ms` for smooth scrolling on budget hardware.
- **Mobile-Friendly Collapsible Sidebar:** Combines desktop browser spoofing (required for WebRTC voice access) with custom touch gestures and floating controls (`☰` / `⚙`) that automatically hide the server/channel drawer when a chat is opened.
- **Pure Black AMOLED Theme:** Applies true `#000000` styling across chat containers, sidebars, and system status/navigation bars to save battery on OLED displays.
- **Built-in Per-App Proxy:** Uses `androidx.webkit.ProxyController` to route only Lowcord's traffic through a custom `SOCKS5` or `HTTP` proxy without affecting other apps on your device.
- **Native File Picker & Download Manager:** Integrates Android's system file chooser (`onShowFileChooser`) for attaching media and `DownloadManager` for saving attachments directly to `Downloads`.
- **Persistent Sessions & Cache Control:** Full `CookieManager` (flushed on pause) and DOM storage integration keeps you logged in across restarts, paired with a one-tap cache cleaner in settings.

## 📥 Installation

1. Go to the **[Releases](https://github.com/Snowsniw371/Lowcord/releases/latest)** page.
2. Download the latest signed **`app-release.apk`** from the **Assets** section.
3. Open the downloaded file on your Android device and allow installation from your browser or file manager if prompted.

> [!NOTE]
> Minimum supported version is **Android 7.0 Nougat (API 24)**. Make sure **Android System WebView** is up to date via Google Play for the best WebRTC and rendering performance.

## 🛠️ Tech Stack

<details>
<summary><b>Click to expand architecture & permissions details</b></summary>

- **Language & UI:** Kotlin, XML Views, Material Components
- **Architecture:**
  - `MainActivity.kt` — WebView lifecycle, native settings dialog, file chooser, and `DownloadManager` integration.
  - `VoiceService.kt` — Foreground microphone service keeping WebRTC voice calls active in the background.
  - `WebTweaks.kt` — Runtime CSS/JS injection for performance mode, AMOLED theme, sidebar drawer gestures, and WebRTC hooks.
  - `PrefsManager.kt` — `SharedPreferences` wrapper for user settings and proxy configuration.
- **Core Libraries:** `androidx.webkit:webkit` (`ProxyController`), `WebView`, `WebChromeClient`, `CookieManager`
- **Declared Permissions:**
  - `INTERNET` — Core network connectivity.
  - `RECORD_AUDIO` & `MODIFY_AUDIO_SETTINGS` — Voice channel microphone input via WebRTC.
  - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE` — Background voice call persistence.
  - `POST_NOTIFICATIONS` — Active voice call status notification (Android 13+).
  - `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`) — Attachment downloads on Android 7.0–9.0.
- **Build Environment:** Developed, compiled, and signed directly on Android using **Code on the Go**.

</details>

## 📄 License

Distributed under the **MIT License**. See [LICENSE](LICENSE) for more details.
