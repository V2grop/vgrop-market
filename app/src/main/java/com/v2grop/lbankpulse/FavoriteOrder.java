package com.v2grop.lbankpulse;
import java.util.*;
public final class FavoriteOrder {
 public static String id(MarketItem m){return m.symbol+"|"+m.marketType;}
 public static List<MarketItem> restore(String saved){
  List<MarketItem> defaults=Favorites.defaults(),out=new ArrayList<>();Map<String,MarketItem> pending=new LinkedHashMap<>();
  for(MarketItem m:defaults)pending.put(id(m),m);
  for(String key:saved.split("\n")){MarketItem m=pending.remove(key);if(m!=null)out.add(m);}
  out.addAll(pending.values());return out;
 }
 public static String save(List<MarketItem> list){StringBuilder b=new StringBuilder();for(MarketItem m:list)b.append(id(m)).append('\n');return b.toString();}
 public static void move(List<MarketItem> list,int from,int to){if(from<0||to<0||from>=list.size()||to>=list.size()||from==to)return;MarketItem m=list.remove(from);list.add(to,m);}
}
