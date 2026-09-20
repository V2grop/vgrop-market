# Architecture and production gates

## Shipped Android app (Android12+)
Native Java activity; dark galaxy theme, adaptive launcher, Telegram attribution. Watchlist is persisted in SharedPreferences. Public exchange adapters reside in LocalResearch. ScenarioEngine, StyleEngine and AdaptiveEngine are deterministic domain computations, tested on the JVM. StyleResearch aggregates available sources without substituting absent values with zero. Worker executors perform requests; UI generation guards discard outdated screen results. Price polling stops in the background. PriceChart renders hourly close history. Optional legacy AI gateway is disabled by default; manual ChatGPT handoff uses user-reviewed clipboard text, not a subscription API.

Canonical identity: exchange + exchange symbol + spot/perp + base/quote. Only verified mappings are used across exchanges. Gold is XAUT token, not XAU/USD. Oil lacks a verified provider adapter and must not silently map to crypto. Listing catalog comes from LBank, rather than a fixed number of coins. Listing does not imply all models have sufficient history.

Daily data and hourly data remain separate. Closed-only OHLCV is sorted/deduplicated, checked for gaps/freshness. Model features use only past candles. The app produces **scenario weights**, never labels them validated probability. Experimental adaptation classifies trend/range/shock; explicit abstention overrides weak/conflicting direction. No trained model is installed. Model contracts in MODEL_CONTRACT.json describe future TFLite/ONNX loading requirements; runtimes are not bundled.

## Optional server
Legacy backend/server.py retains the current app AI contract. backend/v2 adds a separate FastAPI/PostgreSQL historical data service with service-level bearer access. This service is not yet wired to the app and does not serve ML predictions. schema.sql defines exchange instruments, candles, model registry and analysis provenance. collect.py ingests Binance spot; scheduled systemd timer templates are provided, not installed. No deployment on a VPS was performed.

## Target Clean Architecture / MVVM migration
1. Extract domain models: InstrumentId, ClosedCandle, FeatureVector, DataQuality, AnalysisResult, ModelVersion. Keep pure engine parity tests.
2. Kotlin domain module with AnalyseAsset, LoadWatchlist, RefreshMarket use cases; repository/provider interfaces. Avoid business logic in ViewModels.
3. Retrofit/Ktor provider clients, Room entities keyed by full InstrumentId/timeframe/time, explicit TTL and source timestamps. Cached responses always marked stale when applicable; model must reject expired data.
4. ViewModel exposes immutable StateFlow: loading, data, partial-data, failed; lifecycle collection. Move screens incrementally to Material3, preserving right-to-left and TalkBack.
5. WorkManager daily briefings with timezone/calendar rules and deduplication. Existing FundAlertJob is economic-calendar notification only; BTC briefings/watchlist movement alerts are not implemented.
6. ML repository accepts signed/versioned artifacts only after validation. Separate assets/horizons, missing-feature masks, immutable training/calibration cutoffs and post-deployment drift monitoring.

## Feature coverage
| Requested | Current state |
|---|---|
| Watchlist, reorder, spot/perp, top price, galaxy UI | Implemented native UI |
| 4h/day/week, collapsible30/90days | Implemented with data requirements |
| Technical/SMC/combined/adaptive | Implemented research rules |
| Selected Hyperliquid accounts/order book | Implemented limited coverage, not global on-chain |
| BTC candle context | Implemented only for adaptive, aligned timestamps and market kind |
| BTC dominance, Fear & Greed, news sentiment in scores | Not connected; explicitly not used |
| Fundamentals/US calendar | Separate existing panels and alerts; not scored as causal effects |
| Local trained TFLite/ONNX | Contract only |
| Walk-forward/calibration study | Runnable research tools; no promoted model |
| Room/Kotlin/MVVM/Retrofit | Migration roadmap; not falsely represented as installed |
| FastAPI/PostgreSQL | Optional data service; deployment/integration pending |
| Per-user OIDC/auth, rate limits, observability | Production gates pending |

## Before production release
- Android instrumented tests on12/14/15+, rotation/process death, notifications, offline/slow networks, RTL/font scaling, accessibility and screenshots.
- Private production signing certificate/Play App Signing. Current public test keystore preserves compatibility only and is not a secure production trust anchor.
- Rate-limit/retry budgets, on-device cache, policy/license review for each data source, database migrations/backups, dependency audit, TLS deployment and monitoring.
- Multi-asset/time-period purged walk-forward validation including fees/funding/slippage, independent untouched final test, calibrated Brier/reliability metrics, uncertainty intervals and abstention coverage.
- No claim of low error or profitability before those gates pass. No automated trading endpoints.
