package com.vise.baseble.callback;

import com.vise.baseble.exception.BleException;

/**
 * 取得訊號值回復調整
 */
public interface IRssiCallback {
    void onSuccess(int rssi);

    void onFailure(BleException exception);
}
