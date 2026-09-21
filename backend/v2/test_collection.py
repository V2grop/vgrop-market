import unittest
from collect import validate_rows
class CollectionTest(unittest.TestCase):
 def test_valid_and_bad(self):
  rows=[[0,1,2,.5,1,1],[60000,1,2,.5,1,1]];validate_rows(rows,60,120000)
  for bad in (rows+rows,[[0,1,.1,.5,1,1]],[[0,1,2,.5,1,-1]]):
   with self.assertRaises(ValueError):validate_rows(bad,60,120000)
