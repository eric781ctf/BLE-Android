package com.vise.baseble.callback.scan;

import android.os.ParcelUuid;

import com.vise.baseble.model.BluetoothLeDevice;

import java.util.UUID;

/**
 * @Description: 根據指定的UUID過濾設備
 */
public class UuidFilterScanCallback extends ScanCallback {
    private UUID uuid;//設備UUID

    public UuidFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
    }

    public UuidFilterScanCallback setUuid(String uuid) {
        this.uuid = UUID.fromString(uuid);
        return this;
    }

    public UuidFilterScanCallback setUuid(UUID uuid) {
        this.uuid = uuid;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null
                && bluetoothLeDevice.getDevice().getUuids() != null
                && bluetoothLeDevice.getDevice().getUuids().length > 0) {
            for (ParcelUuid parcelUuid : bluetoothLeDevice.getDevice().getUuids()) {
                if (uuid != null && uuid == parcelUuid.getUuid()) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        }
        return tempDevice;
    }
}
