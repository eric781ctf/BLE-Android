package com.vise.baseble.core;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.vise.baseble.ViseBle;
import com.vise.baseble.callback.IBleCallback;
import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.IRssiCallback;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.common.BleConstant;
import com.vise.baseble.common.ConnectState;
import com.vise.baseble.common.PropertyType;
import com.vise.baseble.exception.BleException;
import com.vise.baseble.exception.ConnectException;
import com.vise.baseble.exception.GattException;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.utils.HexUtil;
import com.vise.log.ViseLog;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.vise.baseble.common.BleConstant.MSG_CONNECT_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_CONNECT_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_READ_DATA_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_READ_DATA_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_RECEIVE_DATA_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_RECEIVE_DATA_TIMEOUT;
import static com.vise.baseble.common.BleConstant.MSG_WRITE_DATA_RETRY;
import static com.vise.baseble.common.BleConstant.MSG_WRITE_DATA_TIMEOUT;

/**
 * 設備連接成功後返回的設備訊息
 */
public class DeviceMirror {
    private final DeviceMirror deviceMirror;
    private final String uniqueSymbol;//唯一符號
    private final BluetoothLeDevice bluetoothLeDevice;//設備基礎資訊

    private BluetoothGatt bluetoothGatt;//藍芽GATT
    private IRssiCallback rssiCallback;//取得信號值
    private IConnectCallback connectCallback;//連接回調
    private int connectRetryCount = 0;//目前連接重試次數
    private int writeDataRetryCount = 0;//目前導入數據重試次數
    private int readDataRetryCount = 0;//目前讀取數據重試次數
    private int receiveDataRetryCount = 0;//目前接收數據重試次數
    private boolean isActiveDisconnect = false;//是否主動中斷連線
    private boolean isIndication;//是否是指示器方式
    private boolean enable;//是否設定enable
    private byte[] writeData;//寫入
    private ConnectState connectState = ConnectState.CONNECT_INIT;//設備狀態
    private volatile HashMap<String, BluetoothGattChannel> writeInfoMap = new HashMap<>();//寫入數據GATT
    private volatile HashMap<String, BluetoothGattChannel> readInfoMap = new HashMap<>();//讀取數據GATT
    private volatile HashMap<String, BluetoothGattChannel> enableInfoMap = new HashMap<>();//設定enableGATT
    private volatile HashMap<String, IBleCallback> bleCallbackMap = new HashMap<>();//數據操作回調
    private volatile HashMap<String, IBleCallback> receiveCallbackMap = new HashMap<>();//數據接收回調

    public static String read;

    private final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_CONNECT_TIMEOUT) {
                connectFailure(new TimeoutException());
            } else if (msg.what == MSG_CONNECT_RETRY) {
                connect();
            } else if (msg.what == MSG_WRITE_DATA_TIMEOUT) {
                writeFailure(new TimeoutException(), true);
            } else if (msg.what == MSG_WRITE_DATA_RETRY) {
                write(writeData);
            } else if (msg.what == MSG_READ_DATA_TIMEOUT) {
                readFailure(new TimeoutException(), true);
            } else if (msg.what == MSG_READ_DATA_RETRY) {
                read();
            } else if (msg.what == MSG_RECEIVE_DATA_TIMEOUT) {
                enableFailure(new TimeoutException(), true);
            } else if (msg.what == MSG_RECEIVE_DATA_RETRY) {
                enable(enable, isIndication);
            }
        }
    };
    public static String readBLE = new String();
    public static String Get_value(){
        return readBLE;
    }

    /**
     * 藍芽所有相關操作核心回復調整
     */
    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        /**
         * 連接狀態改變
         * @param gatt GATT
         * @param status 改變前的狀態
         * @param newState 改變後的狀態
         */
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            ViseLog.i("onConnectionStateChange  status: " + status + " ,newState: " + newState +
                    "  ,thread: " + Thread.currentThread());
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                close();
                if (connectCallback != null) {
                    if (handler != null) {
                        handler.removeCallbacksAndMessages(null);
                    }
                    ViseBle.getInstance().getDeviceMirrorPool().removeDeviceMirror(deviceMirror);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        connectState = ConnectState.CONNECT_DISCONNECT;
                        connectCallback.onDisconnect(isActiveDisconnect);
                    } else {
                        connectState = ConnectState.CONNECT_FAILURE;
                        connectCallback.onConnectFailure(new ConnectException(gatt, status));
                    }
                }
            } else if (newState == BluetoothGatt.STATE_CONNECTING) {
                connectState = ConnectState.CONNECT_PROCESS;
            }
        }

        /**
         * 發現服務，取得設備支援的服務列表
         * @param gatt GATT
         * @param status 目前狀態
         */
        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            ViseLog.i("onServicesDiscovered  status: " + status + "  ,thread: " + Thread.currentThread());
            if (handler != null) {
                handler.removeMessages(MSG_CONNECT_TIMEOUT);
            }
            if (status == 0) {
                ViseLog.i("onServicesDiscovered connectSuccess.");
                bluetoothGatt = gatt;
                connectState = ConnectState.CONNECT_SUCCESS;
                if (connectCallback != null) {
                    isActiveDisconnect = false;
                    ViseBle.getInstance().getDeviceMirrorPool().addDeviceMirror(deviceMirror);
                    connectCallback.onConnectSuccess(deviceMirror);
                }
            } else {
                connectFailure(new ConnectException(gatt, status));
            }
        }

        /**
         * 讀取特徵值，讀取特徵值包含的可讀訊息
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 目前狀態
         */


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicRead  status: " + status + ",\n data: " + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            readBLE = HexUtil.convertHexToString(HexUtil.encodeHexStr(characteristic.getValue()).toString());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(readInfoMap, characteristic.getValue(), status, true);
            } else {
                readFailure(new GattException(status), true);
            }
        }

        /**
         * 寫入特徵值發送數據到設備
         * @param gatt GATT
         * @param characteristic 特征值
         * @param status 目前狀態
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            ViseLog.i("onCharacteristicWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(writeInfoMap, characteristic.getValue(), status, false);
            } else {
                writeFailure(new GattException(status), true);
            }
        }

        /**
         * 特徵值改變，接收設備傳回的訊息
         * @param gatt GATT
         * @param characteristic 特徵值
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            ViseLog.i("onCharacteristicChanged data:" + HexUtil.encodeHexStr(characteristic.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            for (Map.Entry<String, IBleCallback> receiveEntry : receiveCallbackMap.entrySet()) {
                String receiveKey = receiveEntry.getKey();
                IBleCallback receiveValue = receiveEntry.getValue();
                for (Map.Entry<String, BluetoothGattChannel> gattInfoEntry : enableInfoMap.entrySet()) {
                    String bluetoothGattInfoKey = gattInfoEntry.getKey();
                    BluetoothGattChannel bluetoothGattInfoValue = gattInfoEntry.getValue();
                    if (receiveKey.equals(bluetoothGattInfoKey)) {
                        receiveValue.onSuccess(characteristic.getValue(), bluetoothGattInfoValue, bluetoothLeDevice);
                    }
                }
            }
        }

        /**
         * 讀取屬性描述值，取得設備目前屬性描述的值
         * @param gatt GATT
         * @param descriptor 屬性描述
         * @param status 目前狀態
         */
        @Override
        public void onDescriptorRead(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorRead  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(readInfoMap, descriptor.getValue(), status, true);
            } else {
                readFailure(new GattException(status), true);
            }
        }

        /**
         * 寫入屬性描述值，根據屬性描述值寫入數據到設備
         * @param gatt GATT
         * @param descriptor 屬性描述值
         * @param status 目前狀態
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
            ViseLog.i("onDescriptorWrite  status: " + status + ", data:" + HexUtil.encodeHexStr(descriptor.getValue()) +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(writeInfoMap, descriptor.getValue(), status, false);
            } else {
                writeFailure(new GattException(status), true);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleSuccessData(enableInfoMap, descriptor.getValue(), status, false);
            } else {
                enableFailure(new GattException(status), true);
            }
        }

        /**
         * 顯示設備訊號值
         * @param gatt GATT
         * @param rssi 設備目前訊號
         * @param status 目前狀態
         */
        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            ViseLog.i("onReadRemoteRssi  status: " + status + ", rssi:" + rssi +
                    "  ,thread: " + Thread.currentThread());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (rssiCallback != null) {
                    rssiCallback.onSuccess(rssi);
                }
            } else {
                if (rssiCallback != null) {
                    rssiCallback.onFailure(new GattException(status));
                }
            }
        }
    };

    public DeviceMirror(BluetoothLeDevice bluetoothLeDevice) {
        deviceMirror = this;
        this.bluetoothLeDevice = bluetoothLeDevice;
        this.uniqueSymbol = bluetoothLeDevice.getAddress() + bluetoothLeDevice.getName();
    }

    /**
     * 連接設備
     *
     * @param connectCallback
     */
    public synchronized void connect(IConnectCallback connectCallback) {
        if (connectState == ConnectState.CONNECT_SUCCESS || connectState == ConnectState.CONNECT_PROCESS
                || (connectState == ConnectState.CONNECT_INIT && connectRetryCount != 0)) {
            ViseLog.e("this connect state is connecting, connectSuccess or current retry count less than config connect retry count.");
            return;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        this.connectCallback = connectCallback;
        connectRetryCount = 0;
        connect();
    }

    /**
     * 鎖定一個可讀寫可通知的通道(回調，操作類型，UUID)
     * @param bleCallback
     * @param bluetoothGattChannel
     */
    public synchronized void bindChannel(IBleCallback bleCallback, BluetoothGattChannel bluetoothGattChannel) {
        if (bleCallback != null && bluetoothGattChannel != null) {
            String key = bluetoothGattChannel.getGattInfoKey();
            PropertyType propertyType = bluetoothGattChannel.getPropertyType();
            if (!bleCallbackMap.containsKey(key)) {
                bleCallbackMap.put(key, bleCallback);
            }
            if (propertyType == PropertyType.PROPERTY_READ) {
                if (!readInfoMap.containsKey(key)) {
                    readInfoMap.put(key, bluetoothGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_WRITE) {
                if (!writeInfoMap.containsKey(key)) {
                    writeInfoMap.put(key, bluetoothGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_NOTIFY) {
                if (!enableInfoMap.containsKey(key)) {
                    enableInfoMap.put(key, bluetoothGattChannel);
                }
            } else if (propertyType == PropertyType.PROPERTY_INDICATE) {
                if (!enableInfoMap.containsKey(key)) {
                    enableInfoMap.put(key, bluetoothGattChannel);
                }
            }
        }
    }

    /**
     * 解除通道
     *
     * @param bluetoothGattChannel
     */
    public synchronized void unbindChannel(BluetoothGattChannel bluetoothGattChannel) {
        if (bluetoothGattChannel != null) {
            String key = bluetoothGattChannel.getGattInfoKey();
            if (bleCallbackMap.containsKey(key)) {
                bleCallbackMap.remove(key);
            }
            if (readInfoMap.containsKey(key)) {
                readInfoMap.remove(key);
            } else if (writeInfoMap.containsKey(key)) {
                writeInfoMap.remove(key);
            } else if (enableInfoMap.containsKey(key)) {
                enableInfoMap.remove(key);
            }
        }
    }

    /**
     * 輸入數據
     * @param data
     */
    public void writeData(byte[] data) {
        if (data == null || data.length > 20) {
            ViseLog.e("this data is null or length beyond 20 byte.");
            return;
        }
        if (!checkBluetoothGattInfo(writeInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
            handler.removeMessages(MSG_WRITE_DATA_RETRY);
        }
        writeDataRetryCount = 0;
        writeData = data;
        write(data);
    }

    /**
     * 讀取數據
     */
    public boolean readData() {
        if (!checkBluetoothGattInfo(readInfoMap)) {
            return Boolean.parseBoolean(null);
        }
        if (handler != null) {
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);
            handler.removeMessages(MSG_READ_DATA_RETRY);
        }
        readDataRetryCount = 0;
        return read();
        //String a =String.valueOf(read());
        //return a;
    }

    /**
     * 取得設備訊號值
     * @param rssiCallback
     */
    public void readRemoteRssi(IRssiCallback rssiCallback) {
        this.rssiCallback = rssiCallback;
        if (bluetoothGatt != null) {
            bluetoothGatt.readRemoteRssi();
        }
    }

    /**
     * 註冊取得數據通知
     * @param isIndication
     */
    public void registerNotify(boolean isIndication) {
        if (!checkBluetoothGattInfo(enableInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
            handler.removeMessages(MSG_RECEIVE_DATA_RETRY);
        }
        receiveDataRetryCount = 0;
        enable = true;
        this.isIndication = isIndication;
        enable(enable, this.isIndication);
    }

    /**
     * 停止取得數據通知
     * @param isIndication
     */
    public void unregisterNotify(boolean isIndication) {
        if (!checkBluetoothGattInfo(enableInfoMap)) {
            return;
        }
        if (handler != null) {
            handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
            handler.removeMessages(MSG_RECEIVE_DATA_RETRY);
        }
        enable = false;
        this.isIndication = isIndication;
        enable(enable, this.isIndication);
    }

    /**
     * 設定接收數據監聽
     * @param key             接收數據回調key，由serviceUUID+characteristicUUID+descriptorUUID組成
     * @param receiveCallback 接收數據回掉
     */
    public void setNotifyListener(String key, IBleCallback receiveCallback) {
        receiveCallbackMap.put(key, receiveCallback);
    }

    /**
     * 取得目前連接重試失敗次數
     */
    public int getConnectRetryCount() {
        return connectRetryCount;
    }

    /**
     * 取得目前讀取數據失敗重試次數
     */
    public int getReadDataRetryCount() {
        return readDataRetryCount;
    }

    /**
     * 取得目前enable數據失敗重試次數
     */
    public int getReceiveDataRetryCount() {
        return receiveDataRetryCount;
    }

    /**
     * 取得目前寫入數據重試失敗次數
     */
    public int getWriteDataRetryCount() {
        return writeDataRetryCount;
    }

    /**
     * 取得設備標示
     */
    public String getUniqueSymbol() {
        return uniqueSymbol;
    }

    /**
     * 取得藍芽GATT
     */
    public BluetoothGatt getBluetoothGatt() {
        return bluetoothGatt;
    }

    /**
     * 取得設備連接狀態
     */
    public ConnectState getConnectState() {
        return connectState;
    }

    /**
     * 取得服務列表
     */
    public List<BluetoothGattService> getGattServiceList() {
        if (bluetoothGatt != null) {
            return bluetoothGatt.getServices();
        }
        return null;
    }

    /**
     * 根據UUID指定服務
     * @param serviceUuid
     */
    public BluetoothGattService getGattService(UUID serviceUuid) {
        if (bluetoothGatt != null && serviceUuid != null) {
            return bluetoothGatt.getService(serviceUuid);
        }
        return null;
    }

    /**
     * 取得某個服務的特徵值列表
     * @param serviceUuid
     */
    public List<BluetoothGattCharacteristic> getGattCharacteristicList(UUID serviceUuid) {
        if (getGattService(serviceUuid) != null && serviceUuid != null) {
            return getGattService(serviceUuid).getCharacteristics();
        }
        return null;
    }

    /**
     * 根據特徵值UUID取得某個服務的指定特徵值
     * @param serviceUuid
     * @param characteristicUuid
     */
    public BluetoothGattCharacteristic getGattCharacteristic(UUID serviceUuid, UUID characteristicUuid) {
        if (getGattService(serviceUuid) != null && serviceUuid != null && characteristicUuid != null) {
            return getGattService(serviceUuid).getCharacteristic(characteristicUuid);
        }
        return null;
    }

    /**
     * 取得啂個特徵值的描述屬性列表
     * @param serviceUuid
     * @param characteristicUuid
     */
    public List<BluetoothGattDescriptor> getGattDescriptorList(UUID serviceUuid, UUID characteristicUuid) {
        if (getGattCharacteristic(serviceUuid, characteristicUuid) != null && serviceUuid != null && characteristicUuid != null) {
            return getGattCharacteristic(serviceUuid, characteristicUuid).getDescriptors();
        }
        return null;
    }

    /**
     * 根據描述屬性UUID取得某個特徵值的指定屬性值
     * @param serviceUuid
     * @param characteristicUuid
     * @param descriptorUuid
     */
    public BluetoothGattDescriptor getGattDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid) {
        if (getGattCharacteristic(serviceUuid, characteristicUuid) != null && serviceUuid != null && characteristicUuid != null && descriptorUuid != null) {
            return getGattCharacteristic(serviceUuid, characteristicUuid).getDescriptor(descriptorUuid);
        }
        return null;
    }

    /**
     * 取得設備詳細訊息
     */
    public BluetoothLeDevice getBluetoothLeDevice() {
        return bluetoothLeDevice;
    }

    /**
     * 設備是否連接
     */
    public boolean isConnected() {
        return connectState == ConnectState.CONNECT_SUCCESS;
    }

    /**
     * 移除操作數據回調
     * @param key
     */
    public synchronized void removeBleCallback(String key) {
        if (bleCallbackMap.containsKey(key)) {
            bleCallbackMap.remove(key);
        }
    }

    /**
     * 移除接收數據回調
     * @param key
     */
    public synchronized void removeReceiveCallback(String key) {
        if (receiveCallbackMap.containsKey(key)) {
            receiveCallbackMap.remove(key);
        }
    }

    /**
     * 移除所有回調
     */
    public synchronized void removeAllCallback() {
        bleCallbackMap.clear();
        receiveCallbackMap.clear();
    }

    /**
     * 重置設備暫存
     */
    public synchronized boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null && bluetoothGatt != null) {
                final boolean success = (Boolean) refresh.invoke(getBluetoothGatt());
                ViseLog.i("Refreshing result: " + success);
                return success;
            }
        } catch (Exception e) {
            ViseLog.e("An exception occured while refreshing device" + e);
        }
        return false;
    }

    /**
     * 主動斷開連線
     */
    public synchronized void disconnect() {
        connectState = ConnectState.CONNECT_INIT;
        connectRetryCount = 0;
        if (bluetoothGatt != null) {
            isActiveDisconnect = true;
            bluetoothGatt.disconnect();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 關閉GATT
     */
    public synchronized void close() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
    }

    @Override
    public String toString() {
        return "DeviceMirror{" +
                "bluetoothLeDevice=" + bluetoothLeDevice +
                ", uniqueSymbol='" + uniqueSymbol + '\'' +
                '}';
    }

    /**
     * 清除設備暫存
     */
    public synchronized void clear() {
        ViseLog.i("deviceMirror clear.");
        disconnect();
        refreshDeviceCache();
        close();
        if (bleCallbackMap != null) {
            bleCallbackMap.clear();
        }
        if (receiveCallbackMap != null) {
            receiveCallbackMap.clear();
        }
        if (writeInfoMap != null) {
            writeInfoMap.clear();
        }
        if (readInfoMap != null) {
            readInfoMap.clear();
        }
        if (enableInfoMap != null) {
            enableInfoMap.clear();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * UUID轉換
     *
     * @param uuid
     * @return 返回UUID
     */
    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }

    /**
     * 檢查BluetoothGattChannel是否為空
     *
     * @param bluetoothGattInfoHashMap
     * @return
     */
    private boolean checkBluetoothGattInfo(HashMap<String, BluetoothGattChannel> bluetoothGattInfoHashMap) {
        if (bluetoothGattInfoHashMap == null || bluetoothGattInfoHashMap.size() == 0) {
            ViseLog.e("this bluetoothGattInfo map is not value.");
            return false;
        }
        return true;
    }

    /**
     * 連接設備
     */
    private synchronized void connect() {
        if (handler != null) {
            handler.removeMessages(MSG_CONNECT_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_CONNECT_TIMEOUT, BleConfig.getInstance().getConnectTimeout());
        }
        connectState = ConnectState.CONNECT_PROCESS;
        if (bluetoothLeDevice != null && bluetoothLeDevice.getDevice() != null) {
            bluetoothLeDevice.getDevice().connectGatt(ViseBle.getInstance().getContext(), false, coreGattCallback);
        }
    }

    /**
     * 設定enable
     * @param enable       是否有功用
     * @param isIndication 是否是指示器方式
     * @return
     */
    private synchronized boolean enable(boolean enable, boolean isIndication) {
        if (handler != null) {
            handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_RECEIVE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());
        }
        boolean success = false;
        for (Map.Entry<String, BluetoothGattChannel> entry : enableInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BluetoothGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null) {
                success = bluetoothGatt.setCharacteristicNotification(bluetoothGattInfoValue.getCharacteristic(), enable);
            }
            BluetoothGattDescriptor bluetoothGattDescriptor = null;
            if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattDescriptor = bluetoothGattInfoValue.getDescriptor();
            } else if (bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                if (bluetoothGattInfoValue.getCharacteristic().getDescriptors() != null
                        && bluetoothGattInfoValue.getCharacteristic().getDescriptors().size() == 1) {
                    bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic().getDescriptors().get(0);
                } else {
                    bluetoothGattDescriptor = bluetoothGattInfoValue.getCharacteristic()
                            .getDescriptor(UUID.fromString(BleConstant.CLIENT_CHARACTERISTIC_CONFIG));
                }
            }
            if (bluetoothGattDescriptor != null) {
                bluetoothGattInfoValue.setDescriptor(bluetoothGattDescriptor);
                if (isIndication) {
                    if (enable) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                } else {
                    if (enable) {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    } else {
                        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    }
                }
                if (bluetoothGatt != null) {
                    bluetoothGatt.writeDescriptor(bluetoothGattDescriptor);
                }
            }
        }
        return success;
    }

    public static String getDATA(){
        return read;
    }
    /**
     * 讀取數據
     */
    private synchronized boolean read() {
        if (handler != null) {
            handler.removeMessages(MSG_READ_DATA_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_READ_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());
        }
        boolean success = false;

        for (Map.Entry<String, BluetoothGattChannel> entry : readInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BluetoothGattChannel bluetoothGattInfoValue = entry.getValue();
            read = new String(String.valueOf(entry.getValue()));
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                success = bluetoothGatt.readDescriptor(bluetoothGattInfoValue.getDescriptor());
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                success = bluetoothGatt.readCharacteristic(bluetoothGattInfoValue.getCharacteristic());
            }
        }
        return success;
    }

    /**
     * 寫入數據
     * @param data
     */
    private synchronized boolean write(byte[] data) {
        if (handler != null) {
            handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
            handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_TIMEOUT, BleConfig.getInstance().getOperateTimeout());
        }
        boolean success = false;
        for (Map.Entry<String, BluetoothGattChannel> entry : writeInfoMap.entrySet()) {
            String bluetoothGattInfoKey = entry.getKey();
            BluetoothGattChannel bluetoothGattInfoValue = entry.getValue();
            if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() != null) {
                bluetoothGattInfoValue.getDescriptor().setValue(data);
                success = bluetoothGatt.writeDescriptor(bluetoothGattInfoValue.getDescriptor());
            } else if (bluetoothGatt != null && bluetoothGattInfoValue.getCharacteristic() != null && bluetoothGattInfoValue.getDescriptor() == null) {
                bluetoothGattInfoValue.getCharacteristic().setValue(data);
                success = bluetoothGatt.writeCharacteristic(bluetoothGattInfoValue.getCharacteristic());
            }
        }
        return success;
    }

    /**
     * 連接失敗處理
     * @param bleException 回調異常
     */
    private void connectFailure(BleException bleException) {
        if (connectRetryCount < BleConfig.getInstance().getConnectRetryCount()) {
            connectRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_CONNECT_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_CONNECT_RETRY, BleConfig.getInstance().getConnectRetryInterval());
            }
            ViseLog.i("connectFailure connectRetryCount is " + connectRetryCount);
        } else {
            if (bleException instanceof TimeoutException) {
                connectState = ConnectState.CONNECT_TIMEOUT;
            } else {
                connectState = ConnectState.CONNECT_FAILURE;
            }
            close();
            if (connectCallback != null) {
                connectCallback.onConnectFailure(bleException);
            }
            ViseLog.i("connectFailure " + bleException);
        }
    }

    /**
     * enable失敗
     * @param bleException
     * @param isRemoveCall
     */
    private void enableFailure(BleException bleException, boolean isRemoveCall) {
        if (receiveDataRetryCount < BleConfig.getInstance().getOperateRetryCount()) {
            receiveDataRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_RECEIVE_DATA_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_RECEIVE_DATA_RETRY, BleConfig.getInstance().getOperateRetryInterval());
            }
            ViseLog.i("enableFailure receiveDataRetryCount is " + receiveDataRetryCount);
        } else {
            handleFailureData(enableInfoMap, bleException, isRemoveCall);
            ViseLog.i("enableFailure " + bleException);
        }
    }

    /**
     * 讀取數據失敗
     * @param bleException
     * @param isRemoveCall
     */
    private void readFailure(BleException bleException, boolean isRemoveCall) {
        if (readDataRetryCount < BleConfig.getInstance().getOperateRetryCount()) {
            readDataRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_READ_DATA_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_READ_DATA_RETRY, BleConfig.getInstance().getOperateRetryInterval());
            }
            ViseLog.i("readFailure readDataRetryCount is " + readDataRetryCount);
        } else {
            handleFailureData(readInfoMap, bleException, isRemoveCall);
            ViseLog.i("readFailure " + bleException);
        }
    }

    /**
     * 寫入數據失敗
     * @param bleException
     * @param isRemoveCall
     */
    private void writeFailure(BleException bleException, boolean isRemoveCall) {
        if (writeDataRetryCount < BleConfig.getInstance().getOperateRetryCount()) {
            writeDataRetryCount++;
            if (handler != null) {
                handler.removeMessages(MSG_WRITE_DATA_TIMEOUT);
                handler.sendEmptyMessageDelayed(MSG_WRITE_DATA_RETRY, BleConfig.getInstance().getOperateRetryInterval());
            }
            ViseLog.i("writeFailure writeDataRetryCount is " + writeDataRetryCount);
        } else {
            handleFailureData(writeInfoMap, bleException, isRemoveCall);
            ViseLog.i("writeFailure " + bleException);
        }
    }

    /**
     * 處裡數據發送成功
     * @param bluetoothGattInfoHashMap
     * @param value                    等待發送的數據
     * @param status                   發送狀態數據
     * @param isRemoveCall             是否需要移除回調
     */
    private synchronized void handleSuccessData(HashMap<String, BluetoothGattChannel> bluetoothGattInfoHashMap, byte[] value, int status,
                                                boolean isRemoveCall) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        String removeBleCallbackKey = null;
        String removeBluetoothGattInfoKey = null;
        for (Map.Entry<String, IBleCallback> callbackEntry : bleCallbackMap.entrySet()) {
            String bleCallbackKey = callbackEntry.getKey();
            IBleCallback bleCallbackValue = callbackEntry.getValue();
            for (Map.Entry<String, BluetoothGattChannel> gattInfoEntry : bluetoothGattInfoHashMap.entrySet()) {
                String bluetoothGattInfoKey = gattInfoEntry.getKey();
                BluetoothGattChannel bluetoothGattInfoValue = gattInfoEntry.getValue();
                if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                    bleCallbackValue.onSuccess(value, bluetoothGattInfoValue, bluetoothLeDevice);
                    removeBleCallbackKey = bleCallbackKey;
                    removeBluetoothGattInfoKey = bluetoothGattInfoKey;
                }
            }
        }
        synchronized (bleCallbackMap) {
            if (isRemoveCall && removeBleCallbackKey != null && removeBluetoothGattInfoKey != null) {
                bleCallbackMap.remove(removeBleCallbackKey);
                bluetoothGattInfoHashMap.remove(removeBluetoothGattInfoKey);
            }
        }
    }

    /**
     *處理數據發送失敗
     *
     * @param bluetoothGattInfoHashMap
     * @param bleExceprion             回調異常
     * @param isRemoveCall             是否需要移除回調
     */
    private synchronized void handleFailureData(HashMap<String, BluetoothGattChannel> bluetoothGattInfoHashMap, BleException bleExceprion,
                                                boolean isRemoveCall) {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        String removeBleCallbackKey = null;
        String removeBluetoothGattInfoKey = null;
        for (Map.Entry<String, IBleCallback> callbackEntry : bleCallbackMap.entrySet()) {
            String bleCallbackKey = callbackEntry.getKey();
            IBleCallback bleCallbackValue = callbackEntry.getValue();
            for (Map.Entry<String, BluetoothGattChannel> gattInfoEntry : bluetoothGattInfoHashMap.entrySet()) {
                String bluetoothGattInfoKey = gattInfoEntry.getKey();
                if (bleCallbackKey.equals(bluetoothGattInfoKey)) {
                    bleCallbackValue.onFailure(bleExceprion);
                    removeBleCallbackKey = bleCallbackKey;
                    removeBluetoothGattInfoKey = bluetoothGattInfoKey;
                }
            }
        }
        synchronized (bleCallbackMap) {
            if (isRemoveCall && removeBleCallbackKey != null && removeBluetoothGattInfoKey != null) {
                bleCallbackMap.remove(removeBleCallbackKey);
                bluetoothGattInfoHashMap.remove(removeBluetoothGattInfoKey);
            }
        }
    }
}
