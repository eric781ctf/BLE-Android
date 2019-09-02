package com.vise.baseble.callback.scan;

import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;

/**
 * @Description: 掃描回調
 */
public interface IScanCallback {
    //發現設備
    void onDeviceFound(BluetoothLeDevice bluetoothLeDevice);

    //掃描完成
    void onScanFinish(BluetoothLeDeviceStore bluetoothLeDeviceStore);

    //掃描超時
    void onScanTimeout();

}
