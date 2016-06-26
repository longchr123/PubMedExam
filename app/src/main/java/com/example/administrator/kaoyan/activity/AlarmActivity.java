package com.example.administrator.kaoyan.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.administrator.kaoyan.R;
import com.example.administrator.kaoyan.util.ConfigUtil;
import com.umeng.analytics.MobclickAgent;

import java.util.Calendar;
import java.util.Date;

public class AlarmActivity extends AppCompatActivity {
    private TextView tv_time;
    private ToggleButton toggleButton;
    private ImageView iv_back;
    private RelativeLayout rl;
    private View view;
    private TimePicker tp;
    private TimeListener times;
    private int chooseHour, chooseMinute;
    private String alarmHour, alarmMinute;
    private Dialog dialog;
    private AlertDialog.Builder builder;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private boolean openAlarm;

    private RelativeLayout rl_exam;
    private Calendar calendar;
    private TextView tv_exam;
    private TextView tv_dis;
    private ToggleButton dateTB;
    private boolean openDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        initView();
        initData();
        createView();
        initListener();
    }

    private void createView() {
        builder = new AlertDialog.Builder(AlarmActivity.this);
        builder.setTitle("设置闹铃时间");
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editor.putInt("alarmHour", chooseHour);
                editor.putInt("alarmMinute", chooseMinute);
                editor.commit();
                setTimeType();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);
        dialog = builder.create();
    }

    private void initData() {
        tp.setIs24HourView(true);
        times = new TimeListener();
        sp = getSharedPreferences(ConfigUtil.spSave, Activity.MODE_PRIVATE);
        chooseHour = sp.getInt("alarmHour", 20);
        chooseMinute = sp.getInt("alarmMinute", 0);
        setTimeType();
        openAlarm = sp.getBoolean("openAlarm", true);
        toggleButton.setChecked(openAlarm);

        if (sp.getInt("year",0)>0){
            int year=sp.getInt("year",0);
            int month=sp.getInt("month",0);
            int day=sp.getInt("day", 0);
            Date    endDate    =   new    Date(year-1900,month,day);
            Date    curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
            month++;
            tv_exam.setText( year+ "年" + month + "月" + day + "日考试");
            tv_dis.setText("距离考试还有" + getGapCount(curDate, endDate) + "天");
        }
        openDate=sp.getBoolean("openDate", true);
        dateTB.setChecked(openDate);
        editor = sp.edit();
    }

    public void setTimeType() {
        if (chooseHour < 10) {
            alarmHour = "0" + chooseHour;
        } else {
            alarmHour = "" + chooseHour;
        }
        if (chooseMinute < 10) {
            alarmMinute = "0" + chooseMinute;
        } else {
            alarmMinute = "" + chooseMinute;
        }
        tv_time.setText(alarmHour + ":" + alarmMinute + "  每日闹铃");
    }

    //dialog的监听器
    private void initListener() {
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(AlarmActivity.this, "闹铃已开启", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AlarmActivity.this, "闹铃已关闭", Toast.LENGTH_SHORT).show();
                }
                editor.putBoolean("openAlarm", isChecked);
                editor.commit();
            }
        });
        dateTB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(AlarmActivity.this, "考试提醒已开启", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AlarmActivity.this, "考试提醒已关闭", Toast.LENGTH_SHORT).show();
                }
                editor.putBoolean("openDate", isChecked);
                editor.commit();
            }
        });
        rl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });
        tp.setOnTimeChangedListener(times);

        rl_exam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(AlarmActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int day) {
                        // TODO Auto-generated method stub
                        Date    endDate    =   new    Date(year-1900,month,day);
                        Date    curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
                        editor.putInt("year",year);
                        editor.putInt("month",month);
                        editor.putInt("day",day);
                        editor.commit();
                        month++;
                        tv_exam.setText(year + "年" + month + "月" + day + "日考试");
                        tv_dis.setText("距离考试还有" + getGapCount(curDate,endDate)+"天");

                    }
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
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

    private void initView() {
        view = LayoutInflater.from(AlarmActivity.this).inflate(R.layout.alarm_dialog, null);
        tv_time = (TextView) findViewById(R.id.tv_time);
        toggleButton = (ToggleButton) findViewById(R.id.tb);
        dateTB = (ToggleButton) findViewById(R.id.tb_exam);
        iv_back = (ImageView) findViewById(R.id.iv_back);
        rl = (RelativeLayout) findViewById(R.id.rl);
        rl_exam = (RelativeLayout) findViewById(R.id.rl_exam);
        tp = (TimePicker) view.findViewById(R.id.tp);
        tv_exam= (TextView) findViewById(R.id.tv_exam);
        tv_dis= (TextView) findViewById(R.id.tv_dis);
        calendar=Calendar.getInstance();
    }

    //timePicker监听器
    class TimeListener implements TimePicker.OnTimeChangedListener {

        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            // TODO Auto-generated method stub
            chooseHour = hourOfDay;
            chooseMinute = minute;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
