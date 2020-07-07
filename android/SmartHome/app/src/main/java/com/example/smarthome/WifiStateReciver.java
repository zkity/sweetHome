package com.example.smarthome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.util.Log;

class WifiStateReceiver extends BroadcastReceiver {
    public MainActivity mainActivity = null;
    public WifiStateReceiver(MainActivity context){
        mainActivity = context;
    }

    private int status = 1;

    public String getTag(){
        return "zkity";
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        // 这个监听wifi的打开与关闭，与wifi的连接无关
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                    break;
            }
        }
        // 这个监听wifi的连接状态即是否连上了一个有效无线路由，
        // 当上边广播的状态是WifiManager.WIFI_STATE_DISABLING，和WIFI_STATE_DISABLED的时候，根本不会接到这个广播。

        // 在上边广播接到广播是WifiManager.WIFI_STATE_ENABLED状态的同时也会接到这个广播，
        // 当然刚打开wifi肯定还没有连接到有效的无线
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            Parcelable parcelableExtra = intent
                    .getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                NetworkInfo.State state = networkInfo.getState();
                boolean isConnected = state == NetworkInfo.State.CONNECTED;// 当然，这边可以更精确的确定状态
                if (isConnected) {
                    Log.i(getTag(), "wifi connected");
                    if(status == 0){
                        // TODO: call fun
                        mainActivity.scenDoorImp();
                    }
                    status = 1;
                } else {
                    Log.i(getTag(), "wifi not connected");
                    status = 0;
                }
            }
        }
        // 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.
        // 最好用的还是这个监听。
        // wifi如果打开，关闭，以及连接上可用的连接都会接到监听。见log
        // 这个广播的最大弊端是比上边两个广播的反应要慢，如果只是要监听wifi，我觉得还是用上边两个配合比较合适
        /*
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {

            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo gprs = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            Log.i(getTag(), "网络状态改变:" + wifi.isConnected() + " 3g:" + gprs.isConnected());
            NetworkInfo info = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (info != null) {
                Log.i(getTag(), "info.getTypeName():" + info.getTypeName());
                Log.i(getTag(), "getSubtypeName():" + info.getSubtypeName());
                Log.i(getTag(), "getState():" + info.getState());
                Log.i(getTag(), "getDetailedState():"
                        + info.getDetailedState().name());
                Log.i(getTag(), "getDetailedState():" + info.getExtraInfo());
                Log.i(getTag(), "getType():" + info.getType());

                if (NetworkInfo.State.CONNECTED == info.getState()) {
                } else if (info.getType() == 1) {
                    if (NetworkInfo.State.DISCONNECTING == info.getState()) {
                    }
                }
            }
        }*/
    }
}