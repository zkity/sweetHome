package com.example.smarthome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smarthome.dao.DeviceBean;
import com.example.smarthome.okhttputil.CallBackUtil;
import com.example.smarthome.okhttputil.OkhttpUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import okhttp3.Call;

public class HomeFragment extends Fragment {
    private String mFrom;
    private LinearLayout editor;
    private FloatingActionButton floatingActionButton;
    static HomeFragment newInstance(String from){
        HomeFragment fragment = new HomeFragment();
        Bundle bundle = new Bundle();
        bundle.putString("from",from);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments()!=null){
            mFrom = getArguments().getString("from");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home,null);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateList();
        editor = getView().findViewById(R.id.page_home_edit);
        floatingActionButton = getActivity().findViewById(R.id.fab);
        showEditor(Boolean.FALSE);
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

    private View addItem(final DeviceBean device){
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LayoutInflater inflater3 = LayoutInflater.from(getActivity());
        View view = new View(getActivity());
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
                final EditText deviceName = getView().findViewById(R.id.page_home_edit_device_name);
                deviceName.setText(device.getDeviceName());
                final Switch door = getView().findViewById(R.id.page_home_edit_door);
                if(device.getScenDoor().equals("0")){
                    door.setChecked(Boolean.FALSE);
                }else{
                    door.setChecked(Boolean.TRUE);
                }
                final Switch voice = getView().findViewById(R.id.page_home_edit_voice);
                if(device.getScenVoice().equals("0")){
                    voice.setChecked(Boolean.FALSE);
                }else{
                    voice.setChecked(Boolean.TRUE);
                }
                final Switch auto = getView().findViewById(R.id.page_home_edit_auto);
                if(device.getScenAuto().equals("0")){
                    auto.setChecked(Boolean.FALSE);
                }else{
                    auto.setChecked(Boolean.TRUE);
                }

                ImageView back = getView().findViewById(R.id.page_home_edit_back);
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

    /**
     * 读取设备列表
     * @return 若存在则返回列表，不存在则返回空列表
     * @throws JSONException
     */
    protected ArrayList<DeviceBean> getDevice() throws JSONException {
        ArrayList<DeviceBean> result = new ArrayList<>();
        DeviceBean deviceBean = new DeviceBean();
        SharedPreferences read = getActivity().getSharedPreferences("device", getActivity().MODE_PRIVATE);
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
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("device", getActivity().MODE_PRIVATE).edit();
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
        LinearLayout homeContainer = new LinearLayout(getActivity());
        homeContainer = getView().findViewById(R.id.page_home_container);
        if(homeContainer != null){
            homeContainer.removeAllViews();
            DeviceBean device = new DeviceBean();
            for(int i=0; i<deviceBeans.size(); i++){
                device = deviceBeans.get(i);
                homeContainer.addView(addItem(device));
            }
        }else{
            Toast.makeText(getActivity(),"is null",Toast.LENGTH_SHORT).show();
        }


    }

}
