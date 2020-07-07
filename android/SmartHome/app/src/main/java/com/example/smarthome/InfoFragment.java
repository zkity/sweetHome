package com.example.smarthome;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;

public class InfoFragment extends Fragment {
    private String mFrom;

    private ImageView locateRf;
    private ImageView macRf;
    private ImageView infoEdit;
    private TextView locate;
    private TextView phone;
    private TextView mail;
    private TextView mac;


    static InfoFragment newInstance(String from){
        InfoFragment fragment = new InfoFragment();
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
        View view = inflater.inflate(R.layout.fragment_info,null);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        locateRf = getView().findViewById(R.id.info_locate_rf);
        macRf = getView().findViewById(R.id.info_mac_rf);
        infoEdit = getView().findViewById(R.id.info_edit);
        locate = getView().findViewById(R.id.info_locate);
        phone = getView().findViewById(R.id.info_phone);
        mail = getView().findViewById(R.id.info_mail);
        mac = getView().findViewById(R.id.info_mac);
        initListener();

        SharedPreferences read = getActivity().getSharedPreferences("info", getActivity().MODE_PRIVATE);
        String macString = read.getString("mac", "None");
        String locateString = read.getString("locate", "None");
        if(!macString.equals("None")){
            mac.setText(macString);
        }
        if(!locateString.equals("None")){
            locate.setText(locateString);
        }
    }



    private void initListener(){
        macRf.setOnClickListener(new ImageView.OnClickListener(){
            @Override
            public void onClick(View v) {
                String macString = getMac(getActivity());
                SharedPreferences.Editor editor = getActivity().getSharedPreferences("info", getActivity().MODE_PRIVATE).edit();
                editor.putString("mac", macString);
                editor.commit();
                mac.setText(macString);
            }
        });

        locateRf.setOnClickListener(new ImageView.OnClickListener(){
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });
    }


    // 动态申请权限
    private void requsetPermission(){
        if (Build.VERSION.SDK_INT>22){
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_COARSE_LOCATION)!=     PackageManager.PERMISSION_GRANTED){
                //先判断有没有权限 ，没有就在这里进行权限的申请
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                android.Manifest.permission.ACCESS_FINE_LOCATION},1);
            }else {

            }
        }else {

        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0&&grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    //这里已经获取到了摄像头的权限，想干嘛干嘛了可以

                }else {
                    //这里是拒绝给APP摄像头权限，给个提示什么的说明一下都可以。
                    Toast.makeText(getActivity(),"请手动打开相机权限",Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }

    }

    public void getLocation(){
        requsetPermission();
        AMapLocationClient mLocationClient = new AMapLocationClient(getActivity());
        AMapLocationClientOption mLocationOption = new AMapLocationClientOption();
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        mLocationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation aMapLocation) {
                if (aMapLocation != null) {
                    if (aMapLocation.getErrorCode() == 0) {
                        //定位成功回调信息，设置相关消息
                        aMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                        double a = aMapLocation.getLatitude();//获取纬度
                        double b = aMapLocation.getLongitude();//获取经度
                        String locateString = "N " + b + " E " + a;
                        SharedPreferences.Editor editor = getActivity().getSharedPreferences("info", getActivity().MODE_PRIVATE).edit();
                        editor.putString("locate", locateString);
                        editor.commit();
                        locate.setText(locateString);
                    }else{
                        Toast.makeText(getActivity(),"error + " + aMapLocation.getErrorInfo(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        mLocationOption.setOnceLocationLatest(true);
        if(null != mLocationClient){
            mLocationClient.setLocationOption(mLocationOption);
            mLocationClient.stopLocation();
            mLocationClient.startLocation();
        }
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
}
