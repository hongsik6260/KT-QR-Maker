#!/usr/bin/env python3
"""v14 poster: 10px red-frame clearance, no extra white QR padding, text phone keyboard."""
from pathlib import Path
import sys,re
root=Path(sys.argv[1] if len(sys.argv)>1 else "wifi-project")
assets=root/"app/src/main/assets"
p=assets/"poster.js"
src=p.read_text(encoding="utf-8")
start=src.index("const frame={left:")
end=src.index("}catch(e){",start)
replacement="""const frame={left:w*.356,right:w*.670,top:h*.588,bottom:h*.787};
// Keep a 10-pixel inset from the red frame. No added white border around QR.
const inset=10;
const qrSide=Math.floor(Math.min(frame.right-frame.left-2*inset,frame.bottom-frame.top-2*inset));
const left=Math.round((frame.left+frame.right-qrSide)/2);
const top=Math.round((frame.top+frame.bottom-qrSide)/2);
const qrCanvas=document.createElement('canvas');
qrCanvas.width=n;qrCanvas.height=n;
const qctx=qrCanvas.getContext('2d');
qctx.fillStyle='#fff';qctx.fillRect(0,0,n,n);
qctx.fillStyle='#111';
for(let r=0;r<n;r++)for(let col=0;col<n;col++)
 if(qr.isDark(r,col))qctx.fillRect(col,r,1,1);
x.save();x.imageSmoothingEnabled=false;
x.drawImage(qrCanvas,left,top,qrSide,qrSide);
x.restore();
"""
src=src[:start]+replacement+src[end:]
p.write_text(src,encoding="utf-8")
# The actual phone input may be created in JS; update both HTML and JS templates.
changed=0
for p in assets.rglob("*"):
 if not p.is_file() or p.suffix.lower() not in (".html",".js"):continue
 t=p.read_text(encoding="utf-8")
 if "posterPhone" not in t:continue
 before=t
 # input attribute order varies, so replace only the matching input tag.
 def fix(m):
  tag=m.group(0)
  if "posterPhone" not in tag:return tag
  for old in ('type="number"',"type='number'",'type="tel',"type='tel'"):
   tag=tag.replace(old,'type="text"')
  for old in ('inputmode="numeric"','inputmode="tel"',"inputmode='numeric'","inputmode='tel'"):
   tag=tag.replace(old,'inputmode="text"')
  tag=tag.replace('pattern="[0-9]*"','').replace('pattern="[0-9-]*"','')
  return tag
 t=re.sub('<input[^>]*>',fix,t,flags=re.I)
 if t!=before:p.write_text(t,encoding="utf-8");changed+=1
print("v14: QR no quiet-zone pixels, frame inset 10px; phone input files changed",changed)
