# Lyd

A local-music Android audio player. Scans your device's media library and plays it — no
streaming, no accounts, no internet permission.

Package name: `au.com.inoaspect.lyd.audio`

## Prerequisites

- JDK 17
- Android SDK (`ANDROID_HOME` set, e.g. `C:\Users\<you>\AppData\Local\Android\Sdk`)
- `keystore.properties` at the repo root, pointing at a release keystore (see below) — required
  for release builds, not for debug builds

This project uses the Gradle wrapper (`gradlew` / `gradlew.bat`), so a separately installed Gradle
is never required.

## Release signing

Release builds are signed using a keystore referenced from `keystore.properties` (git-ignored,
never committed). That file looks like:

```properties
storeFile=keystore/lyd-release.jks
storePassword=...
keyAlias=lyd-release
keyPassword=...
```

If you don't have a keystore yet, generate one with `keytool` (bundled with the JDK):

```powershell
keytool -genkeypair -v -keystore keystore/lyd-release.jks -alias lyd-release `
  -keyalg RSA -keysize 2048 -validity 10000
```

**Back this file up.** If it's lost, you can never publish an update to an app already on Play
Store under the same listing — losing it means starting over with a new app.

## Building a release APK

For sideloading / testing directly on a device:

```powershell
.\gradlew.bat :app:assembleRelease
```

Output: `app\build\outputs\apk\release\app-release.apk`

## Building a release App Bundle (for Play Store)

Play Console requires an `.aab`, not an `.apk`:

```powershell
.\gradlew.bat :app:bundleRelease
```

Output: `app\build\outputs\bundle\release\app-release.aab`

Upload that file directly in Play Console under Testing/Production → Create a new release.

## Publishing an APK to a connected phone

1. Enable Developer Options on the phone, then enable USB debugging (Settings → About phone → tap
   "Build number" 7 times → Developer options → USB debugging).
2. Connect the phone via USB and accept the "Allow USB debugging?" prompt on the device.
3. Confirm it's visible to adb:

   ```powershell
   & "$env:ANDROID_HOME\platform-tools\adb.exe" devices
   ```

4. Install the APK:

   ```powershell
   & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\release\app-release.apk
   ```

   `-r` reinstalls over an existing copy in place. This only works if the existing install was
   signed with the **same** keystore — installing a release build over a debug build (or vice
   versa) fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` because the signatures don't match; in
   that case uninstall the existing app first.

### Debug builds

For local testing where release signing isn't needed:

```powershell
.\gradlew.bat :app:assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

Debug builds are auto-signed with a machine-local debug key — fine for your own device, but
Android will refuse to install a debug build over a release build already on the phone (or vice
versa) for the reason above.

## Running tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```
