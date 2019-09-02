package com.vise.baseble.exception;

import com.vise.baseble.common.BleExceptionCode;

/**
 * @Description: 超時例外
 */
public class TimeoutException extends BleException {
    public TimeoutException() {
        super(BleExceptionCode.TIMEOUT, "Timeout Exception Occurred! ");
    }
}
