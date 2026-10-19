package com.hongja.ktqrmaker;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
  EditText ssid, pass, phone;
  Spinner paper;
  ImageView preview;
  Bitmap poster;
  static final int PICK=1, CAMERA=2;
  Uri photoUri;
  int[] widths={2480,1748,1240,874};
  int[] heights={3508,2480,1748,1240};
  String[] labels={"A4","A5","A6","A7"};
  @Override public void onCreate(Bundle b){super.onCreate(b);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,20,20,20);
    ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
    TextView title=new TextView(this);title.setText("KT QR 홍보물 제작기");title.setTextSize(24);title.setTextColor(Color.RED);root.addView(title);
    addButton(root,"사진 촬영",()->{try{Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAMERA);}catch(Exception e){toast("카메라를 열 수 없습니다");}});
    addButton(root,"이미지 첨부 / 문자 추출",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,PICK);});
    ssid=field(root,"무선랜명 (SSID)","");
    pass=field(root,"무선랜 암호","");
    phone=field(root,"상담 연락처","010-7224-7624");
    paper=new Spinner(this);paper.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(paper);
    addButton(root,"QR 홍보물 미리보기",()->render());
    addButton(root,"PNG 고해상도 저장",()->save(false));
    addButton(root,"PDF 저장",()->save(true));
    preview=new ImageView(this);preview.setAdjustViewBounds(true);root.addView(preview);
    TextView notice=new TextView(this);notice.setText("문자 인식 결과를 꼭 확인하세요. QR은 입력한 와이파이 정보로 생성됩니다. 사진은 서버로 전송하지 않습니다.");root.addView(notice);
  }
  EditText field(LinearLayout root,String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setText(val);root.addView(e);return e;}
  void addButton(LinearLayout root,String label,Runnable r){Button b=new Button(this);b.setText(label);root.addView(b);b.setOnClickListener(v->r.run());}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return; if(req==3){writeOutput(data.getData());return;}
    try{
      InputImage img;
      if(req==PICK){img=InputImage.fromFilePath(this,data.getData());}
      else {Bitmap bmp=(Bitmap)data.getExtras().get("data");img=InputImage.fromBitmap(bmp,0);}
      TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(img).addOnSuccessListener(t->{
        String raw=t.getText();String[] lines=raw.split("\\n");String s="",p="";
        for(int j=0;j<lines.length;j++){String line=lines[j].trim();String low=line.toLowerCase(Locale.ROOT);
          if(low.matches(".*(ssid|무선랜명|네트워크 이름|wifi name).*"))s=value(line,j,lines);
          if(low.matches(".*(password|비밀번호|암호|wifi key|보안키).*"))p=value(line,j,lines);
        }
        if(!s.isEmpty())ssid.setText(s);if(!p.isEmpty())pass.setText(p);
        new AlertDialog.Builder(this).setTitle("인식된 글씨 (확인 후 수정)").setMessage(raw).setPositiveButton("확인",(a,b)->{}).show();
      }).addOnFailureListener(e->toast("문자 인식 실패: "+e.getMessage()));
    }catch(Exception e){toast(e.getMessage());}
  }
  String value(String line,int i,String[] lines){int a=line.indexOf(':');if(a<0)a=line.indexOf('：');String v=a>=0?line.substring(a+1).trim():"";if(v.isEmpty()&&i+1<lines.length)v=lines[i+1].trim();return v;}
  String wifi(){return "WIFI:T:WPA;S:"+esc(ssid.getText().toString())+";P:"+esc(pass.getText().toString())+";;";}
  String esc(String v){String slash=Character.toString((char)92);return v.replace(slash,slash+slash).replace(";",slash+";").replace(":",slash+":").replace(",",slash+",");}
  Bitmap qr(int size)throws Exception{BitMatrix bits=new MultiFormatWriter().encode(wifi(),BarcodeFormat.QR_CODE,size,size,Map.of(EncodeHintType.MARGIN,1,EncodeHintType.ERROR_CORRECTION,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H));Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);for(int y=0;y<size;y++)for(int x=0;x<size;x++)b.setPixel(x,y,bits.get(x,y)?Color.BLACK:Color.WHITE);return b;}
  void render(){try{String s=ssid.getText().toString().trim();if(s.isEmpty()){toast("무선랜명을 입력하세요");return;}
    int idx=paper.getSelectedItemPosition(),w=widths[idx],h=heights[idx];poster=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    Canvas c=new Canvas(poster);c.drawColor(Color.WHITE);Paint p=new Paint(3);float k=w/2480f;
    p.setColor(Color.rgb(225,0,38));c.drawRect(0,0,w,500*k,p);p.setColor(Color.WHITE);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextSize(200*k);c.drawText("kt",135*k,250*k,p);
    p.setTextSize(77*k);c.drawText("더 편리한 일상, KT와 함께",135*k,380*k,p);
    p.setColor(Color.rgb(30,30,30));p.setTextSize(87*k);c.drawText("하이오더  |  인터넷 TV",140*k,690*k,p);c.drawText("핸드폰  |  번호이동",140*k,815*k,p);
    p.setColor(Color.rgb(245,245,245));c.drawRoundRect(120*k,930*k,w-120*k,h-420*k,60*k,60*k,p);
    int q=(int)(1350*k);Bitmap b=qr(q);float left=(w-q)/2f,top=1100*k;c.drawBitmap(b,left,top,p);
    p.setColor(Color.BLACK);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(82*k);
    c.drawText("무선랜명: "+s,165*k,top+q+150*k,p);c.drawText("암호: "+pass.getText().toString(),165*k,top+q+270*k,p);
    p.setColor(Color.rgb(225,0,38));c.drawRect(0,h-360*k,w,h,p);p.setColor(Color.WHITE);p.setTextSize(90*k);c.drawText("가입 및 상담 문의",130*k,h-225*k,p);p.setTextSize(110*k);c.drawText(phone.getText().toString(),130*k,h-85*k,p);
    preview.setImageBitmap(poster);toast("미리보기 생성 완료");
  }catch(Exception e){toast("생성 오류: "+e.getMessage());}}
  void save(boolean pdf){try{render();if(poster==null)return;String name="KT_QR_"+System.currentTimeMillis()+(pdf?".pdf":".png");
    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType(pdf?"application/pdf":"image/png");intent.putExtra(Intent.EXTRA_TITLE,name);
    pendingPdf=pdf;startActivityForResult(intent,3);
  }catch(Exception e){toast(e.getMessage());}}
  boolean pendingPdf;

  void writeOutput(Uri uri){try(OutputStream out=getContentResolver().openOutputStream(uri)){
    if(pendingPdf){PdfDocument doc=new PdfDocument();PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(poster.getWidth(),poster.getHeight(),1).create();PdfDocument.Page page=doc.startPage(info);page.getCanvas().drawBitmap(poster,0,0,null);doc.finishPage(page);doc.writeTo(out);doc.close();}
    else poster.compress(Bitmap.CompressFormat.PNG,100,out);toast("저장 완료");
  }catch(Exception e){toast("저장 실패: "+e.getMessage());}}
}
