# GX Mod Downloader

A native, unofficial Opera GX Mods browser and raw-package downloader for **Android, Windows, and Linux**.

The app uses one Kotlin Multiplatform + Compose Multiplatform codebase. It talks directly to the GX Store catalog service, presents a GX-inspired native interface, and downloads the original `.crx` package without installing or executing the mod.

> This project is not affiliated with or endorsed by Opera. Opera, Opera GX, and GX Mods are trademarks of their respective owners.

## Features

- Native adaptive interface for phones, tablets, Windows, and Linux
- Server-side GX Mods search
- Browse by Desktop or Mobile compatibility
- Component filters for wallpapers, themes, music, sounds, web modding, shaders, fonts, and icons
- Sort by most downloaded, recently updated, newest, or title
- Mod details with covers, creator, version, package size, download count, dates, platforms, and components
- Raw `.crx` downloads from official GX content hosts only
- Redirect validation before download
- CRX2/CRX3 header and ZIP-payload validation on desktop
- Android downloads through the system Download Manager
- Session download history
- True-black GX-inspired styling and immersive Android system bars
- No WebView, Electron, automatic installation, or execution of mod content

## Security model

GX Mod Downloader treats every package as untrusted data.

- Package URLs must use HTTPS.
- The host must be one of the known GX content hosts:
  - `mods.store.gx.me`
  - `play.gxc.gg`
  - `play.gx.games`
- The final path must end in `/mod.crx`.
- Redirects are followed only while they remain valid official GX package URLs.
- Desktop downloads are checked for the `Cr24` header, supported CRX version, a bounded header, and a ZIP payload.
- CSS, shaders, scripts, or other package content are never rendered or executed by this app.
- The app never silently installs or activates a mod.

The GX Store API is not documented as a stable public developer API, so its adapter is intentionally isolated in `GxStoreApi.kt` for future maintenance.

## Project structure

```text
composeApp/src/commonMain     Shared API, models, state, security, and Compose UI
composeApp/src/androidMain    Android activity, immersive mode, images, downloads
composeApp/src/desktopMain    Windows/Linux entry point, images, filesystem downloads
composeApp/src/commonTest     GX URL, CRX header, and API-schema tests
.github/workflows/build.yml   Android, Windows, and Linux CI packages
```

## Build

Requirements:

- JDK 21
- Gradle 8.14.3
- Android SDK 36 for Android builds
- `rpm` tooling for RPM packaging
- Windows packaging tools supported by `jpackage` for MSI/EXE builds

### Android debug APK

```bash
gradle :composeApp:assembleDebug
```

Output:

```text
composeApp/build/outputs/apk/debug/
```

### Run desktop app

```bash
gradle :composeApp:run
```

### Linux packages

```bash
gradle :composeApp:packageDeb :composeApp:packageRpm
```

### Windows installers

Run on Windows:

```powershell
gradle :composeApp:packageMsi :composeApp:packageExe
```

GitHub Actions builds all supported packages on every push to `main`. The Android CI artifact is debug-signed. A permanent private release key must be stored in GitHub Actions secrets before publishing a signed production APK; private signing material must never be committed.

## Data flow

1. Browse requests call `https://api.gx.me/store/v3/mods` with pagination, search, sorting, and tag filters.
2. Details call `https://api.gx.me/store/v3/mods/{shortId}`.
3. The returned versioned `contentUrl` is converted from `/contents` to `/mod.crx`.
4. The package URL is validated against the official-host allowlist.
5. The platform downloader saves the untouched raw package.

## License

Apache License 2.0. See [LICENSE](LICENSE).
