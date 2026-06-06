<!--suppress ALL -->

<h1 align="center">Halcyon</h1>

<p align="center">
  <b>formerly Ella Music, an Android Music Player Inspired by MIUI / HyperOS</b>
</p>

<p align="center">
  <a href="https://github.com/Kifranei/Halcyon/releases"><img src="https://img.shields.io/github/v/release/Kifranei/Halcyon?style=flat&color=6750A4" alt="Version"></a>
  <a href="https://github.com/Kifranei/Halcyon/releases"><img src="https://img.shields.io/github/downloads/Kifranei/Halcyon/total?style=flat&color=orange" alt="Downloads"></a>
  <a href="https://github.com/Kifranei/Halcyon/commits"><img src="https://img.shields.io/github/last-commit/Kifranei/Halcyon?style=flat" alt="Last Commit"></a>
  <a href="https://github.com/Kifranei/Halcyon/blob/main/LICENSE"><img src="https://img.shields.io/github/license/Kifranei/Halcyon?style=flat" alt="License"></a>
  <a href="README.md"><img src="https://img.shields.io/badge/Document-Chinese-red.svg" alt="CN"></a>
</p>

<p align="center">
  <b>Local Music · Local Playlists · WebDAV · Dynamic Player UI · Word-by-Word Lyrics · Floating Lyrics · Status Bar Lyrics</b>
</p>

---

## ✨ Overview

**Halcyon** (formerly **Ella Music**) is an Android local music player built with **Jetpack Compose, Miuix, and AndroidX Media3**.

It focuses on local music and lyrics, with a MIUI / HyperOS-inspired interface, word-by-word lyrics, floating lyrics, status-bar lyrics, dynamic covers, WebDAV remote libraries, LX Music API sources, library analytics, and a highly customizable player experience.

---

## 🚀 Features

### 🎵 Library & Playlists

- Supports local MediaStore scanning and custom folder scanning, with browsing by album, artist, folder, genre, year, composer, and lyricist.
- Provides a dedicated library search page with song, album, artist, lyric, duplicate-song, and full-tag search, plus search history.
- Supports local playlists, favorites, five-star songs, playlist import / export, desktop shortcuts, and custom drag sorting.
- Album grouping uses both album name and album artist to avoid merging same-name albums from different artists.
- Includes library analytics, listening calendar, play-count ranking, listening-duration ranking, format distribution, and quality distribution.

### 🖼 Player UI & Dynamic Covers

- Provides an immersive lyric page, landscape lyric page, and landscape stacked-cover page.
- Supports dynamic video covers matched by song, album, or global fallback.
- Supports global custom wallpapers, launch posters, custom Hi-Res badges, and optional player button outlines.
- Non-immersive player covers can show a Hi-Res / MQ badge.
- The player supports pull-down dismissal, dynamic backgrounds, blurred cover backgrounds, and landscape queue-cover switching.

### 🎤 Lyrics

- Supports LRC, Enhanced LRC, ELRC, TTML, and Lyricify lyric parsing.
- Supports word-by-word lyrics, translations, romanization / phonetics, background vocals, TTML duets, and ELRC V1/V2 duet tags.
- Reads embedded lyrics and external lyric files, including matching `.lrc`, `.ttml`, and `.elrc` files.
- Provides floating desktop lyrics, status-bar lyrics, media notification lyrics, lyric barrage, SuperLyricApi, and Lyric Getter API integration.
- Supports lyric card sharing, font import, lyric offset, tap-to-seek, and secondary-line configuration.

### 🌐 WebDAV & LX Online Music

- Supports WebDAV remote libraries with connection testing, Digest authentication, remote browsing, and remote playback.
- Supports LX Music API sources, online search, streaming playback, cover / lyric retrieval, and local downloads.

### 🎚 Decoding, Tags & Audio Quality

- Uses lyrico-audiotag as the primary local metadata path, supporting artwork, basic tags, and embedded lyrics for common audio formats.
- Provides system, FFmpeg, and automatic decoding modes for better ALAC / AAC / M4A compatibility.
- Supports ReplayGain, shuffle queue restoration, quality labels, and 24-bit / 96 kHz recognition.
- Supports 163 key reading from standalone tags, Comment, and Description fields.

### 🎨 UI & Integrations

- Built with Miuix for a MIUI / HyperOS-inspired interface, including floating bottom navigation, MiniPlayer, blur / Liquid Glass effects, and unified sheets.
- Supports basic Chinese / English UI, in-app language switching, GitHub update page, app logs, backup / restore, and data export.
- Supports song information, tag editing, lyric timing tools, external tag-editor adaptation, and AI song interpretation.
- Supports MediaSession custom commands for favorite and playback-mode controls in notifications / control centers.

---

## 📱 Requirements

| Item | Requirement                                                             |
|:--|:------------------------------------------------------------------------|
| Android Version | Android 11 / API 30 or higher                                           |
| Target SDK | Android 17 / API 37                                                     |
| Default ABI | `arm64-v8a`                                                             |
| Network | Required for WebDAV, LX online sources, and online lyrics               |
| Video Permission | Android 13+ may require video media permission for dynamic video covers |
| Overlay Permission | Required when using floating lyrics                                     |
| Notification Permission | Required on Android 13 and above                                        |

---

## 📦 Download

Download the latest version from [Releases](https://github.com/Kifranei/Halcyon/releases).

Recommended first-time setup:

1. Install Halcyon.
2. Grant music file access permission and choose a scanning mode (media library scanning or custom folder scanning).
3. After scanning completes, the app is ready to use. To display lyrics on other pages, enable the option in the settings page.
4. Configure WebDAV manually if using a remote library.
---

## 🖼 Dynamic Video Covers

Dynamic video covers are used in the playback page cover area. Album-level configuration is recommended:

```text
Music/
├── Album Name.mp4
├── Song A.flac
├── Song B.flac
└── Song C.flac
```

or
```text
Music/Album Name Folder/
├── cover.mp4
├── Song A.flac
├── Song B.flac
└── Song C.flac
```

All songs in the same album can share the same video, avoiding duplicate video files for each song.

Centralized management is supported:

```text
Movies/Ella/DynamicCovers/
├── Album/
│   └── Album Name.mp4
├── Song/
│   └── Artist - Title.mp4
└── cover.mp4
```

Single-file configuration is also supported:
```text
Music/Song File Name.m4a
Music/Song File Name.mp4
```

Actual matching order depends on the implementation: it usually checks the song's local folder first, then checks DynamicCovers for song/album videos, and finally uses the global fallback video.

---

## 🛠 Build

```bash
git clone https://github.com/Kifranei/Halcyon.git
cd Halcyon
./gradlew :app:assembleDebug -PellaAbi=arm64-v8a
```

Windows PowerShell:

```powershell
git clone https://github.com/Kifranei/Halcyon.git
cd Halcyon
.\gradlew.bat :app:assembleDebug -PellaAbi=arm64-v8a
```

Release builds prioritize the following environment variables:

```bash
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

If these variables are not set, the build uses `release.jks` in the project root. If no usable release keystore is available, the release build fails directly to avoid accidentally producing a release package signed with a debug key.

For daily development, use `assembleDebug` for validation. `fastRelease` / release builds are intended for release preparation only. Native libraries are packaged from prebuilt `.so` files by default; rerun the corresponding scripts only when updating FFmpeg or lyrico-audiotag native outputs. Push each completed commit to both GitHub and GitLab remotes.

---

## 🎧 Native Libraries

Prebuilt FFmpeg and lyrico-audiotag native libraries are located at:

```text
ffmpeg-decoder/src/main/jniLibs/arm64-v8a/libffmpegJNI.so
lyrico-audiotag/src/main/jniLibs/arm64-v8a/liblyrico_taglib.so
```

To restore FFmpeg prebuilt inputs after a fresh clone, run:

```powershell
.\scripts\download_ffmpeg_prebuilt.ps1
```

To update FFmpeg native outputs manually on Windows, run:

```powershell
.\build_ffmpeg.ps1
```

To update the lyrico-audiotag / TagLib native output, run:

```powershell
.\build_lyrico_taglib.ps1
```

Normal `assembleDebug` builds do not rebuild native code by default. Before release, verify the APK contains the required arm64-v8a `.so` files. For routine builds, the full FFmpeg source tree is unnecessary.

`liblyrico_taglib.so` is the native tag read/write output from lyrico-audiotag.

---

## 🧱 Open Source & Licenses

The Halcyon main project is licensed under **Apache-2.0**. Third-party components retain their own licenses; see [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).

---

## 👥 Credits

- **BetterLyrics** — Visual reference for blurred cover backgrounds and lyric display.
- **SPlayer** — Visual reference for playback page animations and lyric experience.
- **Lyrico** — Reference for external tag editor adaptation and log page interaction.
- **LX Music Mobile** — Provides LX Music API compatibility implementation and testing reference.
- **Light Cone Music** — Interface design and feature implementation reference.
- Thanks to Miuix, Media3, FFmpeg, Lyricon, SuperLyricApi, LyricGetter-API, lyrico-audiotag / Lyrico, TagLib, 163KeyDecrypter, Kyant Backdrop, Coil, OkHttp, accompanist-lyrics-core, accompanist-lyrics-ui, and other open source projects used by Halcyon.

---

## ⭐ Star History

<p align="center">
  <a href="https://www.star-history.com/#Kifranei/Halcyon&Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=Kifranei/Halcyon&type=Date&theme=dark" />
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=Kifranei/Halcyon&type=Date" />
      <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=Kifranei/Halcyon&type=Date" width="600" />
    </picture>
  </a>
</p>

---

## 👀 Visitor Count

<p align="center">
  <img src="https://count.getloli.com/get/@kifranei_halcyon?theme=capoo-2" alt="Visitor Count" />
</p>
