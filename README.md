# GX Mod Downloader

A native wrapper around the public GX Store that injects **GX Mod Archive Downloader**. It saves either the original `mod.crx` package or an editable raw ZIP payload. It never installs, activates, or executes downloaded mod code.

## Native shells

- **Android:** Kotlin + Android WebView, fully immersive/edge-to-edge, native downloads, CRX-to-ZIP extraction, clipboard bridge, Opera GX handoff, and 56 selectable launcher icon/theme presets.
- **Windows:** C# WinForms + WebView2, native downloads and ZIP extraction, dynamic window icons, and best-effort Opera GX launch with the bundled companion extension.
- **Linux:** GTK3 + WebKitGTK, native downloads and ZIP extraction, desktop icon theme updates, and best-effort Opera GX launch.

## Themes and icons

The included GX icon pack provides 56 presets across Basic, Holo, Holo GX, and Neon families. Selecting a preset changes the injected UI theme. Android also switches the launcher icon through activity aliases. Linux updates the per-user desktop icon. Windows updates the running window/taskbar icon.

## Security boundaries

Only HTTPS package URLs under these GX CDN hosts are accepted:

- `mods.store.gx.me`
- `play.gxc.gg`

The path must start with `/mods/`. Redirect targets are validated again. Downloads are capped at 512 MiB. CRX2 and CRX3 headers are parsed and the ZIP signature is verified before raw export.

## Source bundle

The repository keeps the full cross-platform source in a compact bundle, including an optimized sprite derived from the supplied 56-icon theme pack. Run `python scripts/reconstruct-source.py` after cloning to expand the Android, Windows, Linux, shared userscript, and Opera-extension sources. GitHub Actions performs this automatically before every build.

## Build

GitHub Actions produces:

- Android debug-signed APK
- Windows x64 setup EXE and portable ZIP
- Linux x86_64 AppImage and DEB

The Android build intentionally uses debug signing until a permanent private release key is configured outside the public repository.
