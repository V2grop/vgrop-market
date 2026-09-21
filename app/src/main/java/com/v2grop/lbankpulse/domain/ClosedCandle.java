package com.v2grop.lbankpulse.domain;
public final class ClosedCandle {
 public final long openTime;public final double open,high,low,close,volume;
 public ClosedCandle(long t,double o,double h,double l,double c,double v){openTime=t;open=o;high=h;low=l;close=c;volume=v;}
}
