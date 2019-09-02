package com.vise.baseble.exception;

import com.vise.baseble.common.BleExceptionCode;

/**
 * @Description: 其他例外
 */
public class OtherException extends BleException {
    public OtherException(String description) {
        super(BleExceptionCode.OTHER_ERR, description);
    }
}
