# build_ffmpeg_windows.ps1
# Run this script in PowerShell to build FFmpeg for Android
# Requires: WSL (Windows Subsystem for Linux) with git, make, and Android NDK

$ErrorActionPreference = "Stop"

$FFMPEG_MODULE_PATH = "D:\Repos\Ella\ffmpeg-decoder\src\main"
$NDK_PATH = "$env:ANDROID_HOME\ndk\29.0.14206865"

# Check NDK
if (-not (Test-Path $NDK_PATH)) {
    # Try common NDK locations
    $ndkVersions = Get-ChildItem "$env:ANDROID_HOME\ndk" -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending
    if ($ndkVersions.Count -gt 0) {
        $NDK_PATH = $ndkVersions[0].FullName
        Write-Host "Using NDK: $NDK_PATH"
    } else {
        Write-Host "ERROR: Android NDK not found. Install it via Android Studio > SDK Manager > SDK Tools > NDK"
        exit 1
    }
}

$ENABLED_DECODERS = @("alac", "aac", "mp3", "vorbis", "opus", "flac", "ac3", "eac3", "truehd", "dca", "amrnb", "amrwb", "pcm_mulaw", "pcm_alaw")

Write-Host "=== Building FFmpeg for Android ==="
Write-Host "Module path: $FFMPEG_MODULE_PATH"
Write-Host "NDK path: $NDK_PATH"
Write-Host "Decoders: $($ENABLED_DECODERS -join ', ')"

# Check if FFmpeg source exists
$ffmpegDir = "$FFMPEG_MODULE_PATH\jni\ffmpeg"
if (-not (Test-Path $ffmpegDir)) {
    Write-Host ""
    Write-Host "FFmpeg source not found. Cloning..."
    Push-Location "$FFMPEG_MODULE_PATH\jni"
    wsl git clone git://source.ffmpeg.org/ffmpeg --branch=release/6.0 --depth=1
    Pop-Location
}

# Convert Windows paths to WSL paths
$wslModulePath = wsl wslpath -a $FFMPEG_MODULE_PATH
$wslNdkPath = wsl wslpath -a $NDK_PATH

Write-Host ""
Write-Host "Building FFmpeg via WSL..."
Write-Host "WSL module path: $wslModulePath"
Write-Host "WSL NDK path: $wslNdkPath"

$decoderList = $ENABLED_DECODERS -join " "

wsl bash -c "cd '${wslModulePath}/jni' && chmod +x build_ffmpeg.sh && ./build_ffmpeg.sh '${wslModulePath}' '${wslNdkPath}' 'linux-x86_64' 21 $decoderList"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "=== FFmpeg build completed successfully ==="
    Write-Host "Static libraries are in: $FFMPEG_MODULE_PATH\jni\ffmpeg\android-libs\"
    Write-Host ""
    Write-Host "Now rebuild the app: .\gradlew.bat :app:assembleRelease"
} else {
    Write-Host ""
    Write-Host "=== FFmpeg build FAILED ==="
    exit 1
}
