package com.v2grop.lbankpulse;

import android.content.Context;
import android.content.SharedPreferences;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Public repository configuration, no tokens, tracking, or advertising SDK. */
final class AnnouncementStore {
    static final String URL_VALUE="https://raw.githubusercontent.com/V2grop/vgrop-market/main/config/announcement.json";
    static final long REFRESH_MS=15L*60*1000;
    private final SharedPreferences prefs;
    AnnouncementStore(Context context) { prefs=context.getSharedPreferences("announcement",Context.MODE_PRIVATE); }
    Announcement current(long now) {
        try {
            Announcement a=Announcement.parse(prefs.getString("raw","{}"));
            return a!=null && a.visible(now,prefs.getLong("fetched",0),prefs.getString("dismissed",""))?a:null;
        } catch(Exception e) { return null; }
    }
    void dismiss(String id) { prefs.edit().putString("dismissed",id).apply(); }
    // Invalid or disabled server content immediately clears a previously active notice.
    void accept(String raw,long now) {
        try { Announcement.parse(raw); prefs.edit().putString("raw",raw).putLong("fetched",now).apply(); }
        catch(Exception e) { clear(); }
    }
    void clear() { prefs.edit().remove("raw").remove("fetched").apply(); }
    void refresh() {
        HttpURLConnection c=null;
        try {
            c=(HttpURLConnection)new URL(URL_VALUE).openConnection();
            c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setInstanceFollowRedirects(false);
            c.setUseCaches(false); c.setRequestProperty("Cache-Control","no-cache");
            c.setRequestProperty("Accept","application/json");
            int status=c.getResponseCode();
            if (status==404 || status==410) { clear(); return; }
            if (status!=200) return; // Network/server failure: bounded offline cache only.
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[1024]; int n;
                while((n=in.read(buffer))!=-1) {
                    if(out.size()+n>16384) {clear();return;}
                    out.write(buffer,0,n);
                }
                accept(new String(out.toByteArray(),StandardCharsets.UTF_8),System.currentTimeMillis());
            }
        } catch(Exception ignored) { /* Keep unexpired cache; never interfere with analysis. */ }
        finally { if(c!=null)c.disconnect(); }
    }
}
