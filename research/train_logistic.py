"""Optional candidate trainer, never installs a model in Android.
Requires numpy/scikit-learn/joblib. Explicit chronological partitions per instrument/horizon.
Uses exact-engine JSONL features; output remains candidate even if metrics improve.
"""
import argparse,json,hashlib
FEATURES=['ema9','ema21','ema50','rsi7','rsi14','macd6_13','roc5','return1','body_ratio','wick_balance','atr14','atr_pct','realized_vol_pct','vol_expansion','relative_volume','local_vwap','local_high','local_low','breakout']
def partitions(rows,train_end,val_end,cal_end,test_end):
 if not train_end<val_end<cal_end<test_end:raise ValueError('Cutoffs must increase')
 bounds=[(-1,train_end),(train_end,val_end),(val_end,cal_end),(cal_end,test_end)]
 out=[[r for r in rows if lo<r['timestamp'] and r['label_end']<hi] for lo,hi in bounds]
 if any(len(x)<100 for x in out):raise ValueError('Each purged partition needs 100+ samples')
 if len({r['instrument'] for r in rows})!=1 or any(r['label_end']-r['timestamp']!=300000 for r in rows):raise ValueError('One exact instrument/horizon per candidate')
 return out

def train(path,cutoffs,output):
 import numpy as np,joblib
 from sklearn.pipeline import make_pipeline
 from sklearn.impute import SimpleImputer
 from sklearn.preprocessing import StandardScaler
 from sklearn.linear_model import LogisticRegression
 from scalp_validation import brier
 from walk_forward import calibrate
 with open(path) as f:rows=[json.loads(l) for l in f if l.strip()]
 if any(rows[i]['timestamp']>=rows[i+1]['timestamp'] for i in range(len(rows)-1)):raise ValueError('Unordered samples')
 if any(r.get('feature_schema')!='scalp-features-1' for r in rows):raise ValueError('Schema mismatch')
 tr,va,ca,te=partitions(rows,*cutoffs)
 def xy(a):return np.array([[r['features'].get(k,np.nan) for k in FEATURES] for r in a],dtype=float),np.array([r['actual'] for r in a])
 x,y=xy(tr)
 if len(set(y))!=3:raise ValueError('Training must contain all three classes')
 xv,yv=xy(va);candidates=[]
 for c in (.01,.1,1):
  model=make_pipeline(SimpleImputer(add_indicator=True),StandardScaler(),LogisticRegression(C=c,max_iter=1000,random_state=0));model.fit(x,y);p=model.predict_proba(xv);loss=sum(brier(q,int(t)) for q,t in zip(p,yv))/len(yv);candidates.append((loss,model))
 model=min(candidates,key=lambda z:z[0])[1]
 xc,yc=xy(ca);pc=model.predict_proba(xc)
 temp=min((.5,.75,1,1.5,2,3,5),key=lambda t:sum(brier(calibrate(q,t),int(v)) for q,v in zip(pc,yc)))
 xt,yt=xy(te);pt=[calibrate(q,temp) for q in model.predict_proba(xt)];freq=[sum(y==i)/len(y) for i in range(3)]
 report={'samples':len(te),'brier':sum(brier(q,int(t)) for q,t in zip(pt,yt))/len(yt),'baseline_brier':sum(brier(freq,int(t)) for t in yt)/len(yt),'purged':True,'calibrated':True,'untouched_test':True,'reproducible':True,'net_return':None,'promotion':'blocked: execution-cost evaluation and independent replication required'}
 joblib.dump({'pipeline':model,'temperature':temp,'features':FEATURES},output)
 with open(output,'rb') as f:sha=hashlib.sha256(f.read()).hexdigest()
 with open(path,'rb') as f:data_sha=hashlib.sha256(f.read()).hexdigest()
 metadata=dict(version='logistic-scalp-candidate-1',feature_schema='scalp-features-1',feature_order=FEATURES,asset_class='crypto',instruments=[rows[0]['instrument']],horizon_seconds=300,training_cutoff=cutoffs[0],validation_cutoff=cutoffs[1],calibration_cutoff=cutoffs[2],test_cutoff=cutoffs[3],sha256=sha,dataset_sha256=data_sha,report=report,status='candidate_not_promoted')
 with open(output+'.json','w') as f:json.dump(metadata,f,indent=2)
 print(json.dumps(report,indent=2))
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('samples');p.add_argument('output');p.add_argument('--cutoffs',nargs=4,type=int,required=True);a=p.parse_args();train(a.samples,a.cutoffs,a.output)
