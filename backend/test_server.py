import unittest, time, os, tempfile
from unittest.mock import patch
import server as s
class ResearchTests(unittest.TestCase):
 def test_market_separation(self):
  with patch.object(s,'fetch',return_value={'lastPrice':'100','priceChangePercent':'2','quoteVolume':'500'}) as f:
   self.assertEqual(s.binance('BTC','perp')['kind'],'perp')
   self.assertIn('fapi.binance.com',f.call_args.args[0])
 def test_bybit_fraction(self):
  with patch.object(s,'fetch',return_value={'retCode':0,'result':{'list':[{'lastPrice':'2','price24hPcnt':'0.1','turnover24h':'10'}]}}):
   self.assertEqual(s.bybit('DOGE','spot')['change24h'],10)
 def test_hyper_quote(self):
  with patch.object(s,'fetch',return_value=[{'universe':[{'name':'BTC'}]},[{'markPx':'100','prevDayPx':'80','dayNtlVlm':'1000','openInterest':'5'}]]):
   q=s.hyperliquid('BTC','perp');self.assertEqual(q['quote'],'USDC');self.assertEqual(q['change24h'],25)
 def test_unknown_identity_never_guessed(self):
  with patch.object(s,'fetch') as f:
   self.assertEqual(s.analyze('XTI','perp')['sources'],[]);f.assert_not_called()
 def test_nonfinite_rejected(self):
  for n in ['nan','inf',None]:self.assertIsNone(s.number(n))
 def test_source_failure_isolated(self):
  with patch.object(s,'binance',side_effect=ValueError()),patch.object(s,'bybit',return_value={'price':1,'quote':'USDT'}),patch.object(s,'hyperliquid',side_effect=ValueError()),patch.object(s,'fundamentals',side_effect=ValueError()),patch.object(s,'technical',side_effect=ValueError()),patch.object(s,'lbank',side_effect=ValueError()),patch.object(s,'news',return_value={}):
   r=s.analyze('BTC','spot');self.assertEqual(r['sources'][1]['status'],'ok');self.assertIsNone(r['probabilities'])
 def test_missing_key_no_call(self):
  with patch.dict(os.environ,{'OPENAI_API_KEY':''}),patch.object(s,'fetch') as f:
   with self.assertRaises(ValueError):s.ask('BTC','spot','why')
   f.assert_not_called()
 def test_identity_mismatch(self):
  with patch.object(s,'fetch',return_value={'symbol':'wrong'}):
   with self.assertRaises(ValueError):s.fundamentals('SPX')
 def test_closed_candles_only(self):
  now=int(time.time()*1000)//3600000*3600000
  rows=[[now-(100-i)*3600000,1,1,1,100+i,1,now-(99-i)*3600000-1] for i in range(101)]
  with patch.object(s,'fetch',return_value=rows):
   r=s.technical('BTC','spot');self.assertEqual(r['closed_candles'],100)
   self.assertEqual([x['hours'] for x in r['horizons']],[4,24])
 def test_daily_limit(self):
  with tempfile.TemporaryDirectory() as tmp,patch.dict(os.environ,{'OPENAI_API_KEY':'fake','OPENAI_MODEL':'configured-model','AI_DAILY_REQUEST_LIMIT':'0','PULSE_DB':tmp+'/db'}),patch.object(s,'fetch') as f:
   with self.assertRaises(ValueError):s.ask('BTC','spot','test')
   f.assert_not_called()
if __name__=='__main__':unittest.main()
