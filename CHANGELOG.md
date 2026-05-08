# 更新日志

## 1.0.4 - 2026-05-08

### 亮点

- 新增文件夹详情页排序、搜索与快速索引导航。
- 重构正在播放页，新增基于封面的模糊背景、迷你歌词和发光进度条。
- 新增歌词行点击跳转，以及封面页迷你歌词。
- 新增曲库首次扫描进度显示。
- 新增迷你播放器左右滑动切换上一首/下一首。

### 播放与曲库

- 新增文件夹详情页搜索、排序与快速索引导航，适配大文件夹场景。
- 新增首次扫描进度显示，避免曲库扫描时看起来像卡住。

### 歌词

- 修复 TTML 英文音节片段如 `ne` / `ver` 在应用内歌词页被分开动画显示的问题。
- 新增歌词页点击歌词行跳转播放进度。
- 新增封面页迷你歌词，支持翻译和 TTML 背景和声文本。
- 为逐词歌词新增长音发光效果和 Apple Music 风格间奏点。

### 界面

- 底部导航改为默认悬浮高斯模糊样式。
- 新增迷你播放器左右滑动切换上一首/下一首。
- 更新正在播放页的封面/歌词切换与翻译控制。
- 正在播放页新增基于封面的模糊动态背景，以及缓慢旋转的封面背景。
- 修复高屏幕设备上旋转封面背景边缘露出的问题。
- 更新关于页贡献者致谢：BetterLyrics、SPlayer 和 Mimo-V2.5-Pro。

### 构建

- 版本号更新至 1.0.4。


# Changelog

## 1.0.4 - 2026-05-08

### Highlights

- Added folder-detail sorting, search, and fast index navigation.
- Rebuilt the now-playing screen with cover-derived blurred background, mini lyrics, and a glow seek bar.
- Added lyric-line click seeking and mini lyrics on the cover page.
- Added first-scan progress reporting for the music library.
- Added swipe gestures to the mini player for previous/next track switching.

### Playback And Library

- Added folder-detail search, sorting, and fast index navigation for large folders.
- Added first-scan progress reporting so the library no longer appears frozen while scanning.

### Lyrics

- Fixed TTML English syllable fragments such as `ne` / `ver` being animated separately in the in-app lyric page.
- Added lyric-line click seeking on the lyric page.
- Added mini lyrics on the cover page, including translation and TTML background-vocal text.
- Added long-sustain glow and Apple Music-style interlude dots for word-level lyrics.

### UI

- Changed bottom navigation to a default floating Gaussian-blur style.
- Added swipe gestures to the mini player for previous/next track switching.
- Updated the now-playing cover/lyric toggle and translation controls.
- Added cover-derived blurred dynamic backgrounds and slow cover-background rotation on the now-playing screen.
- Fixed rotated cover-background edge exposure on tall screens.
- Updated About page contributor credits for BetterLyrics, SPlayer, and Mimo-V2.5-Pro.

### Build

- Bumped version to 1.0.4.
