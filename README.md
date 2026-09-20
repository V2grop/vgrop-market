# VGrop Market

Persian Android12+ market research application. **Research preview, not production-ready forecasting or financial advice.**

Download APK and complete source: [Releases](https://github.com/V2grop/vgrop-market/releases).
Build status: [Actions](https://github.com/V2grop/vgrop-market/actions).

## Current app
Galaxy dark UI, sortable watchlist, live-price polling, public market catalog, multi-exchange research, experimental scenario bars for4h/24h/7d/30d/90d, collapsible longer horizons, selectable technical/SMC/Donchian/Hyperliquid/adaptive styles, gold research panel, US calendar, optional server AI and manual ChatGPT handoff.

Select **VGrop تطبیقی • مدل پژوهشی** under analysis styles. To use that style for30/90days, disable the independent long-term-style checkbox. Default previous selections are preserved. Adaptive is deterministic research scoring, not a trained neural model or a claim of high accuracy. Missing feeds are disclosed; no fake prices/probabilities.

## Build
JDK17, Android SDK platform35/build-tools35.0.1, Python3, curl:
```
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
bash build-direct.sh
bash scripts/test.sh
```
APK: build/direct/VGrop-market-v0.14.0-debug.apk. The public compatibility test key is intentionally retained; do not use it as a production signing key. Source/CI require no exchange or OpenAI credentials.
Push a version tag to run build/tests and publish a **prerelease** with APK, source ZIP and SHA256 checksums. Main/PR builds upload artifacts without publishing a release.

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
