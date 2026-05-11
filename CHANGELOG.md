# Changelog

## [1.0.7] - 2026-05-11

### 中文

#### 新增

- 新增桌面歌词悬浮窗服务。
- 桌面歌词支持逐字歌词、翻译、TTML 对唱、`x-bg` 背景人声和背景人声逐字时间轴显示。
- 桌面歌词支持双击显示控制按钮，数秒无操作后自动隐藏。
- 桌面歌词新增播放 / 暂停、上一首、下一首、字号调节、锁定和关闭控制。
- 桌面歌词支持屏幕范围内拖动，避免悬浮窗被拖出屏幕。
- 新增 SuperLyric 发布支持，可向兼容模块发送歌曲信息、逐字歌词、翻译、背景人声和背景人声逐字时间轴。
- 首页、文件夹页和文件夹详情页新增“定位当前歌曲”功能。
- MiniPlayer 新增封面圆形播放进度环。
- MiniPlayer 歌曲 / 歌词文字新增横向滑动与淡入淡出切换动画。
- 专辑歌曲新增按曲目号排序能力。
- 歌曲元数据与扫描流程新增 `trackNumber` 支持。
- 新增 GitHub Actions Android 构建工作流。

#### 优化

- 优化桌面歌词悬浮窗控制逻辑，手动关闭后不会被后续歌词更新立即重新拉起。
- 优化桌面歌词锁定逻辑，锁定后通过通知解除锁定。
- 优化 TTML 桌面歌词布局，主歌词与背景人声分上下两行显示，并按 `v1` / `v2` 左右对齐。
- 优化艺人详情页结构，歌曲列表前置，专辑列表后置。
- 艺人页专辑列表增加专辑封面显示。
- 优化艺人页顶部文字和图标可读性。
- 优化封面读取与缓存，使用 `LruCache` 并记录缺失封面，减少重复扫描。
- 优化封面 Bitmap 加载的 OOM 处理和基于尺寸的缓存策略。
- 优化播放页封面背景和底部控制区视觉样式。
- 播放页动态背景封面请求尺寸调整为 512，以降低打开播放页时的卡顿。
- 首页下拉刷新时不再强制锁定列表滚动，刷新期间仍可滑动列表。
- Lyricon 词幕、Flyme 状态栏歌词、SuperLyric 默认关闭，降低首次安装后的系统干扰。
- 默认解码模式调整为 FFmpeg。

#### 修复

- 修复桌面歌词关闭按钮无效的问题。
- 修复桌面歌词 TTML `x-bg` 与主歌词重叠的问题。
- 修复定位当前歌曲按钮点击一次后，返回页面会反复自动跳到当前歌曲的问题。
- 修复酷我在线歌词“上一句翻译 / 当前原文”错位问题。
- 修复手动选择系统解码时仍可能静默回落到 FFmpeg 的问题；现在只有自动模式才允许系统解码失败后回落到 FFmpeg。

#### 变更

- 版本号更新至 `1.0.7`。
- `versionCode` 更新至 `8`。
- `minSdk` 调整为 `29`。
- 应用显示名称调整为 `Ella Music`。

#### 注意事项

- 桌面歌词需要系统悬浮窗权限。
- SuperLyric 需要用户设备上存在兼容的 SuperLyric 模块或实现。
- Lyricon、Flyme 状态栏歌词和蓝牙歌词仍依赖目标系统或设备能力，不同 ROM、车机和蓝牙设备表现可能不同。

---

### English

#### Added

- Added desktop lyric floating overlay service.
- Desktop lyrics support word-level lyrics, translations, TTML duet lines, `x-bg` background vocals, and background word timing display.
- Desktop lyric controls can be shown by double-tapping the overlay and are hidden again after a short idle timeout.
- Added desktop lyric controls for play / pause, previous, next, font size, lock, and close.
- Desktop lyric dragging is clamped to the visible screen area.
- Added SuperLyric publishing support for song metadata, word-level lyrics, translations, background vocals, and background word timings.
- Added “locate current song” support on Home, Folder, and Folder Detail screens.
- Added a circular playback progress ring around MiniPlayer cover art.
- Added horizontal slide and fade transitions for MiniPlayer song / lyric text.
- Added album song sorting by track number.
- Added `trackNumber` support to song metadata and music scanning.
- Added GitHub Actions Android build workflow.

#### Improved

- Improved desktop lyric overlay control behavior. Manually closing the overlay is now respected and lyric updates no longer immediately reopen it.
- Improved desktop lyric lock behavior, with notification-based unlock.
- Improved TTML desktop lyric layout. Primary and background vocal lines are now displayed on separate rows and aligned left / right according to `v1` / `v2`.
- Improved artist detail page layout by showing songs before albums.
- Artist album rows now show album artwork.
- Improved artist page header text and icon readability.
- Optimized cover art loading and caching with `LruCache` and missing-cover tracking to avoid redundant scans.
- Improved cover bitmap loading with better OOM handling and size-based caching.
- Refined now-playing cover background and bottom control area styling.
- Adjusted now-playing dynamic background artwork request size to 512 to reduce page-opening jank.
- Home pull-to-refresh no longer locks list scrolling while scanning.
- Lyricon, Flyme status-bar lyrics, and SuperLyric now default to off to reduce first-run system interference.
- Default decoder mode is now FFmpeg.

#### Fixed

- Fixed the desktop lyric close button not working.
- Fixed TTML `x-bg` overlap with the primary lyric line in desktop lyrics.
- Fixed the locate-current-song button repeatedly auto-scrolling after returning to a page.
- Fixed Kuwo online lyric pairing where the previous translation could be paired with the current original line.
- Fixed silent FFmpeg fallback when System decoder mode is manually selected. FFmpeg fallback is now limited to Auto mode.

#### Changed

- Bumped version to `1.0.7`.
- Bumped `versionCode` to `8`.
- Bumped `minSdk` to `29`.
- Changed the display app name to `Ella Music`.

#### Notes

- Desktop lyrics require the system overlay permission.
- SuperLyric requires a compatible SuperLyric module or implementation on the user device.
- Lyricon, Flyme status-bar lyrics, and Bluetooth lyrics still depend on target system or device support, so behavior may vary across ROMs, car displays, and Bluetooth devices.
