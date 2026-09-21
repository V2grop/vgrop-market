package com.v2grop.lbankpulse;
import org.json.*;
/** Display-only public context, never a model feature until independently validated. */
public final class MarketContext {
 public static JSONObject load(){JSONObject out=new JSONObject();long now=System.currentTimeMillis()/1000;
  try{JSONObject d=new JSONObject(LocalResearch.http("https://api.coingecko.com/api/v3/global",null)).getJSONObject("data");double v=d.getJSONObject("market_cap_percentage").getDouble("btc");long t=d.getLong("updated_at");if(!Double.isFinite(v)||v<0||v>100||t>now)throw new IllegalArgumentException();out.put("dominance",LocalResearch.obj("value",v,"timestamp",t,"retrieved_at",now,"source","CoinGecko","stale",now-t>7200));}catch(Exception e){try{out.put("dominance",LocalResearch.obj("status","unavailable"));}catch(Exception ignored){}}
  try{JSONObject d=new JSONObject(LocalResearch.http("https://api.alternative.me/fng/?limit=1",null)).getJSONArray("data").getJSONObject(0);int v=d.getInt("value");long t=d.getLong("timestamp");if(v<0||v>100||t>now)throw new IllegalArgumentException();out.put("fear_greed",LocalResearch.obj("value",v,"timestamp",t,"retrieved_at",now,"source","Alternative.me","stale",now-t>172800));}catch(Exception e){try{out.put("fear_greed",LocalResearch.obj("status","unavailable"));}catch(Exception ignored){}}
  return out;
 }
}
