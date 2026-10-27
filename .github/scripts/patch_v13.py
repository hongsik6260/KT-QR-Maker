#!/usr/bin/env python3
"""v13: maximize QR within printed frame and allow hyphenated contact numbers."""
from pathlib import Path
import sys,re
root=Path(sys.argv[1] if len(sys.argv)>1 else "wifi-project")
assets=root/"app/src/main/assets"
p=assets/"poster.js"
s=p.read_text(encoding="utf-8")
old="const safe=Math.max(12,Math.round(w*.012));"
new="// Four-module QR quiet zone is already inside qrCanvas; only 2px frame clearance needed.\nconst safe=Math.max(2,Math.round(w*.002));"
if old not in s: raise RuntimeError("v12 QR safety margin anchor not found")
s=s.replace(old,new)
p.write_text(s,encoding="utf-8")
# Preserve typed hyphens; phone number is a display string, not a numeric value.
for p in assets.glob("*.html"):
 s=p.read_text(encoding="utf-8")
 if 'posterPhone' not in s: continue
 s,n=re.subn(r'(<input\b[^>]*\bid=["\']posterPhone["\'][^>]*?)\btype=["\']number["\']',r'\1type="tel"',s)
 s=s.replace('inputmode="numeric"','inputmode="tel"')
 s=s.replace('pattern="[0-9]*"','pattern="[0-9-]*"')
 p.write_text(s,encoding="utf-8")
 print("phone input HTML:",p.name,"number inputs changed:",n)
# Phone is rendered verbatim by v12 draw logic, including hyphens.
print("v13 QR margin reduced; four-module quiet zone retained")
