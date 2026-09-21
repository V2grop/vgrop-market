package com.v2grop.lbankpulse.data
import org.junit.Test
import org.junit.Assert.*
class CacheFreshnessTest {
 @Test fun staleIsNeverFresh() {
  val c=CacheEntry("Bybit:BTCUSDT:spot",60,"candles","Bybit","spot",1000,1100,2100,"[]")
  assertTrue(c.isFresh(1500));assertFalse(c.isFresh(2200));assertFalse(c.isFresh(1000))
 }
}
