package com.v2grop.lbankpulse;
import org.json.*;
/** Deterministic research model. No trained weights or calibrated probabilities. */
final class AdaptiveEngine {
 static final String VERSION="vgrop-adaptive-1";
 static double clip(double x){return Math.max(-1,Math.min(1,x));}
 static double ema(double[] c,int end,int period){double e=c[0],a=2.0/(period+1);for(int i=1;i<=end;i++)e+=a*(c[i]-e);return e;}
 static JSONObject evaluate(JSONArray rows,int hours,String symbol,JSONArray benchmark)throws Exception{
  boolean daily=hours>=720;int n=rows.length(),look=daily?(hours==720?30:90):hours;
  int required=daily?(hours==720?200:365):hours==168?336:200;
  if(n<required)return null;
  double[] c=new double[n];double atr=0,path=0,volume=0,lastVolume=0;boolean hasVolume=true;
  for(int i=0;i<n;i++){JSONArray r=rows.getJSONArray(i);double o=r.getDouble(1),hi=r.getDouble(2),lo=r.getDouble(3);c[i]=r.getDouble(4);
   if(!Double.isFinite(c[i])||!Double.isFinite(o)||!Double.isFinite(hi)||!Double.isFinite(lo)||Math.min(o,lo)<=0||hi<Math.max(o,c[i])||lo>Math.min(o,c[i]))throw new IllegalArgumentException("Invalid OHLC");
   if(i>0&&r.getLong(0)-rows.getJSONArray(i-1).getLong(0)!=(daily?86400000L:3600000L))throw new IllegalArgumentException("Non-contiguous candles");
   if(i>=n-14)atr+=Math.max(hi-lo,Math.max(Math.abs(hi-c[i-1]),Math.abs(lo-c[i-1])))/14;
   if(i>=n-look)path+=Math.abs(c[i]-c[i-1]);
   if(i>=n-21){double v=r.optDouble(5,Double.NaN);if(!Double.isFinite(v)||v<0)hasVolume=false;else if(i<n-1)volume+=v/20;else lastVolume=v;}
  }
  double last=c[n-1],unit=Math.max(atr,last*.0001),eff=path==0?0:Math.abs(last-c[n-look-1])/path;
  double trend=Math.tanh((ema(c,n-1,50)-ema(c,n-1,200))/(unit*4));
  double momentum=Math.tanh((last-c[n-look-1])/(unit*Math.sqrt(look)));
  double gain=0,loss=0;for(int i=1;i<n;i++){double d=c[i]-c[i-1];if(i<=14){gain+=Math.max(d,0)/14;loss+=Math.max(-d,0)/14;}else{gain=(gain*13+Math.max(d,0))/14;loss=(loss*13+Math.max(-d,0))/14;}}
  double rsi=gain+loss==0?50:100*gain/(gain+loss);
  double fast=c[0],slow=c[0],signal=0,macd=0;for(double v:c){fast+=2.0/13*(v-fast);slow+=2.0/27*(v-slow);macd=fast-slow;signal+=.2*(macd-signal);}
  double macdScore=Math.tanh((macd-signal)/unit);
  double high=0,low=Double.MAX_VALUE;for(int i=n-56;i<n-1;i++){high=Math.max(high,rows.getJSONArray(i).getDouble(2));low=Math.min(low,rows.getJSONArray(i).getDouble(3));}
  double location=clip(2*(last-low)/Math.max(high-low,unit)-1);
  double breakout=last>high?1:last<low?-1:0;
  boolean shock=Math.abs(last-c[n-2])>3*unit;
  String regime=shock?"پرنوسان":eff>.35?"رونددار":"خنثی/کم‌جهت";
  double score=eff>.35?.40*trend+.30*momentum+.20*macdScore+.10*breakout:-.45*location-.25*clip((rsi-50)/30)+.30*macdScore;
  double btc=0;boolean hasBtc=false;String assetClass=symbol.equals("XAUT")?"gold":symbol.startsWith("XTI")?"oil":"crypto";
  if(assetClass.equals("crypto")&&!symbol.equals("BTC")&&benchmark!=null&&benchmark.length()>look){
   int m=benchmark.length();JSONArray end=benchmark.getJSONArray(m-1),prior=benchmark.getJSONArray(m-look-1);
   if(end.getLong(0)==rows.getJSONArray(n-1).getLong(0)&&prior.getLong(0)==rows.getJSONArray(n-look-1).getLong(0)){
    double bp=end.getDouble(4),bb=prior.getDouble(4);if(bp>0&&bb>0&&Double.isFinite(bp)&&Double.isFinite(bb)){btc=Math.tanh(Math.log(bp/bb)*10);hasBtc=true;score=.85*score+.15*btc;}
   }
  }
  boolean conflict=trend*momentum<0;
  double volRatio=hasVolume&&volume>0?lastVolume/volume:Double.NaN;
  if(Double.isFinite(volRatio))score*=Math.max(.65,Math.min(1.1,Math.sqrt(volRatio)));
  boolean abstain=shock||conflict||Math.abs(score)<.18||!assetClass.equals("crypto");
  double neutral=abstain?.68:Math.max(.25,.50-.2*eff);if(abstain)score*=.3;
  String reason="VGrop تطبیقی • پژوهشی • "+regime+" • "+(abstain?"عدم ورود / شواهد ناکافی":"سناریوی جهت‌دار آزمایشی")+"\nEMA۵۰/۲۰۰، RSI، MACD، ATR، ساختار و مومنتوم"+(hasVolume?"؛ حجم بررسی شد":"؛ حجم موجود نیست")+(hasBtc?"؛ زمینه BTC هم‌زمان":"؛ زمینه BTC وارد نشده")+(!assetClass.equals("crypto")?"\nبرای طلا/نفت مدل مستقل اعتبارسنجی‌شده نداریم؛ عدم ورود.":"")+"\nخبر، دامیننس، ترس‌وطمع و داده آنچین در این مدل متصل نیستند؛ این مدل آموزش‌دیده نیست.";
  JSONObject w=StyleEngine.weights(score,neutral,reason);
  JSONObject features=new JSONObject().put("ema50",ema(c,n-1,50)).put("ema200",ema(c,n-1,200)).put("rsi14",rsi).put("macd",macd).put("macd_signal",signal).put("atr14",atr).put("efficiency",eff).put("momentum",momentum).put("support",low).put("resistance",high).put("volume_ratio",Double.isFinite(volRatio)?volRatio:JSONObject.NULL).put("btc_context",hasBtc?btc:JSONObject.NULL);
  return w.put("model",VERSION).put("calibrated",false).put("trained",false).put("abstain",abstain).put("regime",regime).put("asset_class",assetClass).put("features",features);
 }
}
