package com.example.smarthome;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.smarthome.dao.DeviceBean;
import com.example.smarthome.okhttputil.CallBackUtil;
import com.example.smarthome.okhttputil.OkhttpUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.Call;

public class MainActivity extends AppCompatActivity {

    MenuItem menuItemHome;
    MenuItem menuItemInfo;
    int a = 0;
    private int pageNow = 0;


    private Context mContext;

    static final String BASEURL = "http://192.168.199.215:8000";

    private LinearLayout editor;
    private FloatingActionButton floatingActionButton;


    // change the fragment by navigation selection
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            View fragment = findViewById(R.id.nav_host_fragment);
            NavController controller= Navigation.findNavController(fragment);
            BottomNavigationView navigation = findViewById(R.id.nav_view);
            switch (item.getItemId()) {
                case R.id.home:
                    if(pageNow != 0){
                        controller.navigate(R.id.action_navigation_info_to_navigation_home);
                        pageNow = 0;
                        menuItemHome.setIcon(R.drawable.home_on);
                        menuItemInfo.setIcon(R.drawable.heart);
                        showFab(Boolean.TRUE);
                    }
                    return true;
                case R.id.heart:
                    if(pageNow == 0){
                        controller.navigate(R.id.action_navigation_home_to_navigation_info);
                        pageNow = 1;
                        menuItemHome.setIcon(R.drawable.home);
                        menuItemInfo.setIcon(R.drawable.heart_on);
                        showFab(Boolean.FALSE);
                    }
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        BottomNavigationView navigation = findViewById(R.id.nav_view);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        menuItemHome = navigation.getMenu().findItem(R.id.home);
        menuItemInfo = navigation.getMenu().findItem(R.id.heart);
        // the sacn button
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()){
                    case R.id.fab:
                        /*以下是启动我们自定义的扫描活动*/
                        IntentIntegrator intentIntegrator = new IntentIntegrator(MainActivity.this);
                        intentIntegrator.setBeepEnabled(true);
                        /*设置启动我们自定义的扫描活动，若不设置，将启动默认活动*/
                        intentIntegrator.setCaptureActivity(ScanActivity.class);
                        intentIntegrator.initiateScan();
                        break;
                }
            }
        });

        sendReceiver(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        editor = findViewById(R.id.page_home_edit);
        floatingActionButton = findViewById(R.id.fab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private String getMac(Context context){
        WifiManager mWifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String netMac = null;
        if (mWifi.isWifiEnabled()) {
            WifiInfo wifiInfo = mWifi.getConnectionInfo();
            netMac =  wifiInfo.getBSSID();
        }
        return netMac;
    }

    public void scenDoorImp(){
        String nowMac = getMac(this);
        SharedPreferences read = getSharedPreferences("info", MODE_PRIVATE);
        String value = read.getString("mac", "None");
        Log.i("zkity", nowMac + "|" + value);
        if(nowMac.equals(value)){
            ArrayList<DeviceBean> arrayList = new ArrayList<>();
            try {
                arrayList = getDevice();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            for(int i=0; i<arrayList.size(); i++){
                DeviceBean deviceBean = new DeviceBean();
                deviceBean = arrayList.get(i);
                if(deviceBean.getDeviceType().equals("b")){
                    if(deviceBean.getScenDoor().equals("1")){
                        final DeviceBean finalDeviceBean = deviceBean;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //要延时的程序
                                String url = MainActivity.BASEURL + "/light/control/" + finalDeviceBean.getDeviceID() + "/on";
                                OkhttpUtil.okHttpGet(url, new CallBackUtil.CallBackString() {
                                    @Override
                                    public void onFailure(Call call, Exception e) {
                                        Log.i("zkity", "send fail" + e.toString());
                                    }

                                    @Override
                                    public void onResponse(String response) {
                                        Toast.makeText(MainActivity.this,"Door Light",Toast.LENGTH_SHORT).show();
                                        Log.i("zkity", "send success");
                                    }
                                });
                            }
                        },2000); //2000为毫秒单位

                    }
                }
            }
        }else{

        }
    }

    private void sendReceiver(MainActivity context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        WifiStateReceiver wifiStateReceiver = new WifiStateReceiver(context);
        registerReceiver(wifiStateReceiver, filter);
    }

    private void showEditor(Boolean show){
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) editor.getLayoutParams();
        params.weight=LinearLayout.LayoutParams.MATCH_PARENT;
        if(show){
            params.height=LinearLayout.LayoutParams.MATCH_PARENT;
        }else{
            params.height=0;
        }
        editor.setLayoutParams(params);
    }

    private void showFab(Boolean show){
        if(show){
            floatingActionButton.setVisibility(View.VISIBLE);
        }else{
            floatingActionButton.setVisibility(View.GONE);
        }
    }

    /**
     * 更新设别列表的配置
     * @param deviceBean
     */
    protected void updateDevice(DeviceBean deviceBean){
        ArrayList<DeviceBean> arrayList = new ArrayList<>();
        try {
            arrayList = getDevice();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for(int i=0; i<arrayList.size(); i++){
            String did = arrayList.get(i).getDeviceID();
            if(did.equals(deviceBean.getDeviceID())){
                arrayList.get(i).setDeviceName(deviceBean.getDeviceName());
                arrayList.get(i).setScenDoor(deviceBean.getScenDoor());
                arrayList.get(i).setScenVoice(deviceBean.getScenVoice());
                arrayList.get(i).setScenAuto(deviceBean.getScenAuto());
            }
        }
        SharedPreferences.Editor editor = getSharedPreferences("device", MODE_PRIVATE).edit();
        String deviceString = "";
        try {
            deviceString = listToJsonString(arrayList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.putString("list", deviceString);
        editor.commit();
    }

    public void updateList(){
        ArrayList<DeviceBean> deviceBeans = new ArrayList<>();
        try {
            deviceBeans = getDevice();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LinearLayout homeContainer = new LinearLayout(this);
        homeContainer = findViewById(R.id.page_home_container);
        if(homeContainer != null){
            homeContainer.removeAllViews();
            DeviceBean device = new DeviceBean();
            for(int i=0; i<deviceBeans.size(); i++){
                device = deviceBeans.get(i);
                homeContainer.addView(addItem(device));
            }
        }else{
            Toast.makeText(this,"is null",Toast.LENGTH_SHORT).show();
        }


    }

    private View addItem(final DeviceBean device){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LayoutInflater inflater3 = LayoutInflater.from(this);
        View view = new View(this);
        if(device.getDeviceType().equals("b")){
            view = inflater3.inflate(R.layout.light_card, null);
        }else if(device.getDeviceType().equals("c")){
            view = inflater3.inflate(R.layout.curtain_card, null);
        }
        view.setLayoutParams(lp);
        view.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                showEditor(Boolean.TRUE);
                showFab(Boolean.FALSE);
                // TODO: 处理点击事件
                final EditText deviceName = findViewById(R.id.page_home_edit_device_name);
                deviceName.setText(device.getDeviceName());
                final Switch door = findViewById(R.id.page_home_edit_door);
                if(device.getScenDoor().equals("0")){
                    door.setChecked(Boolean.FALSE);
                }else{
                    door.setChecked(Boolean.TRUE);
                }
                final Switch voice = findViewById(R.id.page_home_edit_voice);
                if(device.getScenVoice().equals("0")){
                    voice.setChecked(Boolean.FALSE);
                }else{
                    voice.setChecked(Boolean.TRUE);
                }
                final Switch auto = findViewById(R.id.page_home_edit_auto);
                if(device.getScenAuto().equals("0")){
                    auto.setChecked(Boolean.FALSE);
                }else{
                    auto.setChecked(Boolean.TRUE);
                }

                ImageView back = findViewById(R.id.page_home_edit_back);
                back.setOnClickListener(new ImageView.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        DeviceBean deviceBean = new DeviceBean(device);
                        deviceBean.setDeviceName(deviceName.getText().toString());
                        deviceBean.setScenDoor(door.isChecked() ? "1" : "0");
                        deviceBean.setScenVoice(voice.isChecked() ? "1" : "0");
                        deviceBean.setScenAuto(auto.isChecked() ? "1" : "0");

                        updateDevice(deviceBean);
                        showEditor(Boolean.FALSE);
                        showFab(Boolean.TRUE);
                        updateList();
                    }
                });
            }
        });
        if(device.getDeviceType().equals("b")){
            //TODO: 获取当前的状态
            final Switch lightSwitch = view.findViewById(R.id.home_light_switch);
            final ImageView lightInd = view.findViewById(R.id.home_light_icon);
            String url = MainActivity.BASEURL + "/light/status/" + device.getDeviceID();
            OkhttpUtil.okHttpGet(url, new CallBackUtil.CallBackString() {
                @Override
                public void onFailure(Call call, Exception e) {}

                @Override
                public void onResponse(String response) {
                    JSONObject jsonObject = null;
                    String result = "f";
                    try {
                        jsonObject = new JSONObject(response);
                        result = jsonObject.getString("r");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(result.equals("on")){
                        lightSwitch.setChecked(Boolean.TRUE);
                        lightInd.setImageResource(R.drawable.bulb_on);
                    }else if(result.equals("off")){
                        lightSwitch.setChecked(Boolean.FALSE);
                        lightInd.setImageResource(R.drawable.bulb);
                    }else if(result.equals("m")){

                    }
                }
            });

            TextView homeTextView = view.findViewById(R.id.home_light_name);
            homeTextView.setText(device.getDeviceName());

            lightSwitch.setOnClickListener(new Switch.OnClickListener(){
                @Override
                public void onClick(View v) {
                    // TODO: 发送请求
                    String url = "";
                    if(lightSwitch.isChecked()){
                        url = MainActivity.BASEURL + "/light/control/" + device.getDeviceID() + "/on";
                    }else{
                        url = MainActivity.BASEURL + "/light/control/" + device.getDeviceID() + "/off";
                    }

                    OkhttpUtil.okHttpGet(url, new CallBackUtil.CallBackString() {
                        @Override
                        public void onFailure(Call call, Exception e) {}

                        @Override
                        public void onResponse(String response) {
                            JSONObject jsonObject = null;
                            String result = "f";
                            try {
                                jsonObject = new JSONObject(response);
                                result = jsonObject.getString("r");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(result.equals("s")){
                                // lightSwitch.setChecked(!lightSwitch.isChecked());
                                lightInd.setImageResource(lightSwitch.isChecked() ? R.drawable.bulb_on : R.drawable.bulb);
                            }
                        }
                    });
                }
            });
        }else if(device.getDeviceType().equals("c")){
            // TODO: 控制滑动条
            final SeekBar seekBar = view.findViewById(R.id.home_curtain_switch);
            final ImageView seekBarInd = view.findViewById(R.id.home_curtain_icon);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

                int p = 0;
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    p = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    String url = "";
                    if(p <= 33){
                        seekBar.setProgress(0);
                        url = MainActivity.BASEURL + "/curtain/control/" + device.getDeviceID() + "/a";
                        // TODO:
                    }else if((p > 33) && (p <= 66)){
                        seekBar.setProgress(50);
                        url = MainActivity.BASEURL + "/curtain/control/" + device.getDeviceID() + "/c";
                        // TODO:
                    }else if(p > 66){
                        seekBar.setProgress(100);
                        url = MainActivity.BASEURL + "/curtain/control/" + device.getDeviceID() + "/e";
                        // TODO:
                    }
                    OkhttpUtil.okHttpGet(url, new CallBackUtil.CallBackString() {
                        @Override
                        public void onFailure(Call call, Exception e) {}

                        @Override
                        public void onResponse(String response) {
                            JSONObject jsonObject = null;
                            String result = "f";
                            try {
                                jsonObject = new JSONObject(response);
                                result = jsonObject.getString("r");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            if(response.equals("a")){
                                seekBarInd.setImageResource(R.drawable.curtain_off);
                            }else if(response.equals("c")){
                                seekBarInd.setImageResource(R.drawable.curtain);
                            }else if(response.equals("e")){
                                seekBarInd.setImageResource(R.drawable.curtain);
                            }
                        }
                    });

                }
            });
            //TODO: 获取当前的状态
            String url = MainActivity.BASEURL + "/curtain/status/" + device.getDeviceID();
            OkhttpUtil.okHttpGet(url, new CallBackUtil.CallBackString() {
                @Override
                public void onFailure(Call call, Exception e) {}

                @Override
                public void onResponse(String response) {
                    JSONObject jsonObject = null;
                    String result = "f";
                    try {
                        jsonObject = new JSONObject(response);
                        result = jsonObject.getString("r");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if(result.equals("a")){
                        seekBar.setProgress(0);
                        seekBarInd.setImageResource(R.drawable.curtain_off);
                    }else if(result.equals("c")){
                        seekBar.setProgress(50);
                        seekBarInd.setImageResource(R.drawable.curtain);
                    }else if(result.equals("e")){
                        seekBar.setProgress(100);
                        seekBarInd.setImageResource(R.drawable.curtain);
                    }else if(result.equals("m")){

                    }
                }
            });


            TextView curtainTextView = view.findViewById(R.id.home_curtain_name);
            curtainTextView.setText(device.getDeviceName());
        }



        return view;
    }


    // 动态申请权限
    private void requsetPermission(){
        if (Build.VERSION.SDK_INT>22){
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    android.Manifest.permission.CAMERA)!=     PackageManager.PERMISSION_GRANTED){
                //先判断有没有权限 ，没有就在这里进行权限的申请
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.CAMERA},1);
            }else {

            }
        }else {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //这里已经获取到了摄像头的权限，想干嘛干嘛了可以

                }else {
                    //这里是拒绝给APP摄像头权限，给个提示什么的说明一下都可以。
                    Toast.makeText(MainActivity.this,"请手动打开相机权限",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }

    }

    // 获取二维码扫描结果
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null) {
            if(result.getContents() == null) {
                Toast.makeText(this, "哟，没扫到呢", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Scanned: " + result.getContents(), Toast.LENGTH_LONG).show();
                // 将扫到的结果存到文件中
                String scanString = result.getContents();
                String[] scanStringSplit = scanString.split("#");
                if(scanStringSplit.length > 0){
                    if(scanStringSplit[0].equals("zk")){
                        scanStringSplit = scanString.replace("zk#", "").split("_");
                        if(scanStringSplit.length != 8){
                            Toast.makeText(this, "该二维码不是设备二维码", Toast.LENGTH_LONG).show();
                        }else{
                            DeviceBean db = new DeviceBean(scanStringSplit[0], scanStringSplit[1], scanStringSplit[2],
                                    scanStringSplit[3], scanStringSplit[4], scanStringSplit[5], scanStringSplit[6], scanStringSplit[7]);
                            addDevice(db);
                        }
                    }else{
                        Toast.makeText(this, "该二维码不是设备二维码", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(this, "该二维码不是设备二维码", Toast.LENGTH_LONG).show();
                }

            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 将ArrayList转为json string
     * @param dbl arrayList
     * @return 转成功的String或空数组
     * @throws JSONException
     */
    private String listToJsonString(ArrayList<DeviceBean> dbl) throws JSONException {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        int count = dbl.size();

        for(int i = 0; i < count; i++)
        {
            jsonObject = new JSONObject();
            jsonObject.put("deviceID" , dbl.get(i).getDeviceID());
            jsonObject.put("deviceName", dbl.get(i).getDeviceName());
            jsonObject.put("deviceType", dbl.get(i).getDeviceType());
            jsonObject.put("deviceCode", dbl.get(i).getDeviceCode());
            jsonObject.put("scenDoor", dbl.get(i).getScenDoor());
            jsonObject.put("scenTimer", dbl.get(i).getScenTimer());
            jsonObject.put("scenVoice", dbl.get(i).getScenVoice());
            jsonObject.put("scenAuto", dbl.get(i).getScenAuto());
            jsonArray.put(jsonObject);

        }
        String jsonString = "";
        if(count > 0){
            jsonString = jsonArray.toString(); // 将JSONArray转换得到String
        }
        return jsonString;
    }

    /**
     * 读取设备列表
     * @return 若存在则返回列表，不存在则返回空列表
     * @throws JSONException
     */
    protected ArrayList<DeviceBean> getDevice() throws JSONException {
        ArrayList<DeviceBean> result = new ArrayList<>();
        DeviceBean deviceBean = new DeviceBean();
        SharedPreferences read = getSharedPreferences("device", MODE_PRIVATE);
        String value = read.getString("list", "None");
        if(value.equals("None")){
            return result;
        }else{
            JSONArray jsonArray = new JSONArray(value);
            JSONObject jsonObject;
            for(int i=0; i<jsonArray.length(); i++){
                jsonObject = jsonArray.getJSONObject(i);
                deviceBean = new DeviceBean();
                deviceBean.setDeviceID((String) jsonObject.get("deviceID"));
                deviceBean.setDeviceName((String) jsonObject.get("deviceName"));
                deviceBean.setDeviceType((String) jsonObject.get("deviceType"));
                deviceBean.setDeviceCode((String) jsonObject.get("deviceCode"));
                deviceBean.setScenDoor((String) jsonObject.get("scenDoor"));
                deviceBean.setScenTimer((String) jsonObject.get("scenTimer"));
                deviceBean.setScenVoice((String) jsonObject.get("scenVoice"));
                deviceBean.setScenAuto((String) jsonObject.get("scenAuto"));
                result.add(deviceBean);
            }
        }
        return result;
    }


    /**
     * 添加设备到列表中
     * @param db 设备信息
     */
    protected void addDevice(final DeviceBean db){
        // 查询设备
        final String url = BASEURL +"/pair/" +db.getDeviceID()+"/"+db.getDeviceCode()+"/"+db.getDeviceType();
        OkhttpUtil.okHttpGet(url, new CallBackUtil.CallBackString() {
            @Override
            public void onFailure(Call call, Exception e) {
                Toast.makeText(MainActivity.this,"主控设备离线",Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onResponse(String response) {
                JSONObject jsonObject = null;
                String result = "f";
                try {
                    jsonObject = new JSONObject(response);
                    result = jsonObject.getString("r");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // result = "s";
                if(result.equals("s")){
                    ArrayList<DeviceBean> arrayList = new ArrayList<>();
                    try {
                        arrayList = getDevice();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    SharedPreferences.Editor editor = getSharedPreferences("device", MODE_PRIVATE).edit();
                    // 初次存入
                    if(arrayList.size() == 0){
                        ArrayList<DeviceBean> deviceList = new ArrayList<>();
                        deviceList.add(db);
                        String jsonString = "";
                        try {
                            jsonString = listToJsonString(deviceList);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        if(jsonString.length() > 0){
                            editor.putString("list", jsonString);
                            editor.commit();
                        }
                        Toast.makeText(MainActivity.this, "设备加入成功", Toast.LENGTH_LONG).show();
                        // TODO: 更新ui
                        updateList();
                        // 已有列表
                    }else{
                        // 判断新加入的设备是否已经存在
                        String newDeviceID = db.getDeviceID();
                        int has = 0;
                        for(int i=0; i<arrayList.size(); i++){
                            if(arrayList.get(i).getDeviceID().equals(newDeviceID)){
                                has = 1;
                            }
                        }
                        if(has == 1){
                            Toast.makeText(MainActivity.this, "设备已在列表中", Toast.LENGTH_LONG).show();
                        }else{
                            arrayList.add(db);
                            try {
                                String deviceListString = listToJsonString(arrayList);
                                editor.putString("list", deviceListString);
                                editor.commit();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(MainActivity.this, "设备加入成功", Toast.LENGTH_LONG).show();
                            // TODO: 更新ui
                            updateList();
                        }
                    }
                }else if(result.equals("f")){
                    Toast.makeText(MainActivity.this, "配对码错误", Toast.LENGTH_LONG).show();
                }else if(result.equals("m")){
                    Toast.makeText(MainActivity.this, "设备离线", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

}