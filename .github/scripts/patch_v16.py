#!/usr/bin/env python3
"""v16: phone vertical centering only, retain v15 QR and all other behavior."""
from pathlib import Path
import sys
p=Path(sys.argv[1] if len(sys.argv)>1 else "wifi-project")/"app/src/main/assets/poster.js"
s=p.read_text(encoding="utf-8")
old="const metrics=x.measureText(phone),centerY=h*.925;"
new="const metrics=x.measureText(phone),centerY=h*.953;"
if s.count(old)!=1:raise RuntimeError("Expected exactly one v15 phone center anchor")
p.write_text(s.replace(old,new),encoding="utf-8")
print("v16: phone vertical center restored to black strip")
