# AGENTS.md — MultiCraft repo

## Workflow Rules (user instruction)
- **PUSH SETIAP PERUBAHAN**: Setiap kali membuat satu perubahan/pengeditan,
  WAJIB langsung commit dan push ke repo GitHub (`kdsmedia/MultiCraft`).
  - Remote origin: https://github.com/kdsmedia/MultiCraft.git
  - Push ke branch `main` (sesuai instruksi user "Upload ke main").
  - Gunakan PAT yang user berikan untuk autentikasi push (token default
    GITHUB_TOKEN tidak punya scope push ke repo ini).
- Git config user: openhands <openhands@all-hands.dev>
- Selalu sertakan `Co-authored-by: openhands <openhands@all-hands.dev>` di
  commit message.

## Repo Info
- Asal: https://gitlab.com/MultiCraft/MultiCraft (branch master, commit 7577cf84)
- Mirror ke: https://github.com/kdsmedia/MultiCraft (branch main)
- Jenis: Game sandbox voxel open-source, fork Minetest, multi-platform
  (C++ engine + Lua), target: Android, iOS, Windows, desktop.
- Versi: 1.1.10 (lihat CMakeLists.txt)
- Lisensi: LGPLv3 (engine/code) + CC-BY-SA 3.0 (resource)

## Android Build Environment (commit fb7dfc60 onward)
- minSdkVersion: 21 (changed from 16) — build/android/build.gradle + Makefile
- targetSdkVersion: 27, compileSdkVersion 27, buildToolsVersion 27.0.3
- applicationId: com.altomedia.multicraft

### Toolchain
- JDK 8 (Temurin 1.8.0_432) at /opt/jdk8 — required for Gradle/AGP 3.0.1
- JDK 17 (Temurin 17.0.13) at /opt/jdk17 — required for sdkmanager
- Android SDK at /opt/android-sdk (cmdline-tools, platform-tools,
  platforms;android-27, build-tools;27.0.3, licenses accepted)
- NDK r16b at /opt/android-ndk-r16b (toolchain 4.9, gnustl_static,
  APP_PLATFORM=android-21)
- Env: JAVA_HOME=/opt/jdk8, ANDROID_HOME=/opt/android-sdk,
  ANDROID_NDK=/opt/android-ndk-r16b (script: /tmp/android-env.sh)
- Prerequisites: gcc-multilib g++-multilib gettext (msgfmt)

### Critical system compat fix
- NDK r16b clang needs libncurses.so.5 / libtinfo.so.5 (absent on Debian 13).
  Symlinks: libncurses.so.5 -> libncursesw.so.6, libtinfo.so.5 -> libtinfo.so.6

### Build flow (from build/android)
  source /tmp/android-env.sh
  make debug     # downloads deps + ndk-build -> libs/armeabi-v7a/libmulticraft.so
  ./gradlew assembleDebug --no-daemon   # -> build/outputs/apk/debug/*.apk

### Build fixes committed (required for clean build on modern host)
1. Fix dead download URLs (Makefile): curl dl.uxnr.de dead -> curl.se/download;
   freetype http -> https.
2. Enable C++11: APP_CPPFLAGS += -std=c++11 in jni/Application.mk
   (libintl-lite uses emplace_back / range-for / initializer lists).
3. Replace std::to_string with itos()/snprintf in src/util/string.h,
   src/client.cpp, src/defaultsettings.cpp — gnustl_static lacks
   std::to_string for integer types.
4. Gradle: jcenter()->google()+mavenCentral() (jcenter shut down), wrapper
   2.14.1->4.1 (AGP 3.0.1 requires Gradle 4.1), remove custom clean task.

### Native deps auto-downloaded by Makefile (deps/)
sqlite3, luajit (v2.1, cross-built via make-standalone-toolchain, HOST_CC
"gcc -m32"), Irrlicht (zaki fork), curl (7.60.0), leveldb (v1.20), freetype
(2.9.1), openal-soft (1.18.2, cmake-built), libvorbis-android, libiconv,
libintl-lite.

### Output
- build/outputs/apk/debug/MultiCraft-armeabi-v7a-debug.apk (~11 MB,
  contains lib/armeabi-v7a/libmulticraft.so)
- build/outputs/apk/debug/MultiCraft-x86-debug.apk (no native lib for x86)
- local.properties (ndk.dir + sdk.dir) is build-only, NOT committed.

### Troubleshooting
- LuaJIT build can fail on first make debug; run `make luajit` then `make debug`.
- If openal-soft cmake fails with cannot open libncurses.so.5, recheck the
  compat symlinks above.
- Package name Android: `mobi.MultiCraft`
  (build/android/build.gradle → applicationId, AndroidManifest.xml → package)

## Build
- Engine desktop: CMake (CMakeLists.txt)
- Android: Gradle (build/android/)
- iOS: build/iOS/
- Windows: build/WindowsApp/

## Catatan
- Repo ini adalah clone shallow; untuk operasi yang butuh full history,
  jalankan `git fetch --unshallow`.

## arm64-v8a (64-bit) build — v1.1.11.3 (versionCode 3)
Google Play mewajibkan native lib 64-bit (armeabi-v7a saja → ditolak).
Makefile (build/android/Makefile) di-patch untuk build arm64-v8a:
- Line ~42: `TARGET_ABI = arm64-v8a`, `TARGET_HOST = aarch64-linux`,
  `TARGET_TOOLCHAIN = aarch64-linux-android` (armv7 diberi comment).
- `-fno-lto` ditambahkan ke TARGET_CFLAGS/LDFLAGS_ADDON (GCC 4.9 arm64 butuh).
- LuaJIT: `HOST_CC="gcc"` (bukan "gcc -m32") + `TARGET_SYS=Linux` (host 64-bit
  untuk buildvm; -m32 gagal tanpa gcc-multilib 32-bit).

### NDK r16b sandbox fix (exec permission)
- /tmp/android-ndk-r16b/build/tools/make-standalone-toolchain.sh di-patch:
  setelah install, `find "$INSTALL_DIR" -type f \( ... \) -exec chmod +x {} +`
  + `chmod -R +x` pada bin/libexec/aarch64-linux-android/bin (sandbox
  menghapus exec bit). Tanpa ini, gcc/cc1/as → permission denied.

### Compat libs untuk clang NDK r16b (Debian 13)
- Symlink di /tmp/compatlibs (libncurses.so.5, libncursesw.so.5,
  libtinfo.so.5 → .so.6). Set `LD_LIBRARY_PATH=/tmp/compatlibs` saat build.
- cmake 3.31.6 di /tmp/cmake-3.31.6-linux-x86_64/bin (untuk openal-soft).

### Build urutan deps arm64 (semua elf64-littleaarch64)
leveldb → freetype → curl (distclean dulu; .o armeabi lama bikin "File in
wrong format") → openal-soft → libvorbis-android → luajit → irrlicht (via
ndk-build Irrlicht.mk) → libmulticraft.so (ndk-build APP_PLATFORM=android-21
TARGET_ABI=arm64-v8a). Backup libmulticraft.so armeabi-v7a dulu sebelum
ndk-build (rm -rf obj/local menghapusnya).

### build.gradle v1.1.11.3
- versionCode 3, versionName 1.1.11.3
- `ndk { abiFilters 'armeabi-v8a', 'arm64-v8a' }` — ship 32+64 bit
- splits abi enable:false, include keduanya (universal AAB)
- play-services-games:23.2.0 (v2 SDK — bukan v1; hapus meta-data
  `com.google.android.gms.games.unityGame` dari manifest, itu untuk Unity).
- AAB berisi base/lib/{armeabi-v7a,arm64-v8a}/libmulticraft.so → lolos 64-bit.

### ALTOMEDIA release package
- aab/ + apk/ vc3 (vc2 dihapus), docs/TERMS_OF_SERVICE.txt (baru),
  graphics/screenshots/ (5 PNG 1080x1920; .gitignore di-exception:
  `!ALTOMEDIA/graphics/screenshots/screenshot_*.png`).
- Keystore + local.properties (password Kdsmedia@123) tetap di repo sesuai
  rule user "keystore must NOT be deleted".
