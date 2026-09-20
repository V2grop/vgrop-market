package com.v2grop.lbankpulse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LBankApi {
    private static final String BASE = "https://api.lbkex.com";

    public List<MarketItem> loadAllMarkets() throws Exception {
        String body = get(BASE + "/v2/currencyPairs.do");
        JSONObject root = new JSONObject(body);
        JSONArray data = root.optJSONArray("data");
        if (data == null) throw new IllegalStateException("پاسخ فهرست بازارها معتبر نیست");
        List<MarketItem> out = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) {
            String symbol = data.optString(i, "");
            out.add(new MarketItem(symbol, symbol.replace("_", " / ").toUpperCase(Locale.US), "نقدی"));
        }
        return out;
    }

    private String get(String endpoint) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setConnectTimeout(12000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("User-Agent", "LBankPulse/0.1 Android");
        int status = c.getResponseCode();
        if (status < 200 || status >= 300) throw new IllegalStateException("خطای سرویس بازار: " + status);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) b.append(line);
            return b.toString();
        } finally {
            c.disconnect();
        }
    }
}
