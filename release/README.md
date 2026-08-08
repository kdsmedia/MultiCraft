# MultiCraft Android APK (debug builds)

Built from source (commit pending) with the Android build environment
documented in `AGENTS.md`.

## Files

| APK | ABI | versionCode | Size | Notes |
|-----|-----|-------------|------|-------|
| MultiCraft-armeabi-v7a-debug.apk | armeabi-v7a | 2 | ~11 MB | Contains native engine `libmulticraft.so` |
| MultiCraft-x86-debug.apk | x86 | 3 | ~4 MB | No native lib built for x86 in this build |

## Build info

- Package: `com.altomedia.multicraft`
- Version: 1.1.11.2 (base versionCode 1)
- minSdkVersion: 21
- targetSdkVersion: 27
- compileSdkVersion: 27
- NDK: r16b (toolchain 4.9, gnustl_static)
- AGP: 3.0.1 / Gradle 4.1
- **Update server**: `android-update/ver.txt` in this repo (self-hosted),
  points the update button to **your** package on Google Play.

These are **debug** APKs (signed with the debug key, not for production
release). Rebuild with:

```bash
source /tmp/android-env.sh
cd build/android
make debug
./gradlew assembleDebug --no-daemon
```
