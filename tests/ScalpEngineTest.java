package com.v2grop.lbankpulse;
import com.v2grop.lbankpulse.domain.*;
import java.util.*;import org.json.*;
public final class ScalpEngineTest {
 static final long NOW=1800000000000L;static final InstrumentId ID=new InstrumentId("Bybit","ETHUSDT","ETH","USDT","spot",1);
 static List<ClosedCandle> candles(){List<ClosedCandle>a=new ArrayList<>();for(int i=0;i<180;i++){double p=100+i*.01;a.add(new ClosedCandle(NOW-(180-i)*60000L,p,p+.08,p-.08,p+.01,100));}return a;}
 static OrderBookSnapshot book(long age,double spread,double depth){return new OrderBookSnapshot(ID,NOW-age,NOW-age,101,101+spread,depth,depth,1000,1000);}
 static void check(boolean b){if(!b)throw new AssertionError();}
 static void bad(List<ClosedCandle>a,long now){try{ScalpEngine.validate(a,60000,now);throw new AssertionError("accepted invalid");}catch(IllegalArgumentException ok){}}
 public static void main(String[]args)throws Exception{
  List<ClosedCandle>a=candles();ScalpEngine.validate(a,60000,NOW);JSONObject f=ScalpEngine.features(a);check(f.getDouble("ema9")>f.getDouble("ema21"));check(f.getDouble("rsi7")==100);check(f.getDouble("atr14")>0);
  bad(a,NOW+120000);bad(a,NOW-1);bad(a.subList(0,30),NOW);List<ClosedCandle>b=new ArrayList<>(a);b.remove(40);bad(b,NOW);b=new ArrayList<>(a);b.set(40,b.get(39));bad(b,NOW);b=new ArrayList<>(a);Collections.swap(b,40,41);bad(b,NOW);b=new ArrayList<>(a);b.set(40,new ClosedCandle(b.get(40).openTime,1,0,2,1,1));bad(b,NOW);
  JSONObject r=ScalpEngine.analyze(ID,a,60000,book(0,.01,100000),a,NOW);JSONObject w=r.getJSONObject("scenario_weights");check(w.getInt("up")+w.getInt("neutral")+w.getInt("down")==100);
  check(ScalpEngine.analyze(ID,a,60000,book(11000,.01,100000),a,NOW).getBoolean("abstain"));
  check(ScalpEngine.analyze(ID,a,60000,book(0,1,100000),a,NOW).getBoolean("abstain"));
  check(ScalpEngine.analyze(ID,a,60000,book(0,.01,100),a,NOW).getString("regime").equals("LOW LIQUIDITY"));
  b=new ArrayList<>(a);ClosedCandle last=b.get(179);b.set(179,new ClosedCandle(last.openTime,last.open,last.high*1.05,last.low,last.close*1.05,100));
  check(ScalpEngine.analyze(ID,a,60000,book(0,.01,100000),b,NOW).getString("reason").contains("BTC"));
  check(!ScalpEngine.analyze(ID,a,60000,null,a,NOW).isNull("scenario_weights"));
  check(!ScalpEngine.analyze(ID,a,60000,null,a,NOW+120000).has("scenario_weights"));
  check(!ID.equals(new InstrumentId("Bybit","ETHUSDT","ETH","USDT","perp",1)));
  check(new ScalpRepository((u,j)->{throw new AssertionError("unverified symbol network call");}).analyze("xti_usdt","spot").getBoolean("abstain"));
  System.out.println("ScalpEngineTest passed: features, identity, closed/stale/gap/duplicate/order, book, spread, liquidity, BTC shock, sums");
 }
}
