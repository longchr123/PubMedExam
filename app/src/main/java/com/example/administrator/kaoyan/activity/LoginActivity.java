package com.example.administrator.kaoyan.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.administrator.kaoyan.MainActivity;
import com.example.administrator.kaoyan.R;
import com.example.administrator.kaoyan.util.ConfigUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.utils.SMSLog;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText et_phone, et_yanzheng;
    private TextView tv_forget, tv_get,tv_remind;
    private Button bt_login;
    private SharedPreferences sp;
    private RequestQueue mQueue;
    private StringRequest stringRequest;
    private int time = 60;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        initView();
        initListener();
        SMSSDK.initSDK(this, "13252dbb3e788", "57daae40e4a3a991e5c01960d103a9ec");
        EventHandler eh = new EventHandler() {
            @Override
            public void afterEvent(int event, int result, Object data) {
                Message msg = new Message();
                msg.arg1 = event;
                msg.arg2 = result;
                msg.obj = data;
                handler.sendMessage(msg);
            }
        };
        SMSSDK.registerEventHandler(eh);
    }

    private void initListener() {
        tv_get.setOnClickListener(this);
        bt_login.setOnClickListener(this);
    }

    private void initView() {
        et_phone = (EditText) findViewById(R.id.et_phone);
        et_yanzheng = (EditText) findViewById(R.id.et_yanzheng);
        tv_get = (TextView) findViewById(R.id.tv_get);
        bt_login = (Button) findViewById(R.id.bt_login);
        tv_remind = (TextView) findViewById(R.id.tv_remind);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_get:
                if (!TextUtils.isEmpty(et_phone.getText().toString().trim())) {
                    if (et_phone.getText().toString().trim().length() == 11) {
                        tv_get.setVisibility(View.GONE);
                        SMSSDK.getVerificationCode("86", et_phone.getText().toString().trim());
                    } else {
                        Toast.makeText(LoginActivity.this, "请输入完整手机号码", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "请输入您的手机号码", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bt_login:
                SMSSDK.submitVerificationCode("86", et_phone.getText().toString().trim(), et_yanzheng.getText().toString().trim());
                break;
        }
    }


    //验证码送成功后提示文字
    private void reminderText() {
        tv_remind.setVisibility(View.VISIBLE);
        handlerText.sendEmptyMessageDelayed(1, 1000);
    }

    Handler handlerText = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                if (time > 0) {
                    tv_remind.setText("验证码已发送" + time + "秒");
                    time--;
                    handlerText.sendEmptyMessageDelayed(1, 1000);
                } else {
                    tv_remind.setText("提示信息");
                    time = 60;
                    tv_remind.setVisibility(View.GONE);
                    tv_get.setVisibility(View.VISIBLE);
                }
            } else {
                et_yanzheng.setText("");
                tv_remind.setText("提示信息");
                time = 60;
                tv_remind.setVisibility(View.GONE);
                tv_get.setVisibility(View.VISIBLE);
            }
        }
    };

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {

            super.handleMessage(msg);
            int event = msg.arg1;
            int result = msg.arg2;
            Object data = msg.obj;
            Log.e("event", "event=" + event);
            System.out.println("--------result---0" + event + "--------*" + result + "--------" + data);
            if (result == SMSSDK.RESULT_COMPLETE) {
                System.out.println("--------result---1" + event + "--------*" + result + "--------" + data);
                //短信注册成功后，返回MainActivity,然后提示新好友
                if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {//提交验证码成功
                    create();
                } else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE) {
                    //已经验证
                    Toast.makeText(getApplicationContext(), "验证码已经发送", Toast.LENGTH_SHORT).show();
                    reminderText();
                }
            } else {
                tv_get.setVisibility(View.VISIBLE);
                int status = 0;
                try {
                    ((Throwable) data).printStackTrace();
                    Throwable throwable = (Throwable) data;

                    JSONObject object = new JSONObject(throwable.getMessage());
                    String des = object.optString("detail");
                    status = object.optInt("status");
                    if (!TextUtils.isEmpty(des)) {
                        Toast.makeText(LoginActivity.this, des, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (Exception e) {
                    SMSLog.getInstance().w(e);
                }
            }
        }
    };
    private void create() {
        mQueue = Volley.newRequestQueue(this);
        String httpUrl = "http://www.zhongyuedu.com/api/tk_ky_new_login.php";//公司服务器
        stringRequest = new StringRequest(Request.Method.POST, httpUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String resultCode = jsonObject.getString("resultCode");
                    String result = jsonObject.getString("result");
                    if (resultCode.equals("0")) {
                        sp = getSharedPreferences(ConfigUtil.spSave, Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("number", et_phone.getText().toString().trim());
                        editor.commit();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                        Toast.makeText(LoginActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(LoginActivity.this, "网络连接有问题", Toast.LENGTH_SHORT).show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("username", et_phone.getText().toString().trim());
                return map;
            }
        };
        mQueue.add(stringRequest);
    }
}
