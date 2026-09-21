"""Optional research-data service. Does not execute trades or claim a trained model."""
import os,secrets
from contextlib import asynccontextmanager
from typing import Annotated,Literal
from fastapi import FastAPI,Depends,HTTPException,Query
from fastapi.security import HTTPBearer,HTTPAuthorizationCredentials
import psycopg
from psycopg.rows import dict_row

@asynccontextmanager
async def lifespan(app):
    if len(os.environ.get('VGROP_ACCESS_TOKEN',''))<32:raise RuntimeError('Configure a random VGROP_ACCESS_TOKEN of at least 32 characters')
    if not os.environ.get('DATABASE_URL'):raise RuntimeError('DATABASE_URL required')
    yield
app=FastAPI(title='VGrop Market data service',version='0.15.0',lifespan=lifespan)
security=HTTPBearer(auto_error=False)
def authorized(c:Annotated[HTTPAuthorizationCredentials|None,Depends(security)]):
    expected=os.environ.get('VGROP_ACCESS_TOKEN','')
    if len(expected)<32 or c is None or not secrets.compare_digest(c.credentials,expected):raise HTTPException(401,'Authentication required')
def db():return psycopg.connect(os.environ['DATABASE_URL'],row_factory=dict_row,connect_timeout=5)
@app.get('/health')
def health():return {'status':'up','model_status':'research_only'}
@app.get('/v1/capabilities',dependencies=[Depends(authorized)])
def capabilities():return {'trained_model':False,'calibrated_probabilities':False,'local_engine':'vgrop-adaptive-1','supported_history':['binance-spot'],'future_runtimes':['tflite','onnx'],'note':'Runtime adapters are contracts only; no model artifact is installed.'}
@app.get('/v1/instruments',dependencies=[Depends(authorized)])
def instruments(limit:int=Query(100,ge=1,le=600)):
    with db() as c:return c.execute('SELECT * FROM instruments ORDER BY id LIMIT %s',(limit,)).fetchall()
@app.get('/v1/candles/{instrument_id}',dependencies=[Depends(authorized)])
def candles(instrument_id:int,interval_seconds:Literal[60,300,3600,86400]=3600,limit:int=Query(1000,ge=1,le=5000)):
    with db() as c:
        rows=c.execute('SELECT open_time,open,high,low,close,volume,received_at FROM candles WHERE instrument_id=%s AND interval_seconds=%s ORDER BY open_time DESC LIMIT %s',(instrument_id,interval_seconds,limit)).fetchall()
    return {'instrument_id':instrument_id,'interval_seconds':interval_seconds,'closed_candles':list(reversed(rows)),'coverage':'stored history; check timestamps before inference'}
