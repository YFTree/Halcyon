# Ella Music v1.0.6

## 更新日志

### 亮点

- 新增 Flyme 状态栏歌词适配，支持在部分魅族 / Flyme 设备状态栏显示当前歌词。
- 新增蓝牙车载歌词功能，可将当前歌词写入媒体信息，供部分蓝牙设备或车机显示。
- 新增底部悬浮播放条歌词显示：播放时歌名行显示当前歌词，歌手行显示翻译歌词。
- 新增自定义扫描文件夹和屏蔽文件夹功能，可更灵活地控制本地曲库扫描范围。
- 新增已导入歌词字体删除功能，可清理不再使用的导入字体。
- 新增自动解码模式，并继续优化 LX 在线音乐、WebDAV 音乐库和播放页控制能力。

### 歌词

- Flyme 状态栏歌词改用系统 Ticker 扩展标记推送，并更新状态栏图标与通知通道。
- 蓝牙歌词通过 MediaMetadata 更新当前歌词，部分车机和蓝牙设备可显示当前歌词内容。
- 底部迷你播放器在播放时优先显示当前歌词和翻译，暂停或无歌词时恢复显示歌名和歌手。
- 优化词幕、Ticker、蓝牙歌词等歌词相关设置文案和分组。
- 支持删除已导入的歌词字体，删除当前使用字体时自动恢复为系统默认字体。
- 修复部分 WAV 歌曲读取不到内嵌歌词的问题。

### 曲库与扫描

- 新增自定义扫描文件夹，可手动选择要纳入曲库的本地目录。
- 新增屏蔽文件夹，可长按文件夹进行屏蔽，并在文件夹页统一管理或取消屏蔽。
- 文件夹页新增 WebDAV 音乐库入口和已屏蔽文件夹入口，避免功能入口丢失。
- 屏蔽后曲库为空时，空状态会提示可能被屏蔽规则排除。
- 优化专辑页和文件夹详情页排序，中文、日文、韩文等非 ASCII 标题可按拉丁化读音参与排序和快速索引。

### WebDAV 与 LX 在线音乐

- 修复 WebDAV 音乐库已配置后仍反复弹出设置窗口的问题。
- WebDAV 音乐库支持返回上级目录，并继续优化远程目录浏览体验。
- 恢复文件夹页 WebDAV 音乐库入口。
- 优化 LX 源命名和导入逻辑，统一使用 “LX 源 / LX 在线音乐” 文案。
- 在线歌曲播放与下载流程继续优化。

### 播放与界面

- 播放页新增播放完当前歌曲后暂停、定时暂停、倍速、变调等控制入口。
- 播放页与专辑详情页封面加载上限提升，改善高分辨率封面显示效果。
- 修复 WAV 歌曲在系统媒体通知中可能显示空白封面的问题。
- 优化首页、专辑页、文件夹页、歌手页等列表底部留白，减少底部悬浮播放条遮挡。
- 主界面标题调整为“音乐库”。
- 播放页音频信息新增部分格式 / 声道提示。

### 构建

- 版本号更新至 1.0.6。
- versionCode 更新至 7。

### 说明

- Flyme 状态栏歌词依赖设备 ROM 对 Ticker 扩展能力的支持，不同 Flyme / 魅族设备表现可能不同。
- 蓝牙歌词属于实验功能，不同蓝牙设备、车机或系统媒体面板对实时 MediaMetadata 刷新的支持存在差异。


# Ella Music v1.0.6

## Release Notes

### Highlights

- Added Flyme status-bar lyrics support for selected Meizu / Flyme devices.
- Added Bluetooth car display lyrics support by writing the current lyric line into media metadata for compatible Bluetooth devices and car displays.
- Added lyric display to the floating mini player: the title line shows the current lyric while playing, and the artist line shows the translation when available.
- Added custom scan folders and excluded folders for more flexible local library scanning.
- Added deletion support for imported lyric fonts.
- Added automatic decoder mode, with further improvements to LX online music, the WebDAV library, and playback controls.

### Lyrics

- Reworked Flyme status-bar lyrics using system Ticker extension flags, with updated ticker icon and notification channel handling.
- Bluetooth lyrics now update MediaMetadata so compatible car displays or Bluetooth devices can show the current lyric line.
- The mini player now shows the current lyric and translation during playback, and falls back to song title and artist when paused or when no lyric is available.
- Improved wording and grouping for Lyricon, Ticker, Bluetooth lyrics, and other lyric-related settings.
- Imported lyric fonts can now be deleted; deleting the currently selected font restores the system default.
- Fixed embedded lyric reading for some WAV files.

### Library And Scanning

- Added custom scan folders, allowing specific local directories to be included in the music library.
- Added excluded folders, with long-press blocking from the folder page and a management entry for unblocking.
- Added visible entry cards for the WebDAV library and blocked-folder management on the folder page.
- Improved the empty-state message when all folders may have been excluded by blocking rules.
- Improved album and folder-detail sorting so Chinese, Japanese, Korean, and other non-ASCII titles can participate in Latinized A-Z sorting and fast indexing.

### WebDAV And LX Online Music

- Fixed WebDAV settings dialog appearing repeatedly even after configuration was already saved.
- Added parent-directory navigation to the WebDAV library and continued polishing remote browsing behavior.
- Restored the WebDAV library entry on the folder page.
- Improved LX source naming and import behavior, using clearer “LX Source / LX Online Music” wording.
- Continued improving online playback and download flows.

### Playback And UI

- Added now-playing menu actions for stopping after the current track, sleep timer, playback speed, and pitch control.
- Increased artwork loading limits on the now-playing page and album detail page for better high-resolution cover display.
- Fixed blank system media notification artwork for some WAV songs.
- Improved bottom padding on Home, Album, Folder, Artist, and related list pages to avoid overlap with the floating mini player.
- Renamed the main library title to “音乐库”.
- Added additional format / channel labels to the now-playing audio information area.

### Build

- Bumped version to 1.0.6.
- Bumped versionCode to 7.

### Notes

- Flyme status-bar lyrics depend on ROM support for Flyme / Meizu Ticker extensions, so behavior may vary across devices.
- Bluetooth lyrics are experimental. Real-time MediaMetadata updates may vary depending on the Bluetooth device, car display, or system media panel.