#!/usr/bin/env python3
"""v17: shift phone baseline down slightly; all other poster geometry unchanged."""
from pathlib import Path
import sys
p=Path(sys.argv[1] if len(sys.argv)>1 else "wifi-project")/"app/src/main/assets/poster.js"
s=p.read_text(encoding="utf-8")
old="const metrics=x.measureText(phone),centerY=h*.953;"
new="const metrics=x.measureText(phone),centerY=h*.961;"
if s.count(old)!=1:raise RuntimeError("v16 phone anchor not found exactly once")
p.write_text(s.replace(old,new),encoding="utf-8")
print("v17: phone moved down 0.8 percent of poster height")
