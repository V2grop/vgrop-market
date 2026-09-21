package com.v2grop.lbankpulse;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(7, 17, 31);
    private static final int CARD = Color.rgb(17, 31, 51);
    private static final int CYAN = Color.rgb(53, 214, 198);
    private static final int GREEN = Color.rgb(50, 213, 131);
    private static final int RED = Color.rgb(249, 112, 102);
    private static final int AMBER = Color.rgb(253, 176, 34);
    private static final int TEXT = Color.rgb(244, 247, 251);
    private static final int MUTED = Color.rgb(148, 163, 184);

    private final ExecutorService io = Executors.newFixedThreadPool(3);
    private final LBankApi api = new LBankApi();
    private final LocalResearch local = new LocalResearch();
    private java.util.concurrent.Future<?> analysisTask;
    private LinearLayout content;
    private int generation=0;
    private final android.os.Handler priceHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable priceLoop;
    private boolean foreground=true;
    private List<MarketItem> allMarkets = new ArrayList<>();
    private MarketItem selected = new MarketItem("btc_usdt", "BTC / USDT", "قرارداد دائمی");

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(NAVY);
        getWindow().setNavigationBarColor(NAVY);
        FundAlertJob.schedule(this);
        showShell();
        if(state!=null&&state.containsKey("selected_symbol"))selected=new MarketItem(state.getString("selected_symbol"),state.getString("selected_display"),state.getString("selected_kind"));
        showFavorites();
    }

    private void showShell() {
        LinearLayout root = column();
        root.setBackground(new PulseBackdrop());
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout header = row();
        header.setPadding(dp(18), dp(16), dp(18), dp(12));
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout credit=column();credit.setGravity(Gravity.RIGHT);
        TextView author=text("کاری از vgrop",10,MUTED,false);credit.addView(author);
        TextView telegram=text("t.me/V2grop ↗",11,CYAN,true);telegram.setTextDirection(View.TEXT_DIRECTION_LTR);credit.addView(telegram);
        credit.setMinimumHeight(dp(48));credit.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        credit.setContentDescription("کاری از vgrop؛ بازکردن کانال تلگرام V2grop");
        credit.setOnClickListener(v->{try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("tg://resolve?domain=V2grop")));}catch(android.content.ActivityNotFoundException e){openSource("https://t.me/V2grop");}});
        header.addView(credit,new LinearLayout.LayoutParams(dp(100),-2));
        TextView title = text("VGrop Market",20,TEXT,true);title.setGravity(Gravity.CENTER);title.setTextDirection(View.TEXT_DIRECTION_LTR);
        header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        android.widget.ImageView logo=new android.widget.ImageView(this);logo.setImageResource(R.drawable.ic_launcher);logo.setContentDescription("نشان VGrop market");header.addView(logo,new LinearLayout.LayoutParams(dp(40),dp(40)));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = column();
        content.setPadding(dp(14), dp(4), dp(14), dp(18));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = row();
        nav.setPadding(dp(6), dp(7), dp(6), dp(9));
        nav.setBackground(roundRect(Color.rgb(16,26,43),18));
        addNav(nav, "علاقه‌مندی", this::showFavorites);
        addNav(nav, "همه بازارها", this::showAllMarkets);
        addNav(nav, "تحلیل", () -> showAnalysis(selected));
        addNav(nav, "آمریکا", this::showUsMacro);
        addNav(nav, "تنظیمات", this::showSettings);
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(66)));
        setContentView(root);
    }

    private void addNav(LinearLayout nav, String label, Runnable action) {
        Button b = button(label);
        String icon=label.equals("علاقه‌مندی")?"★":label.equals("همه بازارها")?"▦":label.equals("تحلیل")?"◈":label.equals("آمریکا")?"◎":"⚙";
        b.setText(icon+"\n"+label);b.setTextSize(11);b.setTextColor(MUTED);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setOnClickListener(v -> {for(int i=0;i<nav.getChildCount();i++){Button n=(Button)nav.getChildAt(i);n.setTextColor(MUTED);n.setBackgroundColor(Color.TRANSPARENT);}b.setTextColor(CYAN);b.setBackground(roundRect(Color.rgb(21,48,57),12));action.run();});
        nav.addView(b, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void reset(String title, String subtitle) {
        generation++;
        priceHandler.removeCallbacksAndMessages(null);priceLoop=null;
        if(analysisTask!=null)analysisTask.cancel(true);
        content.removeAllViews();
        content.addView(text(title, 24, TEXT, true));
        TextView sub = text(subtitle, 13, MUTED, false);
        sub.setPadding(0, dp(4), 0, dp(14));
        content.addView(sub);
    }

    private void showFavorites() {
        reset("دیده‌بان بازار", "بازارهای منتخب شما • جابه‌جایی با ↑ و ↓ • ذخیره خودکار");
        SharedPreferences prefs=getSharedPreferences("watchlist",MODE_PRIVATE);
        List<MarketItem> items=Watchlist.load(this);
        Button defaults=button("بازگرداندن فهرست پیش‌فرض");
        defaults.setOnClickListener(v->{Watchlist.save(this,Favorites.defaults());showFavorites();});content.addView(defaults,spaced(-1,dp(48),10));
        LinearLayout list=column();content.addView(list);
        renderFavorites(list,items,prefs);
        if(items.isEmpty())content.addView(cardText("فهرست خالی است؛ در همه بازارها ☆ را بزن تا بازار دلخواه اضافه شود.",CYAN),spaced(-1,-2,12));
        Button browse=button("＋ افزودن ارز از همه بازارها");browse.setOnClickListener(v->showAllMarkets());content.addView(browse,spaced(-1,dp(48),12));
        content.addView(cardText("طلا همان بازار XAUT و نفت همان XTI(CL) قبلی است. جایگاه جدید به معنی اضافه‌شدن منبع قیمت نیست؛ تحلیل نفت تا تأیید نماد و دریافت داده معتبر ممکن است ناموجود باشد.",MUTED),spaced(-1,-2,10));
    }
    private void renderFavorites(LinearLayout list,List<MarketItem> items,SharedPreferences prefs){
        list.removeAllViews();
        for(int index=0;index<items.size();index++){
            final int target=index;MarketItem item=items.get(index);String key=FavoriteOrder.id(item);
            LinearLayout wrapper=row();LinearLayout name=column();addMarketCard(item,name,true);wrapper.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            LinearLayout controls=column();Button up=button("↑"),down=button("↓");
            up.setContentDescription("انتقال "+item.display+" به بالا");down.setContentDescription("انتقال "+item.display+" به پایین");
            up.setEnabled(index>0);down.setEnabled(index<items.size()-1);up.setAlpha(index>0?1f:0.3f);down.setAlpha(index<items.size()-1?1f:0.3f);
            up.setOnClickListener(v->{FavoriteOrder.move(items,target,target-1);Watchlist.save(this,items);renderFavorites(list,items,prefs);});
            down.setOnClickListener(v->{FavoriteOrder.move(items,target,target+1);Watchlist.save(this,items);renderFavorites(list,items,prefs);});
            controls.addView(up,new LinearLayout.LayoutParams(dp(44),dp(40)));
            controls.addView(down,new LinearLayout.LayoutParams(dp(44),dp(40)));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(dp(44),-2);cp.setMargins(dp(6),0,0,0);
            wrapper.addView(controls,cp);list.addView(wrapper,spaced(-1,-2,6));
        }
    }

    private void showAllMarkets() {
        reset("همه بازارهای LBank", "جست‌وجو با نماد، جفت‌ارز یا نام رایج ارز • افزودن به دیده‌بان با ☆");
        if (!allMarkets.isEmpty()) {
            LinearLayout searchBox=row();styleCard(searchBox);searchBox.setPadding(dp(12),dp(3),dp(8),dp(3));
            TextView searchIcon=text("⌕",26,CYAN,true);searchIcon.setGravity(Gravity.CENTER);searchBox.addView(searchIcon,new LinearLayout.LayoutParams(dp(42),dp(52)));
            EditText search=input("نام یا نماد؛ مثل بیت‌کوین، BTC یا DOGE", "");
            search.setSingleLine(true);search.setTextDirection(View.TEXT_DIRECTION_RTL);search.setBackgroundColor(Color.TRANSPARENT);
            search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
            searchBox.addView(search,new LinearLayout.LayoutParams(0,dp(52),1));
            Button clear=button("×");clear.setTextSize(24);clear.setContentDescription("پاک‌کردن جست‌وجو");clear.setVisibility(View.GONE);
            clear.setOnClickListener(v->search.setText(""));searchBox.addView(clear,new LinearLayout.LayoutParams(dp(44),dp(44)));
            content.addView(searchBox,spaced(-1,dp(60),5));
            TextView resultCount=text(allMarkets.size()+" بازار فعال",12,MUTED,false);content.addView(resultCount,spaced(-1,-2,8));
            androidx.recyclerview.widget.RecyclerView list=new androidx.recyclerview.widget.RecyclerView(this);
            list.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            MarketAdapter adapter=new MarketAdapter((item,parent)->addMarketCard(item,parent));list.setAdapter(adapter);
            content.addView(list,new LinearLayout.LayoutParams(-1,dp(480)));adapter.submitList(new ArrayList<>(allMarkets));
            TextView empty=cardText("بازاری پیدا نشد؛ نام یا نماد دیگری را امتحان کن.",AMBER);empty.setVisibility(View.GONE);content.addView(empty);
            search.addTextChangedListener(new android.text.TextWatcher(){
                public void beforeTextChanged(CharSequence s,int st,int c,int a){}
                public void onTextChanged(CharSequence s,int st,int before,int count){
                    List<MarketItem> matches=new ArrayList<>();
                    for(MarketItem m:allMarkets)if(MarketSearch.matches(m,s.toString()))matches.add(m);
                    int found=matches.size();adapter.submitList(matches);empty.setVisibility(found==0?View.VISIBLE:View.GONE);
                    clear.setVisibility(s.length()==0?View.GONE:View.VISIBLE);
                    resultCount.setText(s.length()==0?allMarkets.size()+" بازار فعال":found+" نتیجه برای «"+s+"»");

                }
                public void afterTextChanged(android.text.Editable e){}
            });
            return;
        }
        ProgressBar progress = new ProgressBar(this);
        content.addView(progress, centered(dp(54), dp(54)));
        final int request=generation;
        io.execute(() -> {
            try {
                List<MarketItem> result = api.loadAllMarkets();
                runOnUiThread(() -> {
                    if(request!=generation)return;
                    allMarkets = result;
                    showAllMarkets();
                    Toast.makeText(this, result.size() + " بازار فعال دریافت شد", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {if(request==generation)showError("دریافت بازارها ناموفق بود", e);});
            }
        });
    }

    private void addMarketCard(MarketItem item, LinearLayout parent) {addMarketCard(item,parent,false);}
    private void addMarketCard(MarketItem item, LinearLayout parent,boolean favorites) {
        LinearLayout card = row();
        styleCard(card);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout names = column();
        TextView symbol=text(item.display,15,TEXT,true);symbol.setMaxLines(2);
        names.addView(symbol);
        names.addView(text(item.marketType, 12, MUTED, false));
        String base=baseSymbol(item);int accent=assetColor(base);
        String mark=base.equals("BTC")?"₿":base.equals("ETH")?"Ξ":base.equals("XAUT")?"Au":base.equals("DOGE")?"Ð":base.equals("SOL")?"≋":base.equals("BNB")?"◆":base;
        TextView coin=text(mark,mark.length()>5?8:mark.length()>3?10:20,Color.WHITE,true);
        coin.setTextDirection(View.TEXT_DIRECTION_LTR);coin.setGravity(Gravity.CENTER);
        GradientDrawable coinBg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{accent,Color.rgb(Color.red(accent)/3,Color.green(accent)/3,Color.blue(accent)/3)});
        coinBg.setCornerRadius(dp(16));coinBg.setStroke(dp(1),accent);coin.setBackground(coinBg);coin.setContentDescription("نماد "+base);
        LinearLayout.LayoutParams iconp=new LinearLayout.LayoutParams(dp(48),dp(48));iconp.setMargins(0,0,dp(12),0);card.addView(coin,iconp);
        card.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView action = text("‹", 24, CYAN, true);
        action.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(action);
        Button star=button(Watchlist.contains(this,item)?"★":"☆");star.setTextSize(25);star.setTextColor(AMBER);star.setBackgroundColor(Color.TRANSPARENT);
        star.setContentDescription("افزودن یا حذف "+item.display+" از علاقه‌مندی");
        star.setOnClickListener(v->{Watchlist.toggle(this,item);if(favorites){showFavorites();Toast.makeText(this,"حذف شد؛ برای افزودن دوباره از همه بازارها استفاده کن",Toast.LENGTH_SHORT).show();}else{star.setText(Watchlist.contains(this,item)?"★":"☆");}});
        card.addView(star,new LinearLayout.LayoutParams(dp(44),dp(48)));
        card.setOnClickListener(v -> {
            selected = item;
            showAnalysis(item);
        });
        parent.addView(card, spaced(-1, -2, 7));
    }

    private void showAnalysis(MarketItem item) {
        selected=item;
        reset("تحلیل " + item.display,"مقایسه چند صرافی • بنیادی • دستیار هوشمند");
        final int request=generation;
        content.addView(new ProgressBar(this));
        content.addView(cardText("دریافت مستقیم از منابع؛ تحلیل داخل گوشی و بدون VPS انجام می‌شود. با سبک‌های Hyperliquid دریافت داده ممکن است بیش از یک دقیقه طول بکشد.",CYAN));
        SharedPreferences stylePrefs=getSharedPreferences("analysis_styles",0);
        final boolean[] styles=new boolean[StyleEngine.NAMES.length];for(int i=0;i<StyleEngine.NAMES.length;i++)styles[i]=stylePrefs.getBoolean("s"+i,i==0);
        final boolean dedicated=stylePrefs.getBoolean("long_term_style",true);
        final boolean combine=stylePrefs.getBoolean("combined",false);final String addresses=stylePrefs.getString("addresses","");
        analysisTask=io.submit(()->{
            try {
                org.json.JSONObject report=local.analyze(item.symbol,kind(item),styles[5]);
                new StyleResearch().apply(report,styles,combine,addresses,dedicated);
                runOnUiThread(()->{if(request==generation&&!isFinishing())renderReport(item,report);});
            }catch(Exception e){runOnUiThread(()->{if(request==generation)showError("دریافت تحلیل ناموفق بود",e);});}
        });
    }

    private String baseSymbol(MarketItem item){return item.symbol.split("_")[0].toUpperCase(Locale.US);}
    private String kind(MarketItem item){return item.marketType.contains("دائمی")?"perp":"spot";}
    private String value(org.json.JSONObject d,String key){return d.isNull(key)?"ناموجود":d.optString(key,"ناموجود");}
    private void addSource(String title,String body,String url){
        TextView t=cardText(title+"\n"+body+(url.isEmpty()?"":"\nمنبع: "+url),TEXT);
        t.setTextIsSelectable(true);
        android.text.util.Linkify.addLinks(t,android.text.util.Linkify.WEB_URLS);
        t.setLinkTextColor(CYAN);content.addView(t,spaced(-1,-2,9));
    }
    private void addLivePrice(MarketItem item){
        LinearLayout card=column();styleCard(card);card.setPadding(dp(16),dp(16),dp(16),dp(16));
        card.addView(text("قیمت بازار • تازه‌سازی خودکار",15,CYAN,true));
        TextView price=text("در حال دریافت…",30,TEXT,true);price.setTextIsSelectable(true);card.addView(price);
        TextView meta=text("",12,MUTED,false);card.addView(meta);content.addView(card,spaced(-1,-2,12));
        final int request=generation;
        priceLoop=new Runnable(){boolean running=false;public void run(){
            if(request!=generation||!foreground||isFinishing()||running)return;
            running=true;io.execute(()->{org.json.JSONObject result=null;try{result=local.liveQuote(item.symbol,kind(item));}catch(Exception ignored){}
                final org.json.JSONObject q=result;
                runOnUiThread(()->{running=false;if(request!=generation||!foreground||isFinishing())return;
                    if(q==null){price.setText("قیمت در دسترس نیست");meta.setText("دریافت ناموفق؛ تلاش دوباره پس از ۳۰ ثانیه.");}
                    else{java.text.DecimalFormat format=new java.text.DecimalFormat("#,##0.##########",java.text.DecimalFormatSymbols.getInstance(Locale.US));
                        price.setText(format.format(q.optDouble("price"))+" "+q.optString("quote"));
                        meta.setText(q.optString("source")+" • "+(q.optString("kind").equals("spot")?"نقدی":"دائمی")+(q.optString("price_type").equals("mark")?" • قیمت مارک":" • آخرین معامله")+"\nدریافت: "+DateFormat.getTimeInstance().format(new Date(q.optLong("retrieved_at")*1000))+"\n"+q.optString("basis_note")+"\nتازه‌سازی ۳۰ ثانیه پس از پاسخ؛ قیمت تیک‌به‌تیک نیست.");}
                    priceHandler.postDelayed(this,30000);
                });
            });
        }};priceHandler.post(priceLoop);
    }
    @Override protected void onPause(){super.onPause();foreground=false;priceHandler.removeCallbacksAndMessages(null);}
    @Override protected void onResume(){super.onResume();foreground=true;if(priceLoop!=null){priceHandler.removeCallbacksAndMessages(null);priceHandler.post(priceLoop);}}
    private static String horizonLabel(int hours){return hours==4?"۴ ساعت آینده":hours==24?"۲۴ ساعت آینده":hours==168?"هفتگی • ۷ روز آینده":hours==720?"یک‌ماهه • ۳۰ روز آینده":"سه‌ماهه • ۹۰ روز آینده";}
    private void renderReport(MarketItem item,org.json.JSONObject report){
        reset(item.display+" • "+item.marketType,"تحلیل روی گوشی • "+DateFormat.getDateTimeInstance().format(new Date(report.optLong("generated_at")*1000)));
        addLivePrice(item);
        addChatGptHandoff(report);
        Button refresh=button("به‌روزرسانی تحلیل");refresh.setOnClickListener(v->showAnalysis(item));content.addView(refresh,spaced(-1,dp(48),8));
        addScalpCard(item,report);
        renderScenarioWeights(report);
        addMarketContext();
        org.json.JSONArray chartSources=report.optJSONArray("sources");
        if(chartSources!=null)for(int i=0;i<chartSources.length();i++){org.json.JSONObject src=chartSources.optJSONObject(i);if(src==null||!src.optString("name").equals("technical"))continue;org.json.JSONObject d=src.optJSONObject("data");if(d==null)continue;org.json.JSONArray points=d.optJSONArray("chart_closes");if(points!=null&&points.length()>1){content.addView(text("نمودار بسته‌شدن ساعتی • "+d.optString("source"),15,CYAN,true));content.addView(new PriceChart(this,points),spaced(-1,dp(190),10));}}

        if(baseSymbol(item).equals("XAUT"))addGoldPanel();
        addUsMacroPanel();
        content.addView(cardText(report.optString("status"),MUTED),spaced(-1,-2,8));
        addSource("مقایسه با بازار", "اختلاف بازده ۲۴ساعته با BTC (واحد درصد): "+value(report,"relative_btc_percentage_points")+"\nاختلاف قیمت منابع USDT (درصد): "+value(report,"dispersion_usdt_pct")+"\nاین اعداد، پیش‌بینی یا فرصت آربیتراژ تضمین‌شده نیستند.", "");
        org.json.JSONArray sources=report.optJSONArray("sources");
        if(sources!=null)for(int i=0;i<sources.length();i++){
            org.json.JSONObject source=sources.optJSONObject(i);if(source==null)continue;
            String name=source.optString("name");
            if(!source.optString("status").equals("ok")){addSource(name,source.optString("message"),"");continue;}
            org.json.JSONObject d=source.optJSONObject("data");if(d==null)continue;
            String body;
            if(name.equals("news")){
                addSource("خبرهای رسمی",d.optString("coverage"),"");
                org.json.JSONArray ns=d.optJSONArray("items");
                if(ns!=null)for(int z=0;z<ns.length();z++){org.json.JSONObject n=ns.optJSONObject(z);if(n!=null)addSource(n.optString("title"),n.optString("source")+" • "+n.optString("scope")+"\nانتشار: "+new Date(n.optLong("published_at")*1000),n.optString("source_url"));}
                org.json.JSONArray states=d.optJSONArray("sources");if(states!=null)for(int z=0;z<states.length();z++){org.json.JSONObject n=states.optJSONObject(z);if(n!=null)addSource(n.optString("source"),n.optString("status").equals("ok")?"خوراک بررسی شد؛ خبرهای هفت روز اخیر نمایش داده می‌شوند.":"خوراک در دسترس نیست.","");}
                continue;
            }else if(name.equals("fundamentals")){
                name="تحلیل بنیادی • CoinGecko";
                body="ارزش بازار (دلار): "+value(d,"market_cap_usd")+"\nارزش کاملاً رقیق‌شده: "+value(d,"fdv_usd")+"\nعرضه در گردش: "+value(d,"circulating_supply")+"\nحجم (دلار): "+value(d,"volume_usd")+"\nتورم عرضه / آزادسازی توکن: ناموجود"+"\nحداکثر عرضه: "+value(d,"max_supply")+"\n"+d.optString("assessment")+"\n"+d.optString("limitations")+"\nبه‌روزرسانی منبع: "+value(d,"source_updated_at");
            }else if(name.equals("daily")||name.equals("btc_daily")){
                name=(name.equals("btc_daily")?"زمینه BTC روزانه • ":"تاریخچه روزانه • ")+d.optString("source");body=d.optInt("closed_candles")+" کندل بسته‌شده روزانه • "+d.optString("kind")+"\n"+d.optString("basis_note");
            }else if(name.equals("technical")||name.equals("btc_hourly")){
                name=(name.equals("btc_hourly")?"زمینه BTC ساعتی • ":"تکنیکال • ")+d.optString("source");
                body="روند میانگین ۲۰/۵۰: "+d.optString("trend")+"\nRSI (شاخص قدرت نسبی): "+value(d,"rsi14");
                org.json.JSONArray hs=d.optJSONArray("horizons");
                if(hs!=null)for(int j=0;j<hs.length();j++){org.json.JSONObject h=hs.optJSONObject(j);if(h!=null&&h.has("volatility_scale_pct"))body+="\n"+h.optInt("hours")+" ساعت: مقیاس نوسان تاریخی "+value(h,"volatility_scale_pct")+"٪";}
                body+="\n"+d.optString("explanation")+"\nمقیاس نوسان، بازه پیش‌بینی قیمت یا احتمال نیست.";
            }else{
                body="قیمت: "+value(d,"price")+" "+value(d,"quote")+"\nتغییر ۲۴ ساعت: "+value(d,"change24h")+"٪\nحجم ۲۴ ساعت به ارز مظنه: "+value(d,"volume24h");
                if(!d.isNull("funding_rate")&&d.has("funding_rate"))body+="\nنرخ تأمین مالی (اعشاری): "+value(d,"funding_rate");
                if(!d.isNull("open_interest_usd")&&d.has("open_interest_usd"))body+="\nارزش قراردادهای باز (دلار): "+value(d,"open_interest_usd");
            }
            body+="\nدریافت: "+DateFormat.getDateTimeInstance().format(new Date(d.optLong("retrieved_at")*1000));
            addSource(name,body,d.optString("url"));
        }
        org.json.JSONObject ev=report.optJSONObject("events");
        if(ev!=null){addSource("اخبار و تقویم ۷ روزه",ev.optString("status"),"");org.json.JSONArray list=ev.optJSONArray("items");if(list!=null)for(int i=0;i<list.length();i++){org.json.JSONObject e=list.optJSONObject(i);if(e!=null)addSource(e.optString("title"),"زمان رویداد: "+new Date(e.optLong("event_at")*1000),e.optString("source_url"));}}
        SharedPreferences aiPrefs=getSharedPreferences("settings",MODE_PRIVATE);
        if(!aiPrefs.getBoolean("ai_enabled",false)){
            content.addView(cardText("هوش مصنوعی خاموش است؛ تحلیل بالا با موتور داخلی آماده شد.",CYAN),spaced(-1,-2,8));
            Button enable=button("هوش مصنوعی اختیاری • تنظیمات");enable.setOnClickListener(v->showSettings());content.addView(enable);return;
        }
        EditText question=input("پرسش شما درباره این ارز", "دلایل موافق و مخالف رشد این ارز چیست؟");content.addView(question,spaced(-1,dp(70),8));
        Button ask=button("تحلیل با هوش مصنوعی");content.addView(ask,spaced(-1,dp(52),8));
        TextView answer=cardText("بخش اختیاری هوش مصنوعی از سرور استفاده می‌کند و داده‌ها را دوباره بررسی می‌کند.",CYAN);content.addView(answer);
        final int request=generation;
        ask.setOnClickListener(v->{
            ask.setEnabled(false);answer.setText("در حال بررسی…");
            SharedPreferences prefs=getSharedPreferences("settings",MODE_PRIVATE);
            final String q=question.getText().toString();
            io.execute(()->{try{
                org.json.JSONObject payload=new org.json.JSONObject().put("symbol",baseSymbol(item)).put("kind",kind(item)).put("question",q);
                org.json.JSONObject result=BackendApi.call(prefs.getString("backend",""),SecureTokenStore.read(this),"/ai",payload);
                runOnUiThread(()->{if(request==generation){answer.setText(result.optString("answer"));answer.setTextIsSelectable(true);ask.setEnabled(true);}});
            }catch(Exception e){runOnUiThread(()->{if(request==generation){answer.setText(e.getMessage());ask.setEnabled(true);}});}});
        });
    }

    private void addScalpCard(MarketItem item,org.json.JSONObject report){
        LinearLayout card=column();styleCard(card);card.setPadding(dp(14),dp(14),dp(14),dp(14));
        card.addView(text("اسکلپ ۵ دقیقه‌ای",21,CYAN,true));
        card.addView(text("وزن سناریو • پژوهشی و کالیبره‌نشده",12,AMBER,false));
        TextView details=text("در حال دریافت کندل بسته و دفتر سفارش…",13,MUTED,false);card.addView(details);
        LinearLayout values=row();card.addView(values);content.addView(card,spaced(-1,-2,12));
        Button refresh=button("تازه‌سازی اسکلپ ۵ دقیقه‌ای");card.addView(refresh);
        final int request=generation;
        Runnable load=()->{refresh.setEnabled(false);values.removeAllViews();details.setText("دریافت داده تازه اسکلپ…");io.execute(()->{
            org.json.JSONObject r;try{r=new ScalpRepository().analyze(item.symbol,kind(item));}catch(Exception e){try{r=ScalpEngine.unavailable(e.getMessage());}catch(Exception impossible){return;}}
            final org.json.JSONObject result=r;runOnUiThread(()->{if(request!=generation||isFinishing())return;refresh.setEnabled(true);
                try{report.put("scalp",result);}catch(Exception ignored){}
                com.v2grop.lbankpulse.data.CacheStore.record(this,result.optString("instrument",item.symbol+":"+kind(item)),300,"analysis",result.optString("source","unavailable"),kind(item),result.optLong("last_closed_at"),result.optLong("generated_at"),10000,result.toString());
                details.setText((result.optBoolean("abstain",true)?"عدم معامله • ":"پژوهشی • ")+result.optString("regime")+"\n"+result.optString("reason")+"\n"+
                    result.optString("source")+" • "+result.optString("kind")+" • "+result.optString("quote")+"\nکندل: "+result.optInt("interval_seconds")+" ثانیه • تعداد: "+result.optInt("candles")+
                    "\nآخرین بسته‌شدن: "+(result.has("last_closed_at")?new Date(result.optLong("last_closed_at")):"ناموجود")+"\nسن داده: "+result.optLong("age_seconds")+" ثانیه"+
                    "\nکیفیت: "+result.optString("quality")+"\nنقدشوندگی: "+result.optString("liquidity")+" • نوسان: "+result.optString("volatility")+
                    "\nاسپرد (bp): "+(result.has("spread_bps")?result.optDouble("spread_bps"):"ناموجود")+" • قدرت: "+String.format(Locale.US,"%.2f",result.optDouble("signal_strength",0))+
                    "\nموافق رشد: "+result.optString("bullish_reasons")+"\nموافق نزول: "+result.optString("bearish_reasons")+"\nمحاسبه: "+new Date(result.optLong("generated_at"))+
                    "\nاین تصویر لحظه‌ای است؛ پس از ۱۰ ثانیه دفتر سفارش نیازمند تازه‌سازی است.");
                org.json.JSONObject w=result.optJSONObject("scenario_weights");if(w!=null){String[] k={"up","neutral","down"},labels={"صعود","خنثی","نزول"};int[] colors={GREEN,AMBER,RED};for(int j=0;j<3;j++){LinearLayout box=column();TextView number=text(w.optInt(k[j])+"٪",29,colors[j],true);box.addView(number);box.addView(text(labels[j],12,MUTED,false));android.widget.ProgressBar bar=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(100);bar.setProgress(w.optInt(k[j]));bar.setProgressTintList(android.content.res.ColorStateList.valueOf(colors[j]));box.addView(bar);values.addView(box,new LinearLayout.LayoutParams(0,-2,1));}}
            });
        });};refresh.setOnClickListener(v->load.run());load.run();
    }
    private void addMarketContext(){
        TextView info=cardText("دریافت زمینه بازار • بدون وزن در پیش‌بینی…",MUTED);content.addView(info,spaced(-1,-2,10));final int request=generation;
        io.execute(()->{org.json.JSONObject data=MarketContext.load();runOnUiThread(()->{if(request!=generation||isFinishing())return;StringBuilder b=new StringBuilder("زمینه بازار • صرفاً اطلاع‌رسانی\n");for(String key:new String[]{"dominance","fear_greed"}){org.json.JSONObject d=data.optJSONObject(key);b.append(key.equals("dominance")?"سلطه BTC: ":"ترس و طمع: ");if(d==null||!d.has("value"))b.append("ناموجود");else b.append(d.optDouble("value")).append(" • ").append(d.optString("source")).append(d.optBoolean("stale")?" • قدیمی":" • تازه").append(" • ").append(new Date(d.optLong("timestamp")*1000));b.append("\n");}info.setText(b.toString());});});
    }

    private void renderScenarioWeights(org.json.JSONObject report){
        content.addView(text("وزن سناریوهای تکنیکال",21,TEXT,true),spaced(-1,-2,6));
        content.addView(text("آزمایشی • احتمال آماریِ اعتبارسنجی‌شده نیست",12,AMBER,false),spaced(-1,-2,10));
        Button method=button("انتخاب سبک تحلیل و منابع الهام");
        method.setOnClickListener(v->showStylePicker());
        content.addView(method,spaced(-1,dp(46),10));
        org.json.JSONObject tech=null;
        org.json.JSONArray sources=report.optJSONArray("sources");
        if(sources!=null)for(int i=0;i<sources.length();i++){
            org.json.JSONObject entry=sources.optJSONObject(i);
            if(entry!=null&&entry.optString("name").equals("technical")&&entry.optString("status").equals("ok"))tech=entry.optJSONObject("data");
        }
        if(tech!=null&&!tech.optString("basis_note").isEmpty())content.addView(cardText(tech.optString("basis_note"),AMBER),spaced(-1,-2,8));
        content.addView(text(report.optString("style_mode","سبک فعلی"),15,CYAN,true),spaced(-1,-2,8));
        org.json.JSONArray horizons=report.optJSONArray("style_horizons");
        if(horizons==null)horizons=tech==null?null:tech.optJSONArray("horizons");
        if(horizons==null){
            if(sources!=null)for(int i=0;i<sources.length();i++){org.json.JSONObject e=sources.optJSONObject(i);if(e!=null&&e.optString("name").equals("technical"))content.addView(cardText(e.optString("message"),AMBER),spaced(-1,-2,8));}
            content.addView(cardText("۴ ساعت / ۲۴ ساعت / هفتگی: دادهٔ کافی و تازه برای محاسبه درصدها دریافت نشد. به‌روزرسانی را امتحان کنید.",AMBER),spaced(-1,-2,10));return;}
        LinearLayout longTerm=column();longTerm.setVisibility(View.GONE);
        Button expand=button("تحلیل بلندمدت • یک‌ماهه و سه‌ماهه  ▾");
        expand.setOnClickListener(v->{boolean open=longTerm.getVisibility()!=View.VISIBLE;longTerm.setVisibility(open?View.VISIBLE:View.GONE);expand.setText(open?"بستن تحلیل بلندمدت  ▴":"تحلیل بلندمدت • یک‌ماهه و سه‌ماهه  ▾");});
        for(int i=0;i<horizons.length();i++){
            org.json.JSONObject h=horizons.optJSONObject(i);if(h==null)continue;
            LinearLayout target=h.optInt("hours")>=720?longTerm:content;
            org.json.JSONObject w=h.optJSONObject("scenario_weights");if(w==null){target.addView(cardText(horizonLabel(h.optInt("hours"))+" • "+h.optString("style_label")+"\n"+h.optString("status","داده ناکافی"),AMBER),spaced(-1,-2,10));continue;}
            LinearLayout card=column();styleCard(card);card.setPadding(dp(14),dp(14),dp(14),dp(14));
            card.addView(text(horizonLabel(h.optInt("hours")),18,TEXT,true));
            if(h.optInt("hours")>=720)card.addView(text(h.optString("style_label"),13,CYAN,true));
            LinearLayout metrics=row();metrics.setPadding(0,dp(12),0,dp(12));
            String[] keys={"up","neutral","down"},labels={"صعود","خنثی","نزول"};int[] colors={GREEN,AMBER,RED};
            LinearLayout bar=row();bar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            for(int j=0;j<3;j++){
                int v=w.optInt(keys[j]);LinearLayout box=column();box.setGravity(Gravity.CENTER);
                TextView number=text(v+"٪",29,colors[j],true);number.setGravity(Gravity.CENTER);box.addView(number);
                TextView label=text(labels[j],13,MUTED,false);label.setGravity(Gravity.CENTER);box.addView(label);
                metrics.addView(box,new LinearLayout.LayoutParams(0,-2,1));
                View segment=new View(this);segment.setBackgroundColor(colors[j]);bar.addView(segment,new LinearLayout.LayoutParams(0,dp(7),Math.max(v,1)));
            }
            card.addView(metrics);card.addView(bar);TextView reason=text(w.optString("explanation"),12,MUTED,false);reason.setPadding(0,dp(10),0,dp(6));card.addView(reason);
            card.addView(text(h.optString("basis","منبع: Hyperliquid • داده جانبی"),11,CYAN,false));
            target.addView(card,spaced(-1,-2,10));
        }
        content.addView(expand,spaced(-1,dp(54),10));content.addView(longTerm,spaced(-1,-2,4));
        if(report.has("style_audit")){
            TextView audit=cardText(report.optString("style_audit"),MUTED);audit.setTextIsSelectable(true);audit.setVisibility(View.GONE);Button details=button("جزئیات محاسبه سبک‌ها  ▾");details.setOnClickListener(v->audit.setVisibility(audit.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));content.addView(details,spaced(-1,dp(48),8));content.addView(audit,spaced(-1,-2,10));
            Button copy=button("کپی درخواست تحلیل با سبک انتخابی برای ChatGPT");
            copy.setOnClickListener(v->{android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cb.setPrimaryClip(android.content.ClipData.newPlainText("درخواست تحلیل سبک",ReportHandoff.text(report)));Toast.makeText(this,"درخواست سبک و داده‌های ناقص کپی شد",Toast.LENGTH_LONG).show();});content.addView(copy,spaced(-1,dp(56),12));
        }
        content.addView(cardText("این درصدها وزن آزمایشی سبک‌های انتخابی‌اند، نه احتمال کالیبره یا درصد تغییر قیمت. سبک فاقد داده از ترکیب حذف می‌شود. خبر و فاندامنتال کلان وارد فرمول نشده‌اند.",MUTED),spaced(-1,-2,10));
    }

    private void showStylePicker(){
        SharedPreferences prefs=getSharedPreferences("analysis_styles",0);LinearLayout panel=column();panel.setPadding(dp(18),dp(10),dp(18),dp(10));
        android.widget.CheckBox longStyle=new android.widget.CheckBox(this);longStyle.setText("سبک بلندمدت • دنبال‌کردن روند ۵۰/۲۰۰");longStyle.setTextColor(CYAN);longStyle.setChecked(prefs.getBoolean("long_term_style",true));panel.addView(longStyle);
        panel.addView(text("فعال: تحلیل یک‌ماهه و سه‌ماهه مستقل از تیک‌های پایین با کندل روزانه انجام می‌شود. خاموش: سبک‌های انتخابی پایین اعمال می‌شوند. حداقل ۲۰۰/۳۶۵ روز داده برای سبک بلندمدت لازم است.",12,MUTED,false));
        Button longReference=button("درباره سبک بلندمدت");longReference.setOnClickListener(v->openSource("https://www.fidelity.com/viewpoints/active-investor/moving-averages"));panel.addView(longReference);
        panel.addView(text("VGrop تطبیقی یک مدل پژوهشی قاعده‌محور است، نه شبکه عصبی آموزش‌دیده. برای اجرای آن در افق ماهانه/سه‌ماهه، تیک سبک بلندمدت مستقل را خاموش کنید. نبود شواهد با عدم ورود مشخص می‌شود.",12,AMBER,false));
        android.widget.CheckBox combo=new android.widget.CheckBox(this);combo.setText("تحلیل ترکیبی سبک‌ها");combo.setTextColor(CYAN);combo.setChecked(prefs.getBoolean("combined",false));panel.addView(combo);
        android.widget.CheckBox[] boxes=new android.widget.CheckBox[StyleEngine.NAMES.length];
        for(int i=0;i<StyleEngine.NAMES.length;i++){final int n=i;boxes[i]=new android.widget.CheckBox(this);boxes[i].setText(StyleEngine.NAMES[i]);boxes[i].setTextColor(TEXT);boxes[i].setChecked(prefs.getBoolean("s"+i,i==0));panel.addView(boxes[i]);boxes[i].setOnCheckedChangeListener((v,on)->{if(on&&!combo.isChecked())for(int j=0;j<StyleEngine.NAMES.length;j++)if(j!=n)boxes[j].setChecked(false);});}
        combo.setOnCheckedChangeListener((v,on)->{if(!on){boolean kept=false;for(android.widget.CheckBox b:boxes)if(b.isChecked()){if(kept)b.setChecked(false);kept=true;}}});
        panel.addView(text("آدرس عمومی حساب‌های منتخب Hyperliquid؛ حداکثر ۳ آدرس، هر خط یکی. مالکیت و نهنگ‌بودن به‌صورت خودکار تأیید نمی‌شود.",12,MUTED,false));
        EditText addresses=input("0x…",prefs.getString("addresses",""));addresses.setMinLines(2);addresses.setMaxLines(4);panel.addView(addresses,spaced(-1,dp(90),8));
        panel.addView(text("گزینه الیوت با دونچیان جایگزین شده است. اسمارت‌مانی قاعده‌محور است. جریان سفارش تصویر لحظه‌ای و فقط برای وزن ۴ساعته است. ترکیب سهم برابر دارد؛ سبک بدون داده حذف و علت نمایش داده می‌شود. همه درصدها آزمایشی‌اند.",12,AMBER,false));
        Button sources=button("منابع و حدود روش‌ها");sources.setOnClickListener(v->new android.app.AlertDialog.Builder(this).setMessage("موتور فعلی: اصول بولینگر. دونچیان: کانال سقف/کف گذشته و روند میانگین‌ها؛ وزن‌دهی اختصاصی و آزمایشی. اسمارت‌مانی: قواعد مستقل شکست و جاروب؛ انتساب به ICT ندارد. Hyperliquid: دفتر سفارش و معاملات آدرس‌های منتخب؛ تحلیل کامل آنچین نیست.").setPositiveButton("Hyperliquid",(d,w)->openSource("https://hyperliquid.gitbook.io/hyperliquid-docs/for-developers/api/info-endpoint")).setNeutralButton("دونچیان",(d,w)->openSource("https://www.tradingview.com/support/solutions/43000502253-donchian-channels-dc/")).show());panel.addView(sources);
        ScrollView scroll=new ScrollView(this);scroll.addView(panel);panel.setBackgroundColor(CARD);
        android.app.AlertDialog dialog=new android.app.AlertDialog.Builder(this).setTitle("سبک تحلیل • الهام از روش‌ها").setView(scroll).setPositiveButton("اعمال و تحلیل",null).setNegativeButton("بستن",null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{int count=0;for(android.widget.CheckBox b:boxes)if(b.isChecked())count++;if(count==0){Toast.makeText(this,"حداقل یک سبک انتخاب کن",Toast.LENGTH_SHORT).show();return;}
            try{StyleResearch.addresses(addresses.getText().toString());}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();return;}
            SharedPreferences.Editor edit=prefs.edit().putBoolean("long_term_style",longStyle.isChecked()).putBoolean("combined",combo.isChecked()).putString("addresses",addresses.getText().toString().trim());for(int i=0;i<StyleEngine.NAMES.length;i++)edit.putBoolean("s"+i,boxes[i].isChecked());edit.apply();dialog.dismiss();showAnalysis(selected);
        }));dialog.show();
    }

    private void addChatGptHandoff(org.json.JSONObject report){
        Button share=button("✦  تحلیل با ChatGPT\nبا حساب خودت • بدون سرور");
        share.setTextSize(18);share.setTypeface(Typeface.DEFAULT,Typeface.BOLD);share.setTextColor(Color.WHITE);
        share.setGravity(Gravity.CENTER);share.setPadding(dp(14),dp(12),dp(14),dp(12));share.setMinHeight(dp(84));
        GradientDrawable aiBackground=new GradientDrawable(GradientDrawable.Orientation.TR_BL,new int[]{Color.rgb(185,35,77),Color.rgb(133,38,174),Color.rgb(88,40,176)});
        aiBackground.setCornerRadius(dp(22));aiBackground.setStroke(dp(1),Color.rgb(239,140,209));
        share.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x44ffffff),aiBackground,null));
        share.setElevation(dp(6));
        share.setOnClickListener(v->{
            String payload=ReportHandoff.text(report);
            EditText preview=input("گزارش آماده برای ChatGPT",payload);preview.setMinLines(5);preview.setMaxLines(9);preview.setTextDirection(View.TEXT_DIRECTION_RTL);
            new android.app.AlertDialog.Builder(this).setTitle("ارسال با حساب فعال خودت")
                .setMessage("گزارش را کپی کن و در ChatGPT بچسبان و ارسال کن. حساب را داخل ChatGPT انتخاب یا عوض کن. پاسخ خودکار به این اپ برنمی‌گردد.")
                .setView(preview).setPositiveButton("کپی و بازکردن ChatGPT",(d,w)->{
                    android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Market Pulse",preview.getText().toString()));
                    openSource("https://chatgpt.com/");
                    Toast.makeText(this,"گزارش کپی شد؛ در کادر پیام ChatGPT بچسبان",Toast.LENGTH_LONG).show();
                }).setNeutralButton("اشتراک‌گذاری",(d,w)->{
                    android.content.Intent send=new android.content.Intent(android.content.Intent.ACTION_SEND);
                    send.setType("text/plain");send.putExtra(android.content.Intent.EXTRA_TEXT,preview.getText().toString());
                    try{startActivity(android.content.Intent.createChooser(send,"انتخاب ChatGPT یا برنامه مقصد"));}catch(android.content.ActivityNotFoundException e){Toast.makeText(this,"برنامه‌ای برای دریافت متن پیدا نشد",Toast.LENGTH_SHORT).show();}
                }).setNegativeButton("بستن",null).show();
        });content.addView(share,spaced(-1,-2,16));
    }

    private void addGoldPanel(){
        content.addView(text("GOLD DESK • تحلیل اختصاصی طلا",20,AMBER,true),spaced(-1,-2,8));
        content.addView(cardText("چارچوب مرجع: شورای جهانی طلا (World Gold Council). بررسی فرصت نگهداری طلا از مسیر دلار و نرخ بهره، در کنار روند تکنیکال. این اپ مدل رسمی GRAM یا سرویس یک تحلیلگر انسانی نیست. XAUT توکن طلاست و با XAU/USD تفاوت بازار و ریسک ناشر دارد.",TEXT),spaced(-1,-2,8));
        Button source=button("پژوهش و مدل شورای جهانی طلا");source.setOnClickListener(v->openSource("https://www.gold.org/goldhub/tools/gold-return-attribution-model"));content.addView(source,spaced(-1,dp(44),8));
        TextView result=cardText("در حال بررسی دلار و بازده اوراق آمریکا…",AMBER);content.addView(result,spaced(-1,-2,10));final int request=generation;
        io.execute(()->{String message;try{message=GoldResearch.explain(UsMacro.load());}catch(Exception e){message="داده کلان طلا دریافت نشد؛ درصدهای بالا فقط تکنیکال هستند. پژوهش مرجع از دکمه بالا قابل بازکردن است.";}final String body=message;runOnUiThread(()->{if(request==generation)result.setText(body);});});
    }

    private void openSource(String url){
        try{startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)));}
        catch(android.content.ActivityNotFoundException e){Toast.makeText(this,"مرورگر برای بازکردن منبع پیدا نشد",Toast.LENGTH_SHORT).show();}
    }
    private void showUsMacro(){reset("فاندامنتال آمریکا","اولویت: نرخ بهره، تورم، اشتغال، دلار و بازده اوراق");addUsMacroPanel();}
    private void addUsMacroPanel(){
        content.addView(text("فاندامنتال کلان آمریکا",21,TEXT,true),spaced(-1,-2,10));
        Button calendar=button("تقویم اصلی • Investing.com");calendar.setOnClickListener(v->openSource("https://m.investing.com/economic-calendar/"));content.addView(calendar,spaced(-1,dp(52),8));
        content.addView(cardText("در تقویم Investing کشور United States (آمریکا) و منطقه زمانی خود را انتخاب کنید. مقدار واقعی، پیش‌بینی، قبلی و زمان خبر را همان‌جا ببینید. داده تقویم به‌صورت خودکار وارد موتور نشده است.",MUTED),spaced(-1,-2,8));
        LinearLayout shortcuts=row();
        Button stocks=button("بازار سهام آمریکا");stocks.setOnClickListener(v->openSource("https://www.investing.com/indices/major-indices"));shortcuts.addView(stocks,new LinearLayout.LayoutParams(0,dp(46),1));
        Button rates=button("جلسات فدرال رزرو");rates.setOnClickListener(v->openSource("https://www.federalreserve.gov/monetarypolicy/fomccalendars.htm"));shortcuts.addView(rates,new LinearLayout.LayoutParams(0,dp(46),1));content.addView(shortcuts,spaced(-1,-2,8));
        TextView data=cardText("دریافت داده رسمی آمریکا از FRED…",CYAN);content.addView(data,spaced(-1,-2,10));
        final int request=generation;
        io.execute(()->{try{
            org.json.JSONObject report=UsMacro.load();StringBuilder b=new StringBuilder("داده رسمی • "+report.optString("source")+"\n");
            org.json.JSONArray items=report.getJSONArray("items");
            for(int i=0;i<items.length();i++){
                org.json.JSONObject d=items.getJSONObject(i);b.append("\n").append(d.optString("label")).append("\n");
                if(!d.optString("status").equals("ok")){b.append("داده کافی در پاسخ منبع نبود.\n");continue;}
                b.append(String.format(Locale.US,"%.3f",d.getDouble("value"))).append(" ").append(d.optString("unit"));
                if(!d.isNull("previous"))b.append("\nمشاهده قبلی: ").append(String.format(Locale.US,"%.3f",d.getDouble("previous")));
                b.append("\n").append(d.optBoolean("monthly")?"دوره آماری (تاریخ انتشار نیست): ":"تاریخ مشاهده: ").append(d.optString("observation_date"));
                if(d.optBoolean("stale"))b.append("\nهشدار: تاریخ مشاهده قدیمی است.");
                b.append("\n").append(d.optString("note")).append("\n").append(d.optString("url")).append("\n");
            }
            b.append("\nدریافت: ").append(new Date(report.optLong("retrieved_at")*1000));
            b.append("\nاین داده‌ها ممکن است تأخیری یا بازنگری‌شده باشند. انتظار بازار و اثر قطعی بر کریپتو از این اعداد به‌تنهایی مشخص نمی‌شود؛ درصدهای سناریو همچنان تکنیکال هستند.");
            runOnUiThread(()->{if(request==generation&&!isFinishing()){data.setText(b.toString());data.setTextIsSelectable(true);android.text.util.Linkify.addLinks(data,android.text.util.Linkify.WEB_URLS);data.setLinkTextColor(CYAN);}});
        }catch(Exception e){runOnUiThread(()->{if(request==generation)data.setText("دریافت خودکار داده رسمی آمریکا ناموفق بود. تقویم Investing و منابع بالا همچنان قابل بازکردن‌اند؛ عدد قدیمی یا ساختگی جایگزین نشده است.");});}});
    }

    private void addFundAlertSettings(){
        SharedPreferences prefs=getSharedPreferences("fund_alert",0);
        content.addView(text("هشدار فاندامنتال آمریکا",21,AMBER,true),spaced(-1,-2,8));
        android.widget.Switch enabled=new android.widget.Switch(this);enabled.setText("اعلان رویدادهای سه‌ستاره Investing");enabled.setTextColor(TEXT);enabled.setChecked(prefs.getBoolean("enabled",false));content.addView(enabled,spaced(-1,dp(54),8));
        TextView status=cardText(prefs.getString("status","هنوز بررسی انجام نشده است.")+(prefs.contains("checked")?"\nآخرین تلاش: "+new Date(prefs.getLong("checked",0)):""),MUTED);
        enabled.setOnCheckedChangeListener((v,on)->{prefs.edit().putBoolean("enabled",on).apply();if(on&&android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},810);FundAlertJob.schedule(this);});
        Button time=button(String.format(Locale.US,"زمان بررسی: %02d:%02d نیویورک",prefs.getInt("hour",8),prefs.getInt("minute",30)));
        time.setOnClickListener(v->new android.app.TimePickerDialog(this,(picker,h,m)->{prefs.edit().putInt("hour",h).putInt("minute",m).apply();time.setText(String.format(Locale.US,"زمان بررسی: %02d:%02d نیویورک",h,m));FundAlertJob.schedule(this);},prefs.getInt("hour",8),prefs.getInt("minute",30),true).show());content.addView(time,spaced(-1,dp(48),8));
        content.addView(cardText("پیش‌فرض: ۸:۳۰ نیویورک، یک ساعت پیش از بازگشایی معمول سهام آمریکا. دوشنبه تا جمعه؛ تغییر ساعت تابستانی خودکار است. تعطیلات بورس جداگانه فیلتر نمی‌شوند. اندروید ممکن است اجرای پس‌زمینه را به تأخیر بیندازد؛ زمان دقیق تضمین نیست. در هر روز یک بررسی موفق و در صورت خبر مهم یک اعلان ارسال می‌شود؛ بعضی داده‌ها خودِ ساعت ۸:۳۰ منتشر می‌شوند.",MUTED),spaced(-1,-2,8));
        Button check=button("بررسی تقویم همین حالا");content.addView(check,spaced(-1,dp(48),8));content.addView(status,spaced(-1,-2,8));final int request=generation;
        check.setOnClickListener(v->{check.setEnabled(false);status.setText("در حال دریافت تقویم Investing…");io.execute(()->{String result;try{org.json.JSONArray rows=FundCalendar.fetch(java.time.LocalDate.now(FundCalendar.NY));result=rows.length()==0?"رویداد سه‌ستاره آمریکا برای امروز ثبت نشده است.":FundCalendar.summary(rows);}catch(Exception e){result="بررسی ناموفق؛ به معنی نبود خبر نیست. "+e.getMessage();}final String message=result;runOnUiThread(()->{if(request==generation){status.setText(message);check.setEnabled(true);}});});});
        Button test=button("آزمایش نمایش اعلان");test.setOnClickListener(v->{boolean sent=FundAlertJob.notify(this,"آزمایش هشدار Market Pulse","این پیام آزمایشی است و خبر اقتصادی نیست.");Toast.makeText(this,sent?"اعلان آزمایشی ارسال شد":"مجوز اعلان را در تنظیمات اندروید فعال کن",Toast.LENGTH_LONG).show();});content.addView(test,spaced(-1,dp(46),12));
    }

    private void showSettings() {
        reset("دو روش استفاده از هوش مصنوعی", "تحلیل اصلی با اینترنت و بدون سرور شخصی کار می‌کند. تنظیمات زیر فقط برای دستیار اختیاری هستند.");
        addFundAlertSettings();
        content.addView(cardText("روش ۲ • بدون سرور شخصی: در صفحه تحلیل، دکمه ChatGPT را بزن؛ گزارش را کپی و در ChatGPT ارسال کن. استفاده با حساب فعال همان برنامه یا مرورگر انجام می‌شود. ایمیل و رمز را در Market Pulse وارد نکن. این روش اتصال خودکار حساب یا دریافت خودکار پاسخ نیست.",CYAN),spaced(-1,-2,10));
        Button chat=button("بازکردن ChatGPT برای ورود یا تغییر حساب");chat.setOnClickListener(v->openSource("https://chatgpt.com/"));content.addView(chat,spaced(-1,dp(50),10));
        content.addView(text("روش ۱ • اتصال خودکار از طریق سرور",19,TEXT,true),spaced(-1,-2,10));
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        android.widget.Switch enabled=new android.widget.Switch(this);
        enabled.setText("فعال‌کردن هوش مصنوعی اختیاری");enabled.setTextColor(TEXT);
        enabled.setChecked(prefs.getBoolean("ai_enabled",false));content.addView(enabled,spaced(-1,dp(55),10));
        EditText backend = input("نشانی سرور؛ مثال: https://api.example.com", prefs.getString("backend", ""));
        EditText token = input("توکن دسترسی سرور (الزامی)", readServerToken());
        token.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        content.addView(backend, spaced(-1, dp(58), 7));
        content.addView(token, spaced(-1, dp(58), 7));
        Button save = button("ذخیره تنظیمات");
        save.setOnClickListener(v -> {
            if(enabled.isChecked()&&(!backend.getText().toString().trim().startsWith("https://")||token.getText().toString().trim().isEmpty())){Toast.makeText(this,"برای فعال‌کردن دستیار، نشانی HTTPS و توکن را وارد کنید",Toast.LENGTH_SHORT).show();return;}
            try{SecureTokenStore.save(this,token.getText().toString().trim());}catch(Exception e){Toast.makeText(this,"ذخیره امن توکن ناموفق بود",Toast.LENGTH_LONG).show();return;}
            prefs.edit().putBoolean("ai_enabled",enabled.isChecked()).putString("backend", backend.getText().toString().trim()).apply();
            Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show();
        });
        content.addView(save, spaced(-1, dp(54), 8));
        Button test=button("آزمایش اتصال سرور");content.addView(test,spaced(-1,dp(54),8));
        backend.setEnabled(enabled.isChecked());token.setEnabled(enabled.isChecked());test.setEnabled(enabled.isChecked());
        enabled.setOnCheckedChangeListener((b,on)->{backend.setEnabled(on);token.setEnabled(on);test.setEnabled(on);if(!on)prefs.edit().putBoolean("ai_enabled",false).apply();});
        TextView status=cardText("برای استفاده معمولی، هوش مصنوعی را خاموش بگذارید. آزمایش اتصال بدون مصرف مدل انجام می‌شود.",MUTED);content.addView(status);
        final int request=generation;
        test.setOnClickListener(v->{final String b=backend.getText().toString().trim(),t=token.getText().toString().trim();test.setEnabled(false);io.execute(()->{try{
            org.json.JSONObject r=BackendApi.call(b,t,"/health",null);
            runOnUiThread(()->{if(request==generation){status.setText(r.optBoolean("ai_configured")?"سرور متصل است؛ تنظیمات مدل موجود است. صحت کلید با اولین درخواست مشخص می‌شود.":"سرور متصل است؛ کلید یا مدل هوش مصنوعی تنظیم نشده است.");test.setEnabled(true);}});
        }catch(Exception e){runOnUiThread(()->{if(request==generation){status.setText(e.getMessage());test.setEnabled(true);}});}});});
        content.addView(cardText("برای تعویض حساب OpenAI در آینده، فقط کلید روی سرور عوض می‌شود و نیازی به ساخت دوبارهٔ APK نیست. کلید اصلی هرگز نباید در این صفحه وارد شود.", MUTED), spaced(-1, -2, 8));
        content.addView(cardText("تکنیکال، بنیادی، خبرهای منابع پشتیبانی‌شده و مقایسه صرافی‌ها داخل اپ اجرا می‌شوند. تنها گفت‌وگو با هوش مصنوعی به سرور نیاز دارد. درصدهای احتمالاتی هنوز اعتبارسنجی نشده‌اند.", CYAN), spaced(-1, -2, 8));
    }

    private String readServerToken(){try{return SecureTokenStore.read(this);}catch(Exception e){Toast.makeText(this,"توکن امن قابل خواندن نیست؛ دوباره وارد کنید",Toast.LENGTH_LONG).show();return "";}}
    private void showError(String title, Exception error) {
        content.removeAllViews();
        content.addView(text(title, 21, RED, true));
        content.addView(cardText(error.getMessage() == null ? "خطای ناشناخته" : error.getMessage(), MUTED), spaced(-1, -2, 12));
        Button retry = button("تلاش دوباره");
        retry.setOnClickListener(v -> showAnalysis(selected));
        content.addView(retry, spaced(-1, dp(54), 8));
    }

    private EditText input(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setHintTextColor(MUTED); e.setTextColor(TEXT); e.setText(value);
        e.setTextDirection(View.TEXT_DIRECTION_LTR); e.setPadding(dp(14), 0, dp(14), 0);
        e.setBackground(roundRect(CARD, 14));
        return e;
    }

    private TextView cardText(String value, int color) {
        TextView t = text(value, 13, color, false);
        t.setPadding(dp(14), dp(13), dp(14), dp(13));
        t.setBackground(roundRect(CARD, 14));
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(TEXT); b.setTextSize(12); b.setAllCaps(false);
        b.setMinHeight(0);b.setMinimumHeight(0);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(4),0,dp(4),0);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x3353dfcc),roundRect(Color.rgb(23,49,64),12),null));
        return b;
    }

    private TextView text(String value, float size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private int assetColor(String symbol){
        String[] symbols={"BTC","ETH","XAUT","XTI","DOGE","SOL","BNB","XRP","SUI","SEI","PEPE","FARTCOIN","NOT","AUCTION","KEKIUS","VINE","DOLO","SPX"};
        String[] colors={"#DA871C","#7761DA","#B69235","#597083","#B69739","#8361DD","#BE921F","#4B8DCE","#328CCF","#B85162","#4F9B43","#AD684B","#626A84","#AB59C2","#548E69","#309678","#7962C8","#B8569F"};
        for(int i=0;i<symbols.length;i++)if(symbols[i].equals(symbol))return Color.parseColor(colors[i]);
        return Color.HSVToColor(new float[]{(symbol.hashCode()&0x7fffffff)%360,0.55f,0.65f});
    }

    private void styleCard(View view) {
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(32,39,68),Color.rgb(18,28,48)});
        bg.setCornerRadius(dp(18));bg.setStroke(dp(1),Color.rgb(63,66,100));view.setBackground(bg);view.setElevation(dp(2));
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }

    private LinearLayout.LayoutParams spaced(int w, int h, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(0, 0, 0, dp(bottom));
        return p;
    }

    private LinearLayout.LayoutParams centered(int w, int h) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.gravity = Gravity.CENTER;
        return p;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private String formatPrice(double p) {
        if (p >= 1000) return String.format(Locale.US, "$%,.2f", p);
        if (p >= 1) return String.format(Locale.US, "$%.4f", p);
        return String.format(Locale.US, "$%.8f", p);
    }

    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("selected_symbol",selected.symbol);state.putString("selected_display",selected.display);state.putString("selected_kind",selected.marketType);}
    @Override protected void onDestroy() {
        priceHandler.removeCallbacksAndMessages(null);priceLoop=null;
        if(analysisTask!=null)analysisTask.cancel(true);
        io.shutdownNow();
        super.onDestroy();
    }
}
