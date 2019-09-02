package com.vise.bledemo.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.bledemo.R;
import com.vise.bledemo.common.BluetoothDeviceManager;

import java.util.UUID;

/**
 * BLE Device control for Search UI
 * From middlePlace
 **/
public class DeviceControlAtSeach extends AppCompatActivity {

    public static final String WRITE_CHARACTERISTI_UUID_KEY = "write_uuid_key";
    public static final String NOTIFY_CHARACTERISTIC_UUID_KEY = "notify_uuid_key";
    public static final String WRITE_DATA_KEY = "write_data_key";

    String ServiceUUID_str = "00001823-0000-1000-8000-00805f9b34fb";
    UUID ServiceUUID = UUID.fromString("00001823-0000-1000-8000-00805f9b34fb");
    String URI_UUID_str = "00002ab6-0000-1000-8000-00805f9b34fb";
    UUID URI_UUID = UUID.fromString("00002ab6-0000-1000-8000-00805f9b34fb");
    String Control_UUID_str = "00002aba-0000-1000-8000-00805f9b34fb";  // 1:get  3:post
    String Body_UUID_str = "00002ab9-0000-1000-8000-00805f9b34fb";
    String Status_UUID_str ="00002abb-0000-1000-8000-00805f9b34fb";//讀取若為200則成功

    public static final String CONNECT_FAILED = "Connect failed";
    public static final String CONNECT_SUCCESS = "Connect success";
    /**設備信息*/
    private BluetoothLeDevice mDevice;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_control_search);
        mDevice = getIntent().getParcelableExtra(middlePlace.EXTRA_DEVICE);
        TextView Status = findViewById(R.id.Status_search);
        if (!BluetoothDeviceManager.getInstance().isConnected(mDevice)) {
            Status.setText(CONNECT_FAILED);
            Status.setTextColor(Color.parseColor("#FF0000"));
        }else{
            Status.setText(CONNECT_SUCCESS);
            Status.setTextColor(Color.parseColor("#00DD00"));
        }
    }
}
