package com.vise.baseble.exception;

import com.vise.baseble.common.BleExceptionCode;

/**
 * @Description: 初始化異常
 */
public class InitiatedException extends BleException {
    public InitiatedException() {
        super(BleExceptionCode.INITIATED_ERR, "Initiated Exception Occurred! ");
    }
}
