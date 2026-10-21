#!/usr/bin/env bash
# Sets up a TCL / Android TV as a clean, ad-free kids TV over the local network.
#
#   ./setup.sh [TV_IP]               install Kids Home, disable ad apps, family DNS
#   ./setup.sh [TV_IP] --replace     also disable the stock TCL launcher (use if
#                                    the Home button still opens the TCL screen)
#   ./setup.sh [TV_IP] --scan        just list TCL / Google packages on the TV
#
# Undo everything with ./restore.sh
set -euo pipefail

TV_IP="10.73.1.89"
MODE="setup"
for arg in "$@"; do
  case "$arg" in
    --replace) MODE="replace" ;;
    --scan) MODE="scan" ;;
    *) TV_IP="$arg" ;;
  esac
done

HERE="$(cd "$(dirname "$0")" && pwd)"
TV="$TV_IP:5555"
APK="$HERE/KidsHome.apk"
APK_URL="https://github.com/ayahya1983/My-tv/releases/latest/download/KidsHome.apk"
KIDS_HOME="com.mytv.kidshome/.HomeActivity"
STATE="$HERE/.stock-launcher"

command -v adb >/dev/null || {
  echo "adb not found. Install Android platform-tools:"
  echo "  macOS:  brew install android-platform-tools"
  echo "  Linux:  sudo apt install adb"
  exit 1
}

tv() { adb -s "$TV" shell "$@" </dev/null; }

echo "Connecting to $TV ... (accept 'Allow debugging' on the TV if asked)"
adb connect "$TV" >/dev/null
for _ in $(seq 1 30); do
  [ "$(adb -s "$TV" get-state 2>/dev/null || true)" = "device" ] && break
  sleep 2
  adb connect "$TV" >/dev/null || true
done
[ "$(adb -s "$TV" get-state 2>/dev/null || true)" = "device" ] || {
  echo "Could not connect. Check the TV is on, Network/USB debugging is enabled and the IP is right."
  exit 1
}

if [ "$MODE" = "scan" ]; then
  tv pm list packages | sed 's/^package://' | grep -Ei 'tcl|recommend|partner|ads' | sort
  exit 0
fi

echo "== 1/4 Installing Kids Home"
if [ ! -f "$APK" ]; then
  echo "Downloading $APK_URL"
  curl -fL -o "$APK" "$APK_URL" || {
    echo "Download failed. Get KidsHome.apk from the repo's Releases page and put it next to this script."
    exit 1
  }
fi
adb -s "$TV" install -r "$APK"

echo "== 2/4 Disabling ad / promo apps"
installed="$(tv pm list packages | tr -d '\r')"
grep -Ev '^[[:space:]]*(#|$)' "$HERE/debloat-list.txt" | while read -r pkg; do
  if grep -qx "package:$pkg" <<<"$installed"; then
    tv pm disable-user --user 0 "$pkg" >/dev/null && echo "  disabled $pkg"
  fi
done

echo "== 3/4 Family-safe, ad-blocking DNS (AdGuard Family)"
tv settings put global private_dns_mode hostname
tv settings put global private_dns_specifier family.adguard-dns.com

echo "== 4/4 Making Kids Home the home screen"
tv cmd package set-home-activity "$KIDS_HOME" >/dev/null 2>&1 || true
current="$(tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tr -d '\r' | tail -n1)"
if [ "$MODE" = "replace" ] && [[ "$current" != com.mytv.kidshome/* ]]; then
  stock="${current%%/*}"
  echo "$stock" >"$STATE"
  tv pm disable-user --user 0 "$stock" >/dev/null && echo "  disabled stock launcher $stock"
elif [[ "$current" != com.mytv.kidshome/* ]]; then
  echo "  The TV still prefers: $current"
  echo "  If the Home button doesn't open Kids Home, run: ./setup.sh $TV_IP --replace"
fi
tv input keyevent KEYCODE_HOME

echo
echo "Done. Default parent PIN is 1234 - change it from the Parents button."
