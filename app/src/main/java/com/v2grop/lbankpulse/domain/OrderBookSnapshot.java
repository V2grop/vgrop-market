package com.v2grop.lbankpulse.domain;
public final class OrderBookSnapshot {
 public final InstrumentId instrument; public final long sourceTime,retrievedAt;public final double bid,ask,bidDepth,askDepth,largestBid,largestAsk;
 public OrderBookSnapshot(InstrumentId id,long sourceTime,long retrievedAt,double bid,double ask,double bidDepth,double askDepth,double largestBid,double largestAsk){this.instrument=id;this.sourceTime=sourceTime;this.retrievedAt=retrievedAt;this.bid=bid;this.ask=ask;this.bidDepth=bidDepth;this.askDepth=askDepth;this.largestBid=largestBid;this.largestAsk=largestAsk;}
 public double spreadBps(){return (ask-bid)/((bid+ask)/2)*10000;}
}
