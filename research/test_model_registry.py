import unittest,tempfile,hashlib
from model_registry import verify
from train_logistic import partitions
class ModelGateTest(unittest.TestCase):
 def test_fail_closed(self):
  with self.assertRaises(ValueError):verify('/not-used',{},'scalp-features-1')
 def test_purged_partitions(self):
  rows=[dict(instrument='BTC',timestamp=i*60000,label_end=i*60000+300000) for i in range(1000)]
  p=partitions(rows,15000000,30000000,45000000,60000000)
  for i in range(3):self.assertLess(max(x['label_end'] for x in p[i]),min(x['timestamp'] for x in p[i+1]))
