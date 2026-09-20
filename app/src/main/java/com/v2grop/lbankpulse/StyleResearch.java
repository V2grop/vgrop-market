package com.v2grop.lbankpulse;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
final class StyleResearch {
 interface Net{String read(JSONObject request)throws Exception;}
 final Net net;
 StyleResearch(){this(b->LocalResearch.http("https://api.hyperliquid.xyz/info",b));}
 StyleResearch(Net n){net=n;}
 static JSONObject obj(Object... kv)throws Exception{return LocalResearch.obj(kv);}
 JSONObject book(String coin)throws Exception{
  JSONObject d=new JSONObject(net.read(obj("type","l2Book","coin",coin)));
  if(!d.getString("coin").equals(coin)||Math.abs(System.currentTimeMillis()-d.getLong("time"))>120000)throw new Exception("دفتر سفارش تازه نیست.");
  JSONArray levels=d.getJSONArray("levels");if(levels.length()!=2)throw new Exception("دفتر نامعتبر");
  double buy=0,sell=0;
  for(int side=0;side<2;side++){JSONArray list=levels.getJSONArray(side);if(list.length()<5)throw new Exception("عمق ناکافی");for(int i=0;i<Math.min(20,list.length());i++){JSONObject r=list.getJSONObject(i);double amount=r.getDouble("px")*r.getDouble("sz");if(!Double.isFinite(amount)||amount<=0)throw new Exception("حجم نامعتبر");if(side==0)buy+=amount;else sell+=amount;}}
  return StyleEngine.weights(.35*(buy-sell)/(buy+sell),.55,"عدم‌توازن ارزش حداکثر ۲۰ سطح دفتر سفارش Hyperliquid؛ تصویر لحظه‌ای، سفارش قابل لغو است. جریان معاملات اجراشده یا پیش‌بینی مستقل روزانه/هفتگی نیست.");
 }
 JSONArray fills(String[] addresses)throws Exception{
  if(addresses.length==0)throw new Exception("آدرس حساب وارد نشده است.");
  JSONArray all=new JSONArray();Set<String> seen=new HashSet<>();long start=System.currentTimeMillis()-7L*86400000;
  for(String address:addresses){
   JSONArray rows=new JSONArray(net.read(obj("type","userFillsByTime","user",address,"startTime",start,"aggregateByTime",true)));
   if(rows.length()>=2000)throw new Exception("پاسخ معاملات به سقف رسید؛ پوشش ناقص از ترکیب حذف شد.");
   for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String id=address+":"+r.getString("tid");if(seen.add(id))all.put(r);}
  }return all;
 }
 static JSONObject whale(JSONArray fills,String coin,int hours)throws Exception{
  double buy=0,sell=0;int count=0;long now=System.currentTimeMillis(),cut=now-hours*3600000L;
  for(int i=0;i<fills.length();i++){JSONObject r=fills.getJSONObject(i);if(!r.optString("coin").equals(coin)||r.getLong("time")<cut||r.getLong("time")>now)continue;
   double v=r.getDouble("px")*r.getDouble("sz");if(!Double.isFinite(v)||v<100000)continue;
   String side=r.getString("side");if(side.equals("B"))buy+=v;else if(side.equals("A"))sell+=v;else continue;count++;}
  if(count==0)return null;
  return StyleEngine.weights(.5*(buy-sell)/(buy+sell),.5,count+" اجرای معامله حداقل ۱۰۰هزار دلار در آدرس‌های منتخب؛ خالص خرید/فروش، شامل بازکردن و بستن موقعیت. پوشش همه نهنگ‌ها یا تحلیل کامل آنچین نیست.");
 }
 static String[] addresses(String raw){
  LinkedHashSet<String> out=new LinkedHashSet<>();for(String s:raw.trim().split("[\\s,;]+"))if(!s.isEmpty()){if(!s.matches("0x[0-9a-fA-F]{40}"))throw new IllegalArgumentException("آدرس باید 0x و ۴۰ رقم هگز باشد.");out.add(s.toLowerCase(Locale.US));}
  if(out.size()>3)throw new IllegalArgumentException("حداکثر سه آدرس برای این نسخه.");return out.toArray(new String[0]);
 }
 void apply(JSONObject report,boolean[] selected,boolean combined,String rawAddresses)throws Exception{
  apply(report,selected,combined,rawAddresses,false);
 }
 void apply(JSONObject report,boolean[] selected,boolean combined,String rawAddresses,boolean dedicated)throws Exception{
  JSONObject daily=null;JSONObject tech=null;JSONArray sources=report.getJSONArray("sources");for(int i=0;i<sources.length();i++){JSONObject e=sources.getJSONObject(i);if(e.optString("name").equals("technical")&&e.optString("status").equals("ok"))tech=e.getJSONObject("data");}
  for(int i=0;i<sources.length();i++){JSONObject e=sources.getJSONObject(i);if(e.optString("name").equals("daily")&&e.optString("status").equals("ok"))daily=e.getJSONObject("data");}
  JSONArray rows=tech==null?new JSONArray():tech.optJSONArray("ohlc");if(rows==null)rows=new JSONArray();
  String coin=report.getString("symbol");JSONObject book=null;JSONArray fills=null;String flowError="",whaleError="";
  ExecutorService pool=Executors.newFixedThreadPool(2);
  try{
   Future<JSONObject> bf=selected[3]&&report.optString("kind").equals("perp")?pool.submit(()->book(coin)):null;
   Future<JSONArray> wf=selected[4]&&report.optString("kind").equals("perp")?pool.submit(()->fills(addresses(rawAddresses))):null;
   if(selected[3])try{if(bf==null)throw new Exception("فقط بازار دائمی با نام دقیق Hyperliquid.");book=bf.get(22,TimeUnit.SECONDS);}catch(Exception e){flowError="داده تازه دفتر سفارش دریافت نشد یا بازار پشتیبانی نمی‌شود.";}
   if(selected[4])try{if(wf==null)throw new Exception();fills=wf.get(22,TimeUnit.SECONDS);}catch(Exception e){whaleError="آدرس یا داده معاملات کافی نیست؛ پوشش ناقص وارد درصد نمی‌شود.";}
  }finally{pool.shutdownNow();}
  JSONArray output=new JSONArray();StringBuilder audit=new StringBuilder();
  for(int hours:new int[]{4,24,168,720,2160}){
   boolean longer=hours>=720;JSONArray input=longer?(daily==null?new JSONArray():daily.optJSONArray("ohlc")):rows;if(input==null)input=new JSONArray();
   List<JSONObject> valid=new ArrayList<>();
   if(longer&&dedicated){JSONObject w=StyleEngine.longTrend(input,hours);if(w!=null){w.put("style","سبک بلندمدت • دنبال‌کردن روند ۵۰/۲۰۰");valid.add(w);audit.append("سبک بلندمدت • "+hours+" ساعت: "+w.toString()+"\n");}}
   for(int i=0;i<selected.length;i++){if(longer&&dedicated)break;if(!selected[i])continue;JSONObject w=null;String reason="";
    if(i==5){JSONArray btc=null;String name=longer?"btc_daily":"btc_hourly";for(int z=0;z<sources.length();z++){JSONObject src=sources.getJSONObject(z);JSONObject d=src.optJSONObject("data");if(src.optString("name").equals(name)&&d!=null&&d.optString("kind").equals((longer?daily:tech)==null?"":(longer?daily:tech).optString("kind")))btc=d.optJSONArray("ohlc");}w=AdaptiveEngine.evaluate(input,hours,coin,btc);}
    if(i==0&&!longer&&tech!=null){JSONArray hs=tech.getJSONArray("horizons");for(int k=0;k<hs.length();k++)if(hs.getJSONObject(k).getInt("hours")==hours)w=hs.getJSONObject(k).optJSONObject("scenario_weights");}
    if(i==0&&longer)w=StyleEngine.longTrend(input,hours);
    if(i==1&&(!longer||input.length()>=(hours==720?180:365))&&(hours!=168||input.length()>=336))w=StyleEngine.smc(input,longer?(hours==720?24:168):hours);
    if(i==2&&(hours!=168||input.length()>=336))w=StyleEngine.donchian(input,hours);
    if(i==3){w=hours==4?book:null;reason=hours!=4?"تصویر لحظه‌ای برای روزانه/هفتگی استفاده نمی‌شود.":flowError;}
    if(i==4){w=fills==null||longer?null:whale(fills,coin,hours);reason=longer?"معاملات هفت روز برای افق ماهانه و سه‌ماهه استفاده نمی‌شود.":whaleError.isEmpty()?"معامله با آستانه تعیین‌شده در این بازه نبود.":whaleError;}
    if(w==null){audit.append(hours+" ساعت • "+StyleEngine.NAMES[i]+": ").append(reason.isEmpty()?(longer?"کندل روزانه کافی لازم است: ماهانه ۱۸۰، سه‌ماهه ۳۶۵؛ منبع باید تازه و پیوسته باشد.":"داده یا الگوی معتبر کافی نیست."):reason).append("\n");continue;}
    w=new JSONObject(w.toString()).put("style",StyleEngine.NAMES[i]);valid.add(w);
    audit.append(hours+" ساعت • "+StyleEngine.NAMES[i]+": "+w.optInt("up")+"/"+w.optInt("neutral")+"/"+w.optInt("down")+" (صعود/خنثی/نزول)\n").append(w.optString("explanation")).append("\n");
   }
   JSONObject basis=longer?daily:tech;
   JSONObject h=obj("hours",hours,"style_label",longer&&dedicated?"سبک بلندمدت • دنبال‌کردن روند ۵۰/۲۰۰":"سبک‌های انتخابی","basis",basis==null?"":basis.optString("source")+" / "+basis.optString("quote","USDT")+" • "+basis.optInt("closed_candles")+(longer?" کندل روزانه":" کندل ساعتی")+" • "+basis.optString("kind")+" • "+basis.optString("basis_note"));
   if(valid.isEmpty())h.put("status",hours+" ساعت: سبک انتخابی داده کافی ندارد."+(longer?" ماهانه حداقل ۲۰۰ و سه‌ماهه ۳۶۵ کندل روزانه برای سبک بلندمدت؛ جریان سفارش و معاملات هفت‌روزه برای این افق استفاده نمی‌شوند.":""));
   else h.put("scenario_weights",combined&&!(longer&&dedicated)?StyleEngine.combine(valid):valid.get(0));
   output.put(h);
  }
  report.put("long_term_style_enabled",dedicated).put("style_horizons",output).put("style_audit",audit.toString()).put("style_mode",combined?"ترکیبی • سهم برابر سبک‌های قابل محاسبه":"تک‌سبک");
  // Raw candles are internal calculation inputs, not the ChatGPT export.
  for(int z=0;z<sources.length();z++){JSONObject d=sources.getJSONObject(z).optJSONObject("data");if(d!=null)d.remove("ohlc");}
  if(tech!=null)tech.remove("ohlc");if(daily!=null)daily.remove("ohlc");
 }
}
