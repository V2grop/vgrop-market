"""Explicit public Binance spot collector. Never remaps symbols across exchanges."""
import os,time,math,re,json
import httpx,psycopg

def validate_rows(rows,duration,now):
    for i,r in enumerate(rows):
        t=int(r[0]);o,h,l,c,v=map(float,r[1:6])
        if t%(duration*1000) or t+duration*1000>now or (i and t-int(rows[i-1][0])!=duration*1000):raise ValueError('Open/gap/duplicate/unordered history')
        if not all(math.isfinite(x) for x in (o,h,l,c,v)) or l<=0 or h<max(o,c) or l>min(o,c) or v<0:raise ValueError('OHLCV invalid')

def collect(symbol,interval):
    if not re.fullmatch('[A-Z0-9]{2,25}USDT',symbol):raise ValueError('USDT symbol required')
    if interval not in ('1m','5m','1h','1d'):raise ValueError('interval')
    duration={'1m':60,'5m':300,'1h':3600,'1d':86400}[interval]
    with httpx.Client(timeout=15,follow_redirects=False) as client:
        meta=client.get('https://data-api.binance.vision/api/v3/exchangeInfo',params={'symbol':symbol});meta.raise_for_status();m=meta.json()['symbols'][0]
        if m['symbol']!=symbol or m['baseAsset']!=symbol[:-4] or m['quoteAsset']!='USDT' or not m['isSpotTradingAllowed']:raise ValueError('Identity mismatch')
        r=client.get('https://data-api.binance.vision/api/v3/klines',params={'symbol':symbol,'interval':interval,'limit':1000});r.raise_for_status();rows=r.json()
    now=int(time.time()*1000)
    closed=[r for r in rows if int(r[0])+duration*1000<=now]
    validate_rows(closed,duration,now)
    with psycopg.connect(os.environ['DATABASE_URL'],connect_timeout=5) as c:
        c.execute("INSERT INTO instruments(exchange,symbol,market_kind,base_asset,quote_asset) VALUES('binance',%s,'spot',%s,'USDT') ON CONFLICT DO NOTHING",(symbol,symbol[:-4]))
        instrument=c.execute("SELECT id FROM instruments WHERE exchange='binance' AND symbol=%s AND market_kind='spot'",(symbol,)).fetchone()[0]
        for r in closed:
            if int(r[6])>=now:continue
            values=[float(v) for v in r[1:6]]
            if not all(math.isfinite(v) for v in values):raise ValueError('nonfinite candle')
            c.execute('INSERT INTO candles(instrument_id,interval_seconds,open_time,open,high,low,close,volume) VALUES(%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT(instrument_id,interval_seconds,open_time) DO UPDATE SET open=excluded.open,high=excluded.high,low=excluded.low,close=excluded.close,volume=excluded.volume,received_at=now()',(instrument,duration,int(r[0]),*values))
if __name__=='__main__':
    for symbol in os.environ.get('VGROP_SYMBOLS','BTCUSDT,ETHUSDT,SOLUSDT,XRPUSDT,DOGEUSDT,BNBUSDT').split(','):
        for interval in ('1m','5m','1h','1d'):collect(symbol.strip(),interval)
