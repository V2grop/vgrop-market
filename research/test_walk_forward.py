import unittest
from walk_forward import validate
class TestChronology(unittest.TestCase):
 def test_purge(self):
  rows=[dict(timestamp=i*10,label_end=i*10+20,actual=i%3,p=[.3,.4,.3],abstain=True,return_pct=0) for i in range(100)]
  report=validate(rows)
  self.assertTrue(all(f['train_end']<f['test_start'] for f in report['folds']))
  self.assertEqual(report['mean_net_return_pct_per_sample'],0)
 def test_small(self):
  with self.assertRaises(ValueError):validate([])
if __name__=='__main__':unittest.main()
