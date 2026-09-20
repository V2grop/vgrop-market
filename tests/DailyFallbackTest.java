package com.v2grop.lbankpulse;
import org.json.*;
public class DailyFallbackTest{
 static void check(boolean b){if(!b)throw new AssertionError();}
 static JSONObject report(JSONObject d)throws Exception{return new JSONObject().put("symbol","BTC").put("kind","perp").put("sources",new JSONArray().put(new JSONObject().put("name","daily").put("status","ok").put("data",d)));}
 public static void main(String[] args)throws Exception{
  JSONArray bars=LongTermTest.rows(400,1);JSONArray hl=new JSONArray();for(int i=0;i<bars.length();i++){JSONArray r=bars.getJSONArray(i);hl.put(new JSONObject().put("s","BTC").put("i","1d").put("t",r.get(0)).put("T",r.get(6)).put("o",r.get(1)).put("h",r.get(2)).put("l",r.get(3)).put("c",r.get(4)).put("v",100));}
  LocalResearch l=new LocalResearch((u,b)->{if(b!=null&&b.optString("type").equals("candleSnapshot"))return hl.toString();throw new Exception("offline");});
  JSONObject d=l.daily("BTC","btc_usdt","perp",true);check(d.getString("source").equals("Hyperliquid"));check(d.getString("quote").equals("USDC"));check(d.getInt("closed_candles")==400);
  JSONObject rep=report(d);new StyleResearch(b->{throw new Exception("offline");}).apply(rep,new boolean[]{false,false,false,true,false},true,"",true);
  JSONArray hs=rep.getJSONArray("style_horizons");check(hs.getJSONObject(3).has("scenario_weights"));check(hs.getJSONObject(4).getString("style_label").contains("بلندمدت"));check(!hs.getJSONObject(0).has("scenario_weights"));
  JSONArray lb=new JSONArray();for(int i=0;i<bars.length();i++){JSONArray r=new JSONArray(bars.getJSONArray(i).toString());r.put(0,r.getLong(0)/1000);lb.put(r);}
  LocalResearch fallback=new LocalResearch((u,b)->{if(u.contains("lbkex"))return new JSONObject().put("data",lb).toString();throw new Exception();});d=fallback.daily("BTC","btc_usdt","perp",true);check(d.getString("kind").equals("spot"));check(!d.getString("basis_note").isEmpty());
  check(StyleEngine.longTrend(LongTermTest.rows(199,1),720)==null);check(StyleEngine.longTrend(LongTermTest.rows(200,1),720)!=null);
  check(StyleEngine.longTrend(LongTermTest.rows(364,1),2160)==null);
  check(LocalResearch.dailyRank(new JSONObject().put("kind","spot").put("closed_candles",400),"perp")>LocalResearch.dailyRank(new JSONObject().put("kind","perp").put("closed_candles",100),"perp"));
  System.out.println("PASS Hyperliquid daily fallback, labeled spot reference, independent long style, thresholds");
 }
}
