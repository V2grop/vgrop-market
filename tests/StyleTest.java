package com.v2grop.lbankpulse;
import org.json.*;import java.util.*;
public class StyleTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 static JSONArray rows(){JSONArray rows=new JSONArray();for(int i=0;i<400;i++)rows.put(new JSONArray().put(i*3600000L).put(100).put(101).put(99).put(100));return rows;}
 public static void main(String[] args)throws Exception{
  JSONArray r=rows();check(StyleEngine.smc(r,4).getInt("neutral")==60);check(StyleEngine.donchian(r,4)!=null);
  r.put(399,new JSONArray().put(399L*3600000).put(100).put(103).put(99).put(102));JSONObject w=StyleEngine.smc(r,4);check(w.getInt("up")>w.getInt("down"));
  r.put(399,new JSONArray().put(399L*3600000).put(100).put(101).put(97).put(100));w=StyleEngine.smc(r,4);check(w.getInt("up")>w.getInt("down"));
  JSONArray wave=new JSONArray();int[] ix={0,40,50,60,70,80,90,97,99};double[] val={105,110,80,100,90,125,110,135,130};
  for(int i=0;i<100;i++){int j=0;while(j<ix.length-2&&i>ix[j+1])j++;double c=val[j]+(val[j+1]-val[j])*(i-ix[j])/(ix[j+1]-ix[j]);wave.put(new JSONArray().put(i*3600000L).put(c).put(c+1).put(c-1).put(c));}
  check(StyleEngine.donchian(wave,4)!=null);
  JSONObject a=StyleEngine.weights(.8,.3,"a"),b=StyleEngine.weights(-.8,.3,"b");JSONObject both=StyleEngine.combine(Arrays.asList(a,b));check(Math.abs(both.getInt("up")-both.getInt("down"))<=1);
  JSONArray fills=new JSONArray().put(new JSONObject().put("coin","BTC").put("time",System.currentTimeMillis()).put("px",100000).put("sz",2).put("side","B"));check(StyleResearch.whale(fills,"ETH",4)==null);check(StyleResearch.whale(fills,"BTC",4).getInt("up")>25);
  check(StyleResearch.addresses("").length==0);try{StyleResearch.addresses("bad");throw new AssertionError();}catch(IllegalArgumentException expected){}
  double[] close=new double[400];Arrays.fill(close,100);JSONObject tech=LocalResearch.indicators(close).put("ohlc",rows());
  JSONObject report=new JSONObject().put("symbol","BTC").put("kind","perp").put("sources",new JSONArray().put(new JSONObject().put("name","technical").put("status","ok").put("data",tech)));
  int original=tech.getJSONArray("horizons").getJSONObject(0).getJSONObject("scenario_weights").getInt("up");
  new StyleResearch(q->{throw new AssertionError("Default must not fetch Hyperliquid");}).apply(report,new boolean[]{true,false,false,false,false},false,"");
  check(report.getJSONArray("style_horizons").getJSONObject(0).getJSONObject("scenario_weights").getInt("up")==original);
  new StyleResearch(q->{throw new Exception("offline");}).apply(report,new boolean[]{false,false,false,true,false},false,"");
  check(!report.getJSONArray("style_horizons").getJSONObject(0).has("scenario_weights"));
  check(ReportHandoff.text(report).contains("Hyperliquid"));
  System.out.println("PASS default parity, structure/sweep, Donchian flat and varied markets, combine, missing flow, wallet validation and copy request");
 }
}
