# Scalp rules 1 — v0.15.0

This is an independent deterministic five-minute research engine, not the 4H engine with a changed horizon. Its percentages are **Scenario Weights — Uncalibrated**. No live orders, trained artifact, accuracy or profitability claim.

## Identity and sources
The reviewed base allowlist is BTC, ETH, SOL, XRP, DOGE and BNB. Binance is tried first, Bybit second after source failure. Every request verifies exchange metadata: exact symbol, base, USDT quote, trading status and spot permission or USDT linear perpetual contract/settlement. The complete identity includes exchange, symbol, base, quote, market kind and contract multiplier (1). A source is never blended with another exchange. No PEPE/kPEPE, USDC, XAUT/XAU/USD or oil substitution. Hyperliquid and LBank scalp adapters remain unimplemented; old analysis adapters remain available.

References: [Binance market REST](https://developers.binance.com/docs/binance-spot-api-docs/rest-api/market-data-endpoints), [Bybit candles](https://bybit-exchange.github.io/docs/v5/market/kline), [Bybit order book](https://bybit-exchange.github.io/docs/v5/market/orderbook). Runtime metadata checks do not establish identical token contracts for arbitrary tickers; expansion requires reviewed identities.

## Candles and time
Prefer 240 one-minute candles; native five-minute candles are an explicit fallback. At least 120 closed bars are required. Exactly one terminal currently-open candle may be excluded by the provider adapter. The domain validator rejects any open candle, duplicate, gap, out-of-order timestamp, nonfinite OHLCV, nonpositive price, negative volume or invalid high/low. Bybit's documented reverse ordering is reversed once; malformed order is never sorted away.

Max age of the last bar close is one interval + 15 seconds; book source/retrieval timestamps must both be within 10 seconds, not future. Device/server clock divergence over five seconds rejects the source. BTC must use the same provider, quote, kind and interval, and its last candle must align exactly. Cached analyses retain timestamps and are history only; the live engine always fetches fresh data.

## Features and formulas
- EMA9/21/50: recursive close EMA, alpha=2/(period+1).
- RSI7/14: Wilder smoothed gains/losses.
- MACD equivalent: EMA6 minus EMA13 (no signal-line claim).
- ROC5: five-bar close return; return1: single-bar close return. With native 5m input, these describe 25m and 5m history, while target remains the next five minutes.
- Body ratio=(close-open)/(high-low); wick balance=(lower wick-upper wick)/range.
- ATR14: simple mean of the last 14 true ranges. Realized volatility: RMS log return over 30 bars. Expansion: last30 / preceding30 RMS ratio.
- Relative volume: final volume / mean preceding29 volumes. Local VWAP: 30-bar typical price weighted by base volume, **not exchange session VWAP**. No taker pressure or executed-trade imbalance is inferred from candles.
- Micro breakout: final close beyond previous29 highs/lows. Local structure is rolling high/low, not a discretionary SMC label.
- Book: quote notional depth within 20bp of mid; imbalance=(bidDepth-askDepth)/(sum); spread=(ask-bid)/mid*10000. Largest nearby resting order notionals are displayed. This limited snapshot does not measure all venue liquidity or prove future direction.
- BTC ROC5 and short realized volatility guard against shocks.

Directional score = .30*tanh((EMA9-EMA21)/ATR) + .20*(RSI7-50)/50 + .15*tanh((close-localVWAP)/ATR) + .10*wickBalance + .10*breakout*min(2,relativeVolume) + .10*bookImbalance + .05*tanh(BTC ROC5).
EMA50, RSI14, short MACD and some features are diagnostics recorded for future research, not independently weighted twice. Score weights and thresholds are hand chosen, not optimized or validated.

Scenario logits are exp(2*score), 2, exp(-2*score). Neutral mass is 4 on abstention. Normalize and round up/down; neutral is the remainder so integers sum to 100. Invalid candle history yields **no percentages**. Valid candles with inadequate microstructure can show descriptive scenario weights but always **NO TRADE**.

## Regimes and abstention
TREND: abs(trend)>0.5. BREAKOUT: breakout with relativeVolume>1.5. Otherwise RANGE. LOW LIQUIDITY below 25,000 quote units on either side within 20bp. VOLATILITY SHOCK if ATR/close>0.7%, expansion>3 or last-bar move>1%. CONFLICTING SIGNALS if trend*imbalance<-.20 or trend*momentum<-.25. Regime display has priority rules; all guard reasons remain listed.

Abstain for missing/stale/unreliable book, spread>10bp, low depth, missing/misaligned BTC, BTC five-bar move>0.8% or realized volatility>0.35%, abnormal asset volatility, conflicting evidence, zero volume, missing VWAP, or abs(score)<.15. Binance spot REST lacks a source event timestamp and therefore does **not** pass the book freshness gate. UI timestamps describe a snapshot, not a continuously valid signal. Refresh before evaluating again. Native 5m fallback is more conservative in lookback duration and has no demonstrated scalp edge.

## Costs and evaluation
`research/scalp_validation.py` accepts maker/taker fee (per side), round-trip spread, slippage (per side), and funding total (all basis points). Default illustrative taker round trip = 2*5 + 2 + 2*2 + 0 = 16bp. These are configurable assumptions, not account-specific fees. Maker fills are not guaranteed. Signals simulate at most one overlapping position per instrument. Gross/net return, cost, abstention, reliability, confusion matrix, Brier baselines and regime breakdown are reported independently.

`ScalpExport` replays the exact Java engine, enters at next-bar open, exits at horizon close, and labels +/-10bp as directional. Historical books are not fabricated. OHLC-only replay abstains without books; it cannot validate a tradable microstructure edge. Historical BTC synchronization for altcoin replay remains pending. No model promotion has occurred.
