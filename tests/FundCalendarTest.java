package com.v2grop.lbankpulse;
import org.json.*;import java.time.*;import java.nio.file.*;
public class FundCalendarTest{
 static void check(boolean x){if(!x)throw new AssertionError();}
 public static void main(String[] args)throws Exception{
  LocalDate day=LocalDate.of(2026,9,14);
  JSONObject e=new JSONObject().put("currencyFlag","US").put("importance","3").put("type","event").put("time","2026-09-14T12:30:00Z").put("event","CPI").put("occurrenceId",99);
  JSONArray rows=new JSONArray().put(e).put(e).put(new JSONObject(e.toString()).put("currencyFlag","GB")).put(new JSONObject(e.toString()).put("importance","2"));
  JSONObject dates=new JSONObject().put(day.toString(),rows);
  JSONObject json=new JSONObject().put("props",new JSONObject().put("pageProps",new JSONObject().put("state",new JSONObject().put("economicCalendarStore",new JSONObject().put("calendarEventsByDate",dates)))));
  String html="<script id=\"__NEXT_DATA__\" type=\"application/json\">"+json+"</script>";
  check(FundCalendar.parse(html,day).length()==1);
  try{FundCalendar.parse(html,day.plusDays(1));throw new AssertionError();}catch(IllegalArgumentException expected){}
  try{FundCalendar.parse("<html>blocked</html>",day);throw new AssertionError();}catch(IllegalArgumentException expected){}
  check(!FundCalendar.due(day.atTime(8,29).atZone(FundCalendar.NY),8,30,""));
  check(FundCalendar.due(day.atTime(8,30).atZone(FundCalendar.NY),8,30,""));
  check(!FundCalendar.due(day.atTime(9,0).atZone(FundCalendar.NY),8,30,day.toString()));
  check(!FundCalendar.due(day.minusDays(1).atTime(9,0).atZone(FundCalendar.NY),8,30,""));
  check(day.atTime(8,30).atZone(FundCalendar.NY).getOffset().getTotalSeconds()==-14400);
  check(LocalDate.of(2026,12,14).atTime(8,30).atZone(FundCalendar.NY).getOffset().getTotalSeconds()==-18000);
  if(args.length>0)check(FundCalendar.parse(Files.readString(Path.of(args[0])),LocalDate.of(2026,9,13)).length()==0);
  System.out.println("PASS US/high-impact filtering, deduplication, source failures, date coverage, schedule and DST; live Sunday page parsed");
 }
}
