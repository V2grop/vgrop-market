package com.v2grop.lbankpulse;

public final class MarketSearchTest {
    public static void main(String[] args) {
        MarketItem btc = new MarketItem("btc_usdt", "BTC / USDT", "نقدی");
        MarketItem doge = new MarketItem("doge_usdt", "DOGE / USDT", "نقدی");
        check(MarketSearch.matches(btc, "BTC"), "symbol");
        check(MarketSearch.matches(btc, "btc usdt"), "pair tokens");
        check(MarketSearch.matches(btc, "بیت کوین"), "Persian name");
        check(MarketSearch.matches(doge, "Dogecoin"), "English name");
        check(!MarketSearch.matches(doge, "ethereum"), "unrelated name");
        check(MarketSearch.matches(doge, ""), "empty query");
        System.out.println("MarketSearchTest passed");
    }

    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
}
