package com.v2grop.lbankpulse;

import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.os.SystemClock;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.UiController;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(AndroidJUnit4.class)
public class WatchlistNoticeUiTest {
    static void invoke(MainActivity a,String name)throws Exception{
        java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);
    }
    @Test public void realDragPersistsAcrossRecreation()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            final List<MarketItem>[] original=new List[1];
            scenario.onActivity(a->{try{
                original[0]=Watchlist.load(a);
                Watchlist.save(a,Arrays.asList(new MarketItem("btc_usdt","BTC / USDT","نقدی"),new MarketItem("eth_usdt","ETH / USDT","نقدی"),new MarketItem("doge_usdt","DOGE / USDT","نقدی")));
                invoke(a,"showFavorites");
            }catch(Exception e){throw new AssertionError(e);}});
            onView(withContentDescription("فهرست ارزهای قابل جابه‌جایی")).perform(new ViewAction(){
                public Matcher<View> getConstraints(){return isDisplayed();}
                public String getDescription(){return "Long press first card and drag below second card";}
                public void perform(UiController ui,View view){
                    RecyclerView r=(RecyclerView)view;View first=r.findViewHolderForAdapterPosition(0).itemView;
                    View second=r.findViewHolderForAdapterPosition(1).itemView;
                    int[] p=new int[2];first.getLocationOnScreen(p);
                    float x=p[0]+first.getWidth()*.5f,y=p[1]+first.getHeight()*.5f;
                    int[] q=new int[2];second.getLocationOnScreen(q);float end=q[1]+second.getHeight()*.7f;
                    long down=SystemClock.uptimeMillis();send(ui,down,MotionEvent.ACTION_DOWN,x,y);ui.loopMainThreadForAtLeast(700);
                    for(int i=1;i<=20;i++){send(ui,down,MotionEvent.ACTION_MOVE,x,y+(end-y)*i/20);ui.loopMainThreadForAtLeast(25);}
                    send(ui,down,MotionEvent.ACTION_UP,x,end);ui.loopMainThreadForAtLeast(400);
                }
            });
            scenario.onActivity(a->assertEquals("eth_usdt",Watchlist.load(a).get(0).symbol));
            scenario.recreate();
            scenario.onActivity(a->{assertEquals("eth_usdt",Watchlist.load(a).get(0).symbol);Watchlist.save(a,original[0]);});
        }
    }
    static void send(UiController ui,long down,int action,float x,float y){
        MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);
        try{if(!ui.injectMotionEvent(event))throw new AssertionError("Gesture injection failed");}
        catch(androidx.test.espresso.InjectEventSecurityException e){throw new AssertionError(e);}finally{event.recycle();}
    }
    @Test public void noticeHiddenEnabledDismissedAndDisabled()throws Exception{
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            scenario.onActivity(a->{try{
                AnnouncementStore store=new AnnouncementStore(a);store.clear();invoke(a,"renderAnnouncement");
                java.lang.reflect.Field f=MainActivity.class.getDeclaredField("announcementSlot");f.setAccessible(true);ViewGroup slot=(ViewGroup)f.get(a);
                assertEquals(View.GONE,slot.getVisibility());assertEquals(0,slot.getChildCount());
                String raw="{\"schema_version\":1,\"enabled\":true,\"id\":\"ui-test-unique\",\"title\":\"آزمون\",\"text\":\"متن فارسی\"}";
                a.getSharedPreferences("announcement",0).edit().remove("dismissed").commit();
                store.accept(raw,System.currentTimeMillis());invoke(a,"renderAnnouncement");assertEquals(View.VISIBLE,slot.getVisibility());
                store.dismiss("ui-test-unique");invoke(a,"renderAnnouncement");assertEquals(View.GONE,slot.getVisibility());
                store.accept("{\"enabled\":false}",System.currentTimeMillis());assertNull(store.current(System.currentTimeMillis()));
                store.accept(raw,System.currentTimeMillis());store.accept("bad-json",System.currentTimeMillis());assertNull(store.current(System.currentTimeMillis()));
                store.clear();
            }catch(Exception e){throw new AssertionError(e);}});
        }
    }
}
