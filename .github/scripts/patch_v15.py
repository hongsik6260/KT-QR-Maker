#!/usr/bin/env python3
"""v15: balance QR margins and center phone vertically in black bar."""
from pathlib import Path
import sys
root=Path(sys.argv[1] if len(sys.argv)>1 else "wifi-project")
p=root/"app/src/main/assets/poster.js"
s=p.read_text(encoding="utf-8")
old="const frame={left:w*.356,right:w*.670,top:h*.588,bottom:h*.787};"
new="const frame={left:w*.356,right:w*.670,top:h*.600,bottom:h*.775};"
if old not in s:raise RuntimeError("v14 QR frame anchor missing")
s=s.replace(old,new)
old="const metrics=x.measureText(phone),centerY=h*.953;"
new="const metrics=x.measureText(phone),centerY=h*.925;"
if old not in s:raise RuntimeError("v12 phone vertical center anchor missing")
s=s.replace(old,new)
p.write_text(s,encoding="utf-8")
print("v15: QR top/bottom inset balanced, phone moved to black-bar center")
