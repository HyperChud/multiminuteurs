"""Writes the demo SharedPreferences used for the README screenshots (made-up timers)."""
import json, sys
from xml.sax.saxutils import escape
scenario, theme, now = sys.argv[1], sys.argv[2], int(sys.argv[3])
M = 60_000
def t(i, label, dur, hue, rem=None, running=False, finished=False):
    rem = dur if rem is None else rem
    return {"id": i, "label": label, "duration": dur, "remaining": rem, "endAt": now + rem if running else 0,
            "running": running, "finished": finished, "hue": hue}
timers = [
    t(1, "Pasta", 10 * M, 28, rem=7 * M + 42_000, running=True),
    t(2, "Laundry", 45 * M, 212, rem=31 * M + 5_000, running=True),
    t(3, "Tea", 4 * M, 140, rem=2 * M + 10_000),
    t(4, "Nap", 20 * M, 335),
]
if scenario == "ringing":
    timers[2] = t(3, "Tea", 4 * M, 140, rem=0, finished=True)
open("/tmp/timers.xml", "w").write(
    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
    f'    <string name="list">{escape(json.dumps(timers), {chr(34): "&quot;"})}</string>\n</map>\n')
open("/tmp/settings.xml", "w").write(
    "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n"
    '    <string name="lang">en</string>\n    <boolean name="showMs" value="false" />\n'
    f'    <string name="theme">{theme}</string>\n    <string name="color">violet</string>\n'
    '    <boolean name="pattern" value="true" />\n</map>\n')
