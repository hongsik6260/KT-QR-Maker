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
  Spinner paper, security;
  CheckBox hiddenNetwork;
  ImageView preview, importedPhoto;
  TextView photoStatus;
  Bitmap importedBitmap;
  CropView inlineCrop;
  LinearLayout cropArea;
  Spinner cropMode;
  LinearLayout ocrCandidates;
  Bitmap rotatedPhoto;
  ScrollView mainScroll;
  Bitmap poster;
  static final int PICK=1, CAMERA=2, BACKGROUND=4;
  Bitmap background;
  Uri photoUri;
  int[] widths={2480,1748,1240,874};
  int[] heights={3508,2480,1748,1240};
  String[] labels={"A4","A5","A6","A7"};
  int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  android.graphics.drawable.GradientDrawable shape(int color,int radius){
    android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();
    d.setColor(color);d.setCornerRadius(dp(radius));return d;
  }
  TextView text(String value,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
    if(bold)t.setTypeface(null,Typeface.BOLD);return t;
  }
  LinearLayout card(LinearLayout parent,String number,String heading){
    LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(dp(20),dp(22),dp(20),dp(22));
    card.setBackground(shape(Color.WHITE,22));
    LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,0,0,dp(20));parent.addView(card,cp);
    TextView h=text(number+"   "+heading,19,0xff223d32,true);
    card.addView(h);return card;
  }
  void smallLabel(LinearLayout parent,String label){
    TextView t=text(label,14,0xff283e35,true);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(18),0,dp(8));parent.addView(t,lp);
  }
  EditText input(LinearLayout parent,String placeholder,String initial){
    EditText e=new EditText(this);e.setText(initial);e.setHint(placeholder);e.setSingleLine(true);
    e.setTextSize(16);e.setPadding(dp(16),0,dp(12),0);
    android.graphics.drawable.GradientDrawable d=shape(0xfffbfbf9,12);d.setStroke(dp(1),0xffdeded8);e.setBackground(d);
    parent.addView(e,new LinearLayout.LayoutParams(-1,dp(58)));return e;
  }
  void action(LinearLayout parent,String title,boolean primary,Runnable fn){
    Button b=new Button(this);b.setAllCaps(false);b.setText(title);b.setTextSize(15);
    b.setTextColor(primary?Color.WHITE:0xff263d33);b.setBackground(shape(primary?0xff176c53:Color.WHITE,12));
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),1);lp.setMargins(dp(4),0,dp(4),0);
    parent.addView(b,lp);b.setOnClickListener(v->fn.run());
  }
  @Override public void onCreate(Bundle b){super.onCreate(b);
    getWindow().setStatusBarColor(0xfff4f6f3);getWindow().setNavigationBarColor(0xfff4f6f3);
    ScrollView scroll=new ScrollView(this);mainScroll=scroll;scroll.setFillViewport(true);scroll.setBackgroundColor(0xfff4f6f3);
    LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(22),dp(20),dp(28));
    scroll.addView(root);setContentView(scroll);
    TextView brand=text("KT WiFi QR생성",22,0xff1c4435,true);root.addView(brand);
    TextView affiliation=text("KT서비스남부  |  충청본부 · 대전지점 · 대전지사\\nCS4팀(금산)  김홍식",12,0xff667d70,false);
    affiliation.setText(affiliation.getText().toString().replace("\\n","\n"));
    LinearLayout.LayoutParams affiliationLp=new LinearLayout.LayoutParams(-1,-2);affiliationLp.setMargins(0,dp(8),0,0);root.addView(affiliation,affiliationLp);
    TextView sub=text("현장에서 간편하게 만드는 연결 안내    ·    v1.4 로고 제외",12,0xff7b8981,false);root.addView(sub);
    TextView kicker=text("W I - F I   C A R D   M A K E R",12,0xff197359,true);
    LinearLayout.LayoutParams kl=new LinearLayout.LayoutParams(-1,-2);kl.setMargins(0,dp(52),0,dp(18));root.addView(kicker,kl);
    TextView hero=text("사진 한 장으로,\\n와이파이 연결을 간편하게.",29,0xff1e382e,true);
    hero.setText(hero.getText().toString().replace("\\n","\n"));root.addView(hero);
    TextView intro=text("공유기 라벨을 촬영하고 이름과 암호를 확인하세요.\\nQR코드를 스캔하면 와이파이에 연결할 수 있습니다.",14,0xff79877f,false);
    intro.setText(intro.getText().toString().replace("\\n","\n"));
    LinearLayout.LayoutParams il=new LinearLayout.LayoutParams(-1,-2);il.setMargins(0,dp(20),0,dp(40));root.addView(intro,il);
    try(InputStream in=getAssets().open("kt_background.png")){background=BitmapFactory.decodeStream(in);}catch(Exception ignored){}
    LinearLayout photo=card(root,"①","공유기 사진 불러오기");
    LinearLayout zone=new LinearLayout(this);zone.setOrientation(1);zone.setGravity(Gravity.CENTER);zone.setPadding(dp(10),dp(28),dp(10),dp(28));
    android.graphics.drawable.GradientDrawable zd=shape(0xfff8faf8,18);zd.setStroke(dp(2),0xffcbd9d0,dp(4),dp(3));zone.setBackground(zd);
    LinearLayout.LayoutParams zl=new LinearLayout.LayoutParams(-1,-2);zl.setMargins(0,dp(24),0,dp(16));photo.addView(zone,zl);
    TextView glyph=text("▧",38,0xff176c53,true);glyph.setGravity(Gravity.CENTER);zone.addView(glyph);
    TextView guide=text("무선랜명과 암호가 보이도록 촬영하세요",15,0xff253f34,true);guide.setGravity(Gravity.CENTER);zone.addView(guide);
    TextView privacy=text("사진은 서버로 전송하지 않고 기기에서 읽습니다.",12,0xff8a958e,false);privacy.setGravity(Gravity.CENTER);
    LinearLayout.LayoutParams pl=new LinearLayout.LayoutParams(-1,-2);pl.setMargins(0,dp(12),0,dp(20));zone.addView(privacy,pl);
    LinearLayout buttons=new LinearLayout(this);buttons.setGravity(Gravity.CENTER);zone.addView(buttons);
    action(buttons,"사진 촬영",true,()->{try{Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);startActivityForResult(i,CAMERA);}catch(Exception e){toast("카메라를 열 수 없습니다");}});
    action(buttons,"사진 선택",false,()->{Intent i=new Intent(Intent.ACTION_GET_CONTENT);i.setType("image/*");startActivityForResult(i,PICK);});
    photoStatus=text("사진 없이 직접 입력해도 만들 수 있어요.",13,0xff667d70,false);photo.addView(photoStatus);
    TextView selectAgain=text("사진 위 초록색 영역을 드래그해 이름과 암호 부분만 선택하세요.",13,0xff667d70,false);photo.addView(selectAgain);
    cropArea=new LinearLayout(this);cropArea.setOrientation(1);cropArea.setVisibility(View.GONE);photo.addView(cropArea);
    inlineCrop=new CropView(this,null);
    LinearLayout.LayoutParams imageLp=new LinearLayout.LayoutParams(-1,dp(480));imageLp.setMargins(0,dp(14),0,dp(10));cropArea.addView(inlineCrop,imageLp);
    LinearLayout cropButtons=new LinearLayout(this);cropArea.addView(cropButtons);
    action(cropButtons,"사진 회전",false,()->{if(rotatedPhoto!=null){Matrix m=new Matrix();m.postRotate(90);rotatedPhoto=Bitmap.createBitmap(rotatedPhoto,0,0,rotatedPhoto.getWidth(),rotatedPhoto.getHeight(),m,true);inlineCrop.setBitmap(rotatedPhoto);}});
    action(cropButtons,"전체 선택",false,()->inlineCrop.selectAll());
    smallLabel(cropArea,"읽을 내용");
    cropMode=new Spinner(this);cropMode.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"무선랜명 + 암호","무선랜명만","암호만"}));
    cropArea.addView(cropMode,new LinearLayout.LayoutParams(-1,dp(54)));
    addButton(cropArea,"선택 영역 글자 읽기",()->{if(rotatedPhoto!=null)readSelection(inlineCrop.selected(),cropMode.getSelectedItemPosition());});
    ocrCandidates=new LinearLayout(this);ocrCandidates.setOrientation(1);cropArea.addView(ocrCandidates);
    LinearLayout details=card(root,"②","이름과 암호 확인");
    details.addView(text("사진 인식 후 대소문자와 숫자 0·영문 O 등을 확인하세요.",13,0xff829087,false));
    smallLabel(details,"무선랜명 (SSID)");ssid=input(details,"예: KT_GiGA_5G_1234","");
    smallLabel(details,"암호");pass=input(details,"와이파이 암호 입력","");
    smallLabel(details,"보안 방식");
    LinearLayout options=new LinearLayout(this);options.setGravity(Gravity.CENTER_VERTICAL);details.addView(options);
    security=new Spinner(this);security.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"WPA / WPA2 / WPA3","개방형 (암호 없음)","WEP"}));
    options.addView(security,new LinearLayout.LayoutParams(0,dp(52),1));
    hiddenNetwork=new CheckBox(this);hiddenNetwork.setText("숨김 네트워크");hiddenNetwork.setTextSize(12);options.addView(hiddenNetwork);
    LinearLayout output=card(root,"③","KT 홍보물 만들기");
    smallLabel(output,"가입 및 상담 문의");phone=input(output,"상담 전화번호","010-7224-7624");
    smallLabel(output,"용지 크기");paper=new Spinner(this);paper.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));output.addView(paper);
    addButton(output,"🎨 QR 홍보물 제작 / 미리보기",()->render());
    addButton(output,"⬇ PNG 고해상도 저장",()->save(false));
    addButton(output,"⬇ PDF 저장",()->save(true));
    preview=new ImageView(this);preview.setAdjustViewBounds(true);output.addView(preview);
    output.addView(text("QR코드와 추출한 정보가 KT 광고 원본 배경에 자동으로 들어갑니다.",12,0xff718278,false));
  }
  EditText field(LinearLayout root,String hint,String val){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setText(val);root.addView(e);return e;}
  void addButton(LinearLayout root,String label,Runnable r){
    Button b=new Button(this);b.setText(label);b.setTextColor(Color.WHITE);b.setTextSize(17);b.setAllCaps(false);
    int[] colors=label.contains("문자")?new int[]{0xff299bff,0xff0873ef}:label.contains("제작")?new int[]{0xfffd293d,0xffe00019}:label.contains("PDF")?new int[]{0xff197df5,0xff0661d8}:new int[]{0xff9b51f8,0xff6328e5};
    android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,colors);
    bg.setCornerRadius(22);b.setBackground(bg);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,(int)(60*getResources().getDisplayMetrics().density));lp.setMargins(0,9,0,9);
    root.addView(b,lp);b.setOnClickListener(v->r.run());
  }
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
      importedBitmap=bmp;
      if(ocrCandidates!=null)ocrCandidates.removeAllViews();
      rotatedPhoto=bmp;
      inlineCrop.setBitmap(bmp);
      cropArea.setVisibility(View.VISIBLE);
      photoStatus.setText("사진 불러오기 완료 · 아래 사진에서 초록색 영역을 직접 조절하세요.");
      cropArea.post(()->mainScroll.smoothScrollTo(0,Math.max(0,cropArea.getTop()+((android.view.View)cropArea.getParent()).getTop()-dp(90))));

    }catch(Exception e){toast("사진 오류: "+e.getMessage());}
  }
  void readSelection(Bitmap bmp,int mode){
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener(t->{
      ArrayList<String> candidates=new ArrayList<>();
      for(com.google.mlkit.vision.text.Text.TextBlock block:t.getTextBlocks())
        for(com.google.mlkit.vision.text.Text.Line line:block.getLines()){String v=line.getText().trim();if(!v.isEmpty())candidates.add(v);}
      if(candidates.isEmpty()){toast("글자를 찾지 못했습니다. 영역을 다시 선택하세요.");return;}
      String[] choices=candidates.toArray(new String[0]);
      // Show recognized alternatives below the photograph, never in a popup.
      ocrCandidates.removeAllViews();
      TextView header=text("인식 결과 후보 · 눌러서 적용하세요",15,0xff176c53,true);
      ocrCandidates.addView(header);
      if(mode==0){
        for(int i=0;i<Math.min(choices.length,8);i++){
          final int index=i;
          String first=choices[index],second=index+1<choices.length?choices[index+1]:"";
          Button candidate=new Button(this);
          candidate.setAllCaps(false);
          candidate.setText("무선랜명: "+first+"\n암호: "+second);
          candidate.setTextColor(0xff193d31);
          candidate.setBackground(shape(0xffe9f3ed,12));
          ocrCandidates.addView(candidate,new LinearLayout.LayoutParams(-1,dp(76)));
          candidate.setOnClickListener(v->{ssid.setText(first);pass.setText(second);toast("무선랜명과 암호 적용 완료");});
        }
      }else{
        for(String choice:choices){
          Button candidate=new Button(this);candidate.setAllCaps(false);
          candidate.setText(choice);candidate.setTextColor(0xff193d31);
          candidate.setBackground(shape(0xffe9f3ed,12));
          LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(4),0,dp(4));ocrCandidates.addView(candidate,lp);
          candidate.setOnClickListener(v->{if(mode==1)ssid.setText(choice);else pass.setText(choice);toast("선택한 글자를 적용했습니다");});
        }
      }
    }).addOnFailureListener(e->toast("문자 인식 실패: "+e.getMessage()));
  }
  class CropView extends View{
    Bitmap bitmap;Paint paint=new Paint(3);android.graphics.RectF selection=new android.graphics.RectF(.1f,.25f,.9f,.65f);float px,py,startX,startY;boolean resize,drawing;
    CropView(Context c,Bitmap b){super(c);bitmap=b;}
    void setBitmap(Bitmap b){bitmap=b;selection.set(.1f,.25f,.9f,.65f);invalidate();}
    void selectAll(){selection.set(0,0,1,1);invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.DKGRAY);if(bitmap==null)return;
      android.graphics.RectF dest=fit();c.drawBitmap(bitmap,null,dest,paint);
      paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setColor(0xff16d95b);
      c.drawRect(dest.left+selection.left*dest.width(),dest.top+selection.top*dest.height(),dest.left+selection.right*dest.width(),dest.top+selection.bottom*dest.height(),paint);
      paint.setStyle(Paint.Style.FILL);paint.setColor(0xff16d95b);
      float rx=dest.left+selection.right*dest.width(),by=dest.top+selection.bottom*dest.height();
      c.drawCircle(rx,by,dp(9),paint);
      paint.setColor(Color.WHITE);
    }
    android.graphics.RectF fit(){float scale=Math.min(getWidth()/(float)bitmap.getWidth(),getHeight()/(float)bitmap.getHeight());float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;return new android.graphics.RectF((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);}
    @Override public boolean onTouchEvent(android.view.MotionEvent e){
      if(bitmap==null)return false;
      android.graphics.RectF d=fit();float x=(e.getX()-d.left)/d.width(),y=(e.getY()-d.top)/d.height();
      if(e.getAction()==0){getParent().requestDisallowInterceptTouchEvent(true);px=x;py=y;startX=x;startY=y;resize=Math.abs(x-selection.right)<.10f&&Math.abs(y-selection.bottom)<.10f;drawing=!resize&&!selection.contains(x,y);return true;}
      if(e.getAction()==1||e.getAction()==2){float dx=x-px,dy=y-py;
        if(drawing){selection.set(Math.max(0,Math.min(startX,x)),Math.max(0,Math.min(startY,y)),Math.min(1,Math.max(startX,x)),Math.min(1,Math.max(startY,y)));if(selection.width()<.02f)selection.right=Math.min(1,selection.left+.02f);if(selection.height()<.02f)selection.bottom=Math.min(1,selection.top+.02f);}\n        else if(resize){selection.right=Math.max(selection.left+.08f,Math.min(1,selection.right+dx));selection.bottom=Math.max(selection.top+.05f,Math.min(1,selection.bottom+dy));}
        else {float l=Math.max(0,Math.min(1-selection.width(),selection.left+dx)),t=Math.max(0,Math.min(1-selection.height(),selection.top+dy));selection.offsetTo(l,t);}
        px=x;py=y;invalidate();return true;}return true;
    }
    Bitmap selected(){int x=Math.max(0,(int)(selection.left*bitmap.getWidth())),y=Math.max(0,(int)(selection.top*bitmap.getHeight()));int w=Math.max(1,Math.min(bitmap.getWidth()-x,(int)(selection.width()*bitmap.getWidth()))),h=Math.max(1,Math.min(bitmap.getHeight()-y,(int)(selection.height()*bitmap.getHeight())));return Bitmap.createBitmap(bitmap,x,y,w,h);}
  }
  String value(String line,int i,String[] lines){int a=line.indexOf(':');if(a<0)a=line.indexOf('：');String v=a>=0?line.substring(a+1).trim():"";if(v.isEmpty()&&i+1<lines.length)v=lines[i+1].trim();return v;}
  String wifi(){String type=security.getSelectedItemPosition()==1?"nopass":security.getSelectedItemPosition()==2?"WEP":"WPA";return "WIFI:T:"+type+";S:"+esc(ssid.getText().toString())+";P:"+esc(pass.getText().toString())+";H:"+(hiddenNetwork.isChecked()?"true":"false")+";;";}
  String esc(String v){String slash=Character.toString((char)92);return v.replace(slash,slash+slash).replace(";",slash+";").replace(":",slash+":").replace(",",slash+",");}
  Bitmap qr(int size)throws Exception{BitMatrix bits=new MultiFormatWriter().encode(wifi(),BarcodeFormat.QR_CODE,size,size,Map.of(EncodeHintType.MARGIN,0,EncodeHintType.ERROR_CORRECTION,com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M));Bitmap b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);for(int y=0;y<size;y++)for(int x=0;x<size;x++)b.setPixel(x,y,bits.get(x,y)?Color.BLACK:Color.WHITE);return b;}
  void render(){
    poster=null;
    if(background==null){toast("KT 광고 원본 이미지가 APK에 없습니다. 원본 포함 빌드가 필요합니다.");return;}
    if(ssid.getText().toString().trim().isEmpty()){toast("무선랜명을 입력하세요");return;}
    try{
      int idx=paper.getSelectedItemPosition(),w=widths[idx],h=heights[idx];
      poster=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
      Canvas c=new Canvas(poster);Paint p=new Paint(3);
      c.drawBitmap(background,null,new android.graphics.Rect(0,0,w,h),p);
      c.save();c.scale(w/1085f,h/1536f);
      // Fill the inside of the original red QR frame without covering its border.
      p.setColor(Color.WHITE);c.drawRect(389,912,696,1205,p);
      Bitmap qrImage=qr(300);
      c.drawBitmap(qrImage,null,new android.graphics.Rect(393,915,692,1202),p);
      // Preserve red labels and outlines; edit only the white value fields.
      p.setColor(Color.WHITE);
      c.drawRect(530,1230,778,1272,p);
      // Keep the password field white without painting over its red border.
      c.drawRect(530,1299,758,1359,p);
      p.setColor(0xff111111);
      p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
      p.setTextAlign(Paint.Align.CENTER);
      p.setTextSize(34);
      drawFit(c,p,ssid.getText().toString(),653,1263,234);
      // Center password text vertically inside the white row, clear of its red bottom border.
      drawFit(c,p,pass.getText().toString(),653,1341,224);
      // Keep the original yellow phone icon; repaint only the number area.
      p.setColor(0xff080808);c.drawRect(513,1420,1030,1502,p);
      p.setColor(0xffffff1c);p.setTextSize(63);
      drawFit(c,p,phone.getText().toString(),771,1480,493);
      p.setTextAlign(Paint.Align.LEFT);
      c.restore();preview.setImageBitmap(poster);toast("KT 광고 이미지 생성 완료");
    }catch(Exception e){toast("생성 오류: "+e.getMessage());}
  }
  void drawFit(Canvas c,Paint p,String text,float x,float y,float maxWidth){float initial=p.getTextSize();while(p.measureText(text)>maxWidth && p.getTextSize()>12){p.setTextSize(p.getTextSize()*.92f);}c.drawText(text,x,y,p);p.setTextSize(initial);}
  void save(boolean pdf){
    try{
      render();if(poster==null)return;
      String name="KT_QR_"+System.currentTimeMillis()+(pdf?".pdf":".png");
      if(android.os.Build.VERSION.SDK_INT>=29){
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,name);
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,pdf?"application/pdf":"image/png");
        values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"DCIM/Camera/");
        values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,1);
        Uri collection=pdf?android.provider.MediaStore.Files.getContentUri("external_primary"):
          android.provider.MediaStore.Images.Media.getContentUri("external_primary");
        Uri uri=null;
        try{
          uri=getContentResolver().insert(collection,values);
          if(uri==null)throw new Exception("파일을 생성할 수 없습니다");
          writeOutput(uri,pdf);
          android.content.ContentValues done=new android.content.ContentValues();
          done.put(android.provider.MediaStore.MediaColumns.IS_PENDING,0);
          getContentResolver().update(uri,done,null,null);
          toast("저장 완료: 내 파일 > 내장 저장공간 > DCIM > Camera");
          return;
        }catch(Exception e){
          if(uri!=null)getContentResolver().delete(uri,null,null);
          // Some Android versions reject PDF files inside DCIM.
          if(!pdf)throw e;
        }
      }
      Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      intent.setType(pdf?"application/pdf":"image/png");
      intent.putExtra(Intent.EXTRA_TITLE,name);
      pendingPdf=pdf;startActivityForResult(intent,3);
      if(pdf)toast("이 기기에서는 PDF의 카메라 폴더 자동 저장이 제한되어 위치 선택이 필요합니다");
    }catch(Exception e){toast("저장 실패: "+e.getMessage());}
  }
  boolean pendingPdf;
  void writeOutput(Uri uri){try{writeOutput(uri,pendingPdf);toast("저장 완료");}catch(Exception e){toast("저장 실패: "+e.getMessage());}}
  void writeOutput(Uri uri,boolean pdf)throws Exception{
    try(OutputStream out=getContentResolver().openOutputStream(uri)){
      if(out==null)throw new Exception("저장 위치를 열 수 없습니다");
      if(pdf){
        PdfDocument doc=new PdfDocument();
        try{
          int i=paper.getSelectedItemPosition();int[] pw={595,420,298,210},ph={842,595,420,298};
          PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(pw[i],ph[i],1).create();
          PdfDocument.Page page=doc.startPage(info);
          page.getCanvas().drawBitmap(poster,null,new android.graphics.Rect(0,0,pw[i],ph[i]),new Paint(3));
          doc.finishPage(page);doc.writeTo(out);
        }finally{doc.close();}
      }else if(!poster.compress(Bitmap.CompressFormat.PNG,100,out)){
        throw new Exception("PNG 파일 기록 실패");
      }
    }
  }

}
