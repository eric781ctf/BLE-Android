package com.vise.baseble.callback.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Looper;
import com.vise.baseble.ViseBle;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

/**
 * @Description: 掃描設備回調
 */
public class ScanCallback implements BluetoothAdapter.LeScanCallback, IScanFilter {
    protected Handler handler = new Handler(Looper.myLooper());
    protected boolean isScan = true;//是否開始掃描
    protected boolean isScanning = false;//是否正在掃描
    protected BluetoothLeDeviceStore bluetoothLeDeviceStore;//儲存掃描到的設備
    protected IScanCallback scanCallback;//掃描結果回調

    public ScanCallback(IScanCallback scanCallback) {
        this.scanCallback = scanCallback;
        if (scanCallback == null) {
            throw new NullPointerException("this scanCallback is null!");
        }
        bluetoothLeDeviceStore = new BluetoothLeDeviceStore();
    }

    public ScanCallback setScan(boolean scan) {
        isScan = scan;
        return this;
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void scan() {
        if (isScan) {
            if (isScanning) {
                return;
            }
            bluetoothLeDeviceStore.clear();
            if (BleConfig.getInstance().getScanTimeout() > 0) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isScanning = false;

                        if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                            ViseBle.getInstance().getBluetoothAdapter().stopLeScan(ScanCallback.this);
                        }

                        if (bluetoothLeDeviceStore.getDeviceMap() != null
                                && bluetoothLeDeviceStore.getDeviceMap().size() > 0) {
                            scanCallback.onScanFinish(bluetoothLeDeviceStore);
                        } else {
                            scanCallback.onScanTimeout();
                        }
                    }
                }, BleConfig.getInstance().getScanTimeout());
            }else if (BleConfig.getInstance().getScanRepeatInterval() > 0){
                //如果超時的時間設置為一直在掃描(即<=0)，判斷是否設置重複掃描間隔
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isScanning = false;

                        if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                            ViseBle.getInstance().getBluetoothAdapter().stopLeScan(ScanCallback.this);
                        }

                        if (bluetoothLeDeviceStore.getDeviceMap() != null
                                && bluetoothLeDeviceStore.getDeviceMap().size() > 0) {
                            scanCallback.onScanFinish(bluetoothLeDeviceStore);
                        } else {
                            scanCallback.onScanTimeout();
                        }
                        isScanning = true;
                        if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                            ViseBle.getInstance().getBluetoothAdapter().startLeScan(ScanCallback.this);
                        }
                        handler.postDelayed(this,BleConfig.getInstance().getScanRepeatInterval());
                    }
                }, BleConfig.getInstance().getScanRepeatInterval());
            }
            isScanning = true;
            if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                ViseBle.getInstance().getBluetoothAdapter().startLeScan(ScanCallback.this);
            }
        } else {
            isScanning = false;
            if (ViseBle.getInstance().getBluetoothAdapter() != null) {
                ViseBle.getInstance().getBluetoothAdapter().stopLeScan(ScanCallback.this);
            }
        }
    }

    public ScanCallback removeHandlerMsg() {
        handler.removeCallbacksAndMessages(null);
        bluetoothLeDeviceStore.clear();
        return this;
    }

    @Override
    public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
        BluetoothLeDevice bluetoothLeDevice = new BluetoothLeDevice(bluetoothDevice, rssi, scanRecord, System.currentTimeMillis());
        BluetoothLeDevice filterDevice = onFilter(bluetoothLeDevice);
        if (filterDevice != null) {
            bluetoothLeDeviceStore.addDevice(filterDevice);
            scanCallback.onDeviceFound(filterDevice);
        }
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        return bluetoothLeDevice;
    }

}
