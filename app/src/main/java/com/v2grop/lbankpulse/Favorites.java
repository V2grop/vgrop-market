package com.v2grop.lbankpulse;

import java.util.ArrayList;
import java.util.List;

public final class Favorites {
    private Favorites() {}

    public static List<MarketItem> defaults() {
        List<MarketItem> items = new ArrayList<>();
        items.add(new MarketItem("fartcoin_usdt", "FARTCOIN / USDT", "نقدی"));
        items.add(new MarketItem("fartcoin_usdt", "FARTCOIN / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("not_usdt", "NOT / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("xrp_usdt", "XRP / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("sol_usdt", "SOL / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("auction_usdt", "AUCTION / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("kekius_usdt", "KEKIUS / USDT", "نقدی"));
        items.add(new MarketItem("sei_usdt", "SEI / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("sui_usdt", "SUI / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("pepe_usdt", "PEPE / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("xaut_usdt", "طلا (XAUT) / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("xti_usdt", "نفت XTI(CL) / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("vine_usdt", "VINE / USDT", "نقدی"));
        items.add(new MarketItem("bnb_usdt", "BNB / USDT", "نقدی 3×"));
        items.add(new MarketItem("dolo_usdt", "DOLO / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("spx_usdt", "SPX / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("btc_usdt", "BTC / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("eth_usdt", "ETH / USDT", "قرارداد دائمی"));
        items.add(new MarketItem("doge_usdt", "DOGE / USDT", "قرارداد دائمی"));
        String[] priority={"btc_usdt","eth_usdt","xaut_usdt","xti_usdt"};
        List<MarketItem> ordered=new ArrayList<>();
        for(String symbol:priority)for(MarketItem item:items)if(item.symbol.equals(symbol))ordered.add(item);
        for(MarketItem item:items)if(!ordered.contains(item))ordered.add(item);
        return ordered;
    }
}
