"""Rolling calibration study of fixed model scores, never production promotion.
Input is WalkForwardExport CSV. Labels/entry returns must be produced by that tool.
"""
import csv,json,math,sys

def calibrate(p,t):
    q=[max(x,1e-9)**(1/t) for x in p];s=sum(q);return [x/s for x in q]
def brier(p,y):return sum((x-(i==y))**2 for i,x in enumerate(p))
def validate(rows,min_train=30,fold_size=10):
    if any(rows[i]['timestamp']>=rows[i+1]['timestamp'] for i in range(len(rows)-1)):raise ValueError('Unordered samples')
    scores=[];baseline=[];raw=[];trades=[];folds=[]
    for start in range(min_train,len(rows),fold_size):
        test=rows[start:start+fold_size]
        if len(test)<fold_size:break
        train=[r for r in rows[:start] if r['label_end']<test[0]['timestamp']]
        if len(train)<min_train:continue
        temps=[.5,.75,1,1.5,2,3,5]
        temp=min(temps,key=lambda t:sum(brier(calibrate(r['p'],t),r['actual']) for r in train))
        freq=[sum(r['actual']==i for r in train)/len(train) for i in range(3)]
        folds.append({'train_end':max(r['label_end'] for r in train),'test_start':test[0]['timestamp'],'temperature':temp,'samples':len(test)})
        for r in test:
            p=calibrate(r['p'],temp);scores.append(brier(p,r['actual']));raw.append(brier(r['p'],r['actual']));baseline.append(brier(freq,r['actual']))
            # Long/short diagnostic, flat when abstaining; fixed 20bps round trip.
            side=0 if r['abstain'] else (1 if p[0]>max(p[1:]) else -1 if p[2]>max(p[:2]) else 0)
            trades.append(side*r['return_pct']-.2 if side else 0)
    if not scores:raise ValueError('Insufficient independent samples for calibration and test folds')
    return {'status':'research_only_not_promoted','folds':folds,'test_samples':len(scores),'brier':sum(scores)/len(scores),'raw_brier':sum(raw)/len(raw),'past_frequency_baseline_brier':sum(baseline)/len(baseline),'mean_net_return_pct_per_sample':sum(trades)/len(trades),'cost_bps_round_trip':20,'limits':'Single asset; fixed heuristic; no slippage model beyond fixed cost, no liquidity/funding model; not evidence of general performance.'}
def load(path):
    out=[]
    with open(path) as f:
        for r in csv.DictReader(f):out.append({'timestamp':int(r['timestamp']),'label_end':int(r['label_end']),'actual':int(r['actual']),'p':[float(r[k]) for k in ('up','neutral','down')],'abstain':r['abstain']=='true','return_pct':float(r['return_pct'])})
    return out
if __name__=='__main__':print(json.dumps(validate(load(sys.argv[1])),indent=2))
