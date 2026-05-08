# Ella Music

一款简洁的本地音乐播放器，基于 Jetpack Compose、Miuix 和 Media3 构建，面向 MIUI/HyperOS 风格体验做了界面和交互适配。

English documentation: [README_en.md](README_en.md)

## 功能特性

- 本地音乐扫描、搜索、播放和文件夹浏览
- 首页和专辑页搜索、排序、快速定位与多选管理
- 专辑页、歌曲列表、迷你播放条和沉浸式播放页
- LRC / 增强 LRC / TTML 歌词解析，支持逐字歌词、翻译、背景词和 TTML 对唱布局
- 外置 LRC 与内嵌歌词读取，支持常见中文歌词编码回退
- Lyricon 词幕适配和 AOSP Ticker 歌词通知
- WAV、FLAC、M4A、OGG、OPUS 等格式标签读取
- FFmpeg 扩展解码器，补充 ALAC / M4A 等格式兼容
- ReplayGain 音量均衡
- Miuix 标准设置项、主题切换和默认悬浮底栏

## 构建

```bash
git clone https://github.com/Kifranei/Ella.git
cd Ella
./gradlew assembleDebug
```

Release 包会优先读取环境变量 `RELEASE_STORE_FILE`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`。如果未设置环境变量，会使用项目根目录的 `release.jks`，默认别名 `release`。

FFmpeg 静态库已经放在 `ffmpeg-decoder/src/main/jni/ffmpeg/android-libs`。如需重新编译，可在 Windows 上运行：

```powershell
.\build_ffmpeg.ps1
```

脚本会通过 WSL 使用 Linux 版 Android NDK 编译 FFmpeg。

## 开源项目

| 项目 | 用途 |
|---|---|
| [Miuix](https://github.com/miuix-kmp/miuix) | MIUI/HyperOS 风格 Compose UI 组件 |
| [AndroidX Media3](https://github.com/androidx/media) | 播放、媒体会话和 ExoPlayer FFmpeg 扩展 |
| [FFmpeg](https://ffmpeg.org) | ALAC 等音频格式的软件解码 |
| [Lyricon](https://github.com/proify/lyricon) | 词幕 Provider API 与状态栏歌词 |
| [Jaudiotagger](https://github.com/Adonai/jaudiotagger) | 音频标签、内嵌歌词与封面读取 |
| [Kyant TagLib](https://github.com/Kyant0/TagLib) | Android/Kotlin TagLib 绑定 |
| [Kyant Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | 液态玻璃和背景模糊效果 |
| [Coil](https://github.com/coil-kt/coil) | Compose 图片加载 |

## 致谢

- **Codex (GPT-5.5)** — 1.0.2 至今主要开发与代码协作
- **BetterLyrics** — 播放页封面模糊背景和歌词视觉效果参考
- **SPlayer** — 播放页动效和歌词体验参考
- Retro Music Player 的 jaudiotagger 标签读取思路
- Miuix、Media3、FFmpeg、Lyricon、Jaudiotagger、Kyant TagLib、Backdrop、Coil 等开源项目
- **Mimo-V2.5-Pro** — 1.0.0 至 1.0.1 早期版本主要开发

## 许可证

Ella Music 使用 GPL-3.0-or-later 开源许可协议发布，详见 [LICENSE](LICENSE)。
