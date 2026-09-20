"""Explicit public Binance spot collector. Never remaps symbols across exchanges."""
import os,time,math,re,json
import httpx,psycopg

def collect(symbol,interval):
    if not re.fullmatch('[A-Z0-9]{2,25}USDT',symbol):raise ValueError('USDT symbol required')
    if interval not in ('1h','1d'):raise ValueError('interval')
    duration=3600 if interval=='1h' else 86400
    with httpx.Client(timeout=15,follow_redirects=False) as client:
        r=client.get('https://data-api.binance.vision/api/v3/klines',params={'symbol':symbol,'interval':interval,'limit':1000});r.raise_for_status();rows=r.json()
    now=int(time.time()*1000)
    with psycopg.connect(os.environ['DATABASE_URL'],connect_timeout=5) as c:
        c.execute("INSERT INTO instruments(exchange,symbol,market_kind,base_asset,quote_asset) VALUES('binance',%s,'spot',%s,'USDT') ON CONFLICT DO NOTHING",(symbol,symbol[:-4]))
        instrument=c.execute("SELECT id FROM instruments WHERE exchange='binance' AND symbol=%s AND market_kind='spot'",(symbol,)).fetchone()[0]
        for r in rows:
            if int(r[6])>=now:continue
            values=[float(v) for v in r[1:6]]
            if not all(math.isfinite(v) for v in values):raise ValueError('nonfinite candle')
            c.execute('INSERT INTO candles(instrument_id,interval_seconds,open_time,open,high,low,close,volume) VALUES(%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT(instrument_id,interval_seconds,open_time) DO UPDATE SET open=excluded.open,high=excluded.high,low=excluded.low,close=excluded.close,volume=excluded.volume,received_at=now()',(instrument,duration,int(r[0]),*values))
if __name__=='__main__':
    for symbol in os.environ.get('VGROP_SYMBOLS','BTCUSDT,ETHUSDT').split(','):
        for interval in ('1h','1d'):collect(symbol.strip(),interval)
