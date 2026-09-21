# VGrop Market

Persian Android12+ market research application. **Research preview, not production-ready forecasting or financial advice.**

Download APK and complete source: [Releases](https://github.com/V2grop/vgrop-market/releases).
Build status: [Actions](https://github.com/V2grop/vgrop-market/actions).

## Current app
Galaxy dark UI, sortable watchlist, live-price polling, public market catalog, multi-exchange research, experimental scenario bars for4h/24h/7d/30d/90d, collapsible longer horizons, selectable technical/SMC/Donchian/Hyperliquid/adaptive styles, gold research panel, US calendar, optional server AI and manual ChatGPT handoff.

Select **VGrop تطبیقی • مدل پژوهشی** under analysis styles. To use that style for30/90days, disable the independent long-term-style checkbox. Default previous selections are preserved. Adaptive is deterministic research scoring, not a trained neural model or a claim of high accuracy. Missing feeds are disclosed; no fake prices/probabilities.

## Build
Gradle8.9, JDK17, Android SDK platform35/build-tools35.0.1, Python3, curl:
```
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
bash build-direct.sh
bash scripts/test.sh
```
APK: build/direct/VGrop-market-v0.15.1-debug.apk. The public compatibility test key is intentionally retained; do not use it as a production signing key. Source/CI require no exchange or OpenAI credentials.
Push a version tag to run build/tests and publish a **prerelease** with APK, source ZIP and SHA256 checksums. Main pushes publish the current prerelease; PR builds upload artifacts without publishing.

## Project map
- app/: existing Java Android application and pure analysis engines
- tests/: JVM engine, data, calendar and persistence tests
- research/: CSV exporter and purged walk-forward temperature-calibration study
- backend/: legacy optional AI gateway; v2/: optional FastAPI/PostgreSQL data service
- docs/ARCHITECTURE.md: architecture, current coverage, production gates and Kotlin/Room/MVVM roadmap
- docs/MODEL_CONTRACT.json: future local TFLite/ONNX artifact contract, no installed runtime
- .github/workflows/android.yml: reproducible build/test/release workflow

## Research study
After building/tests, export a closed OHLCV CSV (timestamp_ms,open,high,low,close,volume), then:
```
java -cp build/test-classes:build/test-libs/json.jar com.v2grop.lbankpulse.WalkForwardExport candles.csv 4 > predictions.csv
python3 research/walk_forward.py predictions.csv
```
Calibrators fit only samples whose label windows end before each test fold. This diagnostic never automatically promotes a model to the app. Minimum sample requirements are enforced. Read its limits before drawing performance conclusions.

Brand: VGrop Market · [Telegram](https://t.me/V2grop)

## v0.15.0
Independent **اسکلپ ۵ دقیقه‌ای** card above 4H; closed-bar validation, strict USDT spot/perp identity, EMA9/21/50, RSI7/14, local VWAP, volume/volatility/structure features, timestamped order-book guards, BTC-shock guard and explicit NO TRADE. Initial scalp coverage: six reviewed liquid assets on Binance/Bybit, not all catalog entries. Unverified assets return unavailable.

RecyclerView catalog, zero-width Persian search, Kotlin/Room history cache, Keystore server tokens, complete offline Java discovery and Android31/35 emulator fixtures. Existing engines retained. See [Scalping model](docs/SCALPING_MODEL.md) and [Validation](docs/VALIDATION.md).

No out-of-sample scalp edge has been demonstrated. New multi-asset dataset/replay/cost-validation/logistic-candidate tools are infrastructure, not proof of accuracy. Native 5m fallback and missing book feeds are explicitly disclosed. Room/MVVM migration is incremental; Material3/Retrofit and full market-data cache are pending.

## v0.15.1 — Watchlist and announcements
Long-press and drag watchlist rows to reorder, without arrow controls. Optional announcement is hidden until enabled in [`config/announcement.json`](config/announcement.json). [Persian management guide](docs/ANNOUNCEMENTS.md). No reinstall needed for subsequent notice edits.
