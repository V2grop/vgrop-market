package com.v2grop.lbankpulse;
import android.view.View;import androidx.test.core.app.ActivityScenario;import androidx.test.ext.junit.runners.AndroidJUnit4;import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;import org.junit.runner.RunWith;import org.json.JSONObject;import java.util.*;
import static androidx.test.espresso.Espresso.onView;import static androidx.test.espresso.matcher.ViewMatchers.*;import static androidx.test.espresso.action.ViewActions.*;import static androidx.test.espresso.assertion.ViewAssertions.matches;import static org.junit.Assert.*;
/** Deterministic screen fixtures: no external exchange API required. */
@RunWith(AndroidJUnit4.class)
public class CompatibilityUiTest {
 private static void invoke(MainActivity a,String name)throws Exception{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(a);}
 @Test public void catalogSearchAndRecreation()throws Exception{
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{try{
    assertEquals(View.LAYOUT_DIRECTION_RTL,((android.view.ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0).getLayoutDirection());
    java.lang.reflect.Field f=MainActivity.class.getDeclaredField("allMarkets");f.setAccessible(true);f.set(a,new ArrayList<>(Arrays.asList(new MarketItem("btc_usdt","BTC / USDT","نقدی"),new MarketItem("eth_usdt","ETH / USDT","نقدی"))));invoke(a,"showAllMarkets");
   }catch(Exception e){throw new AssertionError(e);}});
   onView(withHint("نام یا نماد؛ مثل بیت‌کوین، BTC یا DOGE")).perform(replaceText("بیت‌کوین"),closeSoftKeyboard());
   onView(withText("1 نتیجه برای «بیت‌کوین»")).check(matches(isDisplayed()));
   onView(withContentDescription("پاک‌کردن جست‌وجو")).perform(click());
   onView(withText("2 بازار فعال")).check(matches(isDisplayed()));
   scenario.onActivity(a->{MarketItem item=new MarketItem("btc_usdt","BTC / USDT","نقدی");boolean before=Watchlist.contains(a,item);Watchlist.toggle(a,item);assertEquals(!before,Watchlist.contains(a,item));Watchlist.toggle(a,item);});
   scenario.recreate();onView(withText("دیده‌بان بازار")).check(matches(isDisplayed()));
  }
 }
 @Test public void scalpUnavailableAndSecureStorage()throws Exception{
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
   scenario.onActivity(a->{try{
    SecureTokenStore.save(a,"instrumentation-only-token");assertEquals("instrumentation-only-token",SecureTokenStore.read(a));assertFalse(a.getSharedPreferences("settings",0).contains("token"));SecureTokenStore.save(a,"");
    java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("addScalpCard",MarketItem.class,JSONObject.class);m.setAccessible(true);m.invoke(a,new MarketItem("xti_usdt","XTI / USDT","نقدی"),new JSONObject());
   }catch(Exception e){throw new AssertionError(e);}});
   onView(withText("اسکلپ ۵ دقیقه‌ای")).perform(scrollTo()).check(matches(isDisplayed()));
  }
 }
}
