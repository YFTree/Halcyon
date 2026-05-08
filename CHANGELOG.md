# Changelog

## 1.0.3 - 2026-05-08

### Highlights

- Added home and album library sorting, search, and fast index navigation.
- Added multi-select song management with delete support.
- Improved TTML lyric parsing, spacing, translation display, background vocals, and duet alignment.
- Restored Lyricon and AOSP Ticker lyric state after returning from background or lock screen.
- Reworked floating navigation and mini player behavior to reduce visual glitches and content blocking.

### Playback And Library

- Added cached library state so disabling startup scan no longer leaves the app empty.
- Fixed cases where the UI lost the current playing song while audio continued in the service.
- Added safe cover loading and downsampled embedded artwork to avoid oversized bitmap crashes.
- Improved WAV, M4A/ALAC, FLAC, OGG, and OPUS metadata handling through TagLib, Jaudiotagger, MediaMetadataRetriever, and FFmpeg.
- Added release signing fallback for the project-root `release.jks`.

### Lyrics

- Added TTML word-level lyric support, translation display, background vocals, and V1/V2 duet alignment.
- Fixed TTML whitespace loss, including English lines and background lyric lines.
- Fixed CJK lyric parsing regressions where only the first word or segment could be shown.
- Added support for inline timed LRC formats with repeated timestamps and translated lines.
- Fixed Lyricon translation toggle behavior and Ticker lyric refresh after restoring playback.

### UI

- Added album page sorting, search, and fast index navigation.
- Simplified the glass navigation option into a lighter floating bottom bar to reduce jank.
- Improved About page immersive behavior, credits, and open-source project list.
- Updated About page contributor credits to Codex and GPT-5.5.

### Build

- Bumped version to 1.0.3.
- Release APK builds successfully with bundled FFmpeg decoder libraries.
- Added GPL-3.0-or-later license metadata.
