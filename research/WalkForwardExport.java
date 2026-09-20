package com.v2grop.lbankpulse;
import org.json.*;import java.nio.file.*;import java.util.*;
/** CSV rows timestamp_ms,open,high,low,close,volume; closed candles only. */
public class WalkForwardExport{
 public static void main(String[] args)throws Exception{
  if(args.length!=2)throw new IllegalArgumentException("CSV path and horizon hours required");
  int h=Integer.parseInt(args[1]);if(h!=4&&h!=24&&h!=168&&h!=720&&h!=2160)throw new IllegalArgumentException("horizon");
  int bars=h>=720?h/24:h;JSONArray rows=new JSONArray();
  for(String line:Files.readAllLines(Path.of(args[0]))){if(line.isBlank()||line.startsWith("timestamp"))continue;String[] f=line.split(",");if(f.length!=6)throw new IllegalArgumentException("CSV needs six fields");JSONArray r=new JSONArray().put(Long.parseLong(f[0]));for(int j=1;j<6;j++)r.put(Double.parseDouble(f[j]));rows.put(r);}
  System.out.println("timestamp,label_end,actual,up,neutral,down,abstain,return_pct");
  for(int t=399;t+bars<rows.length();t+=bars+1){JSONArray past=new JSONArray();for(int k=0;k<=t;k++)past.put(rows.get(k));JSONObject w=AdaptiveEngine.evaluate(past,h,"BTC",null);if(w==null)continue;
   double entry=rows.getJSONArray(t+1).getDouble(1),end=rows.getJSONArray(t+bars).getDouble(4),ret=end/entry-1;
   double threshold=.5*w.getJSONObject("features").getDouble("atr14")/rows.getJSONArray(t).getDouble(4)*Math.sqrt(bars);
   int label=ret>threshold?0:ret< -threshold?2:1;
   System.out.println(rows.getJSONArray(t).getLong(0)+","+rows.getJSONArray(t+bars).getLong(0)+","+label+","+w.getInt("up")/100.0+","+w.getInt("neutral")/100.0+","+w.getInt("down")/100.0+","+w.getBoolean("abstain")+","+ret*100);
  }
 }
}
