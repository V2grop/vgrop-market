package com.v2grop.lbankpulse;
import com.v2grop.lbankpulse.domain.*;
import java.util.*;
import org.json.*;
/** Independent five-minute deterministic research engine; never a calibrated forecast. */
public final class ScalpEngine {
 public static final String VERSION="scalp-rules-1",SCHEMA="scalp-features-1";
 public static void validate(List<ClosedCandle> rows,long interval,long now){
  if(interval!=60000&&interval!=300000)throw new IllegalArgumentException("Unsupported interval");
  if(rows.size()<120)throw new IllegalArgumentException("Insufficient history: 120 candles required");
  long prev=-1;
  for(ClosedCandle c:rows){
   if(c.openTime<0||c.openTime%interval!=0||c.openTime+interval>now)throw new IllegalArgumentException("Open/future/misaligned candle");
   if(prev>=0&&c.openTime-prev!=interval)throw new IllegalArgumentException("Gap, duplicate or unordered candles");prev=c.openTime;
   for(double x:new double[]{c.open,c.high,c.low,c.close,c.volume})if(!Double.isFinite(x))throw new IllegalArgumentException("Nonfinite OHLCV");
   if(c.low<=0||c.high<Math.max(c.open,c.close)||c.low>Math.min(c.open,c.close)||c.volume<0)throw new IllegalArgumentException("Invalid OHLCV");
  }
  if(now-(rows.get(rows.size()-1).openTime+interval)>interval+15000)throw new IllegalArgumentException("Stale candles");
 }
 static double ema(List<ClosedCandle> a,int p){double e=a.get(0).close,k=2.0/(p+1);for(int i=1;i<a.size();i++)e+=k*(a.get(i).close-e);return e;}
 static double rsi(List<ClosedCandle>a,int p){double g=0,l=0;for(int i=1;i<=p;i++){double d=a.get(i).close-a.get(i-1).close;g+=Math.max(0,d)/p;l+=Math.max(0,-d)/p;}for(int i=p+1;i<a.size();i++){double d=a.get(i).close-a.get(i-1).close;g=(g*(p-1)+Math.max(0,d))/p;l=(l*(p-1)+Math.max(0,-d))/p;}return g+l==0?50:l==0?100:100-100/(1+g/l);}
 public static JSONObject features(List<ClosedCandle>a)throws Exception{
  int n=a.size();ClosedCandle c=a.get(n-1);double atr=0,vol=0,vsum=0,pv=0,rv=0,hi=0,lo=Double.MAX_VALUE,old=0;
  for(int i=n-30;i<n;i++){ClosedCandle x=a.get(i);vsum+=x.volume;pv+=(x.high+x.low+x.close)/3*x.volume;if(i<n-1){vol+=x.volume;hi=Math.max(hi,x.high);lo=Math.min(lo,x.low);}double r=Math.log(x.close/a.get(i-1).close);rv+=r*r;double tr=Math.max(x.high-x.low,Math.max(Math.abs(x.high-a.get(i-1).close),Math.abs(x.low-a.get(i-1).close)));if(i>=n-14)atr+=tr/14;}
  for(int i=n-60;i<n-30;i++){double r=Math.log(a.get(i).close/a.get(i-1).close);old+=r*r;}
  double range=Math.max(c.high-c.low,1e-12),e9=ema(a,9),e21=ema(a,21),e50=ema(a,50);
  return new JSONObject().put("ema9",e9).put("ema21",e21).put("ema50",e50).put("rsi7",rsi(a,7)).put("rsi14",rsi(a,14))
   .put("macd6_13",ema(a,6)-ema(a,13)).put("roc5",(c.close/a.get(n-6).close-1)*100).put("return1",(c.close/a.get(n-2).close-1)*100)
   .put("body_ratio",(c.close-c.open)/range).put("wick_balance",((Math.min(c.open,c.close)-c.low)-(c.high-Math.max(c.open,c.close)))/range)
   .put("atr14",atr).put("atr_pct",atr/c.close*100).put("realized_vol_pct",Math.sqrt(rv/30)*100).put("vol_expansion",old>0?Math.sqrt(rv/old):1)
   .put("relative_volume",vol>0?c.volume/(vol/29):0).put("local_vwap",vsum>0?pv/vsum:JSONObject.NULL).put("local_high",hi).put("local_low",lo)
   .put("breakout",c.close>hi?1:c.close<lo?-1:0);
 }
 public static JSONObject unavailable(String reason)throws Exception{return new JSONObject().put("model",VERSION).put("horizon_seconds",300).put("abstain",true).put("status","NO TRADE").put("reason",reason).put("quality","unavailable").put("generated_at",System.currentTimeMillis());}
 public static JSONObject analyze(InstrumentId id,List<ClosedCandle>a,long interval,OrderBookSnapshot book,List<ClosedCandle>btc,long now)throws Exception{
  JSONObject out=unavailable("").put("instrument",id.key()).put("source",id.exchange).put("kind",id.kind).put("quote",id.quote).put("interval_seconds",interval/1000).put("candles",a.size()).put("generated_at",now);
  try{validate(a,interval,now);}catch(IllegalArgumentException e){return out.put("reason",e.getMessage());}
  out.put("last_closed_at",a.get(a.size()-1).openTime+interval).put("age_seconds",(now-a.get(a.size()-1).openTime-interval)/1000);
  if(id.base.equals("XAUT")||id.base.equals("XTI"))return out.put("reason","Asset class not validated for scalp model");
  JSONObject f=features(a);out.put("features",f).put("quality","closed candles valid; uncalibrated");
  List<String>guards=new ArrayList<>();String regime="RANGE";double imbalance=0,spread=Double.NaN;
  boolean bookOk=book!=null&&id.equals(book.instrument)&&book.sourceTime>0&&now-book.sourceTime>=0&&now-book.sourceTime<=10000&&now-book.retrievedAt>=0&&now-book.retrievedAt<=10000;
  if(bookOk){for(double x:new double[]{book.bid,book.ask,book.bidDepth,book.askDepth})if(!Double.isFinite(x)||x<=0)bookOk=false;if(book.ask<=book.bid)bookOk=false;}
  if(!bookOk)guards.add("دفتر سفارش تازه و قابل اعتماد در دسترس نیست");
  else{spread=book.spreadBps();imbalance=(book.bidDepth-book.askDepth)/(book.bidDepth+book.askDepth);out.put("spread_bps",spread).put("depth_quote",Math.min(book.bidDepth,book.askDepth)).put("book_imbalance",imbalance).put("largest_bid_quote",book.largestBid).put("largest_ask_quote",book.largestAsk);
   if(spread>10)guards.add("اسپرد بیش از ۱۰ واحد پایه");if(Math.min(book.bidDepth,book.askDepth)<25000){guards.add("عمق نقدشوندگی کم در فاصله ۲۰ واحد پایه");regime="LOW LIQUIDITY";}}
  out.put("liquidity",!bookOk?"unavailable":Math.min(book.bidDepth,book.askDepth)<25000?"low":"snapshot depth sufficient");
  double btcMove=0;
  try{validate(btc,interval,now);if(btc.get(btc.size()-1).openTime!=a.get(a.size()-1).openTime)throw new IllegalArgumentException("BTC unaligned");JSONObject bf=features(btc);btcMove=bf.getDouble("roc5");out.put("btc_roc5",btcMove).put("btc_volatility",bf.getDouble("realized_vol_pct"));if(Math.abs(btcMove)>0.8||bf.getDouble("realized_vol_pct")>0.35)guards.add("شوک کوتاه‌مدت BTC");}catch(Exception e){guards.add("زمینه تازه و هم‌زمان BTC در دسترس نیست");}
  double p=a.get(a.size()-1).close,atr=f.getDouble("atr14"),trend=atr>0?Math.tanh((f.getDouble("ema9")-f.getDouble("ema21"))/atr):0;
  double momentum=(f.getDouble("rsi7")-50)/50,breakout=f.getInt("breakout"),vwap=f.isNull("local_vwap")?0:Math.tanh((p-f.getDouble("local_vwap"))/Math.max(atr,1e-12));
  double score=.30*trend+.20*momentum+.15*vwap+.10*f.getDouble("wick_balance")+.10*breakout*Math.min(2,f.getDouble("relative_volume"))+.10*imbalance+.05*Math.tanh(btcMove);
  if(regime.equals("RANGE")){if(Math.abs(trend)>.5)regime="TREND";if(breakout!=0&&f.getDouble("relative_volume")>1.5)regime="BREAKOUT";}
  boolean shock=f.getDouble("atr_pct")>.7||f.getDouble("vol_expansion")>3||Math.abs(f.getDouble("return1"))>1;
  if(shock){regime="VOLATILITY SHOCK";guards.add("نوسان غیرعادی");}
  if(trend*imbalance<-.20||trend*momentum<-.25){regime="CONFLICTING SIGNALS";guards.add("تعارض روند، مومنتوم یا دفتر سفارش");}
  if(f.getDouble("relative_volume")==0||f.isNull("local_vwap"))guards.add("حجم کافی برای VWAP وجود ندارد");
  if(Math.abs(score)<.15)guards.add("شواهد جهت‌دار ضعیف");
  double u=Math.exp(score*2),d=Math.exp(-score*2),neutral=2.0+(guards.isEmpty()?0:2);double total=u+d+neutral;
  int up=(int)Math.round(100*u/total),down=(int)Math.round(100*d/total);
  JSONArray bull=new JSONArray(),bear=new JSONArray();if(trend>0)bull.put("EMA9 بالاتر از EMA21");else bear.put("EMA9 پایین‌تر از EMA21");if(momentum>0)bull.put("RSI7 بالای ۵۰");else bear.put("RSI7 زیر ۵۰");if(vwap>0)bull.put("قیمت بالای VWAP محلی");else bear.put("قیمت زیر VWAP محلی");
  return out.put("scenario_weights",new JSONObject().put("up",up).put("neutral",100-up-down).put("down",down))
   .put("regime",regime).put("signal_strength",Math.abs(score)).put("volatility",shock?"abnormal":"within research guards")
   .put("abstain",!guards.isEmpty()).put("status",guards.isEmpty()?"RESEARCH ONLY":"NO TRADE").put("reason",String.join(" • ",guards)).put("bullish_reasons",bull).put("bearish_reasons",bear)
   .put("label","Scenario Weights — Uncalibrated").put("limitation","Resting orders can be cancelled; imbalance does not prove future direction. No execution or profitable-edge claim.");
 }
}
