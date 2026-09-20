package com.v2grop.lbankpulse;

public final class MarketItem {
    public final String symbol;
    public final String display;
    public final String marketType;
    public double lastPrice;
    public double change24h;
    public double volume;

    public MarketItem(String symbol, String display, String marketType) {
        this.symbol = symbol;
        this.display = display;
        this.marketType = marketType;
    }
}
