package com.vise.bledemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.bledemo.R;
import com.vise.xsnow.cache.SpCache;

public class turning extends AppCompatActivity {

    private static SpCache mSpCache;
    public static BluetoothLeDevice mDevice = LoginActivity.mDevice;
    private ProgressBar progressbar;
    Intent next;

    protected static void onCreate() {
        DeviceControlActivity.DISPLAY_balance.start();
        try {
            DeviceControlActivity.DISPLAY_balance.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
