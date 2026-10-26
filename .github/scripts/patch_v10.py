#!/usr/bin/env python3
"""Patch the original v5 KT Wi-Fi QR project into the v10 layout/OCR version."""
from pathlib import Path
import sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'wifi-project')
assets = root / 'app/src/main/assets'

def replace_section(source, start, end, new):
    i = source.find(start)
    if i < 0:
        raise RuntimeError('Missing source anchor: ' + start)
    j = source.find(end, i + len(start))
    if j < 0:
        raise RuntimeError('Missing source anchor: ' + end)
    return source[:i] + new.rstrip() + '\n' + source[j:]

app_path = assets / 'app.js'
app = app_path.read_text(encoding='utf-8')
app = replace_section(app, 'function parseLabel(text){', 'function renderPhoto(){', r'''
function parseLabel(text){
 const lines=String(text||'').split(/\r?\n/).map(s=>s.trim()).filter(Boolean);
 let ssid='',password='',ssidRow=-1;
 const ssidRx=/(?:KT[_ ](?:GiGA|WiFi)[\w _.-]*|iptime[\w ._-]*|[\w.-]+_[\w._-]+)/i;
 const pwLabel=/(?:암\s*호\s*키?|비\s*밀\s*번\s*호|password|Wi-?Fi\s*key|WLAN\s*key)/i;
 const candidate=v=>{
  const t=String(v||'').replace(/^[\s:：=]+/,'').trim();
  const m=t.match(/^([A-Za-z0-9!@#$%^&*._-]{8,63})(?:\s|$)/);
  return m?m[1]:'';
 };
 for(let i=0;i<lines.length;i++){
  const l=lines[i];
  const labeled=l.match(/(?:무\s*선\s*랜\s*명|SSID|Wi-?Fi\s*(?:name|SSID))\s*[:：]?\s*(.*)/i);
  const m=(labeled?labeled[1]:l).match(ssidRx);
  if(m&&!/MAC\s*ADDRESS|BARCODE|제조|관리\s*번호/i.test(l)){
   if(!ssid){ssid=m[0].trim().replace(/ /g,'_');ssidRow=i;}
  }
 }
 for(let i=0;i<lines.length;i++){
  const l=lines[i],label=l.match(pwLabel);
  if(!label)continue;
  let v=candidate(l.slice(label.index+label[0].length));
  if(!v){
   for(let j=i+1;j<=Math.min(i+2,lines.length-1);j++){
    if(/MAC|BARCODE|ADDRESS|제조|관리|노출|변경|사용|SSID|무\s*선/i.test(lines[j]))break;
    v=candidate(lines[j]);if(v)break;
   }
  }
  if(v&&!v.includes('_GiGA_')){password=v;break;}
 }
 // ML Kit sometimes detects only the Latin value, not the Korean '암호키' label.
 // In a crop containing SSID and password, the next short ASCII row is the key.
 if(!password&&ssidRow>=0){
  for(let j=ssidRow+1;j<=Math.min(ssidRow+3,lines.length-1);j++){
   const l=lines[j];
   if(/MAC|BARCODE|ADDRESS|제조|관리|노출|변경|사용|정격|출력/i.test(l))break;
   const stripped=l.replace(/^(?:암\s*호\s*키?|비\s*밀\s*번\s*호)\s*[:：]?\s*/i,'');
   const v=candidate(stripped);
   if(v&&v!==ssid&&!/^(?:KT_|iptime)/i.test(v)){password=v;break;}
  }
 }
 return{ssid,password};
}
''')
app = replace_section(app, 'function applyOcr(results){', 'window.hongjaOcrResult=', r'''
function applyOcr(results){
 const target=activeTarget;
 results=(results||[]).filter(r=>r.text&&r.text.trim());
 if(!results.length){$('status').textContent='글자를 찾지 못했습니다. 암호 줄만 선택해 다시 읽어주세요.';return;}
 const full=results.find(r=>r.method==='MLKit-full');
 const lines=results.filter(r=>r.method!=='MLKit-full');
 const joined=full?full.text:lines.map(r=>r.text).join('\n');
 let parsed=parseLabel(joined);
 if(!parsed.password){
  const alternate=parseLabel(lines.map(r=>r.text).join('\n'));
  if(!parsed.ssid)parsed.ssid=alternate.ssid;
  parsed.password=alternate.password;
 }
 // A label can be a separate OCR line; use its position to find the Latin value.
 if(!parsed.password){
  const rows=lines.filter(r=>Number.isFinite(r.y)).sort((a,b)=>a.y-b.y||a.x-b.x);
  const ssidLine=rows.find(r=>parsed.ssid&&r.text.replace(/\s/g,'').includes(parsed.ssid));
  if(ssidLine){
   const candidates=rows.filter(r=>r.y>ssidLine.y+ssidLine.h*.4 && r.y<ssidLine.y+Math.max(70,ssidLine.h*4));
   for(const row of candidates){
    const m=row.text.trim().match(/^([A-Za-z0-9!@#$%^&*._-]{8,63})$/);
    if(m&&m[1]!==parsed.ssid&&!/^(?:KT_|iptime)/i.test(m[1])){parsed.password=m[1];break;}
   }
  }
 }
 if(target==='password'&&!parsed.password){
  const s=joined.replace(/^(?:암\s*호\s*키?|비\s*밀\s*번\s*호|password|Wi-?Fi\s*key)\s*[:：]?\s*/i,'').trim();
  const m=s.match(/^([A-Za-z0-9!@#$%^&*._-]{8,63})$/);
  if(m)parsed.password=m[1];
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
 // Display manual alternatives without overwriting the combined result.
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
 $('status').textContent=parsed.password?'무선랜명과 암호 인식을 확인하세요.':'암호를 자동 인식하지 못했습니다. 암호 글자 한 줄만 선택해 다시 읽어주세요.';
}
''')
app_path.write_text(app,encoding='utf-8')

java_path = root / 'app/src/main/java/com/hongja/ktwifi/MainActivity.java'
java = java_path.read_text(encoding='utf-8')
anchor = 'row.put("text",line.getText());arr.put(row);'
if anchor not in java: raise RuntimeError('Native ML Kit line anchor missing')
java=java.replace(anchor, 'row.put("text",line.getText());row.put("method","MLKit-line");android.graphics.Rect rect=line.getBoundingBox();if(rect!=null){row.put("x",rect.left);row.put("y",rect.top);row.put("w",rect.width());row.put("h",rect.height());}arr.put(row);')
anchor='obj.put("results",arr);'
if anchor not in java: raise RuntimeError('Native ML Kit result anchor missing')
java=java.replace(anchor,'JSONObject full=new JSONObject();full.put("method","MLKit-full");full.put("text",result.getText());arr.put(full);obj.put("results",arr);')
java_path.write_text(java,encoding='utf-8')

poster_path=assets/'poster.js'
poster=poster_path.read_text(encoding='utf-8')
poster=replace_section(poster,'const size=Math.round(', '}catch(e){', r'''
// The printed red frame occupies x=366..684 and y=855..1140 in a 1054x1492 image.
// Center a square WITH its 4-module quiet zone inside that frame; never paint over red.
const side=Math.min(w*.254,h*.179),cell=side/(n+8);
const left=w*.498-side/2,top=h*.669-side/2;
x.fillStyle='#fff';x.fillRect(left,top,side,side);
x.fillStyle='#111';
for(let r=0;r<n;r++)for(let col=0;col<n;col++)
 if(qr.isDark(r,col))x.fillRect(left+(col+4)*cell,top+(r+4)*cell,cell,cell);
''')
# The phone belongs at the center of the black bar (not the center of the entire page).
import re
poster,count=re.subn(r'x\.fillText\(phone,w\*\.[0-9]+,h\*\.[0-9]+,w\*\.[0-9]+\)', 'x.fillText(phone,w*.674,h*.926,w*.49)',poster)
if count!=1: raise RuntimeError('Phone position anchor missing')
poster_path.write_text(poster,encoding='utf-8')
print('v10 patched: OCR aggregation + native line bounds, QR frame, phone center')
