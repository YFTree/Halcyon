<!--suppress ALL -->

<h1 align="center">Ella Music</h1>

<p align="center">
  <b>面向 MIUI / HyperOS 风格体验的 Android 音乐播放器</b>
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
  <a href="README_en.md">
    <img src="https://img.shields.io/badge/Document-English-red.svg" alt="EN">
  </a>
</p>

<p align="center">
  <b>本地音乐 · WebDAV · LX 在线源 · 逐字歌词 · 桌面歌词 · 状态栏歌词</b>
</p>

---

## ✨ 项目简介

**Ella Music** 是一款基于 **Jetpack Compose、Miuix 与 AndroidX Media3** 构建的 Android 音乐播放器。

它不只是简单的本地音乐播放器，也集成了 WebDAV 远程音乐库、LX Music API 在线源、LRC / 增强 LRC / TTML 歌词解析、桌面歌词悬浮窗、Lyricon 词幕适配、SuperLyric 适配、蓝牙歌词、FFmpeg 扩展解码和音乐库统计等功能。

项目整体视觉和交互偏向 **MIUI / HyperOS** 风格，适合希望在 Android 上获得轻量、现代、歌词体验完整的音乐播放场景。

---

## 🚀 功能特性

### 🎵 本地音乐播放

- 本地音乐扫描、搜索、播放和文件夹浏览。
- 首页、专辑页、文件夹页和艺人页支持搜索、排序、快速定位与多选管理。
- 支持专辑页、歌手页、歌曲列表、当前播放队列、迷你播放条和沉浸式播放页。
- 中日韩标题可按拉丁化读音参与 A-Z 排序，并缓存排序键以减少首页卡顿。
- 支持播放次数排行、听歌时长排行、格式占比和音质占比等曲库分析。

### 🌐 WebDAV 远程音乐库

- 支持 WebDAV 音乐库配置、连接测试、远程目录浏览和远程音频播放。
- WebDAV 路径支持中文、空格等特殊字符。
- 远程目录会记住最近打开位置，并缓存目录结果以提升浏览体验。
- WebDAV 已独立为单独页面，便于管理远程音乐库。

### 🔎 LX 在线音乐

- 支持 LX Music API 多源导入和集中管理。
- 支持通过 URL 或本地 JS 文件导入音乐源。
- 支持单源搜索、在线播放和下载到 `Music/Ella/`。
- 支持在线音乐封面显示和 Kuwo 歌词获取。
- 播放页右上角菜单提供在线歌曲下载入口。

### 🎤 歌词体验

- 支持 LRC、增强 LRC、TTML 歌词解析。
- 支持逐字歌词、翻译、背景词、`x-bg` 背景人声和 TTML 对唱布局。
- 支持外置 LRC 与内嵌歌词读取。
- 支持常见中文歌词编码回退。
- 歌词页字体可自定义，支持预览系统字体或导入 TTF / OTF / TTC 字体文件。
- 播放页提供封面背景、歌词展示和沉浸式视觉体验。

### 🪟 桌面歌词与系统歌词

- 支持桌面歌词悬浮窗。
- 支持双击显示控制按钮、自动隐藏、屏幕范围内拖动。
- 桌面歌词支持播放 / 暂停、上一首、下一首、字号调节、锁定和关闭。
- 支持 TTML 双行歌词、左右对齐和对唱显示。
- 支持 Lyricon 词幕 Provider。
- 支持 SuperLyric、Flyme / AOSP Ticker 歌词通知和蓝牙歌词适配。

### 🎚 播放与解码

- 支持 WAV、FLAC、M4A、OGG、OPUS 等格式标签读取。
- 支持系统、FFmpeg、自动三种解码模式。
- 默认解码模式为 FFmpeg。
- FFmpeg 扩展解码器用于补充 ALAC / M4A 等格式兼容性。
- 支持 ReplayGain 音量均衡。
- 播放页支持定时暂停、播放完当前歌曲后暂停、倍速播放、变调播放和在线歌曲下载。

### 🎨 界面与设置

- 基于 Miuix 构建 MIUI / HyperOS 风格设置项和界面组件。
- 默认使用悬浮底栏。
- MiniPlayer 支持封面圆形进度环和歌曲 / 歌词切换动画。
- 播放页向全屏封面背景和底部圆形控制按钮风格调整。
- 支持主题切换和常用播放、歌词、扫描、解码配置。

---

## 📱 系统要求

| 项目 | 要求 |
|:--|:--|
| 系统版本 | Android 10.0 / API 29 及以上 |
| 目标版本 | Android API 37 |
| 默认架构 | `arm64-v8a` |
| 网络权限 | WebDAV、LX 在线源、歌词获取等功能需要联网 |
| 悬浮窗权限 | 桌面歌词功能需要悬浮窗权限 |
| 通知权限 | Android 13 及以上系统需要授予通知权限 |

---

## 📦 下载安装

你可以从 [Releases](https://github.com/Kifranei/Ella/releases) 下载最新版本。

首次使用建议按以下步骤配置：

1. 安装 Ella Music。
2. 授予音乐文件读取权限。
3. 等待本地音乐扫描完成。
4. 如需桌面歌词，请在系统设置中授予悬浮窗权限。
5. 如需 WebDAV，请在设置中添加 WebDAV 音乐库。
6. 如需 LX 在线音乐，请导入 LX Music API 源。
7. 如需状态栏歌词，请按需开启 Lyricon / SuperLyric / Ticker / 蓝牙歌词相关选项。

---

## 🛠 构建项目

```bash
git clone https://github.com/Kifranei/Ella.git
cd Ella
./gradlew assembleDebug
```

Release 构建会优先读取以下环境变量：

```bash
RELEASE_STORE_FILE
RELEASE_STORE_PASSWORD
RELEASE_KEY_ALIAS
RELEASE_KEY_PASSWORD
```

如果未设置环境变量，则会使用项目根目录下的 `release.jks`，默认别名为 `release`。

---

## 🎧 FFmpeg

项目已经内置 FFmpeg 静态库：

```text
ffmpeg-decoder/src/main/jni/ffmpeg/android-libs
```

如需重新编译，可在 Windows 上运行：

```powershell
.\build_ffmpeg.ps1
```

脚本会通过 WSL 使用 Linux 版 Android NDK 编译 FFmpeg。

---

## 🧩 生态与能力

| 类别 | 能力 |
|:--|:--|
| 本地音乐 | 扫描、搜索、播放、文件夹浏览、专辑 / 艺人管理 |
| 远程音乐 | WebDAV 连接测试、目录浏览、远程播放 |
| 在线音乐 | LX Music API 多源导入、搜索、在线播放、下载 |
| 歌词 | LRC、增强 LRC、TTML、逐字、翻译、背景人声、对唱布局 |
| 系统歌词 | 桌面歌词、Lyricon、SuperLyric、Ticker 通知、蓝牙歌词 |
| 解码 | Media3、系统解码、FFmpeg 扩展解码 |
| 音频信息 | TagLib、Jaudiotagger、封面、标签、内嵌歌词 |
| 统计 | 格式占比、音质占比、播放次数排行、听歌时长排行 |
| 界面 | Jetpack Compose、Miuix、悬浮底栏、主题切换 |

---

## 🧱 使用的开源项目

| 项目 | 用途 |
|:--|:--|
| [Miuix](https://github.com/miuix-kmp/miuix) | MIUI / HyperOS 风格 Compose UI 组件 |
| [AndroidX Media3](https://github.com/androidx/media) | 播放、媒体会话和 ExoPlayer FFmpeg 扩展 |
| [FFmpeg](https://ffmpeg.org) | ALAC 等音频格式的软件解码 |
| [Lyricon](https://github.com/proify/lyricon) | 词幕 Provider API 与状态栏歌词 |
| [SuperLyricApi](https://github.com/HChenX/SuperLyricApi) | SuperLyric 歌词发布 API |
| [Jaudiotagger](https://github.com/Adonai/jaudiotagger) | 音频标签、内嵌歌词与封面读取 |
| [Kyant TagLib](https://github.com/Kyant0/TagLib) | Android / Kotlin TagLib 绑定 |
| [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | 液态玻璃和背景模糊效果 |
| [Coil](https://github.com/coil-kt/coil) | Compose 图片加载 |
| [QuickJS Android](https://github.com/HarlonWang/quickjs-wrapper-android) | LX Music API JS 源执行 |

---

## 👥 致谢

- **Codex (GPT-5.5)** — 1.0.2 至今主要开发与代码协作。
- **Mimo-V2.5-Pro** — 1.0.0 至 1.0.1 早期版本主要开发。
- **BetterLyrics** — 播放页封面模糊背景和歌词视觉效果参考。
- **SPlayer** — 播放页动效和歌词体验参考。
- **Retro Music Player** — jaudiotagger 标签读取思路参考。
- 感谢 Miuix、Media3、FFmpeg、Lyricon、SuperLyricApi、Jaudiotagger、Kyant TagLib、Backdrop、Coil 等开源项目。

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

## 👀 访问统计

<p align="center">
  <img src="https://count.getloli.com/get/@kifranei_ella?theme=moebooru-h" alt="Visitor Count" />
</p>

---

## 📄 许可证

Ella Music 使用 **GPL-3.0-or-later** 开源许可协议发布，详见 [LICENSE](LICENSE)。
