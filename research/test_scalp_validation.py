import unittest
from scalp_validation import Costs,validate
class ScalpValidationTest(unittest.TestCase):
 def rows(self):return [dict(instrument='binance:BTCUSDT:spot:USDT',timestamp=i*60000,label_end=i*60000+300000,actual=i%3,p=[.5,.3,.2],abstain=False,return_bps=1,regime='TREND') for i in range(600)]
 def test_cost_and_purge(self):
  r=validate(self.rows());self.assertLess(r['net_bps_per_sample'],0);self.assertEqual(Costs().round_trip(),16)
  for f in r['folds']:self.assertLess(f['train_label_end'],f['test_start'])
  self.assertEqual(sum(map(sum,r['confusion_matrix_actual_rows'])),r['samples'])
 def test_abstention(self):
  rows=self.rows()
  for r in rows:r['abstain']=True
  r=validate(rows);self.assertEqual(r['abstention_rate'],1);self.assertEqual(r['net_bps_per_sample'],0)
 def test_identity(self):
  rows=self.rows();rows[0]['instrument']='different'
  with self.assertRaises(ValueError):validate(rows)
 def test_invalid(self):
  with self.assertRaises(ValueError):Costs(slippage_bps=-1).round_trip()
  rows=self.rows();rows[1]['label_end']=9
  with self.assertRaises(ValueError):validate(rows)
if __name__=='__main__':unittest.main()
