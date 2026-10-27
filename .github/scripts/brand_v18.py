#!/usr/bin/env python3
"""Brand Android app, preserving all existing functionality."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import sys,re
root=Path(sys.argv[1])
res=root/"app/src/main/res"
for density,size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
 d=res/("mipmap-"+density);d.mkdir(exist_ok=True,parents=True)
 scale=4;w=size*scale
 im=Image.new("RGB",(w,w),"#D90D21");draw=ImageDraw.Draw(im)
 draw.rounded_rectangle((0,0,w-1,w-1),radius=w//5,fill="#D90D21")
 draw.rounded_rectangle((w*.11,w*.12,w*.89,w*.88),radius=w*.10,fill="white")
 try:font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",int(w*.20))
 except:font=ImageFont.load_default()
 draw.text((w*.5,w*.27),"WiFi",font=font,anchor="mm",fill="#D90D21")
 for x,y in [(0,0),(2,0),(0,2),(2,2)]:
  xx=w*(.25+x*.19);yy=w*(.47+y*.19)
  draw.rectangle((xx,yy,xx+w*.12,yy+w*.12),fill="#171717")
 for x,y in [(1,1),(2,1),(1,2)]:
  xx=w*(.25+x*.19);yy=w*(.47+y*.19)
  draw.rectangle((xx,yy,xx+w*.10,yy+w*.10),fill="#171717")
 im.resize((size,size),Image.Resampling.LANCZOS).save(d/"ic_launcher.png")
manifest=root/"app/src/main/AndroidManifest.xml"
t=manifest.read_text()
t=re.sub(r'android:label="[^"]*"', 'android:label="KT Wi-Fi QR 생성기"',t,count=1)
t=re.sub(r'android:icon="@[^"]+"','android:icon="@mipmap/ic_launcher"',t,count=1)
if 'android:icon=' not in t:
 t=t.replace('<application','<application android:icon="@mipmap/ic_launcher"',1)
manifest.write_text(t)
print("Android branded icon and title applied")
