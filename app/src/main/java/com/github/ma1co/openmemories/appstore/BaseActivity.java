package com.github.ma1co.openmemories.appstore;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

public class BaseActivity extends Activity {
    public enum WifiState {
        DISABLED("Wifi disabled"),
        ENABLING("Enabling wifi..."),
        ENABLED("Wifi enabled"),
        SCANNING("Scanning..."),
        CONNECTING("Connecting..."),
        CONNECTED("Wifi connected");

        private String label;

        WifiState(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;
    private BroadcastReceiver receiver;
    private boolean keepWifiOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Environment.isCamera()) {
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable exp) {
                    Logger.error("Uncaught exception", exp);
                    System.exit(0);
                }
            });
        }

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onWifiStateChanged();
            }
        };
    }

    @Override
    protected void onResume() {
        Logger.info("resuming " + getComponentName().getShortClassName());
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(receiver, f);
        onWifiStateChanged();
    }

    @Override
    protected void onPause() {
        Logger.info("pausing " + getComponentName().getShortClassName());
        super.onPause();
        unregisterReceiver(receiver);
        if (Environment.isCamera() && !keepWifiOn)
            setWifiEnabled(false);
        keepWifiOn = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onWifiStateChanged() {
    }

    public WifiState getWifiState() {
        if (Environment.isEmulator()) {
            return WifiState.CONNECTED;
        } else {
            switch (wifiManager.getWifiState()) {
                case WifiManager.WIFI_STATE_ENABLED:
                    if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected()) {
                        return WifiState.CONNECTED;
                    } else {
                        switch (WifiInfo.getDetailedStateOf(wifiManager.getConnectionInfo().getSupplicantState())) {
                            case SCANNING:
                                return WifiState.SCANNING;
                            case AUTHENTICATING:
                            case CONNECTING:
                            case OBTAINING_IPADDR:
                                return WifiState.CONNECTING;
                            default:
                                return WifiState.ENABLED;
                        }
                    }
                case WifiManager.WIFI_STATE_ENABLING:
                    return WifiState.ENABLING;
                default:
                    return WifiState.DISABLED;
            }
        }
    }

    public void setWifiEnabled(boolean enabled) {
        wifiManager.setWifiEnabled(enabled);
    }

    public void keepWifiOn() {
        keepWifiOn = true;
    }

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
