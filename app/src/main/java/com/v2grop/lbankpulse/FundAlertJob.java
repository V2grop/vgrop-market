package com.v2grop.lbankpulse;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import java.time.*;
import org.json.*;
public final class FundAlertJob extends JobService {
 static final int ID=10010;static final String CHANNEL="us_fund_events";
 private Thread worker;
 static void schedule(Context c){
  JobScheduler scheduler=c.getSystemService(JobScheduler.class);
  if(!c.getSharedPreferences("fund_alert",0).getBoolean("enabled",false)){scheduler.cancel(ID);return;}
  scheduler.schedule(new JobInfo.Builder(ID,new ComponentName(c,FundAlertJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(15*60*1000L).setPersisted(true).build());
 }
 static boolean notify(Context c,String title,String body){
  NotificationManager manager=c.getSystemService(NotificationManager.class);
  manager.createNotificationChannel(new NotificationChannel(CHANNEL,"رویدادهای مهم آمریکا",NotificationManager.IMPORTANCE_DEFAULT));
  if(!manager.areNotificationsEnabled())return false;
  if(manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE)return false;
  Intent target=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(FundCalendar.URL));
  PendingIntent open=PendingIntent.getActivity(c,ID,target,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  manager.notify(ID,new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(open).setAutoCancel(true).build());return true;
 }
 @Override public boolean onStartJob(JobParameters params){
  SharedPreferences prefs=getSharedPreferences("fund_alert",0);ZonedDateTime now=ZonedDateTime.now(FundCalendar.NY);
  if(!prefs.getBoolean("enabled",false)||!FundCalendar.due(now,prefs.getInt("hour",8),prefs.getInt("minute",30),prefs.getString("last_day","")))return false;
  worker=new Thread(()->{
   try{
    JSONArray events=FundCalendar.fetch(now.toLocalDate());
    if(Thread.currentThread().isInterrupted())return;
    if(!prefs.getBoolean("enabled",false))return;
    String text=events.length()==0?"رویداد سه‌ستاره آمریکا در تقویم امروز نبود.":FundCalendar.summary(events);
    if(events.length()>0&&!notify(this,events.length()+" رویداد مهم آمریکا • امروز",text))throw new IllegalStateException("مجوز اعلان یا کانال اعلان خاموش است.");
    prefs.edit().putString("last_day",now.toLocalDate().toString()).putString("status",text).putLong("checked",System.currentTimeMillis()).apply();
   }catch(Exception e){prefs.edit().putString("status","بررسی ناموفق؛ نتیجه خبری مشخص نیست: "+e.getMessage()).putLong("checked",System.currentTimeMillis()).apply();}
   finally{jobFinished(params,false);}
  },"fund-calendar");worker.start();return true;
 }
 @Override public boolean onStopJob(JobParameters params){if(worker!=null)worker.interrupt();return true;}
}
