package com.v2grop.lbankpulse;
import android.content.Context;import android.view.View;import android.graphics.*;import org.json.JSONArray;
/** Accessible close-price chart; no interpolated/live candles. */
final class PriceChart extends View{
 private final double[] values;private final Paint paint=new Paint(3);
 PriceChart(Context c,JSONArray rows){super(c);values=new double[rows.length()];for(int i=0;i<values.length;i++)values[i]=rows.optDouble(i);setContentDescription("نمودار قیمت بسته‌شدن کندل‌های ساعتی؛ "+values.length+" نقطه");}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(values.length<2)return;double lo=Double.MAX_VALUE,hi=-Double.MAX_VALUE;for(double v:values){if(!Double.isFinite(v))return;lo=Math.min(lo,v);hi=Math.max(hi,v);}double range=Math.max(hi-lo,Math.max(hi*.001,.000000001));float pad=16*getResources().getDisplayMetrics().density,w=getWidth()-2*pad,h=getHeight()-2*pad;if(w<=0||h<=0)return;
  paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(0xff34415c);for(int j=0;j<4;j++)c.drawLine(pad,pad+h*j/3,pad+w,pad+h*j/3,paint);
  Path path=new Path();for(int i=0;i<values.length;i++){float x=pad+w*i/(values.length-1),y=pad+h-(float)((values[i]-lo)/range)*h;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
  paint.setColor(0xff38e5cb);paint.setStrokeWidth(2.5f*getResources().getDisplayMetrics().density);c.drawPath(path,paint);
  paint.setStyle(Paint.Style.FILL);paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);paint.setColor(0xffc5cde0);c.drawText(String.format(java.util.Locale.US,"%.6g",hi),pad,pad,paint);c.drawText(String.format(java.util.Locale.US,"%.6g",lo),pad,getHeight()-3,paint);
 }
}
