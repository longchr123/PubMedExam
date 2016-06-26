package com.example.administrator.kaoyan;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.kaoyan.fragment.CuoTiFragment;
import com.example.administrator.kaoyan.fragment.MoreFragment;
import com.example.administrator.kaoyan.fragment.NewsFragment;
import com.example.administrator.kaoyan.fragment.XiTiFragment;
import com.example.administrator.kaoyan.util.ConfigUtil;
import com.example.administrator.kaoyan.util.ExampleUtil;
import com.umeng.analytics.MobclickAgent;

import net.sqlcipher.database.SQLiteDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import cn.jpush.android.api.InstrumentedActivity;


public class MainActivity extends InstrumentedActivity implements RadioGroup.OnCheckedChangeListener {
    private RadioGroup rg;
    private FragmentManager manager;
    private XiTiFragment xiTiFragment;
    private NewsFragment newsFragment;
    private CuoTiFragment cuoTiFragment;
    private MoreFragment moreFragment;
    private Fragment mCurrentFragment;
    private FragmentTransaction transaction;
    private SharedPreferences sp;
    private int alarmHour, alarmMinute;
    private boolean openAlarm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SQLiteDatabase.loadLibs(this);
        initView();
        initDate();
        initListener();
        //开启闹铃
        if (openAlarm) {
            setAlarm(alarmHour, alarmMinute);
        }
        showExamDateDialog();
    }

    /**
     * 显示
     */
    private void showExamDateDialog() {

        if (!sp.getBoolean("openDate",true)){
            return;
        }
        int year=sp.getInt("year",2016);
        int month=sp.getInt("month",11);
        int day=sp.getInt("day", 12);
        Date endDate    =   new    Date(year-1900,month,day);
        Date curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
        month++;
        Dialog dialog=null;

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view= LayoutInflater.from(MainActivity.this).inflate(R.layout.date_dialog, null);
        ((TextView)view.findViewById(R.id.dialog_tv_date)).setText("考试时间：" + year + "年" + month + "月" + day + "日考试");
        ((TextView)view.findViewById(R.id.dialog_tv_dis)).setText("距离考试还有" + getGapCount(curDate, endDate) + "天");
        builder.setView(view);
        dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        if(isTablet(MainActivity.this)){
            dialog.getWindow().setLayout(dip2px(this, 400), dip2px(this, 260));
        }else {
            dialog.getWindow().setLayout(dip2px(this, 300), dip2px(this, 260));
        }
    }
    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 判断当前设备是手机还是平板，代码来自 Google I/O App for Android
     * @param context
     * @return 平板返回 True，手机返回 False
     */
    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    private void setAlarm(int hour, int minute) {
        long systemTime = System.currentTimeMillis();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // 这里时区需要设置一下，不然会有8个小时的时间差
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        // 选择的定时时间
        long selectTime = calendar.getTimeInMillis();
        // 如果当前时间大于设置的时间，那么就从第二天的设定时间开始
        if (systemTime > selectTime) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            selectTime = calendar.getTimeInMillis();
        }
        // 计算现在时间到设定时间的时间差
        long time = selectTime - systemTime;
        long firstTime = systemTime + time;
        setAlarmTime(this, firstTime);
    }

    private void initDate() {
        manager = getFragmentManager();
        xiTiFragment = new XiTiFragment();
        newsFragment = new NewsFragment();
        cuoTiFragment = new CuoTiFragment();
        moreFragment = new MoreFragment();
        transaction = manager.beginTransaction();
        transaction.add(R.id.lin, xiTiFragment).commit();
        mCurrentFragment = xiTiFragment;
        sp = this.getSharedPreferences(ConfigUtil.spSave, Activity.MODE_PRIVATE);
        alarmHour = sp.getInt("alarmHour", 20);
        alarmMinute = sp.getInt("alarmMinute", 0);
        openAlarm = sp.getBoolean("openAlarm", true);
    }

    private void initListener() {
        rg.setOnCheckedChangeListener(this);
    }

    private void initView() {
        rg = (RadioGroup) findViewById(R.id.rg);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        transaction = manager.beginTransaction();
        switch (checkedId) {
            case R.id.rb_xiti:
                changeFragment(xiTiFragment);
                break;
            case R.id.rb_news:
                changeFragment(newsFragment);
                break;
            case R.id.rb_cuoti:
                changeFragment(cuoTiFragment);
                break;
            case R.id.rb_more:
                changeFragment(moreFragment);
                break;
        }
    }

    //要先hide再show
    public void changeFragment(Fragment fragment) {
        if (fragment == null) {
            return;
        }
        transaction = manager.beginTransaction();
        if (mCurrentFragment != null) {
            transaction.hide(mCurrentFragment);
        }
        if (fragment.isAdded()) {
            transaction.show(fragment);
        } else {
            transaction.add(R.id.lin, fragment);
        }
        transaction.commit();
        mCurrentFragment = fragment;
    }

    private void setAlarmTime(Context context, long firstTime) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("android.alarm.kaoyan.action");
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        int interval = 60 * 1000 * 60 * 24;//闹铃时间间隔
        am.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, interval, sender);
    }

    /**
     * 获取两个日期之间的间隔天数
     * @return
     */
    public static int getGapCount(Date startDate, Date endDate) {
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.setTime(startDate);
        fromCalendar.set(Calendar.HOUR_OF_DAY, 0);
        fromCalendar.set(Calendar.MINUTE, 0);
        fromCalendar.set(Calendar.SECOND, 0);
        fromCalendar.set(Calendar.MILLISECOND, 0);

        Calendar toCalendar = Calendar.getInstance();
        toCalendar.setTime(endDate);
        toCalendar.set(Calendar.HOUR_OF_DAY, 0);
        toCalendar.set(Calendar.MINUTE, 0);
        toCalendar.set(Calendar.SECOND, 0);
        toCalendar.set(Calendar.MILLISECOND, 0);
        return (int) ((toCalendar.getTime().getTime() - fromCalendar.getTime().getTime()) / (1000 * 60 * 60 * 24));
    }

    private long mExitTime;

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            } else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public static boolean isForeground = false;

    @Override
    protected void onResume() {
        isForeground = true;
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        isForeground=false;
        super.onPause();
        MobclickAgent.onPause(this);
    }

    private MessageReceiver mMessageReceiver;
    public static final String MESSAGE_RECEIVED_ACTION = "com.example.jpushdemo.MESSAGE_RECEIVED_ACTION";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXTRAS = "extras";

    public void registerMessageReceiver() {
        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MESSAGE_RECEIVED_ACTION);
        registerReceiver(mMessageReceiver, filter);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                String messge = intent.getStringExtra(KEY_MESSAGE);
                String extras = intent.getStringExtra(KEY_EXTRAS);
                StringBuilder showMsg = new StringBuilder();
                showMsg.append(KEY_MESSAGE + " : " + messge + "\n");
                if (!ExampleUtil.isEmpty(extras)) {
                    showMsg.append(KEY_EXTRAS + " : " + extras + "\n");
                }
            }
        }
    }
}
