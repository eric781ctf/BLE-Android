package com.vise.baseble.callback;

import com.vise.baseble.core.BluetoothGattChannel;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.model.BluetoothLeDevice;

/**
*操作回復調整
 */
public interface IBleCallback {
    void onSuccess(byte[] data, BluetoothGattChannel bluetoothGattChannel, BluetoothLeDevice bluetoothLeDevice);

    void onFailure(BleException exception);
}
