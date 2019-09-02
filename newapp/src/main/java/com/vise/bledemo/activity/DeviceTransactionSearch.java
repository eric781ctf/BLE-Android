package com.vise.bledemo.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.vise.baseble.common.PropertyType;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.bledemo.R;
import com.vise.bledemo.common.BluetoothDeviceManager;
import com.vise.xsnow.event.BusManager;

import java.util.UUID;

public class DeviceTransactionSearch extends AppCompatActivity {

    public static final String BALANCE = "Balance";

    private ProgressBar progressbar;
    private TextView TSBalance;
    private TextView TSBalance_name;
    private TextView Txn_response;
    private TextView Title;
    private BluetoothLeDevice mDevice;
    private byte TxndataArr[];

    UUID ServiceUUID = UUID.fromString("00001823-0000-1000-8000-00805f9b34fb"); //1823
    UUID URI_UUID = UUID.fromString("00002ab6-0000-1000-8000-00805f9b34fb");    //2AB6
    UUID DATA_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");   //1001
    UUID RW_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");     //1000
    UUID Control_UUID = UUID.fromString("00002aba-0000-1000-8000-00805f9b34fb");//2ABA   1:get  3:post
    UUID Body_UUID = UUID.fromString("00002ab9-0000-1000-8000-00805f9b34fb");//2AB9
    UUID Status_UUID = UUID.fromString("00002abb-0000-1000-8000-00805f9b34fb");//2ABB  讀取若為200則成功

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_transaction_search);

        BusManager.getBus().register(this);
        mDevice = getIntent().getParcelableExtra(middlePlace.EXTRA_DEVICE);

        init();
    }

    /**
     * 進入頁面先Read Body(2AB9)
     * 再Post至Web3
     * */
    public void BLE_READ(UUID uuid){
        /**Read 2AB9*/
        BluetoothDeviceManager.getInstance().bindChannel(mDevice, PropertyType.PROPERTY_READ, ServiceUUID, uuid, null);
        BluetoothDeviceManager.getInstance().read(mDevice);
    }
    private void init(){
        progressbar = findViewById(R.id.progressBar);
        Txn_response = findViewById(R.id.txnResponse);

        BLE_READ(Body_UUID);
        //如果有Read到TXN 接著做下一步
        if(Txn_response!=null){
            SearchBalance();
        }

    }

    private void SearchBalance(){
        Title = findViewById(R.id.TSearch_title);
        Title.setText(BALANCE);
        progressbar = findViewById(R.id.progressBar);
        progressbar.setVisibility(View.INVISIBLE);
        TSBalance = findViewById(R.id.TSBalance);
        TSBalance.setVisibility(View.VISIBLE);
        TSBalance_name = findViewById(R.id.TSBalance_name);
        TSBalance_name.setVisibility(View.VISIBLE);
        TSBalance.setText("Put balance here");


    }
}
