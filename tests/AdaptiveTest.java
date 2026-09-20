package com.v2grop.lbankpulse;
import org.json.*;
public class AdaptiveTest{
 static void check(boolean b){if(!b)throw new AssertionError();}
 static JSONArray rows(double dir){JSONArray a=new JSONArray();for(int i=0;i<400;i++){double c=1000+dir*i;a.put(new JSONArray().put(i*3600000L).put(c).put(c+1).put(c-1).put(c).put(100));}return a;}
 public static void main(String[] args)throws Exception{
  for(double d:new double[]{-1,0,1}){JSONObject w=AdaptiveEngine.evaluate(rows(d),24,"BTC",null);check(w.getInt("up")+w.getInt("neutral")+w.getInt("down")==100);check(!w.getBoolean("calibrated"));if(d==0)check(w.getBoolean("abstain"));if(d>0)check(w.getInt("up")>w.getInt("down"));if(d<0)check(w.getInt("down")>w.getInt("up"));}
  check(AdaptiveEngine.evaluate(new JSONArray(),4,"BTC",null)==null);
  check(AdaptiveEngine.evaluate(rows(1),4,"XAUT",null).getBoolean("abstain"));
  JSONArray shock=rows(0);shock.put(399,new JSONArray().put(399*3600000L).put(1000).put(1201).put(999).put(1200).put(100));check(AdaptiveEngine.evaluate(shock,4,"BTC",null).getBoolean("abstain"));
  JSONObject w=AdaptiveEngine.evaluate(rows(1),4,"ETH",rows(1));check(!w.getJSONObject("features").isNull("btc_context"));
  JSONArray bad=rows(1);bad.getJSONArray(399).put(0,400*3600000L);try{AdaptiveEngine.evaluate(bad,4,"BTC",null);throw new AssertionError();}catch(IllegalArgumentException expected){}
  double[] close=new double[400];java.util.Arrays.fill(close,1000);JSONObject tech=LocalResearch.indicators(close).put("ohlc",rows(1));
  JSONObject rep=new JSONObject().put("symbol","BTC").put("kind","spot").put("sources",new JSONArray().put(new JSONObject().put("name","technical").put("status","ok").put("data",tech)));
  new StyleResearch(b->{throw new AssertionError();}).apply(rep,new boolean[]{false,false,false,false,false,true},false,"",false);
  check(rep.getJSONArray("style_horizons").getJSONObject(0).getJSONObject("scenario_weights").getString("model").equals(AdaptiveEngine.VERSION));
  System.out.println("PASS adaptive directions, flat/shock abstention, gold policy, gaps, BTC alignment, integration");
 }
}
