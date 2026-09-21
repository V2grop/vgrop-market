package com.v2grop.lbankpulse;
import com.v2grop.lbankpulse.domain.*;
import org.json.*;
import java.util.*;
/** Six reviewed base identities plus runtime exchange metadata checks. No synthetic/scaled mapping. */
public final class ScalpRepository {
 private final LocalResearch.Transport net;
 public ScalpRepository(){this(LocalResearch::http);} ScalpRepository(LocalResearch.Transport n){net=n;}
 static final Set<String> BASES=new HashSet<>(Arrays.asList("BTC","ETH","SOL","XRP","DOGE","BNB"));
 JSONObject get(String url)throws Exception{return new JSONObject(net.read(url,null));}
 public JSONObject analyze(String pair,String kind)throws Exception{
  String[] p=pair.toUpperCase(Locale.US).split("_");
  if(p.length!=2||!p[1].equals("USDT")||!BASES.contains(p[0])||!(kind.equals("spot")||kind.equals("perp")))return ScalpEngine.unavailable("هویت این بازار برای اسکلپ هنوز تأیید نشده؛ فعلاً BTC / ETH / SOL / XRP / DOGE / BNB با USDT");
  List<String> failures=new ArrayList<>();
  for(String exchange:new String[]{"Binance","Bybit"}){
   try{return fetch(exchange,p[0],kind);}catch(Exception e){if(Thread.currentThread().isInterrupted())throw e;failures.add(exchange+": "+e.getMessage());}
  }
  return ScalpEngine.unavailable(String.join(" • ",failures));
 }
 JSONObject fetch(String exchange,String base,String kind)throws Exception{
  boolean bin=exchange.equals("Binance");String root=bin?(kind.equals("spot")?"https://data-api.binance.vision/api/v3":"https://fapi.binance.com/fapi/v1"):"https://api.bybit.com/v5/market";
  String category=kind.equals("spot")?"spot":"linear",symbol=base+"USDT";
  verify(root,bin,category,symbol,base,kind);if(!base.equals("BTC"))verify(root,bin,category,"BTCUSDT","BTC",kind);
  long request=System.currentTimeMillis();long server=bin?get(root+"/time").getLong("serverTime"):get("https://api.bybit.com/v5/market/time").getLong("time");
  if(Math.abs(server-request)>5000)throw new IllegalArgumentException("Device/server clock mismatch");
  long interval=60000;List<ClosedCandle> a,btc;
  try{a=candles(root,bin,category,symbol,1,server);btc=base.equals("BTC")?a:candles(root,bin,category,"BTCUSDT",1,server);ScalpEngine.validate(a,interval,server);ScalpEngine.validate(btc,interval,server);}
  catch(Exception minuteFailure){interval=300000;a=candles(root,bin,category,symbol,5,server);btc=base.equals("BTC")?a:candles(root,bin,category,"BTCUSDT",5,server);ScalpEngine.validate(a,interval,server);ScalpEngine.validate(btc,interval,server);}
  InstrumentId id=new InstrumentId(exchange,symbol,base,"USDT",kind,1);
  OrderBookSnapshot book=null;
  try{
   JSONObject response=get(bin?root+"/depth?symbol="+symbol+"&limit=100":root+"/orderbook?category="+category+"&symbol="+symbol+"&limit=50");
   long received=System.currentTimeMillis();JSONObject b=bin?response:response.getJSONObject("result");
   if(!bin&&(!symbol.equals(b.getString("s"))||response.getInt("retCode")!=0))throw new IllegalArgumentException("Book identity mismatch");
   JSONArray bids=b.getJSONArray(bin?"bids":"b"),asks=b.getJSONArray(bin?"asks":"a");
   double bid=bids.getJSONArray(0).getDouble(0),ask=asks.getJSONArray(0).getDouble(0),mid=(bid+ask)/2;
   double[] bd=depth(bids,mid,true),ad=depth(asks,mid,false);
   // Binance spot REST has no source timestamp: it must not pass the reliable-book gate.
   long sourceTime=bin?b.optLong("T",0):b.getLong("ts");
   book=new OrderBookSnapshot(id,sourceTime,received,bid,ask,bd[0],ad[0],bd[1],ad[1]);
  }catch(Exception ignored){}
  long now=System.currentTimeMillis();JSONObject result=ScalpEngine.analyze(id,a,interval,book,btc,now);
  return result.put("retrieved_at",now).put("mapping_verified",true).put("native_5m_fallback",interval==300000).put("book_timestamp_note",bin&&kind.equals("spot")?"Binance spot REST has no source timestamp; NO TRADE":"Provider event timestamp required");
 }
 void verify(String root,boolean bin,String category,String symbol,String base,String kind)throws Exception{
  JSONObject r=get(bin?root+"/exchangeInfo?symbol="+symbol:root+"/instruments-info?category="+category+"&symbol="+symbol);
  if(!bin&&r.getInt("retCode")!=0)throw new IllegalArgumentException("Metadata rejected");
  JSONArray list=bin?r.getJSONArray("symbols"):r.getJSONObject("result").getJSONArray("list");
  for(int i=0;i<list.length();i++){JSONObject d=list.getJSONObject(i);if(!d.getString("symbol").equals(symbol))continue;
   if(!d.getString(bin?"baseAsset":"baseCoin").equals(base)||!d.getString(bin?"quoteAsset":"quoteCoin").equals("USDT"))break;
   if(!d.getString("status").equals(bin?"TRADING":"Trading"))break;
   if(kind.equals("perp")&&!(d.optString("contractType").equals(bin?"PERPETUAL":"LinearPerpetual")&&d.optString(bin?"marginAsset":"settleCoin").equals("USDT")))break;
   if(kind.equals("spot")&&bin&&!d.optBoolean("isSpotTradingAllowed",false))break;
   return;
  }throw new IllegalArgumentException("Exact instrument not verified: "+symbol+" "+kind);
 }
 List<ClosedCandle> candles(String root,boolean bin,String category,String symbol,int minutes,long now)throws Exception{
  String raw=net.read(bin?root+"/klines?symbol="+symbol+"&interval="+minutes+"m&limit=240":root+"/kline?category="+category+"&symbol="+symbol+"&interval="+minutes+"&limit=240",null);
  JSONArray rows;if(bin)rows=new JSONArray(raw);else{JSONObject r=new JSONObject(raw);if(r.getInt("retCode")!=0||!r.getJSONObject("result").getString("symbol").equals(symbol)||!r.getJSONObject("result").getString("category").equals(category))throw new IllegalArgumentException("Candle identity mismatch");rows=r.getJSONObject("result").getJSONArray("list");}
  List<ClosedCandle> out=new ArrayList<>();long duration=minutes*60000L,previous=-1;
  for(int step=0;step<rows.length();step++){
   JSONArray x=rows.getJSONArray(bin?step:rows.length()-1-step);long t=x.getLong(0);
   if(previous>=0&&t<=previous)throw new IllegalArgumentException("Duplicate/unordered source candles");previous=t;
   if(t+duration>now){if(step!=rows.length()-1||t>now)throw new IllegalArgumentException("Unexpected open candle");continue;}
   out.add(new ClosedCandle(t,x.getDouble(1),x.getDouble(2),x.getDouble(3),x.getDouble(4),x.getDouble(5)));
  }return out;
 }
 static double[] depth(JSONArray rows,double mid,boolean bids)throws Exception{
  double sum=0,max=0,previous=bids?Double.POSITIVE_INFINITY:0;
  for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);double p=r.getDouble(0),q=r.getDouble(1);if(!Double.isFinite(p)||!Double.isFinite(q)||p<=0||q<=0||(bids?p>=previous:p<=previous))throw new IllegalArgumentException("Invalid book levels");previous=p;
   if(Math.abs(p/mid-1)<=.002){sum+=p*q;max=Math.max(max,p*q);}}
  return new double[]{sum,max};
 }
}
