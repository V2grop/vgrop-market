# VGrop Market 0.14.0 — research preview

- Added selectable VGrop Adaptive research style: EMA50/200, Wilder RSI, MACD, ATR, momentum, structure, volume and timestamp-aligned BTC context.
- Trend/range/shock regimes; explicit abstention on conflicting/weak evidence. Gold/oil abstain pending asset-specific validation.
- Original analysis styles, long-term override, watchlist, Telegram attribution and galaxy UI retained; added hourly close chart.
- Added executable walk-forward calibration research tools with temporal purging and cost assumptions.
- Added optional FastAPI/PostgreSQL historical-data service, schema, collector and scheduler templates.
- Added automated Android build/tests and tag-based GitHub prereleases.

IMPORTANT: This is not production-ready ML. Scores are experimental scenario weights, not calibrated probabilities or financial advice. No trained AI artifact is installed. Kotlin/MVVM/Room migration, broader context inputs, per-user auth and deployment verification remain pending; see docs/ARCHITECTURE.md.
APK uses the existing public testing key for upgrade compatibility. It is not a private production-signed release. No trading credentials or wallets are requested.

Initial BTC4h diagnostic:90 test samples; calibrated Brier0.523 vs past-frequency baseline0.484 (lower is better); average cost-adjusted return -0.056% per sample under fixed20bps cost. No demonstrated advantage; calibration is not promoted. Dataset and provenance included in research/.
