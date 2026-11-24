#!/usr/bin/env bash
# Takes the README screenshots on an emulator, with made-up demo data.
set -uo pipefail
pip install -q pillow >/dev/null 2>&1 || true
# primary colour of the violet dark theme = hsl(272 55% 72%)
NEW_COLOR=$(python3 -c "import colorsys;r,g,b=colorsys.hls_to_rgb(272/360,.72,.55);print('%02x%02x%02x'%(round(r*255),round(g*255),round(b*255)))")
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
# Infinite animations keep uiautomator from ever going idle, so buttons are found by colour on a screenshot.
tap_color() { # $1 = hex colour, $2 = x column to scan
  adb exec-out screencap -p > /tmp/scr.png
  local xy; xy=$(python3 - "$1" "$2" <<'PY'
import sys
from PIL import Image
im = Image.open('/tmp/scr.png').convert('RGB'); w, h = im.size
c = tuple(int(sys.argv[1][i:i + 2], 16) for i in (0, 2, 4)); x = int(sys.argv[2])
ys = [y for y in range(h) if sum(abs(a - b) for a, b in zip(im.getpixel((x, y)), c)) < 24]
print(x, (ys[0] + ys[-1]) // 2 if ys else h // 2)
PY
)
  echo "tap $1 at $xy"; adb shell input tap $xy; sleep 3
}
tap_xy() { echo "tap $1 $2"; adb shell input tap "$1" "$2"; sleep 3; }

prefs home dark;    launch; shot home
prefs ringing dark; launch; shot ringing
prefs home dark;    launch; tap_color "${NEW_COLOR}" 250; shot new
prefs home dark;    launch; tap_xy 768 2200; shot settings
prefs home light;   launch; shot light
ls -la "$OUT"
