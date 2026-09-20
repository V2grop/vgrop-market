package com.v2grop.lbankpulse;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Lightweight native backdrop; no downloads or animation cost. */
final class PulseBackdrop extends Drawable {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 public void draw(Canvas c){
  Rect b=getBounds();float w=b.width(),h=b.height();
  p.setShader(new LinearGradient(0,0,w,h,new int[]{0xff181329,0xff0b1529,0xff091e29},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
  p.setShader(new RadialGradient(w*.95f,h*.12f,Math.max(1,w*.9f),new int[]{0x554d31a5,0x004d31a5},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
  p.setShader(new RadialGradient(0,h*.65f,Math.max(1,w*.8f),new int[]{0x25209eae,0x00209eae},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
  p.setShader(new RadialGradient(w*.25f,h*.38f,Math.max(1,w*.7f),new int[]{0x46552b91,0x00552b91},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
  p.setShader(new RadialGradient(w*.75f,h*.75f,Math.max(1,w*.6f),new int[]{0x3531569c,0x0031569c},null,Shader.TileMode.CLAMP));c.drawRect(b,p);
  p.setShader(null);
  java.util.Random stars=new java.util.Random(90210);
  for(int i=0;i<160;i++){
   float x=stars.nextFloat()*w,y=stars.nextFloat()*h,r=(0.3f+stars.nextFloat()*0.65f)*Math.max(1,w/400);
   p.setColor(Color.argb(40+stars.nextInt(100),195,208,255));c.drawCircle(x,y,r,p);
   if(i%29==0){p.setStrokeWidth(Math.max(0.6f,w/650));c.drawLine(x-r*3,y,x+r*3,y,p);c.drawLine(x,y-r*3,x,y+r*3,p);}
  }
 }
 public void setAlpha(int alpha){}
 public void setColorFilter(ColorFilter filter){}
 public int getOpacity(){return PixelFormat.OPAQUE;}
}
