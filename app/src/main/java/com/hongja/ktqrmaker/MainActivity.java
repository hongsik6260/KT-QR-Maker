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
  static final int PICK=1, CAMERA=2, BACKGROUND=4;
  Bitmap background;
  Uri photoUri;
  int[] widths={2480,1748,1240,874};
  int[] heights={3508,2480,1748,1240};
  String[] labels={"A4","A5","A6","A7"};
  @Override public void onCreate(Bundle b){super.onCreate(b);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(20,20,20,20);
    ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);
    TextView title=new TextView(this);title.setText("KT QR 홍보물 제작기");title.setTextSize(24);title.setTextColor(Color.RED);root.addView(title);
    try{background=BitmapFactory.decodeFile(new File(getFilesDir(),"kt_background.png").getAbsolutePath());}catch(Exception ignored){}
    addButton(root,"홍보 배경 이미지 등록 (최초 1회)",()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,BACKGROUND);});
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
  @Override protected void onActivityResult(int req,int result,Intent data){
    super.onActivityResult(req,result,data);
    if(result!=RESULT_OK||data==null)return;
    if(req==3){writeOutput(data.getData());return;}
    if(req==BACKGROUND){try(InputStream in=getContentResolver().openInputStream(data.getData())){Bitmap bmp=BitmapFactory.decodeStream(in);if(bmp==null)throw new Exception("이미지 읽기 실패");try(FileOutputStream out=new FileOutputStream(new File(getFilesDir(),"kt_background.png"))){bmp.compress(Bitmap.CompressFormat.PNG,100,out);}background=bmp;toast("KT 홍보 배경 저장 완료");}catch(Exception e){toast(e.getMessage());}return;}
    try{
      Bitmap bmp;
      if(req==PICK){try(InputStream in=getContentResolver().openInputStream(data.getData())){bmp=BitmapFactory.decodeStream(in);}}
      else bmp=(Bitmap)data.getExtras().get("data");
      if(bmp==null){toast("사진을 읽지 못했습니다");return;}
      showCrop(bmp);
    }catch(Exception e){toast("사진 오류: "+e.getMessage());}
  }
  void showCrop(Bitmap source){
    final Bitmap[] current={source};
    final int[] angle={0};
    LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(16,12,16,12);
    TextView info=new TextView(this);info.setText("초록색 영역을 손가락으로 움직여 글자만 선택하세요. 모서리를 끌어 크기를 조절합니다.");layout.addView(info);
    CropView crop=new CropView(this,current[0]);layout.addView(crop,new LinearLayout.LayoutParams(-1,(int)(360*getResources().getDisplayMetrics().density)));
    Spinner target=new Spinner(this);String[] modes={"무선랜명 + 암호 (두 줄)","무선랜명만","암호만"};
    target.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,modes));layout.addView(target);
    Button rotate=new Button(this);rotate.setText("사진 90도 회전");layout.addView(rotate);
    rotate.setOnClickListener(v->{Matrix m=new Matrix();m.postRotate(90);Bitmap old=current[0];current[0]=Bitmap.createBitmap(old,0,0,old.getWidth(),old.getHeight(),m,true);crop.setBitmap(current[0]);});
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("문자 추출 영역 선택").setView(layout).setNegativeButton("취소",null).setPositiveButton("선택 영역 글자 읽기",null).create();
    dialog.setOnShowListener(v->dialog.getButton(-1).setOnClickListener(w->{
      Bitmap selected=crop.selected();int mode=target.getSelectedItemPosition();dialog.dismiss();readSelection(selected,mode);
    }));dialog.show();
  }
  void readSelection(Bitmap bmp,int mode){
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener(t->{
      ArrayList<String> candidates=new ArrayList<>();
      for(com.google.mlkit.vision.text.Text.TextBlock block:t.getTextBlocks())
        for(com.google.mlkit.vision.text.Text.Line line:block.getLines()){String v=line.getText().trim();if(!v.isEmpty())candidates.add(v);}
      if(candidates.isEmpty()){toast("글자를 찾지 못했습니다. 영역을 다시 선택하세요.");return;}
      String[] choices=candidates.toArray(new String[0]);
      if(mode==0){
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(1);
        EditText name=new EditText(this);name.setHint("무선랜명");name.setText(choices[0]);fields.addView(name);
        EditText key=new EditText(this);key.setHint("암호");key.setText(choices.length>1?choices[1]:"");fields.addView(key);
        TextView all=new TextView(this);all.setText("인식 후보: "+android.text.TextUtils.join(" / ",choices));fields.addView(all);
        new AlertDialog.Builder(this).setTitle("인식 결과 확인 및 수정").setView(fields).setPositiveButton("적용",(d,w)->{ssid.setText(name.getText());pass.setText(key.getText());}).setNegativeButton("취소",null).show();
      }else{
        new AlertDialog.Builder(this).setTitle("인식 후보 선택").setSingleChoiceItems(choices,0,(d,which)->{
          EditText edit=new EditText(this);edit.setSingleLine(true);edit.setText(choices[which]);
          d.dismiss();new AlertDialog.Builder(this).setTitle("글자 확인 및 수정").setView(edit).setPositiveButton("적용",(a,w)->{if(mode==1)ssid.setText(edit.getText());else pass.setText(edit.getText());}).setNegativeButton("취소",null).show();
        }).setNegativeButton("취소",null).show();
      }
    }).addOnFailureListener(e->toast("문자 인식 실패: "+e.getMessage()));
  }
  class CropView extends View{
    Bitmap bitmap;Paint paint=new Paint(3);android.graphics.RectF selection=new android.graphics.RectF(.1f,.25f,.9f,.65f);float px,py;boolean resize;
    CropView(Context c,Bitmap b){super(c);bitmap=b;}
    void setBitmap(Bitmap b){bitmap=b;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.DKGRAY);if(bitmap==null)return;
      android.graphics.RectF dest=fit();c.drawBitmap(bitmap,null,dest,paint);
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);paint.setColor(Color.GREEN);
      c.drawRect(dest.left+selection.left*dest.width(),dest.top+selection.top*dest.height(),dest.left+selection.right*dest.width(),dest.top+selection.bottom*dest.height(),paint);
      paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);
    }
    android.graphics.RectF fit(){float scale=Math.min(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;return new android.graphics.RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      android.graphics.RectF d=fit();float x=(e.getX()-d.left)/d.width(),y=(e.getY()-d.top)/d.height();
      if(e.getAction()==0){px=x;py=y;resize=Math.abs(x-selection.right)<.13f&&Math.abs(y-selection.bottom)<.13f;return true;}
      if(e.getAction()==1||e.getAction()==2){float dx=x-px,dy=y-py;
        if(resize){selection.right=Math.max(selection.left+.08f,Math.min(1,selection.right+dx));selection.bottom=Math.max(selection.top+.05f,Math.min(1,selection.bottom+dy));}
        else {float l=Math.max(0,Math.min(1-selection.width(),selection.left+dx)),t=Math.max(0,Math.min(1-selection.height(),selection.top+dy));selection.offsetTo(l,t);}
        px=x;py=y;invalidate();return true;}return true;
    }
    Bitmap selected(){int x=Math.max(0,(int)(selection.left*bitmap.getWidth())),y=Math.max(0,(int)(selection.top*bitmap.getHeight()));int w=Math.max(1,Math.min(bitmap.getWidth()-x,(int)(selection.width()*bitmap.getWidth()))),h=Math.max(1,Math.min(bitmap.getHeight()-y,(int)(selection.height()*bitmap.getHeight())));return Bitmap.createBitmap(bitmap,x,y,w,h);}
  }
  String value(String line,int i,String[] lines){int a=line.indexOf(':');if(a<0)a=line.indexOf('：');String v=a>=0?line.substring(a+1).trim():"";if(v.isEmpty()&&i+1<lines.length)v=lines[i+1].trim();return v;}
  String wifi(){return "WIFI:T:WPA;S:"+esc(ssid.getText().toString())+";P:"+esc(pass.getText().toString())+";;";}
  String esc(String v){String slash=Character.toString((char)92);return v.replace(slash,slash+slash).replace(";",slash+";").replace(":",slash+":").replace(",",slash+",");}
  Bitmap qr(int size)throws Exception{BitMatrix bits=new MultiFormatWriter().encode(wifi(),BarcodeFormat.QR_CODE,size,size,Map.of(EncodeHintType.MARGIN,1,EncodeHintType.ERROR_CORRECTION,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H));Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);for(int y=0;y<size;y++)for(int x=0;x<size;x++)b.setPixel(x,y,bits.get(x,y)?Color.BLACK:Color.WHITE);return b;}
  void render(){try{String s=ssid.getText().toString().trim();if(s.isEmpty()){toast("무선랜명을 입력하세요");return;}
    int idx=paper.getSelectedItemPosition(),w=widths[idx],h=heights[idx];poster=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
    Canvas c=new Canvas(poster);c.drawColor(Color.WHITE);Paint p=new Paint(3);float k=w/1085f;
    if(background!=null){
      c.drawBitmap(background,null,new android.graphics.Rect(0,0,w,h),p);
      // Mask old QR and original sample text, leaving surrounding artwork intact.
      p.setColor(Color.WHITE);
      c.drawRoundRect(380*k,865*k,698*k,1146*k,8*k,8*k,p);
      int q=(int)(258*k);Bitmap qrImage=qr(q);
      c.drawBitmap(qrImage,410*k,877*k,p);
      // White text-value areas only: retain original red field labels and borders.
      c.drawRect(527*k,1156*k,774*k,1210*k,p);
      c.drawRect(527*k,1225*k,774*k,1280*k,p);
      p.setColor(Color.BLACK);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(34*k);
      drawFit(c,p,ssid.getText().toString(),535*k,1199*k,226*k);
      drawFit(c,p,pass.getText().toString(),535*k,1265*k,226*k);
      p.setColor(Color.BLACK);c.drawRoundRect(497*k,1347*k,1027*k,1427*k,22*k,22*k,p);
      p.setColor(Color.YELLOW);p.setTextSize(57*k);
      drawFit(c,p,phone.getText().toString(),520*k,1409*k,480*k);
    }else{
      p.setColor(Color.rgb(225,0,38));c.drawRect(0,0,w,500*k,p);
      p.setColor(Color.WHITE);p.setTextSize(115*k);c.drawText("KT Wi-Fi",100*k,260*k,p);
      p.setColor(Color.BLACK);p.setTextSize(54*k);c.drawText("무선랜명: "+ssid.getText(),100*k,1200*k,p);
      c.drawText("암호: "+pass.getText(),100*k,1290*k,p);
      Bitmap qrImage=qr((int)(560*k));c.drawBitmap(qrImage,260*k,500*k,p);
      c.drawText("상담: "+phone.getText(),100*k,1430*k,p);
    }
    preview.setImageBitmap(poster);toast("미리보기 생성 완료");
  }catch(Exception e){toast("생성 오류: "+e.getMessage());}}
  void drawFit(Canvas c,Paint p,String text,float x,float y,float maxWidth){float initial=p.getTextSize();while(p.measureText(text)>maxWidth && p.getTextSize()>12){p.setTextSize(p.getTextSize()*.92f);}c.drawText(text,x,y,p);p.setTextSize(initial);}
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
