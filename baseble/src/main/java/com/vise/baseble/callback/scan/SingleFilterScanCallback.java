package com.vise.baseble.callback.scan;

import com.vise.baseble.ViseBle;
import com.vise.baseble.model.BluetoothLeDevice;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description: 設置掃描指定的單個設備，一班是以設備名稱和MAC位址
 */
public class SingleFilterScanCallback extends ScanCallback {
    private AtomicBoolean hasFound = new AtomicBoolean(false);
    private String deviceName;//指定設備名稱
    private String deviceMac;//指定設備MAC

    public SingleFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public ScanCallback setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public ScanCallback setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        if (!hasFound.get()) {
            if (bluetoothLeDevice != null && bluetoothLeDevice.getAddress() != null && deviceMac != null
                    && deviceMac.equalsIgnoreCase(bluetoothLeDevice.getAddress().trim())) {
                hasFound.set(true);
                isScanning = false;
                removeHandlerMsg();
                ViseBle.getInstance().stopScan(SingleFilterScanCallback.this);
                tempDevice = bluetoothLeDevice;
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
                scanCallback.onScanFinish(bluetoothLeDeviceStore);
            } else if (bluetoothLeDevice != null && bluetoothLeDevice.getName() != null && deviceName != null
                    && deviceName.equalsIgnoreCase(bluetoothLeDevice.getName().trim())) {
                hasFound.set(true);
                isScanning = false;
                removeHandlerMsg();
                ViseBle.getInstance().stopScan(SingleFilterScanCallback.this);
                tempDevice = bluetoothLeDevice;
                bluetoothLeDeviceStore.addDevice(bluetoothLeDevice);
                scanCallback.onScanFinish(bluetoothLeDeviceStore);
            }
        }
        return tempDevice;
    }
}
