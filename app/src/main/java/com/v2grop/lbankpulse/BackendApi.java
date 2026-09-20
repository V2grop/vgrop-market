package com.v2grop.lbankpulse;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
public final class BackendApi {
    public static JSONObject call(String base,String token,String path,JSONObject body) throws Exception {
        URL url=new URL(base.replaceAll("/+$", "")+path);
        if(!url.getProtocol().equals("https") || url.getUserInfo()!=null) throw new Exception("نشانی سرور باید HTTPS باشد.");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();
        c.setInstanceFollowRedirects(false); c.setConnectTimeout(12000); c.setReadTimeout(90000);
        c.setRequestProperty("Authorization","Bearer "+token); c.setRequestProperty("Content-Type","application/json");
        try {
            if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);try(java.io.OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int code=c.getResponseCode(); InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();
            if(stream==null) throw new Exception("خطای اتصال: "+code);
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            try(InputStream in=stream){byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1){bytes.write(chunk,0,n);if(bytes.size()>2000000)throw new Exception("پاسخ بیش از حد بزرگ است.");}}
            JSONObject result=new JSONObject(bytes.toString("UTF-8"));
            if(code<200||code>=300)throw new Exception(result.optString("error","خطای سرور: "+code));
            return result;
        } finally {c.disconnect();}
    }
}
