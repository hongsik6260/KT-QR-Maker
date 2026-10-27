import io,os,re,sys,zipfile,tkinter as tk
from tkinter import filedialog,messagebox,ttk
from PIL import Image,ImageDraw,ImageFont,ImageTk
import qrcode
from pathlib import Path
ROOT=Path(getattr(sys,"_MEIPASS",Path(__file__).parent))
BG=ROOT/"kt_background.png"
def font(size,bold=False):
 paths=[r"C:\\Windows\\Fonts\\malgunbd.ttf" if bold else r"C:\\Windows\\Fonts\\malgun.ttf","/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc"]
 for p in paths:
  try:return ImageFont.truetype(p,size)
  except:pass
 return ImageFont.load_default()
def fit(draw,text,box,maxsize=42):
 for size in range(maxsize,13,-1):
  f=font(size,True)
  if draw.textbbox((0,0),text,font=f)[2]<=box:return f
 return font(14)
def render(ssid,password,phone):
 if not BG.exists():raise RuntimeError("KT background image missing")
 im=Image.open(BG).convert("RGB")
 w,h=im.size;d=ImageDraw.Draw(im)
 # Coordinates correspond to the v17 A4 poster layout.
 q=qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M,border=0,box_size=12)
 q.add_data("WIFI:T:WPA;S:"+ssid.replace(";","\\;")+";P:"+password.replace(";","\\;")+";;")
 q.make(fit=True)
 qr=q.make_image(fill_color="black",back_color="white").convert("RGB")
 x1,y1,x2,y2=[int(v) for v in (w*.356,h*.600,w*.670,h*.775)]
 side=min(x2-x1-20,y2-y1-20)
 qr=qr.resize((side,side),Image.Resampling.NEAREST)
 im.paste(qr,((x1+x2-side)//2,(y1+y2-side)//2))
 # White value panels: match poster red label/value layout.
 for txt,cy in [(ssid,.814),(password,.866)]:
  x=int(w*.483);y=int(h*cy);maxwidth=int(w*.285)
  d.text((x,y),txt,font=fit(d,txt,maxwidth,int(w*.031)),fill="#101010",anchor="lm")
 # Phone text is centered inside the right black footer region.
 x=int(w*.700);y=int(h*.961)
 d.text((x,y),phone,font=fit(d,phone,int(w*.36),int(w*.042)),fill="white",anchor="mm")
 return im
class App:
 def __init__(self,root):
  self.root=root;root.title("KT Wi-Fi QR 생성기 · PC");root.geometry("540x660")
  self.vars=[tk.StringVar() for _ in range(3)]
  tk.Label(root,text="KT Wi-Fi QR 생성기",font=("Malgun Gothic",21,"bold"),fg="#D90D21").pack(pady=12)
  for label,var in zip(("무선랜명 (SSID)","암호","전화번호 (- 입력 가능)"),self.vars):
   tk.Label(root,text=label,font=("Malgun Gothic",11)).pack(anchor="w",padx=25)
   tk.Entry(root,textvariable=var,font=("Malgun Gothic",12)).pack(fill="x",padx=25,pady=(2,10))
  buttons=tk.Frame(root);buttons.pack(pady=8)
  tk.Button(buttons,text="미리보기",command=self.preview,width=12).pack(side="left",padx=4)
  tk.Button(buttons,text="PNG 저장",command=self.png,width=12).pack(side="left",padx=4)
  tk.Button(buttons,text="PDF 저장",command=self.pdf,width=12).pack(side="left",padx=4)
  self.view=tk.Label(root);self.view.pack(pady=8)
  tk.Label(root,text="PC 버전: 사진 OCR은 미지원 · A4 이미지/PDF 저장",fg="#666").pack()
 def image(self):
  ssid,password,phone=[v.get().strip() for v in self.vars]
  if not ssid or not password:raise ValueError("무선랜명과 암호를 입력하세요.")
  return render(ssid,password,phone)
 def preview(self):
  try:
   im=self.image();im.thumbnail((370,430))
   self.photo=ImageTk.PhotoImage(im);self.view.configure(image=self.photo)
  except Exception as e:messagebox.showerror("오류",str(e))
 def png(self):
  try:
   im=self.image();p=filedialog.asksaveasfilename(defaultextension=".png",filetypes=[("PNG","*.png")])
   if p:im.save(p)
  except Exception as e:messagebox.showerror("오류",str(e))
 def pdf(self):
  try:
   im=self.image();p=filedialog.asksaveasfilename(defaultextension=".pdf",filetypes=[("PDF","*.pdf")])
   if p:im.save(p,"PDF",resolution=300)
  except Exception as e:messagebox.showerror("오류",str(e))
if __name__=="__main__":
 root=tk.Tk();App(root);root.mainloop()
