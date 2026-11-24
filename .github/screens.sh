#!/usr/bin/env bash
# Takes the README screenshots on an emulator, with made-up demo data.
set -euo pipefail
PKG=com.example.multiminuteurs
OUT=shots; mkdir -p "$OUT"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS || true
adb shell settings put global window_animation_scale 1
# clean status bar
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1200 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null

adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 4
adb shell am force-stop $PKG

prefs() { # $1 = scenario, $2 = theme
  local now; now=$(( $(adb shell date +%s | tr -d '\r') * 1000 ))
  python3 .github/demo.py "$1" "$2" "$now"
  for f in timers settings; do
    adb push "/tmp/$f.xml" "/data/local/tmp/$f.xml" >/dev/null
    adb shell chmod 644 "/data/local/tmp/$f.xml"
    adb shell run-as $PKG sh -c "'mkdir -p shared_prefs && cp /data/local/tmp/$f.xml shared_prefs/$f.xml'"
  done
}
launch() { adb shell am force-stop $PKG; adb shell monkey -p $PKG -c android.intent.category.LAUNCHER 1 >/dev/null; sleep 5; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "shot $1"; }
tap_text() { # taps the first on-screen element showing this text
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null
  local xy; xy=$(python3 - "$1" <<'PY'
import re, sys
s = open('/tmp/ui.xml').read()
for m in re.finditer(r'<node [^>]*>', s):
    n = m.group(0)
    if f'text="{sys.argv[1]}"' in n or f'content-desc="{sys.argv[1]}"' in n:
        a = list(map(int, re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n).groups()))
        print((a[0] + a[2]) // 2, (a[1] + a[3]) // 2); break
PY
)
  echo "tap $1 at $xy"; adb shell input tap $xy; sleep 3
}

prefs home dark;    launch; shot home
prefs ringing dark; launch; shot ringing
prefs home dark;    launch; tap_text "New"; shot new
prefs home dark;    launch; tap_text "Settings"; shot settings
prefs home light;   launch; shot light
ls -la "$OUT"
