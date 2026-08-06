#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
APPDIR="$ROOT/dist/AppDir"
VERSION="0.2.1"
rm -rf "$APPDIR"
meson setup "$ROOT/dist/linux-build" "$ROOT/linux" --prefix=/usr --buildtype=release --wipe
meson compile -C "$ROOT/dist/linux-build"
DESTDIR="$APPDIR" meson install -C "$ROOT/dist/linux-build"
mkdir -p "$APPDIR/usr/share/icons/hicolor/512x512/apps" "$APPDIR/DEBIAN"
unzip -p "$ROOT/assets/gx-icon-pack.zip" app_icon/basic/classic.png > "$APPDIR/usr/share/icons/hicolor/512x512/apps/dev.alastorkaneki.gxmods.png"
cat > "$APPDIR/AppRun" <<'RUN'
#!/usr/bin/env bash
HERE="$(dirname "$(readlink -f "$0")")"
export GXMOD_DATA_DIR="$HERE/usr/share/gx-mod-downloader"
export XDG_DATA_DIRS="$HERE/usr/share:${XDG_DATA_DIRS:-/usr/local/share:/usr/share}"
exec "$HERE/usr/bin/gx-mod-downloader" "$@"
RUN
chmod +x "$APPDIR/AppRun"
ln -sf usr/share/applications/dev.alastorkaneki.gxmods.desktop "$APPDIR/dev.alastorkaneki.gxmods.desktop"
ln -sf usr/share/icons/hicolor/512x512/apps/dev.alastorkaneki.gxmods.png "$APPDIR/dev.alastorkaneki.gxmods.png"
ln -sf dev.alastorkaneki.gxmods.png "$APPDIR/.DirIcon"
cat > "$APPDIR/DEBIAN/control" <<CONTROL
Package: gx-mod-downloader
Version: $VERSION
Section: utils
Priority: optional
Architecture: amd64
Maintainer: Alastor-Kaneki
Depends: libgtk-3-0, libwebkit2gtk-4.1-0, libjson-glib-1.0-0, libsoup-3.0-0, libarchive13
Description: Native GX Store archive wrapper
 Saves original CRX backups or editable ZIP archives without installing mods.
CONTROL
mkdir -p "$ROOT/dist"
dpkg-deb --build "$APPDIR" "$ROOT/dist/GX-Mod-Downloader-${VERSION}-linux-amd64.deb"
