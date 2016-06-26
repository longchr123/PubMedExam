package com.example.administrator.kaoyan.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.administrator.kaoyan.R;
import com.example.administrator.kaoyan.adapter.XiTiListAdapter;
import com.example.administrator.kaoyan.bean.ExamSmallItem;
import com.example.administrator.kaoyan.util.ConfigUtil;
import com.example.administrator.kaoyan.util.SQLiteUtil;
import com.umeng.analytics.MobclickAgent;

import net.sqlcipher.database.SQLiteDatabase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class UpDataActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private ListView lv;
    private XiTiListAdapter adapter;
    private List<ExamSmallItem> examSmallItems = new ArrayList<>();
    private SQLiteDatabase examSqLite;
    private SharedPreferences sp;
    private InputStream inputStream;
    private OutputStream outputStream;
    private URLConnection connection;
    private Dialog dialog1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1://下载完成
                    dialog1.dismiss();
                    Toast.makeText(UpDataActivity.this, "更新完成", Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_up_data);
        initView();
        initData();
        initListener();
    }

    private void initListener() {
        lv.setOnItemClickListener(this);
    }

    private void initData() {
        adapter = new XiTiListAdapter(examSmallItems, this);
        lv.setAdapter(adapter);
        sp = getSharedPreferences(ConfigUtil.spSave, Activity.MODE_PRIVATE);
        getDataFromSqLite();
    }

    private void getDataFromSqLite() {
        examSqLite = SQLiteDatabase.openOrCreateDatabase(ConfigUtil.examTypeFileName, ConfigUtil.mi_ma, null);
        examSmallItems = SQLiteUtil.getExamTableData(examSqLite, "exam");
        adapter.setData(examSmallItems);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        if (sp.getBoolean(position + "", true)) {
            Toast.makeText(this, "还没有数据哦，请先下载", Toast.LENGTH_SHORT).show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("确定更新数据吗？");
            builder.setTitle("提示");
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog1 = new AlertDialog.Builder(UpDataActivity.this).setTitle("更新中...").
                            setView(new ProgressBar(UpDataActivity.this)).setCancelable(false).show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            downFile(examSmallItems.get(position).getFormalDBURL(), position);
                        }
                    }).start();
                }
            });
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    }

    private void initView() {
        lv = (ListView) findViewById(R.id.lv);
    }

    private void downFile(String urlString, int position) {
        try {
            URL url = new URL(urlString);
            connection = url.openConnection();
            if (connection.getReadTimeout() == 5) {
                Toast.makeText(this, "网络连接有问题", Toast.LENGTH_SHORT).show();
            }
            inputStream = connection.getInputStream();

        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            outputStream = openFileOutput(ConfigUtil.getNormalSqLite(position), Context.MODE_PRIVATE);
            byte[] buffer = new byte[200];
            //开始读取
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            Message message2 = new Message();
            message2.what = 1;
            handler.sendMessage(message2);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
