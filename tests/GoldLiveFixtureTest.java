package com.v2grop.lbankpulse;
import org.json.*;
import java.nio.file.*;
public class GoldLiveFixtureTest {
 public static void main(String[] args)throws Exception{
  String raw=Files.readString(Path.of(args[0]));
  LocalResearch research=new LocalResearch((url,body)->{
   if(url.contains("lbkex"))return raw;
   throw new java.io.IOException("Other providers unavailable");
  });
  JSONObject result=research.goldTechnical("perp");
  for(int i=0;i<3;i++){
   JSONObject h=result.getJSONArray("horizons").getJSONObject(i),w=h.getJSONObject("scenario_weights");
   if(w.getInt("up")+w.getInt("neutral")+w.getInt("down")!=100)throw new AssertionError("sum");
   System.out.println(h.getInt("hours")+"h "+w.getInt("up")+"/"+w.getInt("neutral")+"/"+w.getInt("down"));
  }
  if(!result.getString("kind").equals("spot")||!result.getString("basis_note").contains("جایگزین"))throw new AssertionError("Wrong basis");
  System.out.println("PASS real LBank closed candle fixture and explicit spot fallback");
 }
}
