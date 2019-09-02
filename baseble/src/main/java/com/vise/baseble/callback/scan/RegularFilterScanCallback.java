package com.vise.baseble.callback.scan;

import android.text.TextUtils;

import com.vise.baseble.model.BluetoothLeDevice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description: 根據規則過濾掃描設備，目前是根據訊號範圍內指定設備名稱過濾
 */
public class RegularFilterScanCallback extends ScanCallback {
    private Pattern pattern;
    private Matcher matcher;
    private String regularDeviceName;//正規表達式表式的設備名稱
    private int deviceRssi;//設備訊號值

    public RegularFilterScanCallback(IScanCallback scanCallback) {
        super(scanCallback);
        pattern = Pattern.compile("^[\\x00-\\xff]*$");
    }

    public RegularFilterScanCallback setRegularDeviceName(String regularDeviceName) {
        this.regularDeviceName = regularDeviceName;
        if (!TextUtils.isEmpty(this.regularDeviceName)) {
            pattern = Pattern.compile(this.regularDeviceName);
        }
        return this;
    }

    public RegularFilterScanCallback setDeviceRssi(int deviceRssi) {
        this.deviceRssi = deviceRssi;
        return this;
    }

    @Override
    public BluetoothLeDevice onFilter(BluetoothLeDevice bluetoothLeDevice) {
        BluetoothLeDevice tempDevice = null;
        String tempName = bluetoothLeDevice.getName();
        int tempRssi = bluetoothLeDevice.getRssi();
        if (!TextUtils.isEmpty(tempName)) {
            matcher = pattern.matcher(tempName);
            if (this.deviceRssi < 0) {
                if (matcher.matches() && tempRssi >= this.deviceRssi) {
                    tempDevice = bluetoothLeDevice;
                }
            } else {
                if (matcher.matches()) {
                    tempDevice = bluetoothLeDevice;
                }
            }
        }
        return tempDevice;
    }
}
