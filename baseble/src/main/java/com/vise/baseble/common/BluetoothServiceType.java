package com.vise.baseble.common;

import android.bluetooth.BluetoothClass;

public enum BluetoothServiceType {
    AUDIO(BluetoothClass.Service.AUDIO),    //音頻
    CAPTURE(BluetoothClass.Service.CAPTURE),    //捕捉
    INFORMATION(BluetoothClass.Service.INFORMATION),    //訊息
    LIMITED_DISCOVERABILITY(BluetoothClass.Service.LIMITED_DISCOVERABILITY),    //有限發現
    NETWORKING(BluetoothClass.Service.NETWORKING),  //網路
    OBJECT_TRANSFER(BluetoothClass.Service.OBJECT_TRANSFER),    //對象傳輸
    POSITIONING(BluetoothClass.Service.POSITIONING),    //定位
    RENDER(BluetoothClass.Service.RENDER),  //给予
    TELEPHONY(BluetoothClass.Service.TELEPHONY);    //電話

    private int code;

    BluetoothServiceType(int code) {
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
