package com.v2grop.lbankpulse;
import org.json.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
public class LocalResearchTest {
 static int passed=0;
 static synchronized void check(boolean b,String s){if(!b)throw new AssertionError(s);passed++;}
 static JSONArray bars(int format){
  JSONArray rows=new JSONArray();long hour=System.currentTimeMillis()/3600000*3600000;
  for(int i=0;i<=100;i++){long start=hour-(100-i)*3600000L;JSONArray r=new JSONArray();r.put(format==2?start/1000:start);r.put(1).put(1).put(1).put(100+i).put(1000).put(start+3599999);rows.put(r);}return rows;
 }
 static JSONObject source(JSONObject r,String name)throws Exception{JSONArray a=r.getJSONArray("sources");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("name").equals(name))return a.getJSONObject(i);throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  if(args.length>0&&args[0].equals("live")){
   JSONObject r=new LocalResearch((u,b)->{try{return LocalResearch.http(u,b);}catch(Exception e){System.out.println(new java.net.URL(u).getHost()+": "+e.getClass().getSimpleName()+" "+e.getMessage());throw e;}}).analyze("btc_usdt","perp");JSONArray a=r.getJSONArray("sources");for(int i=0;i<a.length();i++){JSONObject x=a.getJSONObject(i);System.out.println(x.getString("name")+": "+x.getString("status"));}return;
  }
  double[] flat=new double[100];Arrays.fill(flat,100);
  JSONObject f=LocalResearch.indicators(flat);check(f.getDouble("rsi14")==50,"flat RSI");check(f.getString("trend").equals("خنثی"),"flat trend");check(f.getJSONArray("horizons").getJSONObject(0).getDouble("volatility_scale_pct")==0,"flat volatility");
  double[] rising=new double[100];for(int i=0;i<100;i++)rising[i]=i+1;
  check(LocalResearch.indicators(rising).getDouble("rsi14")==100,"rising RSI");
  check(LocalResearch.indicators(rising).getJSONArray("horizons").getJSONObject(0).isNull("probabilities"),"no fabricated probabilities");
  check(LocalResearch.num("NaN")==null&&LocalResearch.num("Infinity")==null,"reject nonfinite");
  LocalResearch local=new LocalResearch((u,b)->{throw new IOException("network down");});
  for(int fmt=0;fmt<3;fmt++)check(local.candles(bars(fmt),"test","https://test","spot",fmt).getInt("closed_candles")==100,"closed candles format "+fmt);
  JSONArray gap=bars(0);gap.remove(20);try{local.candles(gap,"test","https://test","spot",0);throw new AssertionError("gap accepted");}catch(IOException expected){passed++;}
  JSONArray reverse=new JSONArray();JSONArray input=bars(1);for(int i=input.length()-1;i>=0;i--)reverse.put(input.get(i));check(local.candles(reverse,"Bybit","https://test","perp",1).getInt("closed_candles")==100,"reverse sort");
  JSONObject offline=local.analyze("btc_usdt","perp");check(source(offline,"Binance").getString("status").equals("unavailable"),"offline degrades");check(offline.isNull("relative_btc_percentage_points"),"missing comparison not zero");
  AtomicInteger requests=new AtomicInteger();
  LocalResearch fixture=new LocalResearch((url,body)->{
   requests.incrementAndGet();check(!url.contains("example.com")&&!url.contains("openai.com"),"no server or AI request");
   if(url.contains("klines"))return bars(0).toString();
   if(url.contains("data-api.binance.vision"))return "{\"lastPrice\":\"100\",\"priceChangePercent\":\"2\",\"quoteVolume\":\"1000\"}";
   if(url.contains("api.bybit.com"))throw new IOException("blocked");
   if(url.contains("lbkex.com")&&url.contains("ticker"))return "{\"data\":[{\"ticker\":{\"latest\":\"102\",\"turnover\":\"1000\"}}]}";
   if(url.contains("federalreserve"))return "<rss><channel/></rss>";
   throw new IOException("fixture unavailable");
  });
  JSONObject result=fixture.analyze("btc_usdt","spot");check(source(result,"Binance").getString("status").equals("ok"),"Binance survives Bybit failure");check(source(result,"technical").getString("status").equals("ok"),"local technical succeeds without server");check(Math.abs(result.getDouble("dispersion_usdt_pct")-2)<1e-8,"comparable quotes spread");
  AtomicInteger cross=new AtomicInteger();LocalResearch unknown=new LocalResearch((u,b)->{if(u.contains("binance")||u.contains("bybit")||u.contains("hyperliquid"))cross.incrementAndGet();throw new IOException();});
  unknown.analyze("kekius_usdt","spot");check(cross.get()==0,"unknown identity not guessed");
  LocalResearch bybit=new LocalResearch((u,b)->"{\"retCode\":0,\"result\":{\"list\":[{\"lastPrice\":\"1\",\"price24hPcnt\":\"0.1\",\"turnover24h\":\"2\"}]}}");
  check(bybit.bybit("DOGE","spot").getDouble("change24h")==10,"Bybit fraction conversion");
  LocalResearch malicious=new LocalResearch((u,b)->"<!DOCTYPE rss [<!ENTITY evil SYSTEM 'file:///etc/passwd'>]><rss/>");
  check(malicious.news("BTC").getJSONArray("sources").getJSONObject(0).getString("status").equals("unavailable"),"unsafe XML rejected");
  try{fixture.analyze("btc_usdt?evil=x","spot");throw new AssertionError("unsafe symbol accepted");}catch(IllegalArgumentException expected){passed++;}
  try{fixture.lbank("btc_usdt","perp");throw new AssertionError("perp treated as spot");}catch(IOException expected){passed++;}
  LocalResearch hyperFallback=new LocalResearch((u,b)->{
   if(b!=null&&b.optString("type").equals("candleSnapshot")){
    JSONArray out=new JSONArray();JSONArray rows=bars(0);
    for(int j=0;j<rows.length();j++){JSONArray c=rows.getJSONArray(j);out.put(LocalResearch.obj("s","FARTCOIN","i","1h","t",c.getLong(0),"T",c.getLong(6),"o",1,"h",1,"l",1,"c",c.get(4),"v",1));}return out.toString();
   }throw new IOException("other providers blocked");
  });
  check(hyperFallback.technical("FARTCOIN","fartcoin_usdt","perp",true).getString("source").equals("Hyperliquid"),"Hyperliquid candle fallback");
  JSONObject shortReport=LocalResearch.indicators(flat);
  check(shortReport.getJSONArray("horizons").length()==3&&shortReport.getJSONArray("horizons").getJSONObject(2).optJSONObject("scenario_weights")==null,"weekly unavailable preserves short horizons");
  double[] full=new double[1000];for(int k=0;k<full.length;k++)full[k]=100+k*0.1;
  JSONArray hs=LocalResearch.indicators(full).getJSONArray("horizons");
  check(hs.getJSONObject(2).getInt("hours")==168&&hs.getJSONObject(2).has("scenario_weights"),"weekly weights attached to report");
  System.out.println("PASS: "+passed+" assertions; engine runs without backend config or AI credentials.");
 }
}
