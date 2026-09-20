package com.v2grop.lbankpulse;
import org.json.*;
/** Gold-specific macro interpretation, independent of uncalibrated technical weights. */
public final class GoldResearch {
 static String explain(JSONObject macro)throws Exception{
  StringBuilder b=new StringBuilder("ارزیابی اختصاصی عوامل طلا\n");
  JSONArray items=macro.getJSONArray("items");int used=0;
  for(int i=0;i<items.length();i++){
   JSONObject d=items.getJSONObject(i);String id=d.optString("id");
   if(!id.equals("DGS10")&&!id.equals("DGS2")&&!id.equals("DTWEXBGS"))continue;
   b.append("\n").append(d.optString("label")).append(": ");
   if(!d.optString("status").equals("ok")||d.optBoolean("stale")||d.isNull("previous")||!d.has("previous")){
    b.append("داده تازه و قابل مقایسه موجود نیست.\n");continue;
   }
   double change=d.getDouble("value")-d.getDouble("previous");used++;
   b.append(d.getDouble("value")).append(" • ").append(d.optString("observation_date"));
   b.append("\nدر مقایسه با ").append(d.optString("previous_date")).append(": ");
   b.append(Math.abs(change)<0.000001?"بدون تغییر؛ جهت مشخصی نمی‌دهد.":change>0?"افزایش؛ در صورت ثبات سایر عوامل، فشار بالقوه بر طلا.":"کاهش؛ در صورت ثبات سایر عوامل، حمایت بالقوه از طلا.").append("\n");
  }
  if(used==0)b.append("\nعامل تازه‌ای برای ارزیابی کلان در دسترس نیست.\n");
  return b.append("\nاین تفسیر کیفی است و در درصدهای تکنیکال دخالت ندارد. بازده اسمی جایگزین نرخ واقعی نیست؛ شاخص دلار این بخش DXY نیست. داده انتظارات تورمی، جریان صندوق‌های طلا، خرید بانک‌های مرکزی و ریسک سیاسی در این ارزیابی خودکار موجود نیست.").toString();
 }
}
