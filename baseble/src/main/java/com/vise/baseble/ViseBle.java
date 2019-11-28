package com.vise.baseble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.vise.baseble.callback.IConnectCallback;
import com.vise.baseble.callback.scan.IScanCallback;
import com.vise.baseble.callback.scan.ScanCallback;
import com.vise.baseble.callback.scan.SingleFilterScanCallback;
import com.vise.baseble.common.BleConfig;
import com.vise.baseble.common.ConnectState;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.core.DeviceMirrorPool;
import com.vise.baseble.exception.TimeoutException;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.baseble.model.BluetoothLeDeviceStore;
import com.vise.log.ViseLog;

/**
 * @Description: BLE設備操作
 */
public class ViseBle {
    private Context context;//上下文
    private BluetoothManager bluetoothManager;//藍芽管理
    private BluetoothAdapter bluetoothAdapter;//藍芽適配器
    private DeviceMirrorPool deviceMirrorPool;//設備連接池
    private DeviceMirror lastDeviceMirror;//上次操作設備鏡像

    private static ViseBle instance;//入口操作管理
    private static BleConfig bleConfig = BleConfig.getInstance();

    /**
     * 單例方式獲取藍芽通訊入口
     * @return 返回ViseBluetooth
     */
    public static ViseBle getInstance() {
        if (instance == null) {
            synchronized (ViseBle.class) {
                if (instance == null) {
                    instance = new ViseBle();
                }
            }
        }
        return instance;
    }

    private ViseBle() {
    }

    /**
     * 獲取配置對象，可進行相關配置的修改
     */
    public static BleConfig config() {
        return bleConfig;
    }

    /**
     * 初始化
     * @param context 上下文
     */
    public void init(Context context) {
        if (this.context == null && context != null) {
            this.context = context.getApplicationContext();
            bluetoothManager = (BluetoothManager) this.context.getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            deviceMirrorPool = new DeviceMirrorPool();
        }
    }

    /**
     * 開始掃描
     * @param scanCallback 自定義回調
     */
    public void startScan(ScanCallback scanCallback) {
        if (scanCallback == null) {
            throw new IllegalArgumentException("this ScanCallback is Null!");
        }
        scanCallback.setScan(true).scan();
    }

    /**
     * 停止掃描
     * @param scanCallback 自定義回調
     */
    public void stopScan(ScanCallback scanCallback) {
        if (scanCallback == null) {
            throw new IllegalArgumentException("this ScanCallback is Null!");
        }
        scanCallback.setScan(false).removeHandlerMsg().scan();
    }

    /**
     * 連接設備
     * @param bluetoothLeDevice
     * @param connectCallback
     */
    public void connect(BluetoothLeDevice bluetoothLeDevice, IConnectCallback connectCallback) {
        if (bluetoothLeDevice == null) {
            ViseLog.e("This bluetoothLeDevice is null.");
            return;
        }else if(connectCallback == null){
            ViseLog.e("This connectCallback is null.");
            return;
        }
        if (deviceMirrorPool != null && !deviceMirrorPool.isContainDevice(bluetoothLeDevice)) {
            DeviceMirror deviceMirror = new DeviceMirror(bluetoothLeDevice);
            if (lastDeviceMirror != null && !TextUtils.isEmpty(lastDeviceMirror.getUniqueSymbol())
                    && lastDeviceMirror.getUniqueSymbol().equals(deviceMirror.getUniqueSymbol())) {
                deviceMirror = lastDeviceMirror;//防止重复创建设备镜像
            }
            deviceMirror.connect(connectCallback);
            lastDeviceMirror = deviceMirror;
        } else {
            ViseLog.i("This device is connected.");
        }
    }

    /**
     * 連接指定MAC位址的設備
     * @param mac             設備MAC位址
     * @param connectCallback 連接回調
     */
    public void connectByMac(String mac, final IConnectCallback connectCallback) {
        if (mac == null || connectCallback == null) {
            ViseLog.e("This mac or connectCallback is null.");
            return;
        }
        startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {

            }

            @Override
            public void onScanFinish(final BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                if (bluetoothLeDeviceStore.getDeviceList().size() > 0) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
                        }
                    });
                } else {
                    connectCallback.onConnectFailure(new TimeoutException());
                }
            }

            @Override
            public void onScanTimeout() {
                connectCallback.onConnectFailure(new TimeoutException());
            }

        }).setDeviceMac(mac));
    }

    /**
     * 連接指定名稱的設備
     * @param name            設備名稱
     * @param connectCallback 連接回調
     */
    public void connectByName(String name, final IConnectCallback connectCallback) {
        if (name == null || connectCallback == null) {
            ViseLog.e("This name or connectCallback is null.");
            return;
        }
        startScan(new SingleFilterScanCallback(new IScanCallback() {
            @Override
            public void onDeviceFound(BluetoothLeDevice bluetoothLeDevice) {

            }

            @Override
            public void onScanFinish(final BluetoothLeDeviceStore bluetoothLeDeviceStore) {
                if (bluetoothLeDeviceStore.getDeviceList().size() > 0) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            connect(bluetoothLeDeviceStore.getDeviceList().get(0), connectCallback);
                        }
                    });
                } else {
                    connectCallback.onConnectFailure(new TimeoutException());
                }
            }

            @Override
            public void onScanTimeout() {
                connectCallback.onConnectFailure(new TimeoutException());
            }

        }).setDeviceName(name));
    }

    /**
     * 取得連接池中的設備鏡像，若無則返回空
     * @param bluetoothLeDevice
     * @return
     */
    public DeviceMirror getDeviceMirror(BluetoothLeDevice bluetoothLeDevice) {
        if (deviceMirrorPool != null) {
            return deviceMirrorPool.getDeviceMirror(bluetoothLeDevice);
        }
        return null;
    }

    /**
     * 獲取設備連接狀態
     * @param bluetoothLeDevice
     * @return
     */
    public ConnectState getConnectState(BluetoothLeDevice bluetoothLeDevice) {
        if (deviceMirrorPool != null) {
            return deviceMirrorPool.getConnectState(bluetoothLeDevice);
        }
        return ConnectState.CONNECT_DISCONNECT;
    }

    /**
     * 判斷該設備是否已連接
     * @param bluetoothLeDevice
     * @return
     */
    public boolean isConnect(BluetoothLeDevice bluetoothLeDevice) {
        if (deviceMirrorPool != null) {
            return deviceMirrorPool.isContainDevice(bluetoothLeDevice);
        }
        return false;
    }

    /**
     * 斷開指定設備
     * @param bluetoothLeDevice
     */
    public void disconnect(BluetoothLeDevice bluetoothLeDevice) {
        if (deviceMirrorPool != null) {
            deviceMirrorPool.disconnect(bluetoothLeDevice);
        }
    }

    /**
     * 斷開所有設備
     */
    public void disconnect() {
        if (deviceMirrorPool != null) {
            deviceMirrorPool.disconnect();
        }
    }

    /**
     * 清除資源，在退出應用時調用
     */
    public void clear() {
        if (deviceMirrorPool != null) {
            deviceMirrorPool.clear();
        }
    }

    /**
     * 獲取Context
     * @return 返回Context
     */
    public Context getContext() {
        return context;
    }

    /**
     * 獲取藍芽管理
     * @return 返回藍芽管理
     */
    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    /**
     * 獲取藍芽適配器
     * @return 返回藍芽適配器
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * 獲取設備鏡像池
     * @return
     */
    public DeviceMirrorPool getDeviceMirrorPool() {
        return deviceMirrorPool;
    }

    /**
     * 獲取當前連接失敗重試次數
     * @return
     */
    public int getConnectRetryCount() {
        if (lastDeviceMirror == null) {
            return 0;
        }
        return lastDeviceMirror.getConnectRetryCount();
    }

    /**
     * 獲取當前讀取數據失敗重試次數
     * @return
     */
    public int getReadDataRetryCount() {
        if (lastDeviceMirror == null) {
            return 0;
        }
        return lastDeviceMirror.getReadDataRetryCount();
    }

    /**
     * 獲取當前enable數據失敗重試次數
     * @return
     */
    public int getReceiveDataRetryCount() {
        if (lastDeviceMirror == null) {
            return 0;
        }
        return lastDeviceMirror.getReceiveDataRetryCount();
    }

    /**
     * 獲取當前寫入數據失敗重試次數
     * @return
     */
    public int getWriteDataRetryCount() {
        if (lastDeviceMirror == null) {
            return 0;
        }
        return lastDeviceMirror.getWriteDataRetryCount();
    }
}
