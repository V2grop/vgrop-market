"""Purged walk-forward evaluation of timestamped exact-engine exports. No auto promotion.
Rows: instrument, timestamp, label_end (ms), actual (0/1/2), p, abstain,
return_bps, regime; optional adverse_long_bps/adverse_short_bps.
Costs are explicit round-trip assumptions, NOT observed executions.
"""
import math,statistics
from dataclasses import dataclass
@dataclass(frozen=True)
class Costs:
    maker_bps:float=2
    taker_bps:float=5
    spread_bps:float=2
    slippage_bps:float=2
    funding_bps:float=0
    execution:str='taker'
    def round_trip(self):
        vals=(self.maker_bps,self.taker_bps,self.spread_bps,self.slippage_bps,self.funding_bps)
        if not all(math.isfinite(v) and v>=0 for v in vals) or self.execution not in ('maker','taker'):raise ValueError('Invalid cost assumptions')
        return 2*(self.taker_bps if self.execution=='taker' else self.maker_bps)+self.spread_bps+2*self.slippage_bps+self.funding_bps

def brier(p,y):return sum((v-(i==y))**2 for i,v in enumerate(p))
def mean(x):return statistics.mean(x) if x else None
def validate(rows,costs=Costs(),min_train=120,fold_size=60):
    cost=costs.round_trip()
    if len({r['instrument'] for r in rows})!=1:raise ValueError('Evaluate instruments independently')
    for i,r in enumerate(rows):
        if r['label_end']!=r['timestamp']+300000:raise ValueError('Five-minute labels required')
        if i and r['timestamp']<=rows[i-1]['timestamp']:raise ValueError('Unordered/duplicate timestamps')
        if len(r['p'])!=3 or any(not math.isfinite(v) or v<0 for v in r['p']) or abs(sum(r['p'])-1)>1e-6:raise ValueError('Invalid weights')
        if r['actual'] not in (0,1,2) or not math.isfinite(r['return_bps']):raise ValueError('Invalid label')
    evaluated=[];folds=[]
    for start in range(min_train,len(rows),fold_size):
        test=rows[start:start+fold_size]
        if len(test)<fold_size:continue
        train=[r for r in rows[:start] if r['label_end']<test[0]['timestamp']]
        if len(train)<min_train:continue
        freq=[sum(r['actual']==i for r in train)/len(train) for i in range(3)]
        folds.append({'train_label_end':max(r['label_end'] for r in train),'test_start':test[0]['timestamp'],'test_end':test[-1]['label_end'],'samples':len(test)})
        for r in test:evaluated.append((r,freq))
    if len(folds)<2:raise ValueError('At least two purged test periods required')
    matrix=[[0]*3 for _ in range(3)];scores=[];base=[];neutral=[];momentum=[];uniform=[];gross=[];net=[];fees=[];rel=[[] for _ in range(10)];regimes={};abstain=0;last_exit=-1;mae=[]
    for r,freq in evaluated:
        p=r['p'];y=r['actual'];pred=max(range(3),key=lambda i:p[i]);matrix[y][pred]+=1
        scores.append(brier(p,y));base.append(brier(freq,y));neutral.append(brier([0,1,0],y));uniform.append(brier([1/3]*3,y))
        m=r.get('momentum_class',1);momentum.append(brier([int(i==m) for i in range(3)],y))
        rel[min(9,int(max(p)*10))].append((max(p),int(pred==y)))
        abstain+=int(r['abstain']);side=0 if r['abstain'] or pred==1 or r['timestamp']<last_exit else (1 if pred==0 else -1)
        g=side*r['return_bps'];c=cost if side else 0;gross.append(g);fees.append(c);net.append(g-c)
        if side:last_exit=r['label_end']
        key='adverse_long_bps' if side==1 else 'adverse_short_bps'
        if side and key in r:mae.append(r[key])
        regimes.setdefault(r['regime'],[]).append({'brier':scores[-1],'net':net[-1],'abstain':r['abstain']})
    return {'status':'research_only_not_promoted','instrument':rows[0]['instrument'],'samples':len(evaluated),'folds':folds,'brier':mean(scores),'baseline_brier':mean(base),'always_neutral_brier':mean(neutral),'momentum_brier':mean(momentum),'uniform_brier':mean(uniform),'directional_accuracy':sum(matrix[i][i] for i in range(3))/len(evaluated),'confusion_matrix_actual_rows':matrix,'abstention_rate':abstain/len(evaluated),'gross_bps_per_sample':mean(gross),'cost_bps_per_sample':mean(fees),'net_bps_per_sample':mean(net),'max_adverse_excursion_bps':max(mae) if mae else None,'cost_assumptions':vars(costs),'reliability':[{'count':len(b),'confidence':mean([x[0] for x in b]),'accuracy':mean([x[1] for x in b])} for b in rel],'regimes':{k:{'samples':len(v),'brier':mean([x['brier'] for x in v]),'net_bps':mean([x['net'] for x in v]),'abstention_rate':mean([int(x['abstain']) for x in v])} for k,v in regimes.items()},'limitations':'Weights uncalibrated. Historical books/trades required to validate full microstructure. No overlapping simulated positions; no fill model; not evidence of profitability.'}
if __name__=='__main__':
    import argparse,json
    p=argparse.ArgumentParser();p.add_argument('samples');p.add_argument('--maker-bps',type=float,default=2);p.add_argument('--taker-bps',type=float,default=5);p.add_argument('--spread-bps',type=float,default=2);p.add_argument('--slippage-bps',type=float,default=2);p.add_argument('--funding-bps',type=float,default=0);p.add_argument('--execution',choices=['maker','taker'],default='taker');a=p.parse_args()
    with open(a.samples) as f:rows=[json.loads(line) for line in f if line.strip()]
    print(json.dumps(validate(rows,Costs(a.maker_bps,a.taker_bps,a.spread_bps,a.slippage_bps,a.funding_bps,a.execution)),indent=2))
