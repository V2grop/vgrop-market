"""Fail-closed artifact gate; explicit human approval remains mandatory."""
import hashlib,json,math
REQUIRED=('version','feature_schema','feature_order','asset_class','instruments','horizon_seconds','training_cutoff','validation_cutoff','calibration_cutoff','test_cutoff','sha256','report')
def verify(path,metadata,expected_schema):
 if any(k not in metadata for k in REQUIRED):raise ValueError('Incomplete model provenance')
 with open(path,'rb') as f:digest=hashlib.sha256(f.read()).hexdigest()
 if digest!=metadata['sha256'] or metadata['feature_schema']!=expected_schema:raise ValueError('Artifact/schema mismatch')
 cuts=[metadata[k] for k in ('training_cutoff','validation_cutoff','calibration_cutoff','test_cutoff')]
 if not all(isinstance(x,int) for x in cuts) or any(a>=b for a,b in zip(cuts,cuts[1:])):raise ValueError('Invalid temporal partitions')
 r=metadata['report']
 if not r.get('purged') or not r.get('untouched_test') or not r.get('calibrated') or r.get('samples',0)<1000 or not r.get('reproducible'):raise ValueError('Validation gates incomplete')
 for k in ('brier','baseline_brier','net_return'): 
  if k not in r or not math.isfinite(r[k]):raise ValueError('Invalid metrics')
 if r['brier']>=r['baseline_brier'] or r['net_return']<=0:raise ValueError('No demonstrated improvement after costs')
 return {'status':'eligible_for_human_review','automatic_promotion':False,'sha256':digest}
