package com.v2grop.lbankpulse;
import org.json.*;
public class GoldResearchTest {
 public static void main(String[] args)throws Exception{
  final int[] calls={0};long hour=System.currentTimeMillis()/3600000*3600000;
  LocalResearch research=new LocalResearch((url,body)->{
   if(url.contains("bybit")||url.contains("lbkex"))throw new java.io.IOException("blocked");
   if(!url.contains("instId=XAUT-USDT"))throw new AssertionError("wrong instrument");
   int page=calls[0]++;JSONArray a=new JSONArray();
   for(int i=0;i<300;i++){long t=hour-(page*300+i+1L)*3600000;
    a.put(new JSONArray().put(t).put("2500").put("2502").put("2498").put(2500+i*.01).put("1").put("1").put("2500").put("1"));}
   return new JSONObject().put("code","0").put("data",a).toString();
  });
  JSONObject t=research.goldTechnical("perp");
  if(!t.getString("kind").equals("spot")||!t.getString("basis_note").contains("جایگزین")||t.getInt("closed_candles")!=600)throw new AssertionError("fallback metadata");
  if(t.getJSONArray("horizons").getJSONObject(2).optJSONObject("scenario_weights")==null)throw new AssertionError("weekly missing");
  JSONObject item=new JSONObject().put("id","DGS10").put("label","10y").put("status","ok").put("value",4).put("previous",3.9).put("stale",false);
  JSONObject macro=new JSONObject().put("items",new JSONArray().put(item));
  if(!GoldResearch.explain(macro).contains("فشار بالقوه"))throw new AssertionError("direction");
  item.put("stale",true);
  if(GoldResearch.explain(macro).contains("فشار بالقوه"))throw new AssertionError("stale interpreted");
  System.out.println("Gold fallback, weekly and macro freshness tests passed");
 }
}
