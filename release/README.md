# MultiCraft Android APK

Built from source with the Android build environment documented in
`AGENTS.md`. **Release APKs** are signed with the project's release keystore
(`CN=KDS Media, O=Altomedia Indonesia`); debug APKs use the Android debug key.

## Files

| APK | ABI | versionCode | Signed | Size | Notes |
|-----|-----|-------------|--------|------|-------|
| MultiCraft-armeabi-v7a-release.apk | armeabi-v7a | 2 | release key | ~10 MB | Signed, production-style |
| MultiCraft-x86-release.apk | x86 | 3 | release key | ~4 MB | Signed, production-style |
| MultiCraft-armeabi-v7a-debug.apk | armeabi-v7a | 2 | debug key | ~11 MB | Debug, contains native engine `libmulticraft.so` |
| MultiCraft-x86-debug.apk | x86 | 3 | debug key | ~4 MB | Debug, no native lib for x86 |

## Build info

- Package: `com.altomedia.multicraft`
- Version: 1.1.11.2 (base versionCode 1)
- minSdkVersion: 21, targetSdkVersion: 27, compileSdkVersion: 27
- NDK: r16b (toolchain 4.9, gnustl_static), AGP 3.0.1 / Gradle 4.1
- **Update server**: `android-update/ver.txt` in this repo (self-hosted),
  points the update button to **your** package on Google Play.
- **Signing**: release keystore is generated locally (git-ignored) and wired
  via `local.properties`. Never commit the keystore or its password.

## Rebuild

```bash
source /tmp/android-env.sh
cd build/android
make debug                 # build native .so (needs NDK r16b)
./gradlew assembleDebug    # debug APKs (debug key)
./gradlew assembleRelease   # release APKs (your release key)
```

> Note: `google-services.json` (Firebase) is present locally and git-ignored.
> The google-services Gradle plugin is commented out in `build.gradle` because
> this app currently has no Firebase SDK dependency and the plugin's default
> play-services 11.4.2 clashes with support-compat:27.1.1. Enable the plugin
> (uncomment in `build.gradle`) once you add a Firebase SDK dependency.
