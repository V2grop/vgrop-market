import unittest
from dataset import check
class DatasetTest(unittest.TestCase):
 def test_gap_and_ohlc(self):
  check([[0,1,2,.5,1,1],[60000,1,2,.5,1,1]],60000,120000)
  for rows in ([[0,1,2,.5,1,1],[120000,1,2,.5,1,1]],[[0,1,.2,.5,1,1]]):
   with self.assertRaises(ValueError):check(rows,60000,180000)
