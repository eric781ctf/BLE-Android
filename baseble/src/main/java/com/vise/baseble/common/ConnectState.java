package com.vise.baseble.common;

/**
 *連接狀態描述
 */
public enum ConnectState {
    CONNECT_INIT(-1),           //連接初始化
    CONNECT_PROCESS(0x00),      //連接中
    CONNECT_SUCCESS(0x01),      //連接成功
    CONNECT_FAILURE(0x02),      //連接失敗
    CONNECT_TIMEOUT(0x03),      //連接逾時
    CONNECT_DISCONNECT(0x04);   //連接中斷

    private int code;

    ConnectState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
