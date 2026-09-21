package com.v2grop.lbankpulse;
import android.content.*;
import org.json.*;
import java.util.*;
final class Watchlist {
 static List<MarketItem> load(Context c){
  android.content.SharedPreferences p=c.getSharedPreferences("watchlist",0);
  if(!p.contains("items_v2"))return FavoriteOrder.restore(p.getString("order",""));
  try{return decode(p.getString("items_v2","[]"));}catch(Exception e){return FavoriteOrder.restore(p.getString("order",""));}
 }
 static List<MarketItem> decode(String raw)throws Exception{
  List<MarketItem> out=new ArrayList<>();Set<String> seen=new HashSet<>();JSONArray a=new JSONArray(raw);
  for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);MarketItem m=new MarketItem(o.getString("symbol"),o.getString("display"),o.getString("type"));if(seen.add(FavoriteOrder.id(m)))out.add(m);}return out;
 }
 static String encode(List<MarketItem> items){
  JSONArray a=new JSONArray();try{for(MarketItem m:items)a.put(new JSONObject().put("symbol",m.symbol).put("display",m.display).put("type",m.marketType));}catch(Exception e){throw new IllegalStateException(e);}return a.toString();
 }
 static void save(Context c,List<MarketItem> items){String payload=encode(items);c.getSharedPreferences("watchlist",0).edit().putString("items_v2",payload).apply();long now=System.currentTimeMillis();com.v2grop.lbankpulse.data.CacheStore.record(c,"user:watchlist",0,"watchlist","user","mixed-explicit-per-row",now,now,30L*86400000,payload);}

 static boolean contains(Context c,MarketItem m){for(MarketItem x:load(c))if(FavoriteOrder.id(x).equals(FavoriteOrder.id(m)))return true;return false;}
 static void toggle(Context c,MarketItem m){List<MarketItem> a=load(c);boolean found=a.removeIf(x->FavoriteOrder.id(x).equals(FavoriteOrder.id(m)));if(!found)a.add(m);save(c,a);}
}
