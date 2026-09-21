package com.v2grop.lbankpulse.domain;
import java.util.Objects;
/** Identity includes exchange, settlement quote and multiplier. Never ticker-only. */
public final class InstrumentId {
 public final String exchange,symbol,base,quote,kind; public final double multiplier;
 public InstrumentId(String exchange,String symbol,String base,String quote,String kind,double multiplier){
  if(exchange==null||symbol==null||base==null||quote==null||!(kind.equals("spot")||kind.equals("perp"))||!Double.isFinite(multiplier)||multiplier<=0)throw new IllegalArgumentException("Invalid identity");
  this.exchange=exchange;this.symbol=symbol;this.base=base;this.quote=quote;this.kind=kind;this.multiplier=multiplier;
 }
 public String key(){return exchange+":"+symbol+":"+kind+":"+base+":"+quote+":"+multiplier;}
 @Override public boolean equals(Object o){return o instanceof InstrumentId&&key().equals(((InstrumentId)o).key());}
 @Override public int hashCode(){return Objects.hash(key());}
}
