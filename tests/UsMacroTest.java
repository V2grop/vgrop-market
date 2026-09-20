package com.v2grop.lbankpulse;
import org.json.*;
import java.nio.file.*;
import java.time.*;
import java.io.*;
import java.util.zip.*;
public final class UsMacroTest {
 static JSONObject item(JSONObject r,String id)throws Exception{JSONArray a=r.getJSONArray("items");for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("id").equals(id))return a.getJSONObject(i);throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  if(args.length>0){JSONObject r=UsMacro.parse(Files.readAllBytes(Paths.get(args[0])),LocalDate.now(ZoneOffset.UTC));System.out.println("Live FRED archive parsed: "+r.getJSONArray("items").length()+" series");for(int i=0;i<r.getJSONArray("items").length();i++){JSONObject d=r.getJSONArray("items").getJSONObject(i);System.out.println(d.getString("id")+": "+d.getString("status")+" @ "+d.optString("observation_date"));}return;}
  String csv="observation_date,CPIAUCSL,PAYEMS,UNRATE,DFEDTARU\n2025-02-01,100,,,\n2025-12-01,,100,,\n2026-01-01,,110,4.0,\n2026-02-01,103,113,4.1,\n2026-03-10,,,,3.75\n2027-01-01,999,999,999,999\n";
  JSONObject r=UsMacro.parse(csv.getBytes("UTF-8"),LocalDate.of(2026,3,13));
  if(Math.abs(item(r,"CPIAUCSL").getDouble("value")-3)>1e-8)throw new AssertionError("YOY");
  if(item(r,"PAYEMS").getDouble("value")!=3||item(r,"PAYEMS").getDouble("previous")!=10)throw new AssertionError("Payroll difference");
  if(item(r,"DFEDTARU").getDouble("value")!=3.75)throw new AssertionError("Future contamination");
  if(!item(r,"DGS2").getString("status").equals("unavailable"))throw new AssertionError("Missing series");
  if(item(r,"UNRATE").getBoolean("stale"))throw new AssertionError("Monthly age");
  try{UsMacro.parse("<html>error</html>".getBytes(),LocalDate.now());throw new AssertionError("Accepted invalid data");}catch(IOException good){}
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(out)){z.putNextEntry(new ZipEntry("monthly.csv"));z.write(csv.getBytes("UTF-8"));z.closeEntry();}
  if(item(UsMacro.parse(out.toByteArray(),LocalDate.of(2026,3,13)),"PAYEMS").getDouble("value")!=3)throw new AssertionError("ZIP");
  System.out.println("PASS: US macro CSV/ZIP, YOY, payroll, missing/future data and date semantics");
 }
}
