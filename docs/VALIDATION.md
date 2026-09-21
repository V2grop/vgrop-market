# Validation: correctness is not forecast skill

## Software correctness
`build-direct.sh` now invokes Gradle to build Java+Kotlin, Room and RecyclerView, runs Kotlin unit tests and verifies the APK signature. `scripts/test.sh` discovers all `tests/*Test.java` except explicit `*Live*` network fixtures; it executes domain fixtures and research unit tests. The existing Gold, US macro and watchlist order tests are no longer silently omitted. Backend legacy and v2 tests both run in CI.

New tests exercise closed-only data, freshness, gaps, duplicates, ordering, invalid OHLC, stale books, spread/depth guards, BTC shock, identity, exact 100% totals, cost accounting, temporal purge, dataset validation, zero-width Persian search and cache expiry. Emulator jobs on API31 and API35 use injected catalog fixtures (no live network dependency) for launch, RTL, search/clear, favorites, recreation, scalp-card rendering and Keystore token round trip. These do not prove all devices, rotation layouts, process-death recovery or notification flows are correct.

## Predictive validation
**No out-of-sample scalp performance result is supplied or claimed for v0.15.0.** No trained model replaces the previous adaptive engine. Existing BTC4h diagnostic remains visible: 90 samples, Brier about .523 vs .484 historical-frequency baseline, negative cost-adjusted mean. That diagnostic did not demonstrate an edge.

New infrastructure:
1. `dataset.py ASSET INTERVAL START_MS END_MS OUTPUT.csv` downloads reviewed Binance spot identities, paginates and validates closed history, and writes hash/provenance manifests. Supported initial assets: BTC/ETH/SOL/XRP/DOGE/BNB; intervals 1m/5m/1h/1d.
2. Collect at least three separated market periods per asset; freeze manifests before model development. Source history availability and delistings must be documented. Never silently join exchanges or market kinds.
3. `ScalpExport data.csv data.csv.manifest.json > samples.jsonl` replays exact engine with future labels outside feature windows. Missing historical books/trades remain missing and cause abstention.
4. `python3 research/scalp_validation.py samples.jsonl --taker-bps 5 --spread-bps 2 --slippage-bps 2` evaluates multiple purged chronological test folds per instrument. Frequency, always-neutral, momentum and uniform baselines are included. A rolling series of folds alone is not broad cross-regime evidence. Use distinct period datasets and report all outcomes.
5. Optional `train_logistic.py` creates a separate logistic candidate. Fit imputation/scaling on training only, select regularization on validation, fit temperature on calibration, evaluate final untouched test once. All four cutoffs explicit; rows whose labels cross boundaries are excluded. Do not tune after seeing final test. Dependencies: numpy, scikit-learn, joblib (not Android runtime).
6. `model_registry.verify` checks SHA256, schema, ordered cutoffs, evidence flags, sample floor and baseline/cost improvement before **human review**, not deployment. It cannot establish that a report is truthful by itself. Independent reproducibility, signed artifacts and uncertainty intervals remain necessary.

The trainer is not run on production data in this release. Net-return validation blocks promotion until complete. No LightGBM/XGBoost/TFLite/ONNX model is bundled. Unit-test-generated series are synthetic correctness fixtures and are never described as historical performance.

## Remaining empirical work
Capture event-time historical order books/trades, align BTC for altcoins, evaluate distinct asset/period/horizon partitions, quantify sampling uncertainty and execution impact, test cost stress scenarios, add calibrated reliability intervals and independent untouched-test replication. No profitability or low-error claim before these results exist.
