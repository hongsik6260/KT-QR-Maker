#!/usr/bin/env python3
"""Apply v12 fixes after patch_v11.py to the unpacked v5 Android project."""
from pathlib import Path
import sys

root=Path(sys.argv[1] if len(sys.argv)>1 else "wifi-project")
assets=root/"app/src/main/assets"

def replace_between(src,start,end,replacement):
    a=src.find(start)
    if a<0: raise RuntimeError("v12 missing start anchor: "+start)
    b=src.find(end,a+len(start))
    if b<0: raise RuntimeError("v12 missing end anchor: "+end)
    return src[:a]+replacement.rstrip()+"\n"+src[b:]

app_file=assets/"app.js"
app=app_file.read_text(encoding="utf-8")
if "MLKit-full-Korean" not in app: raise RuntimeError("Apply patch_v11.py before patch_v12.py")
app=replace_between(app,"function parseLabel(text){","function renderPhoto(){",r'''
function parseLabel(text){
 const lines=String(text||'').normalize('NFKC').split(/\r?\n/).map(s=>s.replace(/[\u200b-\u200d\ufeff]/g,'').trim()).filter(Boolean);
 const ssidRx=/(?:KT[_ ](?:GiGA|WiFi)[\w _.-]*|iptime[\w ._-]*|[\w.-]+_[\w._-]+)/i;
 const pwRx=/(?:암\s*호\s*키?|비\s*밀\s*번\s*호|패\s*스\s*워\s*드|password|pass\s*word|Wi-?Fi\s*key|WLAN\s*key)/i;
 let ssid='',password='',ssidRow=-1;
 function keyCandidate(value){
  const t=String(value||'').normalize('NFKC').replace(/[\u200b-\u200d\ufeff]/g,'').replace(/^[\s:：=]+/,'').trim();
  const joined=t.replace(/\s+/g,'');
  if(/^[A-Za-z0-9!@#$%^&*._-]{8,63}$/.test(joined)&&!/^(?:KT_|iptime)/i.test(joined))return joined;
  const m=t.match(/(?:^|[\s:：=])([A-Za-z0-9!@#$%^&*._-]{8,63})(?=\s|$)/);
  return m&&!/^(?:KT_|iptime)/i.test(m[1])?m[1]:'';
 }
 for(let i=0;i<lines.length;i++){
  const line=lines[i],labeled=line.match(/(?:무\s*선\s*랜\s*명|SSID|Wi-?Fi\s*(?:name|SSID))\s*[:：]?\s*(.*)/i);
  const m=(labeled?labeled[1]:line).match(ssidRx);
  if(m&&!/MAC\s*ADDRESS|BARCODE|제조|관리\s*번호/i.test(line)){
   if(!ssid){ssid=m[0].trim().replace(/ /g,'_');ssidRow=i;}
  }
 }
 for(let i=0;i<lines.length;i++){
  const label=pwRx.exec(lines[i]);if(!label)continue;
  let key=keyCandidate(lines[i].slice(label.index+label[0].length));
  if(!key){
   for(let j=i+1;j<=Math.min(i+3,lines.length-1);j++){
    if(/MAC|BARCODE|ADDRESS|제조|관리|노출|변경|사용|SSID|무\s*선|정격|출력/i.test(lines[j]))break;
    key=keyCandidate(lines[j]);if(key)break;
   }
  }
  if(key){password=key;break;}
 }
 if(!password&&ssidRow>=0){
  for(let j=ssidRow+1;j<=Math.min(ssidRow+3,lines.length-1);j++){
   if(/MAC|BARCODE|ADDRESS|제조|관리|노출|변경|사용|정격|출력/i.test(lines[j]))break;
   const stripped=lines[j].replace(/^(?:암\s*호\s*키?|비\s*밀\s*번\s*호)\s*[:：]?\s*/i,'');
   const key=keyCandidate(stripped);
   if(key&&key!==ssid){password=key;break;}
  }
 }
 return{ssid,password};
}
''')
app=replace_between(app,"function applyOcr(results){","window.hongjaOcrResult=",r'''
function applyOcr(results){
 const target=activeTarget;
 results=(results||[]).filter(r=>r.text&&r.text.trim());
 if(!results.length){$('status').textContent='글자를 찾지 못했습니다. 암호 줄만 선택해 다시 읽어주세요.';return;}
 const full=results.filter(r=>String(r.method||'').startsWith('MLKit-full'));
 const lines=results.filter(r=>!String(r.method||'').startsWith('MLKit-full'));
 // Parse each recognizer independently first. Concatenating full Latin and Korean
 // output can place duplicate SSID/label lines between the password and its label.
 const attempts=[...full.map(r=>r.text),lines.filter(r=>/Korean/i.test(r.method||'')).map(r=>r.text).join('\n'),
  lines.filter(r=>/Latin/i.test(r.method||'')).map(r=>r.text).join('\n'),
  lines.map(r=>r.text).join('\n')];
 let parsed={ssid:'',password:''};
 for(const s of attempts){
  const v=parseLabel(s);
  if(!parsed.ssid&&v.ssid)parsed.ssid=v.ssid;
  if(!parsed.password&&v.password)parsed.password=v.password;
 }
 // Korean label and Latin password can be two different OCR detections.
 // Compare their image coordinates rather than trusting OCR text order.
 if(!parsed.password){
  const rows=lines.filter(r=>Number.isFinite(Number(r.y))&&Number.isFinite(Number(r.x))).map(r=>({
   text:r.text.trim(),x:Number(r.x),y:Number(r.y),w:Number(r.w)||0,h:Number(r.h)||18
  }));
  const labels=rows.filter(r=>/(?:암\s*호\s*키?|비\s*밀\s*번\s*호|password|Wi-?Fi\s*key)/i.test(r.text));
  function candidate(t){
   const s=String(t||'').normalize('NFKC').replace(/^[\s:：=]+/,'').replace(/\s+/g,'');
   return /^[A-Za-z0-9!@#$%^&*._-]{8,63}$/.test(s)&&
    !/^(?:KT_|iptime)/i.test(s)&&s!==parsed.ssid?s:'';
  }
  for(const label of labels){
   const direct=parseLabel(label.text).password;
   if(direct){parsed.password=direct;break;}
   const near=rows.map(r=>{
    const dy=Math.abs((r.y+r.h/2)-(label.y+label.h/2));
    const sameRow=dy<=Math.max(20,label.h*.9)&&r.x>=label.x+label.w*.4;
    const nextRow=r.y>=label.y+label.h*.5&&r.y<=label.y+label.h*3;
    return{r,score:sameRow?dy*.3+Math.max(0,r.x-label.x-label.w)*.01:
      nextRow?dy+30:1e9};
   }).sort((a,b)=>a.score-b.score);
   for(const entry of near){
    if(entry.score>=1e9)break;
    const key=candidate(entry.r.text);
    if(key){parsed.password=key;break;}
   }
   if(parsed.password)break;
  }
 }
 // When only the password row was cropped, the label may not be present.
 if(!parsed.password&&target==='password'){
  for(const s of attempts){
   const stripped=s.trim().replace(/^(?:암\s*호\s*키?|비\s*밀\s*번\s*호|password|Wi-?Fi\s*key)\s*[:：]?\s*/i,'');
   const key=stripped.replace(/\s+/g,'');
   if(/^[A-Za-z0-9!@#$%^&*._-]{8,63}$/.test(key)&&!/^(?:KT_|iptime)/i.test(key)){parsed.password=key;break;}
  }
 }
 $('raw').textContent=results.map(r=>(r.method||'MLKit')+'\n'+r.text).join('\n\n');
 $('rawArea').hidden=false;
 const options=[];
 if(target==='both'){
  if(parsed.ssid)$('ssid').value=parsed.ssid;
  if(parsed.password)$('password').value=parsed.password;
  if(parsed.ssid||parsed.password)options.push(parsed);
 }else if(target==='password'){
  if(parsed.password){$('password').value=parsed.password;options.push({password:parsed.password});}
 }else{
  if(parsed.ssid){$('ssid').value=parsed.ssid;options.push({ssid:parsed.ssid});}
 }
 for(const row of lines){
  const v=target==='both'?parseLabel(row.text):target==='password'?{password:row.text.trim().replace(/\s/g,'')}:{ssid:row.text.trim()};
  if(v.ssid||v.password)options.push(v);
 }
 invalidate();
 const list=$('ocrCandidates');list.replaceChildren();list.hidden=false;
 const title=document.createElement('p');title.className='hint';
 title.textContent='인식 후보입니다. 사진과 대조해 정확한 값을 선택하세요.';list.append(title);
 const seen=new Set();
 for(const v of options){
  const key=JSON.stringify(v);if(seen.has(key))continue;seen.add(key);
  const bt=document.createElement('button');bt.className='secondary';
  bt.style.cssText='display:block;width:100%;text-align:left;margin:8px 0;overflow-wrap:anywhere;white-space:pre-wrap';
  bt.textContent=(v.ssid?'무선랜명: '+v.ssid+'\n':'')+(v.password?'암호: '+v.password:'');
  bt.onclick=()=>{if(v.ssid)$('ssid').value=v.ssid;if(v.password)$('password').value=v.password;invalidate();};list.append(bt);
 }
 $('status').textContent=parsed.password?'인식한 암호를 사진과 대조해 주세요.':'암호를 자동 인식하지 못했습니다. 암호 한 줄을 선택해 다시 읽어주세요.';
}
''')
app_file.write_text(app,encoding="utf-8")

poster_file=assets/"poster.js"
poster=poster_file.read_text(encoding="utf-8")
poster=replace_between(poster,"const side=Math.min(","}catch(e){",r'''
// Keep all four quiet-zone modules, but maximize QR within the existing red frame.
// Background design dimensions: 1054x1492; frame x=366..684, y=855..1139.
const frame={left:w*.350,right:w*.650,top:h*.574,bottom:h*.762};
const safe=Math.max(6,Math.round(w*.006));
const qrSide=Math.floor(Math.min(frame.right-frame.left-2*safe,frame.bottom-frame.top-2*safe));
const left=Math.round((frame.left+frame.right-qrSide)/2);
const top=Math.round((frame.top+frame.bottom-qrSide)/2);
const qrCanvas=document.createElement('canvas');
qrCanvas.width=n+8;qrCanvas.height=n+8;
const qctx=qrCanvas.getContext('2d');
qctx.fillStyle='#fff';qctx.fillRect(0,0,n+8,n+8);
qctx.fillStyle='#111';
for(let r=0;r<n;r++)for(let col=0;col<n;col++)
 if(qr.isDark(r,col))qctx.fillRect(col+4,r+4,1,1);
x.save();x.imageSmoothingEnabled=false;
x.drawImage(qrCanvas,left,top,qrSide,qrSide);
x.restore();
''')
poster=replace_between(poster,"x.textAlign='left';x.textBaseline='middle';","msg.textContent='홍보물 생성 완료.",r'''
// Background input fields start after the printed red labels at x=485.
// Fit by font size (not horizontal stretching) inside the white rounded rectangles.
const fieldLeft=w*.483,fieldRight=w*.755,fieldWidth=fieldRight-fieldLeft;
function drawField(value,y){
 const str=String(value||'');
 let font=Math.round(w*.032),minimum=Math.max(13,Math.round(w*.013));
 x.textAlign='left';x.textBaseline='middle';x.fillStyle='#141414';
 do{x.font='bold '+font+'px sans-serif';if(x.measureText(str).width<=fieldWidth||font<=minimum)break;font--;}while(font>minimum);
 x.fillText(str,fieldLeft,y,fieldWidth);
}
drawField(ssid,h*.791);
drawField(sec==='nopass'?'없음':pw,h*.841);
const phone=$('posterPhone').value.trim();
if(phone){
 x.textAlign='center';x.fillStyle='#fff';
 x.font='bold '+Math.round(w*.045)+'px sans-serif';
 const metrics=x.measureText(phone),centerY=h*.926;
 const ascent=metrics.actualBoundingBoxAscent||w*.035;
 const descent=metrics.actualBoundingBoxDescent||w*.006;
 x.textBaseline='alphabetic';
 x.fillText(phone,w*.674,centerY+(ascent-descent)/2,w*.52);
}
''')
poster_file.write_text(poster,encoding="utf-8")
print("v12 patched: OCR label/value pairing, centered maximum QR with 4-module quiet zone, fitted fields, phone alignment")
