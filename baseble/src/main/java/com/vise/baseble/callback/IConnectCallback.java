package com.vise.baseble.callback;

import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.exception.BleException;

/**
 * 連接設備回調
 */
public interface IConnectCallback {
    //連接成功
    void onConnectSuccess(DeviceMirror deviceMirror);

    //連接失敗
    void onConnectFailure(BleException exception);

    //連接中斷
    void onDisconnect(boolean isActive);
}
