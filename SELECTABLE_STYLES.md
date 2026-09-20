> 0.12: Elliott has been replaced by Donchian. See RELEASE-v0.12.md for the current method and daily horizons. The 0.11 notes below are historical.

# Selectable styles — 0.11.0

Default remains the exact previous indicator scenario engine. Initial selection is default only. Selecting a single style switches percentage output; combination must be enabled to select multiple styles. The combination is an equal arithmetic mean of valid style weights per horizon, rounded to sum to 100. Missing methods are excluded and listed, never silently replaced with default.

Independent experimental rules:
- SMC-inspired: prior 20/55/168-hour OHLC extremes, latest close breakout or wick sweep returning inside; double sweep is ambiguous. Scores +/-.65 breakout, +/-.45 sweep, neutral .35 or .60. This is not full ICT, order blocks, FVG or proof of institutional activity.
- Elliott-inspired: confirmed close-price pivots with 2/4/8 bars on both sides; latest six alternating pivots tested for simple non-overlapping untruncated impulse, wave 2 not beyond origin, wave 3 not shortest. Conservative opposite .25 score after fifth wave, neutral .60. No complete wave count, subdivisions, diagonals or ABC catalog. No pattern means unavailable.
- Hyperliquid order book: first up to 20 levels per side, at least five, exact coin and timestamp within two minutes; notional imbalance scaled .35, neutral .55. This is a resting-order snapshot, not executed order flow; used only in experimental 4h weights, no daily/weekly extrapolation.
- Selected Hyperliquid accounts: up to three explicitly entered public master/subaccount addresses. No automatic whale discovery or identity attribution. userFillsByTime over seven days; >=2000 rows fails closed due to possible truncation. Deduplicates tid per address. Includes individual fills >=100,000 USDC; buy/sell imbalance scaled .5, neutral .50. Includes opens and closes, not long/short inference. No qualifying fills means unavailable, not neutral. This is exchange activity, not comprehensive onchain transfers or all whales.

OHLC validity and closed-candle freshness/gap checks are retained. Weekly candle styles require >=336 closed hours. No learned/calibrated probability model or improved accuracy claim.

Missing-data fallback: selection/results/unavailable reasons included in ChatGPT handoff and dedicated copy button. Prompt requests the selected methods and fresh sourced data if available, explicitly no invented percentages. Manual paste/send; no automatic subscription access.

Validation: build and signature; original 305 scenario and 32 local assertions; unchanged default output; structure/sweep, valid and absent Elliott pattern, equal combination, missing flow handling, wallet syntax, copy payload. No device UI or live Hyperliquid account validation. No API keys or private wallet keys are used.

Sources:
https://www.elliottwave.com/waveopedia/impulse/
https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/info-endpoint
