package com.vise.baseble.common;

/**
異常
 */
public enum BleExceptionCode {
    TIMEOUT,    //超時
    CONNECT_ERR,    //連接異常
    GATT_ERR,   //GATT異常
    INITIATED_ERR,  //初始化異常
    OTHER_ERR   //其他問題
}
