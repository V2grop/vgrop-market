package com.v2grop.lbankpulse;
import org.json.*;
public class LongTermTest{
 static void check(boolean b){if(!b)throw new AssertionError();}
 static JSONArray rows(int n,double direction){JSONArray a=new JSONArray();long today=System.currentTimeMillis()/86400000*86400000;for(int i=0;i<n;i++){double c=500+direction*i;long t=today-(n-i)*86400000L;a.put(new JSONArray().put(t).put(c).put(c+1).put(c-1).put(c).put(100).put(t+86399999));}return a;}
 static void sum(JSONObject w)throws Exception{check(w!=null);check(w.getInt("up")+w.getInt("neutral")+w.getInt("down")==100);}
 public static void main(String[] args)throws Exception{
  check(StyleEngine.longTrend(rows(179,1),720)==null);check(StyleEngine.longTrend(rows(364,1),2160)==null);
  for(int h:new int[]{720,2160})for(double d:new double[]{-1,0,1}){JSONObject w=StyleEngine.longTrend(rows(400,d),h);sum(w);if(d>0)check(w.getInt("up")>w.getInt("down"));if(d<0)check(w.getInt("up")<w.getInt("down"));sum(StyleEngine.donchian(rows(400,d),h));}
  LocalResearch local=new LocalResearch((u,b)->{throw new Exception();});
  JSONObject day=local.candles(rows(400,1),"fixture","https://fixture","spot",0,86400000);check(day.getInt("closed_candles")==400);
  JSONArray gap=rows(400,1);gap.remove(5);try{local.candles(gap,"f","https://f","spot",0,86400000);throw new AssertionError();}catch(java.io.IOException expected){}
  JSONArray stale=rows(400,1);for(int i=0;i<stale.length();i++){JSONArray r=stale.getJSONArray(i);r.put(0,r.getLong(0)-4*86400000L);r.put(6,r.getLong(6)-4*86400000L);}try{local.candles(stale,"f","https://f","spot",0,86400000);throw new AssertionError();}catch(java.io.IOException expected){}
  JSONObject report=new JSONObject().put("symbol","BTC").put("kind","spot").put("sources",new JSONArray().put(new JSONObject().put("name","daily").put("status","ok").put("data",day)));
  new StyleResearch(b->{throw new AssertionError();}).apply(report,new boolean[]{true,true,true,false,false},true,"");JSONArray h=report.getJSONArray("style_horizons");check(h.length()==5);check(!h.getJSONObject(0).has("scenario_weights"));sum(h.getJSONObject(3).getJSONObject("scenario_weights"));sum(h.getJSONObject(4).getJSONObject("scenario_weights"));check(!day.has("ohlc"));
  LocalResearch quote=new LocalResearch((u,b)->{if(u.contains("ticker/24hr.do"))return "{\"data\":[{\"ticker\":{\"latest\":\"0.000012\"}}]}";throw new Exception();});check(quote.liveQuote("pepe_usdt","spot").getDouble("price")==.000012);try{quote.liveQuote("unknown_usdt","perp");throw new AssertionError();}catch(java.io.IOException expected){}
  System.out.println("PASS long-term data thresholds, directional weights, daily gaps/staleness, combination, quote identity");
 }
}
