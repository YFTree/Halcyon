<!--suppress ALL -->

<h1 align="center">Ella Music</h1>

<p align="center">
  <b>An Android music player tailored for a MIUI / HyperOS-style experience</b>
</p>

<p align="center">
  <a href="https://github.com/Kifranei/Ella/releases">
    <img src="https://img.shields.io/github/v/release/Kifranei/Ella?style=flat&color=6750A4" alt="Version">
  </a>
  <a href="https://github.com/Kifranei/Ella/releases">
    <img src="https://img.shields.io/github/downloads/Kifranei/Ella/total?style=flat&color=orange" alt="Downloads">
  </a>
  <a href="https://github.com/Kifranei/Ella/commits">
    <img src="https://img.shields.io/github/last-commit/Kifranei/Ella?style=flat" alt="Last Commit">
  </a>
  <a href="https://github.com/Kifranei/Ella/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/Kifranei/Ella?style=flat" alt="License">
  </a>
  <a href="README.md">
    <img src="https://img.shields.io/badge/Document-Chinese-red.svg" alt="CN">
  </a>
</p>

<p align="center">
  <b>Local Music · WebDAV · LX Online Sources · Word-level Lyrics · Desktop Lyrics · Status-bar Lyrics</b>
</p>

---

## ✨ Overview

**Ella Music** is an Android music player built with **Jetpack Compose, Miuix, and AndroidX Media3**.

It is not just a basic local music player. It also includes WebDAV remote library support, LX Music API online sources, LRC / enhanced LRC / TTML lyric parsing, desktop lyric overlay, Lyricon integration, SuperLyric integration, Bluetooth lyrics, FFmpeg extension decoding, and music library analytics.

The app is designed around a **MIUI / HyperOS-inspired** visual and interaction style, aiming to provide a lightweight, modern, and lyric-focused music experience on Android.

---

## 🚀 Features

### 🎵 Local music playback

- Local music scanning, search, playback, and folder browsing.
- Search, sorting, fast indexing, and multi-select management for home, album, folder, and artist pages.
- Album pages, artist pages, song lists, current queue, mini player, and immersive now-playing screen.
- CJK titles can participate in A-Z sorting through Latinized sort keys, with cached keys to reduce home-screen stalls.
- Library analytics for play-count ranking, listen-time ranking, format distribution, and quality distribution.

### 🌐 WebDAV remote library

- WebDAV library configuration, connection testing, remote directory browsing, and remote audio playback.
- WebDAV paths support Chinese characters, spaces, and other special characters.
- Remote folders remember the last opened path and cache folder results for a smoother browsing experience.
- WebDAV has been moved into its own page for clearer remote-library management.

### 🔎 LX online music

- Multi-source LX Music API import and centralized source management.
- Import sources from URLs or local JS files.
- Single-source search, online playback, and downloads to `Music/Ella/`.
- Online artwork display and Kuwo lyric fetching for online music.
- Online song downloads are available from the now-playing overflow menu.

### 🎤 Lyric experience

- LRC, enhanced LRC, and TTML lyric parsing.
- Word-level lyrics, translations, background words, `x-bg` background vocals, and TTML duet layout.
- External LRC and embedded lyric reading.
- Fallback support for common Chinese lyric encodings.
- Custom lyric-page fonts from system-font previews or imported TTF / OTF / TTC files.
- Cover background and immersive lyric presentation on the now-playing screen.

### 🪟 Desktop and system lyrics

- Desktop lyric floating overlay.
- Double-tap controls, auto-hide, and screen-bounded dragging.
- Desktop lyric controls for play / pause, previous, next, font size, lock, and close.
- TTML two-line lyrics, left / right alignment, and duet display.
- Lyricon Provider integration.
- SuperLyric, Flyme / AOSP ticker lyric notifications, and Bluetooth lyric support.

### 🎚 Playback and decoding

- WAV, FLAC, M4A, OGG, OPUS, and other audio tag reading.
- System, FFmpeg, and Auto decoder modes.
- FFmpeg is the default decoder mode.
- FFmpeg extension decoder improves compatibility for ALAC / M4A and other formats.
- ReplayGain volume normalization.
- Sleep timer, stop-after-current, playback speed, pitch control, and online song downloads.

### 🎨 UI and settings

- MIUI / HyperOS-style settings and UI components based on Miuix.
- Default floating bottom navigation.
- MiniPlayer circular progress ring and song / lyric transition animation.
- Now-playing page adjusted toward full-cover background and rounded bottom controls.
- Theme switching and common playback, lyric, scanning, and decoder settings.

---

## 📱 Requirements

| Item | Requirement |
|:--|:--|
| Android version | Android 10.0 / API 29 or later |
| Target SDK | Android API 37 |
| Default ABI | `arm64-v8a` |
| Network | Required for WebDAV, LX online sources, and online lyrics |
| Overlay permission | Required for desktop lyrics |
| Notification permission | Required on Android 13 and later |

---

## 📦 Download

Download the latest build from [Releases](https://github.com/Kifranei/Ella/releases).

Recommended first-time setup:

1. Install Ella Music.
2. Grant music file access permission.
3. Wait for the local music scan to complete.
4. Grant overlay permission if you want to use desktop lyrics.
5. Configure WebDAV if you use a remote music library.
6. Import LX Music API sources if you need online music.
7. Enable Lyricon / SuperLyric / Ticker / Bluetooth lyric options as needed.

---

## 🛠 Build

```bash
git clone https://github.com/Kifranei/Ella.git
cd Ella
./gradlew assembleDebug
```

Release builds first read the following environment variables:

```bash
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

If these variables are not set, the project-root `release.jks` will be used with the default alias `release`.

---

## 🎧 FFmpeg

Prebuilt FFmpeg static libraries are included here:

```text
ffmpeg-decoder/src/main/jni/ffmpeg/android-libs
```

To rebuild them on Windows, run:

```powershell
.\build_ffmpeg.ps1
```

The script builds FFmpeg through WSL with the Linux Android NDK.

---

## 🧩 Ecosystem

| Category | Capability |
|:--|:--|
| Local music | Scanning, search, playback, folder browsing, album / artist management |
| Remote music | WebDAV connection testing, directory browsing, remote playback |
| Online music | LX Music API source import, search, streaming, downloads |
| Lyrics | LRC, enhanced LRC, TTML, word-level lyrics, translation, background vocals, duet layout |
| System lyrics | Desktop lyrics, Lyricon, SuperLyric, ticker notifications, Bluetooth lyrics |
| Decoding | Media3, system decoder, FFmpeg extension decoder |
| Audio metadata | TagLib, Jaudiotagger, artwork, tags, embedded lyrics |
| Analytics | Format distribution, quality distribution, play-count ranking, listen-time ranking |
| UI | Jetpack Compose, Miuix, floating bottom navigation, theme switching |

---

## 🧱 Open-source projects

| Project | Purpose |
|:--|:--|
| [Miuix](https://github.com/miuix-kmp/miuix) | MIUI / HyperOS-style Compose UI components |
| [AndroidX Media3](https://github.com/androidx/media) | Playback, media session, and ExoPlayer FFmpeg extension |
| [FFmpeg](https://ffmpeg.org) | Software decoding for ALAC and other audio formats |
| [Lyricon](https://github.com/proify/lyricon) | Lyric Provider API and status-bar lyrics |
| [SuperLyricApi](https://github.com/HChenX/SuperLyricApi) | SuperLyric publishing API |
| [Jaudiotagger](https://github.com/Adonai/jaudiotagger) | Audio tags, embedded lyrics, and embedded artwork |
| [Kyant TagLib](https://github.com/Kyant0/TagLib) | Android / Kotlin TagLib binding |
| [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | Liquid glass and backdrop blur effects |
| [Coil](https://github.com/coil-kt/coil) | Image loading for Compose |
| [QuickJS Android](https://github.com/HarlonWang/quickjs-wrapper-android) | Running LX Music API JavaScript sources |

---

## 👥 Credits

- **Codex (GPT-5.5)** — Lead development and code collaboration since 1.0.2.
- **Mimo-V2.5-Pro** — Lead development for early 1.0.0 to 1.0.1 builds.
- **BetterLyrics** — Visual reference for the blurred cover background and lyric presentation.
- **SPlayer** — Visual reference for now-playing motion and lyric experience.
- **Retro Music Player** — Reference for the jaudiotagger-based tag reading approach.
- Thanks to Miuix, Media3, FFmpeg, Lyricon, SuperLyricApi, Jaudiotagger, Kyant TagLib, Backdrop, Coil, and the other open-source projects used by Ella Music.

---

## ⭐ Star History

<p align="center">
  <a href="https://www.star-history.com/#Kifranei/Ella&Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Kifranei/Ella&type=Date&theme=dark" />
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=Kifranei/Ella&type=Date" />
      <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=Kifranei/Ella&type=Date" width="600" />
    </picture>
  </a>
</p>

---

## 👀 Visit Statistics

<p align="center">
  <img src="https://count.getloli.com/get/@kifranei_ella?theme=moebooru-h" alt="Visitor Count" />
</p>

---

## 📄 License

Ella Music is licensed under **GPL-3.0-or-later**. See [LICENSE](LICENSE).
