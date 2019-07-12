//安卓美的遥控
package com.example.controller.activity;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.ConsumerIrManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.widget.TextView;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.example.controller.R;
import com.example.controller.airConditioner.AirConditionerState;
import com.example.controller.code.CommandCode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;


public class RemoteControlActivity extends AppCompatActivity implements View.OnClickListener, IStatus {
    private static final String TAG = "SharedPreferencesairinfo";
    private final String AirInfo = "airstate"; //保存数据SharedPreferences文件的名字;


    private ConsumerIrManager IR;
    private AirConditionerState acs;  //空调状态对象，缩写
    private int InfraredFrequency = 38000;  //红外线频率 38KHz
    private int[] setArray = CommandCode.set;
    private int[] closeArray = CommandCode.power_off;
    private int[] verticalArray = CommandCode.vertical;
    private TextView temp,modeShow,windSpeed,airWindDir,windDirAuto;
    private Map<String, Object> params;
    private MyRecognizer myRecognizer;
    private Handler handler;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.controller);
        initEvent();
        initUI();
        initPermission();
        handler = new Handler() {

            /*
             * @param msg
             */
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                handleMsg(msg);
            }

        };
        IRecogListener listener = new MessageStatusRecogListener(handler);
        myRecognizer = new MyRecognizer(this, listener);
        params = new LinkedHashMap<String, Object>();
    }

    private void handleMsg(Message msg) {
        switch (msg.what) { // 处理MessageStatusRecogListener中的状态回调
            case STATUS_FINISHED:
                if (msg.arg2 == 1) {
                    String voice_result = new String(msg.obj.toString());
                    yuyinShibie(voice_result);
                    sendNormalIR(acs);
                    show();
                    Toast.makeText(this, "发送命令成功"+voice_result, Toast.LENGTH_SHORT).show();
                }
                break;
            case STATUS_NONE:
            case STATUS_READY:
            case STATUS_SPEAKING:
            case STATUS_RECOGNITION:
                break;
            default:
                break;

        }
    }
    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    private void initEvent() {
        //获取ConsumerIrManager实例
        IR = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        acs = new AirConditionerState();
        getAirconditionState();
    }

    private void initUI() {

        Button power = findViewById(R.id.btn_power);
        Button tmp_up = findViewById(R.id.btn_tmp_up);
        Button tmp_down = findViewById(R.id.btn_tmp_down);
        Button mode = findViewById(R.id.btn_mode);
        Button win_speed = findViewById(R.id.btn_wind_speed);
        Button win_direction = findViewById(R.id.btn_wind_direction);
        Button voice = findViewById(R.id.btn_voice);
        win_speed.setOnClickListener(this);
        win_direction.setOnClickListener(this);
        power.setOnClickListener(this);
        mode.setOnClickListener(this);
        tmp_up.setOnClickListener(this);
        tmp_down.setOnClickListener(this);
        voice.setOnClickListener(this);
        temp = findViewById(R.id.tmp_display);
        modeShow = findViewById(R.id.mode);
        windSpeed = findViewById(R.id.text_speed);
        airWindDir = findViewById(R.id.text_direct);
    }
    @Override
    public void onClick(View v) {
        int x;      //一个暂存数据的局部变量
        switch (v.getId()){
            case R.id.btn_power :
                if(acs.getmPower()==0){

                    acs.setmPower(1);
                    sendNormalIR(acs);
                    saveAirconditionerState();
                    show();
                }else {
                    acs.setmPower(0);
                    sendCloseIR();
                    saveAirconditionerState();
                    temp.setText("");
                    modeShow.setText("");
                    windSpeed.setText("");
                    airWindDir.setText("");
                }
                break;
            case R.id.btn_mode :
                x = (acs.getmMode() + 1) % 5 ;
                acs.setmMode(x);
                if(acs.getmConstantWind())
                    acs.setmConstantWind(false);
                if(acs.getmTmpUndefined())
                    acs.setmTmpUndefined(false);
                switch (x){
                    case 0: acs.setmConstantWind(true); break;
                    case 2: acs.setmConstantWind(true); break;
                    case 4: acs.setmTmpUndefined(true); break;  //送风模式，设置温度为未定义
                }
                sendNormalIR(acs);
                saveAirconditionerState();
                show();
                break;
            case R.id.btn_tmp_up:
                x = acs.getmTmp();
                if(acs.getmMode() != 4)
                {
                    x = x < 30 ? x+1 : x;
                    acs.setmTmp(x);
                }
                sendNormalIR(acs);
                saveAirconditionerState();
                show();
                break;
            case R.id.btn_tmp_down :
                x = acs.getmTmp();
                if(acs.getmMode() != 4){
                    x = x > 17 ? x-1 : x;
                    acs.setmTmp(x);
                }
                sendNormalIR(acs);
                saveAirconditionerState();
                show();
                break;
            case R.id.btn_wind_speed:
                x = acs.getmWindSpeed();
                int mode = acs.getmMode();
                if(mode != 0 && mode != 2) {
                    x = (x + 1) % 4;
                    acs.setmWindSpeed(x);
                }
                sendNormalIR(acs);
                show();
                break;
            case R.id.btn_wind_direction:
                x = acs.getmWindDir();
                if(x==0){
                    IR.transmit(InfraredFrequency, verticalArray);
                    acs.setmWindDir(1);
                }
                else{
                    IR.transmit(InfraredFrequency, verticalArray);
                    acs.setmWindDir(0);
                }
                show();
                break;
            case R.id.btn_voice:
                myRecognizer.start(params);
                break;
        }
    }
    private void show(){
        int tmp = acs.getmTmp();
        int mod = acs.getmMode();
        int spd = acs.getmWindSpeed();
        int dire = acs.getmWindDir();
        String str2;
        String str3;
        String str4;
        if(mod==0) str2 = "模式：自动";
        else if(mod==1) str2 = "模式：制冷";
        else if(mod==2) str2 = "模式：抽湿";
        else if(mod==3) str2 = "模式：制热";
        else str2 = "模式：送风";

        if(spd==0) str3 = "风速：自动";
        else if(spd==1) str3 = "风速：低风";
        else if(spd==2) str3 = "风速：中风";
        else if(spd==3) str3 = "风速：高风";
        else str3 = "模式：固定风";

        if(dire==0) str4 = "风向：左右";
        else str4 = "风向：上下";

        String str1 = ""+tmp+"℃";
        temp.setText(str1);
        modeShow.setText(str2);
        windSpeed.setText(str3);
        airWindDir.setText(str4);
    }
    private void sendCloseIR() {
        //关闭空调
        //发射关闭空调的编码
        IR.transmit(InfraredFrequency, closeArray);
    }

    private void sendNormalIR(AirConditionerState acs) {
        //将空调的状态解析成可被安卓红外控制类识别的整数数组，然后发射
        int[] B = getCodeOfB(acs);
        int[] C = getCodeOfC(acs);
        System.arraycopy(B, 0, setArray, 34, B.length);
        System.arraycopy(C, 0, setArray, 66, B.length);
        System.arraycopy(B, 0, setArray, 134, B.length);
        System.arraycopy(C, 0, setArray, 166, B.length);
        int[] Bt = reverse(B);
        int[] Ct = reverse(C);
        System.arraycopy(Bt, 0, setArray, 50, Bt.length);
        System.arraycopy(Ct, 0, setArray, 82, Bt.length);
        System.arraycopy(Bt, 0, setArray, 150, Bt.length);
        System.arraycopy(Ct, 0, setArray, 182, Bt.length);
        IR.transmit(InfraredFrequency, CommandCode.set);
    }

    private int[] reverse(int[] arr) {
        for(int i = 1; i < arr.length; i += 2){
            arr[i] = (arr[i] == 1620) ? 540 : 1620;
        }
        return arr;
    }

    private int[] getCodeOfB(AirConditionerState acs) {
        //根据空调的状态获取对应的B模块的01代码，再用高低电平持续时间替换0或1
        int[] B = new int[8];
        //begin
        B[0] = B[1] = B[2] = B[3] = B[4] = 1;
        int mWindSpeed;
        mWindSpeed = acs.getmWindSpeed();
        if(acs.getmConstantWind())
            mWindSpeed = 4;
        if(mWindSpeed == 0) { B[7] = 1; B[6] = 0; B[5] = 1;}//自动 101
        else if(mWindSpeed == 1) {B[7] = 1; B[6] = 0; B[5] = 0;} //低风 100
        else if(mWindSpeed == 2) {B[7] = 0; B[6] = 1; B[5] = 0;} //中风 010
        else if(mWindSpeed == 3) {B[7] = 0; B[6] = 0; B[5] = 1;} // 高风 001
        else {B[7] = 0; B[6] = 0; B[5] = 0;} //固定风 000

        return changeToMilitime(B);
    }
    private int[] getCodeOfC(AirConditionerState acs) {
        //根据空调的状态获取对应的模块的01代码，再用高低电平持续时间替换0或1
        int[] C = new int[8];
        C[0] = C[1] = 0;
        int mode = acs.getmMode();
        int Tmp = acs.getmTmp();
        if(mode == 0) { C[2] = 0; C[3] = 1;} //自动
        else if(mode == 1) { C[2] = 0; C[3] = 0;} //制冷
        else if(mode == 2) { C[2] = 1; C[3] = 0;} //抽湿
        else if(mode == 3) { C[2] = 1; C[3] = 1;} //制热
        else if(mode == 4) { C[2] = 0; C[3] = 1;} //送风 ， 仅在温度为无定义 1110时，有效；

        //温度模式；
        if(Tmp == 17) {C[7] = 0; C[6] = 0; C[5] = 0; C[4] = 0;} //17- - 0000
        else if(Tmp == 18) {C[7] = 0; C[6] = 0; C[5] = 0; C[4] = 1;}//18- - 0001
        else if(Tmp == 19) {C[7] = 0; C[6] = 0; C[5] = 1; C[4] = 1;}//19- - 0011
        else if(Tmp == 20) {C[7] = 0; C[6] = 0; C[5] = 1; C[4] = 0;}//20- - 0010
        else if(Tmp == 21) {C[7] = 0; C[6] = 1; C[5] = 1; C[4] = 0;}//21- - 0110
        else if(Tmp == 22) {C[7] = 0; C[6] = 1; C[5] = 1; C[4] = 1;}//22- - 0111
        else if(Tmp == 23) {C[7] = 0; C[6] = 1; C[5] = 0; C[4] = 1;}//23- -0101
        else if(Tmp == 24) {C[7] = 0; C[6] = 1; C[5] = 0; C[4] = 0;}//24- -0100
        else if(Tmp == 25) {C[7] = 1; C[6] = 1; C[5] = 0; C[4] = 0;}//25- -1100
        else if(Tmp == 26) {C[7] = 1; C[6] = 1; C[5] = 0; C[4] = 1;}//26- - 1101
        else if(Tmp == 27) {C[7] = 1; C[6] = 0; C[5] = 0; C[4] = 1;}//27- -1001
        else if(Tmp == 28) {C[7] = 1; C[6] = 0; C[5] = 0; C[4] = 0;}//28- -1000
        else if(Tmp == 29) {C[7] = 1; C[6] = 0; C[5] = 1; C[4] = 0;}//29- -1010
        else               {C[7] = 1; C[6] = 0; C[5] = 1; C[4] = 1;}//30- -1011

        if(acs.getmTmpUndefined()){
            C[7] = 1; C[6] = 1; C[5] = 1; C[4] = 0;
        }

        return changeToMilitime(C);
    }
    private int[] changeToMilitime(int[] arr){
        //把01状态转为 电平数值；
        int zeroLow = 540, zeroHigh = 540, oneLow = 540, oneHigh = 1620;
        int[] result = new int[16];
        int j = 0;
        //将C数组中的数据，按7-0的顺序，把1和0扩展成onelow，onehign，zerolow，zerohigh，且顺序为C[7]-c[0]
        for(int i=7; i>=0; i--){
            if(arr[i] == 0) {
                result[j*2] = zeroLow;
                result[j*2+1] = zeroHigh;
            }
            else {
                result[2*j] = oneLow;
                result[2*j+1] = oneHigh;
            }
            j++;
        }
        return result;
    }

    public void getAirconditionState(){
        SharedPreferences airinfo = getSharedPreferences(AirInfo, MODE_PRIVATE);
        //String username = airinfo.getString("username", null);//读取username
        int wendu = airinfo.getInt("wendu", 23);//读取温度，23度
        int fengsu = airinfo.getInt("fengsu",1);//风速，低风
        int fengxiang = airinfo.getInt("fengxiang", 1);//垂直
        int moshi = airinfo.getInt("moshi", 1);//读取模式，默认制冷
        Boolean mTmpUndefined = airinfo.getBoolean("mTmpUndefined",false);
        Boolean mConstantWind = airinfo.getBoolean("mConstantWind",false);
        acs.setmWindDir(fengxiang);
        acs.setmWindSpeed(fengsu);
        acs.setmMode(moshi);
        acs.setmTmp(wendu);

        acs.setmTmpUndefined(mTmpUndefined);
        acs.setmConstantWind(mConstantWind);
        //设置温度，风速，风向，模式
    }
    private void saveAirconditionerState() {
        //向文件写入空调状态 使用SharedPreferences
        SharedPreferences airinfo = getSharedPreferences(AirInfo,MODE_PRIVATE );
        SharedPreferences.Editor editor = airinfo.edit();// 获取Editor
        //得到Editor后，写入需要保存的数据；
        editor.putInt("wendu",acs.getmTmp());
        editor.putInt("fengsu",acs.getmWindSpeed());
        editor.putInt("fengxiang",acs.getmWindDir());
        editor.putInt("moshi",acs.getmMode());
        editor.putBoolean("mTmpUndefined",acs.getmTmpUndefined());
        editor.putBoolean("mConstantWind",acs.getmConstantWind());
        editor.commit(); //提交修改；
    }




    //语音识别后的字符串传入，可实现对空调状态的改变
    public void yuyinShibie(String yuyin){
        //目前语音可识别温度，空调模式，风速的控制；识别字符串后，直接改变空调状态
        String wendu = "温度";
        String moshi = "模式";
        String fengsu = "风速";
        String fengxiang = "风向";
        if(yuyin.contains(wendu)){
            Log.d("语音识别结果", "yuyinShibie: "+yuyin);
            if(yuyin.contains("十七"))       acs.setmTmp(17);
            else if(yuyin.contains("17"))    acs.setmTmp(17);
            else if(yuyin.contains("十八"))  acs.setmTmp(18);
            else if(yuyin.contains("18"))    acs.setmTmp(18);
            else if(yuyin.contains("十九"))  acs.setmTmp(19);
            else if(yuyin.contains("19"))    acs.setmTmp(19);
            else if(yuyin.contains("二十一"))  acs.setmTmp(21);
            else if(yuyin.contains("21"))    acs.setmTmp(21);
            else if(yuyin.contains("二十二"))  acs.setmTmp(22);
            else if(yuyin.contains("22"))    acs.setmTmp(22);
            else if(yuyin.contains("二十三"))  acs.setmTmp(23);
            else if(yuyin.contains("23"))    acs.setmTmp(23);
            else if(yuyin.contains("二十四"))  acs.setmTmp(24);
            else if(yuyin.contains("24"))    acs.setmTmp(24);
            else if(yuyin.contains("二十五"))  acs.setmTmp(25);
            else if(yuyin.contains("25"))    acs.setmTmp(25);
            else if(yuyin.contains("二十六"))   acs.setmTmp(26);
            else if(yuyin.contains("26"))    acs.setmTmp(26);
            else if(yuyin.contains("二十七"))   acs.setmTmp(27);
            else if(yuyin.contains("27"))    acs.setmTmp(27);
            else if(yuyin.contains("二十八"))  acs.setmTmp(28);
            else if(yuyin.contains("28"))    acs.setmTmp(28);
            else if(yuyin.contains("二十九"))  acs.setmTmp(29);
            else if(yuyin.contains("29"))    acs.setmTmp(29);
            else if(yuyin.contains("三十"))    acs.setmTmp(30);
            else if(yuyin.contains("30"))    acs.setmTmp(30);
            else if(yuyin.contains("二十"))  acs.setmTmp(20);
            else if(yuyin.contains("20"))    acs.setmTmp(20);
        }
        if(yuyin.contains(moshi)) {
            if(yuyin.contains("自动")) {
                acs.setmMode(0);
                acs.setmConstantWind(true);
            }
            else if(yuyin.contains("制冷"))   acs.setmMode(1);
            else if(yuyin.contains("制热"))    acs.setmMode(3);
            else if(yuyin.contains("送风")) {
                acs.setmMode(4);
                acs.setmTmpUndefined(true);
            }
            else if(yuyin.contains("抽湿")) {
                acs.setmMode(2);
            }
        }
        if (yuyin.contains(fengsu)) {
            if(yuyin.contains("低风")) acs.setmWindSpeed(1);
            else if(yuyin.contains("中风")) acs.setmWindSpeed(2);
            else if(yuyin.contains("高风")) acs.setmWindSpeed(3);
            else if(yuyin.contains("自动")) acs.setmWindSpeed(0);
        }
        if (yuyin.contains( fengxiang )){
            if(yuyin.contains("水平")) acs.setmWindDir(0);
            else if(yuyin.contains("垂直"))acs.setmWindDir(1);
        }
    }

}
