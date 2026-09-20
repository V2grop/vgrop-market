package com.v2grop.lbankpulse;
import org.json.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.zip.*;

/** Official US observations; Investing is a calendar reference, not a data feed. */
public final class UsMacro {
 static final String[] IDS={"DFEDTARU","DGS2","DGS10","CPIAUCSL","CPILFESL","PCEPILFE","UNRATE","PAYEMS","DTWEXBGS"};
 static final String[] LABELS={"سقف بازه هدف نرخ بهره فدرال رزرو","بازده اوراق خزانه ۲ساله","بازده اوراق خزانه ۱۰ساله","تورم مصرف‌کننده؛ CPI","تورم هسته مصرف‌کننده؛ Core CPI","تورم هسته مخارج مصرف؛ Core PCE","نرخ بیکاری آمریکا","اشتغال غیرکشاورزی؛ تغییر ماهانه","شاخص گسترده دلار فدرال رزرو"};
 private static volatile JSONObject cached;
 public static JSONObject load()throws Exception{
  JSONObject old=cached;if(old!=null&&System.currentTimeMillis()/1000-old.optLong("retrieved_at")<900)return new JSONObject(old.toString());
  String url="https://fred.stlouisfed.org/graph/fredgraph.csv?id="+String.join(",",IDS)+"&cosd="+LocalDate.now(ZoneOffset.UTC).minusYears(2);
  HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);c.setRequestProperty("User-Agent","MarketPulse/0.5");
  byte[] bytes;
  try{if(c.getResponseCode()!=200)throw new IOException("FRED HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream()){bytes=bounded(in,3000000);}}finally{c.disconnect();}
  JSONObject result=parse(bytes,LocalDate.now(ZoneOffset.UTC));result.put("retrieved_at",System.currentTimeMillis()/1000);cached=result;return new JSONObject(result.toString());
 }
 static byte[] bounded(InputStream in,int max)throws IOException{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(o.size()+n>max)throw new IOException("Dataset too large");o.write(b,0,n);}return o.toByteArray();}
 static JSONObject parse(byte[] bytes,LocalDate today)throws Exception{
  Map<String,TreeMap<LocalDate,Double>> series=new HashMap<>();for(String id:IDS)series.put(id,new TreeMap<>());
  if(bytes.length>=2&&bytes[0]=='P'&&bytes[1]=='K'){
   try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;int entries=0,total=0;while((e=zip.getNextEntry())!=null){if(++entries>20)throw new IOException("Too many entries");byte[] raw=bounded(zip,3000000);total+=raw.length;if(total>8000000)throw new IOException("Archive too large");if(e.getName().endsWith(".csv"))readCsv(new String(raw,"UTF-8"),series,today);}}
  }else readCsv(new String(bytes,"UTF-8"),series,today);
  JSONArray items=new JSONArray();int available=0;
  for(int i=0;i<IDS.length;i++){
   String id=IDS[i];TreeMap<LocalDate,Double> values=series.get(id);JSONObject d=new JSONObject().put("id",id).put("label",LABELS[i]).put("url","https://fred.stlouisfed.org/series/"+id);
   if(values.isEmpty()){items.put(d.put("status","unavailable"));continue;}available++;
   LocalDate date=values.lastKey();double latest=values.get(date);Map.Entry<LocalDate,Double> prior=values.lowerEntry(date);
   boolean inflation=i>=3&&i<=5,monthly=i>=3&&i<=7;
   Double shown=latest,previous=prior==null?null:prior.getValue();String unit=i<=2||i==6?"درصد":i==8?"شاخص؛ ژانویه ۲۰۰۶ = ۱۰۰":"";
   if(inflation){Double yearAgo=values.get(date.minusMonths(12));shown=yearAgo!=null&&yearAgo>0?(latest/yearAgo-1)*100:null;unit="درصد تغییر نسبت به ماه مشابه سال قبل؛ تعدیل فصلی‌شده";
    if(prior!=null){Double p=values.get(prior.getKey().minusMonths(12));previous=p!=null&&p>0?(prior.getValue()/p-1)*100:null;}
   }else if(id.equals("PAYEMS")){
    Double lastMonth=values.get(date.minusMonths(1));shown=lastMonth==null?null:latest-lastMonth;Double before=values.get(date.minusMonths(2));previous=lastMonth!=null&&before!=null?lastMonth-before:null;unit="هزار شغل؛ تفاضل سری تعدیل فصلی‌شده";
   }
   d.put("status",shown==null?"insufficient":"ok").put("value",shown==null?JSONObject.NULL:shown).put("previous",previous==null?JSONObject.NULL:previous).put("observation_date",date.toString()).put("previous_date",prior==null?JSONObject.NULL:prior.getKey().toString()).put("unit",unit).put("monthly",monthly);
   d.put("stale",java.time.temporal.ChronoUnit.DAYS.between(date,today)>(monthly?100:14));
   String note=inflation?"محاسبه از شاخص رسمی؛ ممکن است با تورم غیرتعدیل‌شده تقویم تفاوت داشته باشد.":id.equals("PAYEMS")?"بر پایه آخرین نسخه داده؛ ممکن است با عدد اولیه روز انتشار متفاوت باشد.":id.equals("DTWEXBGS")?"این شاخص، DXY نیست و داده لحظه‌ای بازار محسوب نمی‌شود.":"مقدار مشاهده‌شده؛ پیش‌بینی یا انتظار بازار نیست.";
   d.put("note",note);items.put(d);
  }
  if(available==0)throw new IOException("No valid official observations");
  return new JSONObject().put("items",items).put("source","FRED • Federal Reserve / BLS / BEA").put("calendar_url","https://m.investing.com/economic-calendar/");
 }
 static void readCsv(String csv,Map<String,TreeMap<LocalDate,Double>> series,LocalDate today){
  String[] rows=csv.replace("\uFEFF","").split("\\r?\\n");if(rows.length<2)return;String[] header=rows[0].split(",",-1);
  if(!header[0].equals("observation_date")&&!header[0].equals("DATE"))return;
  for(int i=1;i<rows.length;i++){String[] cells=rows[i].split(",",-1);if(cells.length!=header.length)continue;LocalDate date;try{date=LocalDate.parse(cells[0]);}catch(Exception e){continue;}if(date.isAfter(today))continue;
   for(int j=1;j<header.length;j++){TreeMap<LocalDate,Double> values=series.get(header[j]);if(values==null)continue;try{double v=Double.parseDouble(cells[j]);if(Double.isFinite(v))values.put(date,v);}catch(Exception ignored){}}
  }
 }
}
