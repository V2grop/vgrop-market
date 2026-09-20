package com.v2grop.lbankpulse;
import java.util.*;
public final class FavoriteOrderTest {
 public static void main(String[] args){
  List<MarketItem> list=FavoriteOrder.restore("");String[] first={"btc_usdt","eth_usdt","xaut_usdt","xti_usdt"};
  for(int i=0;i<4;i++)if(!list.get(i).symbol.equals(first[i]))throw new AssertionError("priority");
  if(list.size()!=19)throw new AssertionError("lost market");
  String key=FavoriteOrder.id(list.get(18));FavoriteOrder.move(list,18,0);
  List<MarketItem> restored=FavoriteOrder.restore(FavoriteOrder.save(list));if(!FavoriteOrder.id(restored.get(0)).equals(key))throw new AssertionError("persistence");
  long fart=restored.stream().filter(m->m.symbol.equals("fartcoin_usdt")).count();if(fart!=2)throw new AssertionError("merged spot/perp");
  List<MarketItem> dirty=FavoriteOrder.restore("invalid\n"+key+"\n"+key);if(dirty.size()!=19)throw new AssertionError("duplicate or missing");
  FavoriteOrder.move(restored,0,-1);FavoriteOrder.move(restored,999,0);if(restored.size()!=19)throw new AssertionError("invalid move");
  System.out.println("PASS favorite priorities, restore, duplicate filtering, spot/perp identity and move boundaries");
 }
}
