package com.vise.baseble.callback.scan;

import com.vise.baseble.model.BluetoothLeDevice;

import java.util.List;

/**
 * @Description: 指定設備進行過濾，一班使用名稱或是MAC位址
 */
public class ListFilterScanCallback extends ScanCallback {
    private List<String> deviceNameList;//指定設備名稱
    private List<String> deviceMacList;//指定設備MAC

    public ListFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public ListFilterScanCallback setDeviceNameList(List<String> deviceNameList) {
        this.deviceNameList = deviceNameList;
        return this;
    }

    public ListFilterScanCallback setDeviceMacList(List<String> deviceMacList) {
        this.deviceMacList = deviceMacList;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        if (deviceNameList != null && deviceNameList.size() > 0) {
            for (String deviceName : deviceNameList) {
                if (bluetoothLeDevice != null && bluetoothLeDevice.getName() != null && deviceName != null
                        && deviceName.equalsIgnoreCase(bluetoothLeDevice.getName().trim())) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        } else if (deviceMacList != null && deviceMacList.size() > 0) {
            for (String deviceMac : deviceMacList) {
                if (bluetoothLeDevice != null && bluetoothLeDevice.getAddress() != null && deviceMac != null
                        && deviceMac.equalsIgnoreCase(bluetoothLeDevice.getAddress().trim())) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        }
        return tempDevice;
    }
}
