#!/usr/bin/env bash
# Undoes setup.sh: re-enables everything it disabled, resets DNS and removes Kids Home.
set -euo pipefail

TV_IP="${1:-10.73.1.89}"
HERE="$(cd "$(dirname "$0")" && pwd)"
TV="$TV_IP:5555"
STATE="$HERE/.stock-launcher"

adb connect "$TV" >/dev/null
tv() { adb -s "$TV" shell "$@" </dev/null; }

if [ -f "$STATE" ]; then
  tv pm enable "$(cat "$STATE")" >/dev/null && echo "re-enabled $(cat "$STATE")"
  rm -f "$STATE"
fi
grep -Ev '^[[:space:]]*(#|$)' "$HERE/debloat-list.txt" | while read -r pkg; do
  tv pm enable "$pkg" >/dev/null 2>&1 && echo "re-enabled $pkg" || true
done
tv settings put global private_dns_mode opportunistic
tv settings delete global private_dns_specifier >/dev/null
adb -s "$TV" uninstall com.mytv.kidshome >/dev/null 2>&1 || true
tv input keyevent KEYCODE_HOME
echo "TV restored."
