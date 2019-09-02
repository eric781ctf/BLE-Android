package com.vise.baseble.callback.scan;

import com.vise.baseble.model.BluetoothLeDevice;

/**
 * @Description: 掃描過濾，依據需求實現過濾的規則
 */
public interface IScanFilter {
    BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice);
}
