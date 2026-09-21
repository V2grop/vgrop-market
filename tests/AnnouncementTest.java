package com.v2grop.lbankpulse;
import org.json.JSONObject;
public final class AnnouncementTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    static JSONObject fixture()throws Exception{return new JSONObject().put("schema_version",1).put("enabled",true).put("id","one").put("title","سلام").put("text","خبر جدید").put("url","https://t.me/V2grop");}
    static void rejects(JSONObject o)throws Exception{try{Announcement.parse(o.toString());throw new AssertionError("accepted invalid notice");}catch(IllegalArgumentException|org.json.JSONException expected){}}
    public static void main(String[]args)throws Exception{
        check(Announcement.parse("{}") == null);
        check(Announcement.parse("{\"enabled\":false}") == null);
        check(Announcement.parse("{\"enabled\":\"true\"}") == null);
        long now=1800000000000L;
        Announcement a=Announcement.parse(fixture().toString());
        check(a.visible(now,now,""));check(!a.visible(now,now,"one"));
        check(!a.visible(now+Announcement.CACHE_TTL_MS,now,""));check(!a.visible(now,now+1,""));
        a=Announcement.parse(fixture().put("starts_at",now/1000+10).put("ends_at",now/1000+20).toString());
        check(!a.visible(now,now,""));check(a.visible(now+10000,now,""));check(!a.visible(now+20000,now,""));
        for(String url:new String[]{"http://evil.test","javascript:alert(1)","intent://x","https://user:pass@example.com","https:///missing"})rejects(fixture().put("url",url));
        rejects(fixture().put("schema_version",2));rejects(fixture().put("text","x".repeat(361)));
        rejects(fixture().put("title"," "));rejects(fixture().put("starts_at",20).put("ends_at",10));
        System.out.println("AnnouncementTest passed: defaults, HTTPS, schema, lengths, schedule, dismissal and cache expiry");
    }
}
