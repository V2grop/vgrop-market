package com.v2grop.lbankpulse;
import org.json.*;
import java.util.*;
/** Explicit independent heuristics; no institutional intent or calibrated probabilities. */
final class StyleEngine {
 static final String[] NAMES={"سبک فعلی • روند و اندیکاتورها","اسمارت‌مانی • ساختار و جاروب قیمت","دونچیان • روند و شکست کانال","جریان سفارش Hyperliquid","معاملات آدرس‌های منتخب Hyperliquid","VGrop تطبیقی • مدل پژوهشی"};
 static JSONObject weights(double score,double neutral,String why)throws Exception{
  score=Math.max(-1,Math.min(1,score));int[] v=ScenarioEngine.round100(new double[]{(1-neutral)*(0.5+0.35*score),neutral,(1-neutral)*(0.5-0.35*score)});
  return new JSONObject().put("up",v[0]).put("neutral",v[1]).put("down",v[2]).put("explanation",why);
 }
 static JSONObject smc(JSONArray rows,int hours)throws Exception{
  int n=rows.length(),look=hours==4?20:hours==24?55:168;if(n<Math.max(80,look+3))return null;
  double upper=0,lower=Double.MAX_VALUE;
  for(int i=n-look-1;i<n-1;i++){upper=Math.max(upper,rows.getJSONArray(i).getDouble(2));lower=Math.min(lower,rows.getJSONArray(i).getDouble(3));}
  JSONArray last=rows.getJSONArray(n-1);double h=last.getDouble(2),l=last.getDouble(3),c=last.getDouble(4);
  double score=0;String why="شکست یا جاروب معتبر در پنجره انتخابی دیده نشد.";
  if(c>upper){score=.65;why="بسته‌شدن بالای سقف قبلی؛ شکست صعودی ساختار ساده‌شده.";}
  else if(c<lower){score=-.65;why="بسته‌شدن زیر کف قبلی؛ شکست نزولی ساختار ساده‌شده.";}
  else if(h>upper&&l<lower){why="جاروب هر دو سمت؛ جهت مبهم.";}
  else if(l<lower){score=.45;why="عبور از کف و بسته‌شدن دوباره داخل محدوده؛ نامزد جاروب فروش.";}
  else if(h>upper){score=-.45;why="عبور از سقف و بسته‌شدن دوباره داخل محدوده؛ نامزد جاروب خرید.";}
  return weights(score,score==0?.60:.35,why+" این قواعد، اثبات فعالیت پول هوشمند یا پیاده‌سازی کامل ICT نیستند.");
 }
 static JSONObject donchian(JSONArray rows,int horizon)throws Exception{
  int look=horizon==4?20:horizon==24?55:horizon==720?55:horizon==2160?90:168;
  int required=horizon==720?180:horizon==2160?365:Math.max(80,look+3);
  if(rows.length()<required)return null;
  int n=rows.length();double high=0,low=Double.MAX_VALUE,fast=rows.getJSONArray(0).getDouble(4),slow=fast;
  for(int i=0;i<n;i++){JSONArray r=rows.getJSONArray(i);double c=r.getDouble(4);fast+=2.0/21*(c-fast);slow+=2.0/51*(c-slow);if(i>=n-look-1&&i<n-1){high=Math.max(high,r.getDouble(2));low=Math.min(low,r.getDouble(3));}}
  double close=rows.getJSONArray(n-1).getDouble(4),width=Math.max(high-low,close*.00001);
  double position=Math.max(-1,Math.min(1,2*(close-low)/width-1)),trend=Math.tanh((fast-slow)/width*4);
  double breakout=close>high?1:close<low?-1:0;
  double score=.45*trend+.30*position+.25*breakout;
  return weights(score,.35+.20*(1-Math.abs(trend)),"دونچیان "+look+" کندل؛ سقف و کف بدون کندل آخر، روند میانگین نمایی ۲۰/۵۰ و موقعیت قیمت. "+(breakout>0?"شکست سقف":breakout<0?"شکست کف":"قیمت داخل کانال")+"؛ وزن آزمایشی، امکان شکست کاذب وجود دارد.");
 }
 static JSONObject longTrend(JSONArray rows,int hours)throws Exception{
  int required=hours==720?200:365,n=rows.length();if(n<required)return null;
  int look=hours==720?30:90;double fast=rows.getJSONArray(0).getDouble(4),slow=fast,path=0;
  for(int i=0;i<n;i++){double c=rows.getJSONArray(i).getDouble(4);fast+=2.0/51*(c-fast);slow+=2.0/201*(c-slow);if(i>=n-look)path+=Math.abs(c-rows.getJSONArray(i-1).getDouble(4));}
  double last=rows.getJSONArray(n-1).getDouble(4),prior=rows.getJSONArray(n-look-1).getDouble(4);
  double efficiency=path==0?0:Math.abs(last-prior)/path;
  double score=(.6*Math.tanh(Math.log(fast/slow)*10)+.4*Math.tanh(Math.log(last/prior)*5))*(.5+.5*efficiency);
  return weights(score,.4+.2*(1-efficiency),"سبک بلندمدت • دنبال‌کردن روند: میانگین نمایی روزانه ۵۰/۲۰۰، حرکت "+look+"روزه و انسجام حرکت؛ "+n+" کندل روزانه بسته‌شده. افق "+look+" روز، وزن آزمایشی و اعتبارسنجی‌نشده.");
 }
 static JSONObject combine(List<JSONObject> values)throws Exception{
  double[] a=new double[3];String[] keys={"up","neutral","down"};StringBuilder why=new StringBuilder("ترکیب با سهم برابر سبک‌های دارای داده: ");
  for(JSONObject v:values){for(int j=0;j<3;j++)a[j]+=v.getInt(keys[j])/100.0/values.size();why.append(v.optString("style")).append("؛ ");}
  boolean abstain=false;for(JSONObject v:values)abstain|=v.optBoolean("abstain");if(abstain)why.append(" عدم ورود: مدل تطبیقی شواهد کافی ندارد.");
  int[] rounded=ScenarioEngine.round100(a);return new JSONObject().put("abstain",abstain).put("up",rounded[0]).put("neutral",rounded[1]).put("down",rounded[2]).put("explanation",why.toString());
 }
}
