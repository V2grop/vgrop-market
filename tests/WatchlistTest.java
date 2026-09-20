package com.v2grop.lbankpulse;
import java.util.*;
public class WatchlistTest {
 public static void main(String[] args)throws Exception{
  List<MarketItem> a=Favorites.defaults();a.remove(0);
  a.add(new MarketItem("new_usdt","NEW / USDT","نقدی"));
  List<MarketItem> b=Watchlist.decode(Watchlist.encode(a));
  if(b.size()!=19||!b.get(18).symbol.equals("new_usdt"))throw new AssertionError();
  b.clear();if(!Watchlist.decode(Watchlist.encode(b)).isEmpty())throw new AssertionError();
  a.add(a.get(0));if(Watchlist.decode(Watchlist.encode(a)).size()!=19)throw new AssertionError();
  System.out.println("PASS removal persistence, new markets, empty list and deduplication");
 }
}
