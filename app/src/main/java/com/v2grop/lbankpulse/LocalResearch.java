package com.v2grop.lbankpulse;

import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import java.text.SimpleDateFormat;

/** Public-data-only research. No backend address, token or AI dependency. */
public final class LocalResearch {
    interface Transport { String read(String url,JSONObject body) throws Exception; }
    interface Job { JSONObject run() throws Exception; }
    private final Transport net;
    private final Map<String,String> ids=new HashMap<>();
    public LocalResearch(){this(LocalResearch::http);}
    LocalResearch(Transport transport){
        net=transport;
        String[] symbols={"BTC","ETH","DOGE","SOL","XRP","FARTCOIN","SPX","BNB","SUI","SEI","PEPE","NOT","AUCTION","XAUT","VINE"};
        String[] coins={"bitcoin","ethereum","dogecoin","solana","ripple","fartcoin","spx6900","binancecoin","sui","sei-network","pepe","notcoin","auction","tether-gold","vine"};
        for(int i=0;i<symbols.length;i++)ids.put(symbols[i],coins[i]);
    }
    static long now(){return System.currentTimeMillis()/1000;}
    static JSONObject obj(Object... pairs)throws Exception{JSONObject d=new JSONObject();for(int i=0;i<pairs.length;i+=2)d.put((String)pairs[i],pairs[i+1]==null?JSONObject.NULL:pairs[i+1]);return d;}
    static Double num(Object x){try{double d=Double.parseDouble(String.valueOf(x));return Double.isFinite(d)?d:null;}catch(Exception e){return null;}}
    JSONObject get(String url)throws Exception{return new JSONObject(net.read(url,null));}
    static String http(String endpoint,JSONObject body)throws Exception{
        URL url=new URL(endpoint);if(!url.getProtocol().equals("https"))throw new IOException("HTTPS required");
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setConnectTimeout(7000);c.setReadTimeout(9000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent","MarketPulse/0.3");c.setRequestProperty("Accept","application/json, application/xml, text/xml");
        try{
            if(body!=null){c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>4000000)throw new IOException("Response too large");}return out.toString("UTF-8");
            }
        }finally{c.disconnect();}
    }
    JSONObject quote(String name,String kind,String unit,Object price,Object change,Object vol,String url)throws Exception{
        Double p=num(price);if(p==null||p<=0)throw new IOException("Invalid price");
        return obj("source",name,"kind",kind,"quote",unit,"price",p,"change24h",num(change),"volume24h",num(vol),"url",url,"retrieved_at",now());
    }
    JSONObject binance(String s,String kind)throws Exception{
        String base=kind.equals("spot")?"https://data-api.binance.vision/api/v3":"https://fapi.binance.com/fapi/v1";
        String url=base+"/ticker/24hr?symbol="+s+"USDT";JSONObject d=get(url);
        return quote("Binance",kind,"USDT",d.get("lastPrice"),d.opt("priceChangePercent"),d.opt("quoteVolume"),url);
    }
    JSONObject bybit(String s,String kind)throws Exception{
        String url="https://api.bybit.com/v5/market/tickers?category="+(kind.equals("spot")?"spot":"linear")+"&symbol="+s+"USDT";
        JSONObject root=get(url);if(root.optInt("retCode",-1)!=0)throw new IOException("Provider rejected request");
        JSONObject d=root.getJSONObject("result").getJSONArray("list").getJSONObject(0);Double change=num(d.opt("price24hPcnt"));
        JSONObject q=quote("Bybit",kind,"USDT",d.get("lastPrice"),change==null?null:change*100,d.opt("turnover24h"),url);
        q.put("funding_rate",num(d.opt("fundingRate")));q.put("open_interest_usd",num(d.opt("openInterestValue")));return q;
    }
    JSONObject lbank(String pair,String kind)throws Exception{
        if(!kind.equals("spot"))throw new IOException("LBank perpetual adapter unavailable");
        String url="https://api.lbkex.com/v2/ticker/24hr.do?symbol="+pair;
        JSONObject d=get(url).getJSONArray("data").getJSONObject(0).getJSONObject("ticker");
        return quote("LBank",kind,pair.split("_")[1].toUpperCase(Locale.US),d.get("latest"),null,d.opt("turnover"),url);
    }
    JSONObject hyper(String s,String kind)throws Exception{
        if(!kind.equals("perp"))throw new IOException("Hyperliquid spot unavailable");
        String url="https://api.hyperliquid.xyz/info";JSONArray data=new JSONArray(net.read(url,obj("type","metaAndAssetCtxs")));
        JSONArray universe=data.getJSONObject(0).getJSONArray("universe");
        for(int i=0;i<universe.length();i++)if(universe.getJSONObject(i).getString("name").equals(s)&&!universe.getJSONObject(i).optBoolean("isDelisted")){
            JSONObject d=data.getJSONArray(1).getJSONObject(i);double p=d.getDouble("markPx"),prev=d.getDouble("prevDayPx");
            JSONObject q=quote("Hyperliquid",kind,"USDC",p,prev>0?(p/prev-1)*100:null,d.opt("dayNtlVlm"),url);
            q.put("funding_rate",num(d.opt("funding")));q.put("funding_interval_hours",1);q.put("price_type","mark");
            Double oi=num(d.opt("openInterest"));q.put("open_interest_usd",oi==null?JSONObject.NULL:oi*p);return q;
        }throw new IOException("Exact instrument not found");
    }
    JSONObject fundamentals(String s)throws Exception{
        if(!ids.containsKey(s))throw new IOException("Asset identity unverified");
        String id=ids.get(s),url="https://api.coingecko.com/api/v3/coins/"+id+"?localization=false&tickers=false&community_data=false&developer_data=false";
        JSONObject d=get(url);if(!d.getString("symbol").equalsIgnoreCase(s))throw new IOException("Identity mismatch");
        JSONObject m=d.getJSONObject("market_data");Double cap=num(m.getJSONObject("market_cap").opt("usd")),fdv=num(m.getJSONObject("fully_diluted_valuation").opt("usd"));
        return obj("source","CoinGecko","url","https://www.coingecko.com/en/coins/"+id,"retrieved_at",now(),"source_updated_at",d.opt("last_updated"),
            "market_cap_usd",cap,"fdv_usd",fdv,"circulating_supply",num(m.opt("circulating_supply")),"max_supply",num(m.opt("max_supply")),
            "assessment",cap!=null&&cap>0&&fdv!=null&&fdv/cap>2?"ارزش رقیق‌شده بیش از دو برابر ارزش بازار است؛ ریسک افزایش عرضه نیازمند بررسی است.":"عرضه و ارزش بازار به‌تنهایی برای قضاوت درباره ارزش پروژه کافی نیست.",
            "limitations","زمان آزادسازی، تمرکز دارندگان و درآمد پروژه در این داده گزارش نشده‌اند.");
    }
    JSONObject technical(String s,String pair,String kind,boolean mapped)throws Exception{
        if(s.equals("XAUT")&&mapped)return goldTechnical(kind);
        List<String> failed=new ArrayList<>();
        if(mapped){
            try{
                String base=kind.equals("spot")?"https://data-api.binance.vision/api/v3":"https://fapi.binance.com/fapi/v1";
                String url=base+"/klines?symbol="+s+"USDT&interval=1h&limit=1000";
                return candles(new JSONArray(net.read(url,null)),"Binance",url,kind,0);
            }catch(Exception e){failed.add("Binance");}
            try{
                String url="https://api.bybit.com/v5/market/kline?category="+(kind.equals("spot")?"spot":"linear")+"&symbol="+s+"USDT&interval=60&limit=1000";
                JSONObject d=get(url);if(d.optInt("retCode",-1)!=0)throw new IOException();
                return candles(d.getJSONObject("result").getJSONArray("list"),"Bybit",url,kind,1);
            }catch(Exception e){failed.add("Bybit");}
        }
        if(mapped&&kind.equals("perp")){
            try{
                String url="https://api.hyperliquid.xyz/info";
                JSONArray raw=new JSONArray(net.read(url,obj("type","candleSnapshot","req",obj("coin",s,"interval","1h","startTime",System.currentTimeMillis()-1000L*3600000,"endTime",System.currentTimeMillis()))));
                JSONArray rows=new JSONArray();
                for(int i=0;i<raw.length();i++){
                    JSONObject c=raw.getJSONObject(i);if(!c.getString("s").equals(s)||!c.getString("i").equals("1h"))throw new IOException("Candle identity mismatch");
                    rows.put(new JSONArray().put(c.getLong("t")).put(c.get("o")).put(c.get("h")).put(c.get("l")).put(c.get("c")).put(c.get("v")).put(c.getLong("T")));
                }
                JSONObject result=candles(rows,"Hyperliquid",url,kind,0);result.put("quote","USDC");return result;
            }catch(Exception e){failed.add("Hyperliquid");}
        }
        if(kind.equals("spot")){
            String url="https://api.lbkex.com/v2/kline.do?symbol="+pair+"&type=hour1&size=1000&time="+(now()-1000*3600);
            return candles(get(url).getJSONArray("data"),"LBank",url,kind,2);
        }
        throw new IOException("کندل کافی از "+failed+" دریافت نشد.");
    }
    JSONObject daily(String s,String pair,String kind,boolean mapped)throws Exception{
        List<Callable<JSONObject>> jobs=new ArrayList<>();
        if(mapped){
            jobs.add(()->{String base=kind.equals("spot")?"https://data-api.binance.vision/api/v3":"https://fapi.binance.com/fapi/v1";
                String url=base+"/klines?symbol="+s+"USDT&interval=1d&limit=1000";
                return candles(new JSONArray(net.read(url,null)),"Binance",url,kind,0,86400000L);});
            jobs.add(()->{String url="https://api.bybit.com/v5/market/kline?category="+(kind.equals("spot")?"spot":"linear")+"&symbol="+s+"USDT&interval=D&limit=1000";
                JSONObject d=get(url);if(d.optInt("retCode",-1)!=0)throw new IOException("Bybit");
                return candles(d.getJSONObject("result").getJSONArray("list"),"Bybit",url,kind,1,86400000L);});
            if(kind.equals("perp"))jobs.add(()->{
                String url="https://api.hyperliquid.xyz/info";
                JSONArray raw=new JSONArray(net.read(url,obj("type","candleSnapshot","req",obj("coin",s,"interval","1d","startTime",System.currentTimeMillis()-1000L*86400000,"endTime",System.currentTimeMillis()))));
                JSONArray rows=new JSONArray();
                for(int i=0;i<raw.length();i++){JSONObject c=raw.getJSONObject(i);if(!c.getString("s").equals(s)||!c.getString("i").equals("1d"))throw new IOException("Identity mismatch");rows.put(new JSONArray().put(c.getLong("t")).put(c.get("o")).put(c.get("h")).put(c.get("l")).put(c.get("c")).put(c.get("v")).put(c.getLong("T")));}
                return candles(rows,"Hyperliquid",url,kind,0,86400000L).put("quote","USDC");
            });
        }
        if(kind.equals("spot")||mapped)jobs.add(()->{
            String url="https://api.lbkex.com/v2/kline.do?symbol="+pair+"&type=day1&size=1000&time="+(now()-1000L*86400);
            JSONObject d=candles(get(url).getJSONArray("data"),"LBank",url,"spot",2,86400000L).put("quote",pair.split("_")[1].toUpperCase(Locale.US));
            if(!kind.equals("spot"))d.put("basis_note","مرجع جایگزین نقدی "+s+"؛ تحلیل بلندمدت این بازار نقدی است، نه قرارداد دائمی انتخابی.");
            return d;
        });
        if(jobs.isEmpty())throw new IOException("بازار روزانه تأیید نشده است.");
        ExecutorService pool=Executors.newFixedThreadPool(jobs.size());CompletionService<JSONObject> done=new ExecutorCompletionService<>(pool);
        JSONObject best=null;long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(22);
        try{
            for(Callable<JSONObject> job:jobs)done.submit(job);
            for(int i=0;i<jobs.size();i++){
                Future<JSONObject> future=done.poll(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);if(future==null)break;
                try{JSONObject d=future.get();JSONArray raw=d.optJSONArray("ohlc");if(raw==null||raw.length()<80)continue;
                    if(best==null||dailyRank(d,kind)>dailyRank(best,kind))best=d;
                    if(d.optString("kind").equals(kind)&&d.optInt("closed_candles")>=365)return d;
                }catch(ExecutionException ignored){}
            }
            if(best!=null)return best;
            throw new IOException("منابع روزانه پاسخ تازه و پیوسته ندادند؛ اینترنت یا پشتیبانی بازار را بررسی کنید.");
        }finally{pool.shutdownNow();}
    }
    static int dailyRank(JSONObject d,String requested){int n=d.optInt("closed_candles");return (n>=365?3000:n>=200?2000:0)+(d.optString("kind").equals(requested)?1000:0)+Math.min(n,999);}
    JSONObject liveQuote(String pair,String kind)throws Exception{
        String s=pair.split("_")[0].toUpperCase(Locale.US);
        if(kind.equals("spot"))try{return lbank(pair,kind);}catch(Exception ignored){}
        if(ids.containsKey(s)&&pair.toLowerCase(Locale.US).endsWith("_usdt")){
            try{return binance(s,kind);}catch(Exception ignored){}
            try{return bybit(s,kind);}catch(Exception ignored){}
            if(kind.equals("perp"))try{return hyper(s,kind);}catch(Exception ignored){}
            if(s.equals("XAUT"))return lbank(pair,"spot").put("basis_note","مرجع نقدی XAUT؛ قیمت قرارداد دائمی یا XAU/USD نیست.");
        }
        throw new IOException("قیمت تازه این بازار دریافت نشد.");
    }
    JSONObject goldTechnical(String kind)throws Exception{
        ExecutorService pool=Executors.newFixedThreadPool(2);
        CompletionService<JSONObject> done=new ExecutorCompletionService<>(pool);
        done.submit(()->goldExchangeTechnical(kind));
        done.submit(()->{
            String url="https://api.lbkex.com/v2/kline.do?symbol=xaut_usdt&type=hour1&size=1000&time="+(now()-1000*3600);
            return goldLabel(candles(get(url).getJSONArray("data"),"LBank • XAUT نقدی",url,"spot",2),kind);
        });
        List<String> errors=new ArrayList<>();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(38);
        try{
            for(int i=0;i<2;i++){
                Future<JSONObject> f=done.poll(Math.max(1,deadline-System.nanoTime()),TimeUnit.NANOSECONDS);
                if(f==null)break;
                try{return f.get();}catch(ExecutionException e){errors.add(e.getCause().getMessage());}
            }
            throw new IOException("منابع طلا پاسخ معتبر ندادند: "+errors+"؛ اتصال اینترنت یا محدودیت صرافی را بررسی کنید.");
        }finally{pool.shutdownNow();}
    }
    JSONObject goldExchangeTechnical(String kind)throws Exception{
        try{
            String url="https://api.bybit.com/v5/market/kline?category="+(kind.equals("spot")?"spot":"linear")+"&symbol=XAUTUSDT&interval=60&limit=1000";
            JSONObject d=get(url);if(d.optInt("retCode",-1)!=0)throw new IOException("Bybit unavailable");
            return goldLabel(candles(d.getJSONObject("result").getJSONArray("list"),"Bybit",url,kind,1),kind);
        }catch(Exception ignored){}
        try{
            JSONArray combined=new JSONArray();String after="";
            String base="https://www.okx.com/api/v5/market/history-candles?instId=XAUT-USDT&bar=1H&limit=300";
            for(int page=0;page<2;page++){
                JSONObject d;
                try{d=get(base+after);if(!d.optString("code").equals("0"))throw new IOException("OKX "+d.optString("code"));}
                catch(Exception e){if(combined.length()>=80)break;throw e;}
                JSONArray rows=d.getJSONArray("data");if(rows.length()==0)break;long oldest=Long.MAX_VALUE;
                for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);oldest=Math.min(oldest,r.getLong(0));if(!r.optString(8).equals("1"))continue;
                    combined.put(new JSONArray().put(r.getLong(0)).put(r.get(1)).put(r.get(2)).put(r.get(3)).put(r.get(4)).put(r.get(5)).put(r.getLong(0)+3599999));}
                after="&after="+oldest;
            }
            return goldLabel(candles(combined,"OKX • XAUT نقدی",base,"spot",0),kind);
        }catch(Exception ignored){}
        String url="https://api.bybit.com/v5/market/kline?category=spot&symbol=XAUTUSDT&interval=60&limit=1000";
        JSONObject d=get(url);if(d.optInt("retCode",-1)!=0)throw new IOException("Gold history unavailable");
        return goldLabel(candles(d.getJSONObject("result").getJSONArray("list"),"Bybit • XAUT نقدی",url,"spot",1),kind);
    }
    JSONObject goldLabel(JSONObject data,String requested)throws Exception{
        data.put("basis_note",data.optString("kind").equals(requested)?"مبنای تحلیل طلا: XAUT / USDT در بازار انتخابی؛ قیمت جهانی XAU/USD نیست.":"منبع جایگزین: XAUT / USDT نقدی؛ این درصدها مربوط به مرجع نقدی طلا هستند، نه قیمت یا تأمین مالی قرارداد دائمی LBank.");
        data.put("asset","XAUT");return data;
    }
    JSONObject candles(JSONArray data,String source,String url,String kind,int format)throws Exception{
        return candles(data,source,url,kind,format,3600000L);
    }
    JSONObject candles(JSONArray data,String source,String url,String kind,int format,long interval)throws Exception{
        TreeMap<Long,Double> ordered=new TreeMap<>();TreeMap<Long,JSONArray> ohlc=new TreeMap<>();long current=System.currentTimeMillis();
        for(int i=0;i<data.length();i++){
            JSONArray row=data.getJSONArray(i);long start=row.getLong(0)*(format==2?1000L:1L);
            long close=format==0?row.getLong(6):start+interval-1;
            if(close>=current)continue;Double price=num(row.opt(4));if(price==null||price<=0)throw new IOException("Invalid candle");ordered.put(start,price);
            double o=row.getDouble(1),hi=row.getDouble(2),lo=row.getDouble(3);
            if(Double.isFinite(o)&&Double.isFinite(hi)&&Double.isFinite(lo)&&lo>0&&hi>=Math.max(o,price)&&lo<=Math.min(o,price))
                ohlc.put(start,new JSONArray().put(start).put(o).put(hi).put(lo).put(price).put(num(row.opt(5))==null?JSONObject.NULL:num(row.opt(5))));
        }
        if(ordered.size()<80||current-(ordered.lastKey()+interval)>2*interval)throw new IOException("کندل کافی یا تازه نیست.");
        long prev=0;for(long t:ordered.keySet()){if(prev>0&&t-prev!=interval)throw new IOException("شکاف در کندل‌ها");prev=t;}
        double[] c=new double[ordered.size()];int i=0;for(double x:ordered.values())c[i++]=x;
        JSONObject result=interval==86400000L?obj("closed_candles",c.length,"interval","1d"):indicators(c);JSONArray chart=new JSONArray();for(int z=Math.max(0,c.length-120);z<c.length;z++)chart.put(c[z]);result.put("chart_closes",chart);JSONArray raw=new JSONArray();if(ohlc.size()==ordered.size())for(JSONArray r:ohlc.values())raw.put(r);result.put("ohlc",raw);result.put("source",source);result.put("url",url);result.put("kind",kind);result.put("retrieved_at",now());result.put("last_candle_close",(ordered.lastKey()+interval)/1000);return result;
    }
    static JSONObject indicators(double[] c)throws Exception{
        if(c.length<80)throw new IllegalArgumentException("Insufficient history");
        for(double x:c)if(!Double.isFinite(x)||x<=0)throw new IllegalArgumentException("Invalid close");
        int n=c.length;double gain=0,loss=0,m20=0,m50=0;
        for(int i=n-14;i<n;i++){double d=c[i]-c[i-1];gain+=Math.max(d,0);loss+=Math.max(-d,0);}
        for(int i=n-50;i<n;i++){m50+=c[i]/50;if(i>=n-20)m20+=c[i]/20;}
        double rsi=ScenarioEngine.evaluate(c,4).rsi;
        double mean=0;double[] r=new double[72];for(int i=0;i<72;i++){r[i]=Math.log(c[n-72+i]/c[n-73+i]);mean+=r[i]/72;}
        double variance=0;for(double x:r)variance+=(x-mean)*(x-mean)/71;
        JSONArray h=new JSONArray();for(int hours:new int[]{4,24,168}){
            if(hours==168&&c.length<336){h.put(obj("hours",168,"probabilities",null,"status","برای هفتگی حداقل ۳۳۶ کندل بسته‌شده ساعتی لازم است."));continue;}
            ScenarioEngine.Result w=ScenarioEngine.evaluate(c,hours);
            h.put(obj("hours",hours,"probabilities",null,"volatility_scale_pct",historicalScale(c,hours),
                "scenario_weights",obj("up",w.up,"neutral",w.neutral,"down",w.down,"model",ScenarioEngine.VERSION,
                    "rsi_wilder",w.rsi,"efficiency",w.efficiency,"disagreement",w.disagreement,"regime",w.regime,"band_z",w.bandPosition,"breakout",w.breakoutSignal,
                    "explanation", "وضعیت بازار: "+w.regime+" • "+(w.breakoutSignal>0?"شکست سقف قیمت‌های بسته‌شدن":w.breakoutSignal<0?"شکست کف قیمت‌های بسته‌شدن":"داخل کانال قیمت بسته‌شدن")+"\nموقعیت باند نوسان: "+(w.bandPosition>1?"نزدیک بخش بالایی":w.bandPosition< -1?"نزدیک بخش پایینی":"میانی")+" • روند میانگین نمایی: "+(w.trend>0.05?"صعودی":w.trend< -0.05?"نزولی":"کم‌جهت")+
                    " • قدرت حرکت: "+(w.momentum>0.05?"مثبت":w.momentum< -0.05?"منفی":"کم‌جهت")+
                    " • انسجام حرکت: "+Math.round(w.efficiency*100)+"٪")));
        }
        String trend=Math.abs(m20-m50)<1e-10?"خنثی":m20>m50?"صعودی":"نزولی";
        String explanation="میانگین ۲۰ساعته "+(trend.equals("صعودی")?"بالاتر از":trend.equals("نزولی")?"پایین‌تر از":"برابر با")+" میانگین ۵۰ساعته است. "+(rsi>70?"قدرت حرکت بالاست؛ RSI بالای ۷۰ به‌تنهایی علامت فروش نیست.":rsi<30?"فشار فروش بالاست؛ RSI زیر ۳۰ تضمین بازگشت قیمت نیست.":"RSI در محدوده میانی قرار دارد.");
        return obj("rsi14",rsi,"trend",trend,"explanation",explanation,"closed_candles",n,"horizons",h);
    }
    static double historicalScale(double[] c,int hours){
        int window=hours==168?336:72;double sum=0,ss=0;
        for(int i=c.length-window;i<c.length;i++){if(i==0)continue;double r=Math.log(c[i]/c[i-1]);sum+=r;ss+=r*r;}
        int count=Math.min(window,c.length-1);
        return Math.sqrt(Math.max(0,(ss-sum*sum/count)/(count-1))*hours)*100;
    }
    JSONObject news(String s)throws Exception{
        JSONArray items=new JSONArray(),statuses=new JSONArray();
        String[][] feeds=s.equals("ETH")?new String[][]{{"Federal Reserve","https://www.federalreserve.gov/feeds/press_all.xml"},{"Ethereum Foundation","https://blog.ethereum.org/feed.xml"}}:new String[][]{{"Federal Reserve","https://www.federalreserve.gov/feeds/press_all.xml"}};
        for(String[] f:feeds){try{
            String xml=net.read(f[1],null);if(xml.contains("<!DOCTYPE")||xml.contains("<!ENTITY"))throw new IOException("Unsafe XML");
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setExpandEntityReferences(false);
            Document doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList nodes=doc.getElementsByTagName("item");int count=0;
            for(int i=0;i<nodes.getLength()&&count<8;i++){
                Element e=(Element)nodes.item(i);String date=tag(e,"pubDate"),link=tag(e,"link");
                long stamp;try{SimpleDateFormat parser=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z",Locale.US);parser.setLenient(false);stamp=parser.parse(date).getTime()/1000;}catch(Exception ex){continue;}
                if(stamp>now()||stamp<now()-7*86400||!link.startsWith("https://"))continue;
                items.put(obj("title",tag(e,"title"),"source",f[0],"source_url",link,"published_at",stamp,"scope",f[0].equals("Federal Reserve")?"کلان اقتصادی":"پروژه"));count++;
            }statuses.put(obj("source",f[0],"status","ok"));
        }catch(Exception e){statuses.put(obj("source",f[0],"status","unavailable"));}}
        return obj("items",items,"sources",statuses,"coverage","دریافت مستقیم اخبار رسمی کلان؛ پوشش اختصاصی پروژه فعلاً فقط ETH. پوشش کامل خبر یا رویداد آینده تضمین نمی‌شود.");
    }
    static String tag(Element e,String name){NodeList list=e.getElementsByTagName(name);return list.getLength()==0?"":list.item(0).getTextContent();}
    JSONObject safe(String name,Job job)throws Exception{
        try{return obj("name",name,"status","ok","data",job.run());}
        catch(Exception e){return obj("name",name,"status","unavailable","message",name.equals("technical")?String.valueOf(e.getMessage()):"داده دریافت نشد؛ ممکن است منبع محدود، نماد پشتیبانی‌نشده یا تاریخچه ناکافی باشد.");}
    }
    public JSONObject analyze(String pair,String kind)throws Exception{return analyze(pair,kind,false);}
    public JSONObject analyze(String pair,String kind,boolean includeContext)throws Exception{
        if(!pair.matches("[a-zA-Z0-9]+_[a-zA-Z0-9]+")||!(kind.equals("spot")||kind.equals("perp")))throw new IllegalArgumentException("نماد یا بازار نامعتبر است.");
        String s=pair.split("_")[0].toUpperCase(Locale.US);boolean mapped=ids.containsKey(s)&&pair.toLowerCase(Locale.US).endsWith("_usdt");
        LinkedHashMap<String,Job> jobs=new LinkedHashMap<>();
        if(includeContext&&mapped&&!s.equals("BTC")){jobs.put("btc_hourly",()->technical("BTC","btc_usdt",kind,true));jobs.put("btc_daily",()->daily("BTC","btc_usdt",kind,true));}
        if(mapped){jobs.put("Binance",()->binance(s,kind));jobs.put("Bybit",()->bybit(s,kind));jobs.put("Hyperliquid",()->hyper(s,kind));jobs.put("BTC benchmark",()->binance("BTC",kind));jobs.put("ETH benchmark",()->binance("ETH",kind));}
        jobs.put("daily",()->daily(s,pair,kind,mapped));jobs.put("LBank",()->lbank(pair,kind));jobs.put("technical",()->technical(s,pair,kind,mapped));jobs.put("fundamentals",()->fundamentals(s));jobs.put("news",()->news(s));
        ExecutorService pool=Executors.newFixedThreadPool(6);List<Callable<JSONObject>> tasks=new ArrayList<>();
        for(Map.Entry<String,Job> e:jobs.entrySet())tasks.add(()->safe(e.getKey(),e.getValue()));
        JSONArray sources=new JSONArray();
        try{
            List<Future<JSONObject>> futures=pool.invokeAll(tasks,45,TimeUnit.SECONDS);int i=0;
            for(String name:jobs.keySet()){Future<JSONObject> f=futures.get(i++);sources.put(f.isCancelled()?obj("name",name,"status","unavailable","message","مهلت دریافت داده تمام شد."):f.get());}
        }finally{pool.shutdownNow();}
        Double own=null,btc=null;double min=Double.POSITIVE_INFINITY,max=0;int count=0;
        for(int i=0;i<sources.length();i++){
            JSONObject r=sources.getJSONObject(i);if(!r.getString("status").equals("ok"))continue;JSONObject d=r.getJSONObject("data");String name=r.getString("name");
            if(name.equals("Binance"))own=num(d.opt("change24h"));if(name.equals("BTC benchmark"))btc=num(d.opt("change24h"));
            if(mapped&&!name.contains("benchmark")&&d.optString("quote").equals("USDT")&&num(d.opt("price"))!=null){double p=d.getDouble("price");min=Math.min(min,p);max=Math.max(max,p);count++;}
        }
        return obj("symbol",s,"kind",kind,"generated_at",now(),"sources",sources,"probabilities",null,
            "relative_btc_percentage_points",own!=null&&btc!=null?own-btc:null,"dispersion_usdt_pct",count>=2?(max/min-1)*100:null,
            "status","موتور داخلی گوشی • بدون سرور شخصی • نیازمند اینترنت. "+(mapped?"USDT و USDC و بازار نقدی/دائمی جدا هستند.":"تطبیق بین صرافی‌ها تأیید نشده؛ تنها داده مستقیم همان بازار قابل استفاده است."),
            "events",obj("items",new JSONArray(),"status","خوراک تأییدشده تقویم آینده متصل نیست؛ رویداد فردا یا هفته بعد حدس زده نمی‌شود."));
    }
}
