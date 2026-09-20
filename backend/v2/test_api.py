import os,unittest
from unittest.mock import patch
from fastapi.testclient import TestClient
from app import app
class ApiTest(unittest.TestCase):
 def test_access(self):
  with patch.dict(os.environ,{'VGROP_ACCESS_TOKEN':'x'*40,'DATABASE_URL':'postgresql://unused'}):
   with TestClient(app) as c:
    self.assertEqual(c.get('/health').status_code,200)
    self.assertEqual(c.get('/v1/capabilities').status_code,401)
    r=c.get('/v1/capabilities',headers={'Authorization':'Bearer '+'x'*40})
    self.assertFalse(r.json()['trained_model'])
    self.assertEqual(c.get('/v1/candles/1?limit=999999',headers={'Authorization':'Bearer '+'x'*40}).status_code,422)
 def test_fail_closed(self):
  with patch.dict(os.environ,{'VGROP_ACCESS_TOKEN':''}):
   with self.assertRaises(RuntimeError):
    with TestClient(app):pass
if __name__=='__main__':unittest.main()
