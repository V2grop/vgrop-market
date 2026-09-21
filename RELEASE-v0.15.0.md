# VGrop Market 0.15.0 — five-minute research mode (prerelease)

## Implemented
- Independent five-minute scalp engine/card above 4H; existing horizons/styles retained.
- Closed-only 1m/native5m input, strict identity, data quality and timestamp validation, spread/depth/BTC-shock/conflict abstention.
- Initial reviewed BTC/ETH/SOL/XRP/DOGE/BNB USDT spot/perp mappings, runtime Binance/Bybit metadata checks.
- EMA/RSI/VWAP/volume/volatility/structure features, descriptive scenario weights totaling 100, explicit NO TRADE and provenance.
- RecyclerView/ListAdapter catalog; Persian zero-width normalization; existing stars/count/clear preserved.
- Kotlin/Room history cache and additive watchlist mirror; Keystore-backed encrypted server tokens.
- Display-only dominance/Fear & Greed; explicit missing unlock/inflation data.
- Multi-asset dataset provenance, exact-engine replay, configurable execution costs, purged walk-forward metrics and logistic-candidate infrastructure; no promoted ML model.
- Backend 1m/5m history support, validation, additive scalp/book schema.
- Automatic Java test discovery, Python correctness tests, Android12/15 emulator fixture jobs.

## Partial
Incremental Kotlin/domain/repository/StateFlow migration; legacy Activity remains. Room caches analyses and mirrors watchlist; full data/instrument/model cache pending. Fundamental/news enrichment is limited. CI tests are correctness checks, not market performance validation.

## Not implemented
Historical book/trade collection and full scalp backtest, incremental-value validation of sentiment/context, Hyperliquid/LBank scalp adapters, broad verified scalp coverage, trained on-device ML, Material3/full MVVM/Retrofit migration, verified oil data and production signing/deployment.

## Validation and limitations
No real out-of-sample 5M accuracy or cost-adjusted edge demonstrated. Existing negative BTC4h diagnostic remains unchanged. Missing/stale data abstains; Binance spot REST lacks book event timestamps and therefore fails the reliable-book guard. Percentages are uncalibrated Scenario Weights. No automated trading.

## Regression risks
Build now requires Gradle/Kotlin/Room dependencies. Public debug certificate retained for compatibility. Device/emulator coverage is finite; font-scale/rotation/process-death/notification behavior needs broader device verification. Existing watchlist preferences remain canonical. External APIs may be unavailable or rate-limited. This remains a prerelease.
