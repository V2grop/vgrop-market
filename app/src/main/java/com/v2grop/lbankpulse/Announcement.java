package com.v2grop.lbankpulse;

import java.net.URI;
import org.json.JSONObject;

/** Plain-text, fail-closed remote notice. No HTML, scripts, or remote layout. */
final class Announcement {
    static final long CACHE_TTL_MS = 24L * 60 * 60 * 1000;
    final String id, title, text, link, button;
    final long startsAt, endsAt;

    private Announcement(String id, String title, String text, String link, String button,
                         long startsAt, long endsAt) {
        this.id=id; this.title=title; this.text=text; this.link=link; this.button=button;
        this.startsAt=startsAt; this.endsAt=endsAt;
    }

    static Announcement parse(String raw) throws Exception {
        if (raw == null || raw.length() > 16384) throw new IllegalArgumentException("Notice too large");
        JSONObject o = new JSONObject(raw);
        if (!Boolean.TRUE.equals(o.opt("enabled"))) return null;
        if (o.getInt("schema_version") != 1) throw new IllegalArgumentException("Unsupported schema");
        String id=field(o,"id",80,true), title=field(o,"title",80,true);
        String text=field(o,"text",360,true), link=field(o,"url",2048,false);
        String button=field(o,"button",40,false);
        if (!link.isEmpty()) {
            URI uri=new URI(link);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getUserInfo()!=null)
                throw new IllegalArgumentException("HTTPS link required");
        }
        long start=o.has("starts_at")?o.getLong("starts_at"):0;
        long end=o.has("ends_at")?o.getLong("ends_at"):0;
        if (start<0 || end<0 || (end>0 && end<=start)) throw new IllegalArgumentException("Invalid schedule");
        return new Announcement(id,title,text,link,button.isEmpty()?"بیشتر بدانید ↗":button,start,end);
    }

    private static String field(JSONObject o,String key,int max,boolean required) throws Exception {
        Object value=o.opt(key);
        if (value==null && !required) return "";
        if (!(value instanceof String)) throw new IllegalArgumentException("Invalid "+key);
        String s=((String)value).trim();
        if (s.length()>max || (required && s.isEmpty())) throw new IllegalArgumentException("Invalid "+key);
        return s;
    }

    boolean visible(long nowMs,long fetchedMs,String dismissedId) {
        long seconds=nowMs/1000;
        return nowMs>=fetchedMs && nowMs-fetchedMs<CACHE_TTL_MS &&
            seconds>=startsAt && (endsAt==0 || seconds<endsAt) && !id.equals(dismissedId);
    }
}
