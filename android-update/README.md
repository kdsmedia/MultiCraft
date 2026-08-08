# Android Update Server (self-hosted via raw git)

This folder is the **update-check endpoint** for the MultiCraft Android app.
The app downloads `ver.txt` from this folder on every launch and compares
`version_code` with its own `versionCode` to decide whether to show an
"update available" dialog.

## Live URL (raw git)

```
https://raw.githubusercontent.com/kdsmedia/MultiCraft/main/android-update/ver.txt
```

This URL is hardcoded in
`build/android/src/main/java/com/altomedia/multicraft/MainActivity.java`
(`UPDATE_LINK`).

## ver.txt format

```json
{
    "version_code": 1,
    "package": "com.altomedia.multicraft",
    "content_en": "<html>...</html>",
    "content_ru": "<html>...</html>"
}
```

| Field | Meaning |
|-------|---------|
| `version_code` | Minimum version code. If the installed app's `versionCode` is **lower** than this, the update dialog is shown. Set this to the `versionCode` of the **newest** APK you have published to Google Play. |
| `package` | Package name the update button opens in Google Play (`market://details?id=<package>`). Must be your published app's applicationId: `com.altomedia.multicraft`. |
| `content_en` / `content_ru` | HTML shown in the update dialog (English / Russian). |

## How to push a new update

1. Bump `versionCode` (and `versionName`) in
   `build/android/build.gradle` -> `defaultConfig`.
2. Build the new APK (`make debug && ./gradlew assembleDebug`).
3. Publish the new APK to Google Play (your developer account).
4. Edit `android-update/ver.txt`: set `version_code` to the **new** versionCode
   you just published, and tweak the `content_*` message if needed.
5. Commit & push this file to `main`.

Now every user on an older build will see the update dialog, and the
**Update** button opens **your** app on Google Play.

## Current state

- App base `versionCode`: 1 (see `build/android/build.gradle`).
  The published APKs add a per-ABI offset (`armeabi-v7a` => +1, `x86` => +2),
  so the actual APK versionCode is `2` (armeabi-v7a) / `3` (x86).
- `ver.txt` `version_code`: 1  → lower than every installed APK's versionCode,
  so **no update dialog** is shown right now (the installed build is current).
- Update button target: `com.altomedia.multicraft` (your package).

## How to choose `version_code` when pushing an update

The dialog shows when `installedVersionCode < ver.txt.version_code`. Because
of the per-ABI offset, set `version_code` to a value **strictly greater than
the highest versionCode of the previous APKs**:

- Old APKs (base 1): armeabi-v7a=2, x86=3.
- You bump base to 2 and publish. New APKs: armeabi-v7a=3, x86=4.
- To make all old installs see the update, set `ver.txt.version_code` to **4**
  (i.e. `new_base + 2`, the max ABI offset). Old armeabi-v7a (2) and old x86
  (3) are both < 4 → they get the dialog; the new builds (3 / 4) are not < 4
  for x86, but x86 equals 4 so it is fine (`<` is strict).

Simplest rule: when you publish a new base versionCode `N`, set
`ver.txt.version_code` to `N + 2`.

## Notes

- The app falls back to `market://details?id=<own package>` if the server
  response is unreadable, so it can never redirect to a third-party app.
- The dialog also has "Remind me later" and "Ignore this version" buttons;
  the ignore list is stored per-device in SharedPreferences.
