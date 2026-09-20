CREATE TABLE IF NOT EXISTS instruments (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 exchange text NOT NULL, symbol text NOT NULL, market_kind text NOT NULL CHECK(market_kind IN ('spot','perp')),
 base_asset text NOT NULL, quote_asset text NOT NULL,
 UNIQUE(exchange,symbol,market_kind)
);
CREATE TABLE IF NOT EXISTS candles (
 instrument_id bigint NOT NULL REFERENCES instruments(id), interval_seconds integer NOT NULL CHECK(interval_seconds>0),
 open_time bigint NOT NULL, open double precision NOT NULL CHECK(open>0), high double precision NOT NULL,
 low double precision NOT NULL CHECK(low>0), close double precision NOT NULL CHECK(close>0), volume double precision NOT NULL CHECK(volume>=0),
 received_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(instrument_id,interval_seconds,open_time),
 CHECK(high>=greatest(open,close) AND low<=least(open,close))
);
CREATE TABLE IF NOT EXISTS model_registry (
 version text PRIMARY KEY, feature_schema text NOT NULL, artifact_sha256 text,
 trained boolean NOT NULL DEFAULT false, calibrated boolean NOT NULL DEFAULT false,
 status text NOT NULL CHECK(status IN ('research','candidate','validated','retired')),
 validation_report jsonb NOT NULL DEFAULT '{}'
);
CREATE TABLE IF NOT EXISTS analyses (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 instrument_id bigint NOT NULL REFERENCES instruments(id), horizon_hours integer NOT NULL,
 as_of timestamptz NOT NULL, model_version text NOT NULL REFERENCES model_registry(version),
 scores jsonb NOT NULL, quality jsonb NOT NULL, provenance jsonb NOT NULL,
 UNIQUE(instrument_id,horizon_hours,as_of,model_version)
);
CREATE INDEX IF NOT EXISTS candle_lookup ON candles(instrument_id,interval_seconds,open_time DESC);
