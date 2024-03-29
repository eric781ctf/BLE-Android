package com.vise.bledemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.bledemo.R;
import com.vise.bledemo.adapter.DeviceAdapter;
import com.vise.log.ViseLog;

import java.util.ArrayList;

/**設備掃描介面*/
public class DeviceScanActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 100;

    private BluetoothLeDevice mDevice;

    private ListView deviceLv;
    private TextView scanCountTv;

    //設備掃描結果顯示
    private DeviceAdapter adapter;

    private BluetoothLeDeviceStore bluetoothLeDeviceStore = new BluetoothLeDeviceStore();

    /**初始化掃描*/
    private ScanCallback periodScanCallback = new ScanCallback(new IScanCallback() {
        @Override
        public void onDeviceFound(final BluetoothLeDevice bluetoothLeDevice) {
            ViseLog.i("Founded Scan Device:" + bluetoothLeDevice);
            bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter != null && bluetoothLeDeviceStore != null) {
                        adapter.setListAll(bluetoothLeDeviceStore.getDeviceList());
                        updateItemCount(adapter.getCount());
                    }
                }
            });
        }

        @Override
        public void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore) {
            ViseLog.i("scan finish " + bluetoothLeDeviceStore);
        }

        @Override
        public void onScanTimeout() {
            ViseLog.i("scan timeout");
        }

    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scan);
        init();
    }

    private void init() {
        deviceLv = findViewById(android.R.id.list);
        scanCountTv = findViewById(R.id.scan_device_count);

        adapter = new DeviceAdapter(this);
        deviceLv.setAdapter(adapter);

        deviceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                //點擊進入到設備詳細訊息介面
                BluetoothLeDevice device = (BluetoothLeDevice) adapter.getItem(position);
                if (device == null || device.getName()!="HpService") {
                    System.out.println("wrong Device");

                }else {
                    Intent intent = new Intent(DeviceScanActivity.this, LoginActivity.class);
                    intent.putExtra("extra_device", device);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startScan();
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopScan();
        invalidateOptionsMenu();
        bluetoothLeDeviceStore.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Menu顯示
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.scan, menu);
        if (periodScanCallback != null && !periodScanCallback.isScanning()) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_progress_indeterminate);
        }
        return true;
    }

    /**
     * 點擊Menu的處理
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan://開始掃描
                startScan();
                break;
            case R.id.menu_stop://停止掃描
                stopScan();
                break;
        }
        return true;
    }

    /**開始掃描*/
    private void startScan() {
        updateItemCount(0);
        if (adapter != null) {
            adapter.setListAll(new ArrayList<BluetoothLeDevice>());
        }
        ViseBle.getInstance().startScan(periodScanCallback);
        invalidateOptionsMenu();
    }

    /**停止掃描*/
    private void stopScan() {
        ViseBle.getInstance().stopScan(periodScanCallback);
        invalidateOptionsMenu();
    }

    /**更新掃描到的數量
     * @param count*/
    private void updateItemCount(final int count) {
        scanCountTv.setText(getString(R.string.formatter_item_count, String.valueOf(count)));
    }

}
