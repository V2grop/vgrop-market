package com.v2grop.lbankpulse;
import java.nio.file.*;import java.util.*;import org.json.*;import com.v2grop.lbankpulse.domain.*;
/** Exact on-device engine replay. No invented historical book. Missing book means abstention. */
public final class ScalpExport {
 public static void main(String[]args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("Usage: ScalpExport candles.csv manifest.json");
  byte[] bytes=Files.readAllBytes(Paths.get(args[0]));JSONObject m=new JSONObject(Files.readString(Paths.get(args[1])));
  StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format("%02x",b&255));if(!hash.toString().equals(m.getString("sha256")))throw new IllegalArgumentException("Dataset hash mismatch");
  long interval=m.getLong("interval_ms");if(interval!=60000&&interval!=300000)throw new IllegalArgumentException("Scalp interval required");
  InstrumentId id=new InstrumentId(m.getString("exchange"),m.getString("symbol"),m.getString("base"),m.getString("quote"),m.getString("kind"),m.getDouble("contract_multiplier"));
  List<ClosedCandle>a=new ArrayList<>();String[]lines=new String(bytes,java.nio.charset.StandardCharsets.UTF_8).split("\\R");for(int i=1;i<lines.length;i++){if(lines[i].isBlank())continue;String[]v=lines[i].split(",");a.add(new ClosedCandle(Long.parseLong(v[0]),Double.parseDouble(v[1]),Double.parseDouble(v[2]),Double.parseDouble(v[3]),Double.parseDouble(v[4]),Double.parseDouble(v[5])));}
  int steps=(int)(300000/interval);
  for(int i=120;i+steps<=a.size();i++){
   List<ClosedCandle>history=a.subList(Math.max(0,i-240),i);long asof=a.get(i-1).openTime+interval;
   List<ClosedCandle>btc=id.base.equals("BTC")?history:Collections.emptyList();JSONObject result=ScalpEngine.analyze(id,history,interval,null,btc,asof);if(!result.has("scenario_weights"))continue;
   // Entry at next-bar open, exit at fifth-minute close; guard future path continuity separately.
   for(int j=i;j<i+steps;j++)if(a.get(j).openTime!=asof+(j-i)*interval)throw new IllegalArgumentException("Label gap");
   double entry=a.get(i).open,exit=a.get(i+steps-1).close,ret=(exit/entry-1)*10000,low=entry,high=entry;
   for(int j=i;j<i+steps;j++){low=Math.min(low,a.get(j).low);high=Math.max(high,a.get(j).high);}
   JSONObject w=result.getJSONObject("scenario_weights");double roc=result.getJSONObject("features").getDouble("roc5");
   System.out.println(new JSONObject().put("instrument",id.key()).put("timestamp",asof).put("label_end",asof+300000).put("actual",ret>10?0:ret< -10?2:1).put("p",new JSONArray().put(w.getInt("up")/100.0).put(w.getInt("neutral")/100.0).put(w.getInt("down")/100.0)).put("abstain",result.getBoolean("abstain")).put("return_bps",ret).put("regime",result.getString("regime")).put("momentum_class",roc>.1?0:roc<-.1?2:1).put("adverse_long_bps",(1-low/entry)*10000).put("adverse_short_bps",(high/entry-1)*10000).put("features",result.getJSONObject("features")).put("feature_schema",ScalpEngine.SCHEMA).put("dataset_sha256",hash.toString()));
  }
 }
}
