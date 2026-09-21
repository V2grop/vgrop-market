package com.v2grop.lbankpulse;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Pure search matcher shared by the market screen and unit tests. */
public final class MarketSearch {
    private static final Map<String, String> NAMES = new HashMap<>();

    static {
        add("BTC", "bitcoin بیت کوین بیتکوین");
        add("ETH", "ethereum ether اتریوم اتر");
        add("XAUT", "tether gold gold طلا تترگلد");
        add("XTI", "oil crude oil نفت نفت خام wti");
        add("DOGE", "dogecoin دوج دوج کوین");
        add("SOL", "solana سولانا");
        add("BNB", "binance coin بایننس بایننس کوین");
        add("XRP", "ripple ریپل");
        add("ADA", "cardano کاردانو");
        add("TRX", "tron ترون");
        add("TON", "toncoin تون تون کوین");
        add("DOT", "polkadot پولکادات");
        add("AVAX", "avalanche آوالانچ");
        add("LINK", "chainlink چین لینک چینلینک");
        add("LTC", "litecoin لایت کوین لایتکوین");
        add("BCH", "bitcoin cash بیت کوین کش");
        add("SHIB", "shiba inu شیبا شیبا اینو");
        add("PEPE", "pepe پپه");
        add("FARTCOIN", "fartcoin فارت کوین فارتکوین");
        add("SPX", "spx6900 اس پی ایکس");
        add("SUI", "sui سویی");
        add("SEI", "sei سی");
        add("NOT", "notcoin نات کوین ناتکوین");
    }

    private MarketSearch() {}

    private static void add(String symbol, String names) { NAMES.put(symbol, names); }

    public static boolean matches(MarketItem item, String query) {
        String q = normalize(query);
        if (q.isEmpty()) return true;
        String base = item.symbol.split("_")[0].toUpperCase(Locale.US);
        String aliases = NAMES.getOrDefault(base, "");
        String searchable = normalize(item.symbol + " " + item.display + " " + base + " " + aliases);
        if (searchable.contains(q)) return true;
        for (String token : q.split(" ")) if (!token.isEmpty() && !searchable.contains(token)) return false;
        return true;
    }

    public static String friendlyName(MarketItem item) {
        String base = item.symbol.split("_")[0].toUpperCase(Locale.US);
        String names = NAMES.get(base);
        if (names == null) return "";
        return names.split(" ")[0];
    }

    static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US)
                .replace('ي', 'ی').replace('ك', 'ک')
                .replace('\u200c', ' ').replace('\u200d', ' ').replace('_', ' ').replace('/', ' ').replace('-', ' ')
                .replaceAll("\\s+", " ");
    }
}
