package com.v2grop.lbankpulse;
import org.json.*;
public class HandoffTest {
 public static void main(String[] args)throws Exception{
  JSONObject r=new JSONObject().put("symbol","XAUT").put("kind","perp").put("token","DO_NOT_EXPORT").put("sources",new JSONArray().put(new JSONObject().put("name","technical").put("status","unavailable")));
  String out=ReportHandoff.text(r);
  if(out.contains("DO_NOT_EXPORT")||!out.contains("XAUT")||!out.contains("unavailable"))throw new AssertionError();
  System.out.println("PASS handoff excludes credentials and preserves missing-data status");
 }
}
