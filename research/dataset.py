"""Reproducible multi-asset OHLCV dataset with provenance; six reviewed Binance spot mappings.
Download separate date windows. No exchange joins. Network collection is never an offline CI gate.
"""
import argparse,csv,hashlib,json,math,time,urllib.request,urllib.parse
ASSETS={'BTC','ETH','SOL','XRP','DOGE','BNB'}
INTERVALS={'1m':60000,'5m':300000,'1h':3600000,'1d':86400000}
def check(rows,interval,end):
 for i,r in enumerate(rows):
  t=int(r[0]);o,h,l,c,v=map(float,r[1:6])
  if t%interval or t+interval>end or (i and t-int(rows[i-1][0])!=interval):raise ValueError('Open, gap, duplicate or unordered candle')
  if not all(math.isfinite(x) for x in (o,h,l,c,v)) or l<=0 or h<max(o,c) or l>min(o,c) or v<0:raise ValueError('Invalid OHLCV')
def collect(asset,interval,start,end,path):
 if asset not in ASSETS or interval not in INTERVALS or not 0<=start<end<=int(time.time()*1000):raise ValueError('Invalid verified scope')
 def get(endpoint,params):
  with urllib.request.urlopen('https://data-api.binance.vision/api/v3/'+endpoint+'?'+urllib.parse.urlencode(params),timeout=20) as r:return json.load(r)
 symbol=asset+'USDT';meta=get('exchangeInfo',{'symbol':symbol})['symbols'][0]
 if (meta['symbol'],meta['baseAsset'],meta['quoteAsset'])!=(symbol,asset,'USDT') or not meta['isSpotTradingAllowed']:raise ValueError('Instrument identity')
 rows=[];cursor=start
 while cursor<end:
  page=get('klines',{'symbol':symbol,'interval':interval,'startTime':cursor,'endTime':end-1,'limit':1000})
  if not page:break
  rows.extend(r for r in page if int(r[0])+INTERVALS[interval]<=end)
  nxt=int(page[-1][0])+INTERVALS[interval]
  if nxt<=cursor:raise ValueError('Pagination stalled')
  cursor=nxt;time.sleep(.15)
 if not rows:raise ValueError('Empty history')
 check(rows,INTERVALS[interval],end)
 with open(path,'w') as f:
  w=csv.writer(f);w.writerow(['timestamp_ms','open','high','low','close','volume']);w.writerows(r[:6] for r in rows)
 with open(path,'rb') as f:digest=hashlib.sha256(f.read()).hexdigest()
 manifest={'schema':'ohlcv-1','exchange':'Binance','symbol':symbol,'base':asset,'quote':'USDT','kind':'spot','contract_multiplier':1,'interval_ms':INTERVALS[interval],'requested_start':start,'requested_end':end,'actual_start':int(rows[0][0]),'actual_end':int(rows[-1][0])+INTERVALS[interval],'retrieved_at':int(time.time()*1000),'source':'https://data-api.binance.vision/api/v3/klines','sha256':digest,'rows':len(rows)}
 with open(path+'.manifest.json','w') as f:json.dump(manifest,f,indent=2)
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('asset');p.add_argument('interval');p.add_argument('start_ms',type=int);p.add_argument('end_ms',type=int);p.add_argument('output');a=p.parse_args();collect(a.asset,a.interval,a.start_ms,a.end_ms,a.output)
