package com.vise.baseble.common;

import static com.vise.baseble.common.BleConstant.DEFAULT_CONN_TIME;
import static com.vise.baseble.common.BleConstant.DEFAULT_MAX_CONNECT_COUNT;
import static com.vise.baseble.common.BleConstant.DEFAULT_OPERATE_TIME;
import static com.vise.baseble.common.BleConstant.DEFAULT_RETRY_COUNT;
import static com.vise.baseble.common.BleConstant.DEFAULT_RETRY_INTERVAL;
import static com.vise.baseble.common.BleConstant.DEFAULT_SCAN_REPEAT_INTERVAL;
import static com.vise.baseble.common.BleConstant.DEFAULT_SCAN_TIME;

/**
藍芽通訊配置
 */
public class BleConfig {
    private static BleConfig instance;

    private int scanTimeout = DEFAULT_SCAN_TIME;//掃描超過時間（毫秒）
    private int connectTimeout = DEFAULT_CONN_TIME;//連接超時時間（毫秒）
    private int operateTimeout = DEFAULT_OPERATE_TIME;//操作超時時間（毫秒）
    private int connectRetryCount = DEFAULT_RETRY_COUNT;//連接重試次數
    private int connectRetryInterval = DEFAULT_RETRY_INTERVAL;//連接重試間隔（毫秒）
    private int operateRetryCount = DEFAULT_RETRY_COUNT;//操作重試次數
    private int operateRetryInterval = DEFAULT_RETRY_INTERVAL;//操作重試間隔時間（毫秒）
    private int maxConnectCount = DEFAULT_MAX_CONNECT_COUNT;//最大連接數量

    //yankee
    private int scanRepeatInterval = DEFAULT_SCAN_REPEAT_INTERVAL;//每隔X時間重覆掃描 (毫秒)

    private BleConfig() {
    }

    public static BleConfig getInstance() {
        if (instance == null) {
            synchronized (BleConfig.class) {
                if (instance == null) {
                    instance = new BleConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 獲得發送超時時間
     * */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * 設定發送超時時間
     */
    public BleConfig setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }

    /**
     * 取得連接超時時間
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 設定連接超時時間
     */
    public BleConfig setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 取得掃描超時時間
     */
    public int getScanTimeout() {
        return scanTimeout;
    }

    /**
     * 設定掃描超時時間
     */
    public BleConfig setScanTimeout(int scanTimeout) {
        this.scanTimeout = scanTimeout;
        return this;
    }

    /**
     * 取得連接重試次數
     */
    public int getConnectRetryCount() {
        return connectRetryCount;
    }

    /**
     *設定連接重試次數
     */
    public BleConfig setConnectRetryCount(int connectRetryCount) {
        this.connectRetryCount = connectRetryCount;
        return this;
    }

    /**
     * 取得連接重試間隔時間
     */
    public int getConnectRetryInterval() {
        return connectRetryInterval;
    }

    /**
     * 設定連接重試間隔時間
     */
    public BleConfig setConnectRetryInterval(int connectRetryInterval) {
        this.connectRetryInterval = connectRetryInterval;
        return this;
    }

    /**
     * 取得最大連接數量
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * 設定最大連接數量
     */
    public BleConfig setMaxConnectCount(int maxConnectCount) {
        this.maxConnectCount = maxConnectCount;
        return this;
    }

    /**
     * 取得操作重試次數
     */
    public int getOperateRetryCount() {
        return operateRetryCount;
    }

    /**
     * 設定操作重試次數
     */
    public BleConfig setOperateRetryCount(int operateRetryCount) {
        this.operateRetryCount = operateRetryCount;
        return this;
    }

    /**
     * 取得操作重試間隔時間
     */
    public int getOperateRetryInterval() {
        return operateRetryInterval;
    }

    /**
     * 設定操作重試間隔時間
     */
    public BleConfig setOperateRetryInterval(int operateRetryInterval) {
        this.operateRetryInterval = operateRetryInterval;
        return this;
    }

    /**
     * 取得掃描間隔時間
     */
    public int getScanRepeatInterval() {
        return scanRepeatInterval;
    }

    /**
     * 設定掃描的間隔時間(毫秒)
     */
    public BleConfig setScanRepeatInterval(int scanRepeatInterval) {
        this.scanRepeatInterval = scanRepeatInterval;
        return this;
    }
}
