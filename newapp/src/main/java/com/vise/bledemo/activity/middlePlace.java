package com.vise.bledemo.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.vise.baseble.common.PropertyType;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.bledemo.R;
import com.vise.bledemo.common.BluetoothDeviceManager;
import com.vise.xsnow.cache.SpCache;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * The UI for going to Transaction UI or Search UI
 * It'll connect Ble device first at this class
 **/
public class middlePlace extends AppCompatActivity{

    public static final String WRITE_CHARACTERISTI_UUID_KEY = "write_uuid_key";
    public static final String WRITE_DATA_KEY = "write_data_key";
    public static final String EXTRA_DEVICE = "extra_device";
    public static final String CONNECT_FAILED = "Connect failed";
    public static final String CONNECT_SUCCESS = "Connect success";

    private static BluetoothLeDevice mDevice;
    private TextView Loading;
    private TextView Status;
    private Button TestBTN;
    private Button Transaction;
    private Button resetconnect;
    private Button STARTTEST;
    private static SpCache mSpCache;
    private ProgressBar progressbar;

    static UUID ServiceUUID = UUID.fromString("00001823-0000-1000-8000-00805f9b34fb"); //1823   Write
    static UUID URI_UUID = UUID.fromString("00002ab6-0000-1000-8000-00805f9b34fb");    //2AB6   Write
    static UUID DATA_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");   //1001   Write
    static UUID RW_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");     //1000   Write
    static UUID Control_UUID = UUID.fromString("00002aba-0000-1000-8000-00805f9b34fb");//2ABA   Write    1:get  3:post
    UUID Body_UUID = UUID.fromString("00002ab9-0000-1000-8000-00805f9b34fb");   //2AB9   Read
    UUID Status_UUID = UUID.fromString("00002abb-0000-1000-8000-00805f9b34fb"); //2ABB   Read     讀取若為200則成功
    URI Get_token_Web3 = URI.create("http://192.168.50.20:5000/get_token");

    String Get2ABA = "1";
    String publickey = "showPublickey";
    String Address="address";
    String Address_store;
    String priv_hash_BLE = "priv_hash";
    String testAPI = "test";
    String priv_hash_store = "NOTHING";
    String response_Web3;
    String TOKEN;
    String VALUE_FOR_Test = "9,9,10,2";
    byte [] IV = new byte[16];
    byte [] KEY = new byte[16];
    String TEST;

    StringEntity priv_hash_change;
    JSONObject json;

    @Override
    protected void onDestroy(){
        super.onDestroy();
        BluetoothDeviceManager.getInstance().disconnect(mDevice);
    }
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.middleplace);
        mDevice = getIntent().getParcelableExtra("extra_device");
        Status = findViewById(R.id.Status_status);
        Transaction = findViewById(R.id.transaction);
        Loading = findViewById(R.id.space_2);
        progressbar = findViewById(R.id.progressbar);
        TestBTN = findViewById(R.id.test);
        mSpCache = new SpCache(this);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        BluetoothDeviceManager.getInstance().connect(mDevice);
        //連線設備
        while (!BluetoothDeviceManager.getInstance().isConnected(mDevice)){
            BluetoothDeviceManager.getInstance().connect(mDevice);
            delay(500);
        }
        Toast.makeText(this, "Connect", Toast.LENGTH_SHORT).show();
        //priv_hash_Thread_URI.start();
        DoThread_1.start();
        DoThread_2.start();
        middlePlace();
    }
    public void middlePlace(){
        Transaction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BluetoothDeviceManager.getInstance().isConnected(mDevice)) {
                    System.out.println("Out of connect");
                    new AlertDialog.Builder(middlePlace.this)
                            .setTitle("未連線至藍芽設備")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent next = new Intent(middlePlace.this,DeviceControlActivity.class);

                                    startActivity(next);
                                    finish();
                                }
                            }).show();
                }else{
                    Intent next = new Intent(middlePlace.this,DeviceControlActivity.class);
                    next.putExtra(middlePlace.EXTRA_DEVICE, mDevice);
                    Bundle bundle = new Bundle();
                    bundle.putString("priv_hash",priv_hash_store);
                    bundle.putString("Address",Address_store);
                    bundle.putString("Token",TOKEN);
                    bundle.putByteArray("IV",IV);
                    bundle.putByteArray("KEY",KEY);
                    next.putExtras(bundle);
                    startActivity(next);
                }
            }
        });
        TestBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                test.start();
            }
        });
    }

    /**函式*/
    public static void delay(int ms){
        try {
            Thread.currentThread();
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }//執行緒暫停的函式
    public String getTOKEN(){
        json =new JSONObject();
        try {
            json.put("id",priv_hash_store);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            priv_hash_change = new StringEntity(json.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Web3Control.Post_to_Web3(Get_token_Web3,priv_hash_change);
        response_Web3 = Web3Control.get_response();
        return response_Web3;
    }//向Web3 取得Token 的函式
    public static void BLE_READ(){
        BluetoothDeviceManager.getInstance().bindChannel(mDevice, PropertyType.PROPERTY_READ, ServiceUUID, UUID.fromString("00002ab9-0000-1000-8000-00805f9b34fb"), null);
        BluetoothDeviceManager.getInstance().read(mDevice);
    }//透過 UUID : 2AB9 讀取 Body
    public static void BLE_URI(String URI){
        //用 URI UUID 建立通道
        mSpCache.put(WRITE_CHARACTERISTI_UUID_KEY + mDevice.getAddress(), URI_UUID.toString());
        BluetoothDeviceManager.getInstance().bindChannel(mDevice, PropertyType.PROPERTY_WRITE, ServiceUUID, URI_UUID, null);
        mSpCache.remove(WRITE_CHARACTERISTI_UUID_KEY+mDevice.getAddress());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSpCache.put(WRITE_DATA_KEY + mDevice.getAddress(), URI.getBytes(StandardCharsets.UTF_8));
        }
        //透過 URI UUID 所建立的通道傳送URL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BluetoothDeviceManager.getInstance().write(mDevice, URI.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("WRITE URI : " + URI + " SUCCESS");
        mSpCache.remove(WRITE_DATA_KEY+mDevice.getAddress());
    }//透過 UUID : 2AB6 傳送URI
    public static void BLE_RW(String word){
        //用 R/W UUID 建立通道
        mSpCache.put(WRITE_CHARACTERISTI_UUID_KEY + mDevice.getAddress(), RW_UUID.toString());
        BluetoothDeviceManager.getInstance().bindChannel(mDevice, PropertyType.PROPERTY_WRITE, ServiceUUID, RW_UUID, null);
        mSpCache.remove(WRITE_CHARACTERISTI_UUID_KEY + mDevice.getAddress());

        //透過 R/W UUID 所建立的通道傳送數據
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSpCache.put(WRITE_DATA_KEY + mDevice.getAddress(), word.getBytes(StandardCharsets.UTF_8));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BluetoothDeviceManager.getInstance().write(mDevice, word.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("Send R/W : " + word + " SUCCESS");
        mSpCache.remove(WRITE_DATA_KEY + mDevice.getAddress());
    }//透過 UUID : 1000  傳送 R/W
    public void CHECK_BLE_STATUS(TextView textview){
        if (!BluetoothDeviceManager.getInstance().isConnected(mDevice)) {
            textview.setText(CONNECT_FAILED);
            textview.setTextColor(Color.parseColor("#FF0000"));
        }else {
            textview.setText(CONNECT_SUCCESS);
            textview.setTextColor(Color.parseColor("#00DD00"));
        }
    }//檢查 BLE 連線狀態
    public static void BLE_DATA(String data){
        //用 DATA UUID 建立通道
        mSpCache.put(WRITE_CHARACTERISTI_UUID_KEY + mDevice.getAddress(), DATA_UUID.toString());
        BluetoothDeviceManager.getInstance().bindChannel(mDevice, PropertyType.PROPERTY_WRITE, ServiceUUID, DATA_UUID, null);
        mSpCache.remove(WRITE_CHARACTERISTI_UUID_KEY + mDevice.getAddress());
        System.out.println("Make connect Channel:DATA UUID");

        //透過DATA UUID 建立的通道傳送打包後的值
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSpCache.put(WRITE_DATA_KEY + mDevice.getAddress(), data.getBytes(StandardCharsets.UTF_8));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BluetoothDeviceManager.getInstance().write(mDevice, data.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("Send DATA : " + data + " SUCCESS");
        mSpCache.remove(WRITE_DATA_KEY + mDevice.getAddress());
    }//透過 UUID : 1000 傳送 DATA
    public static void BLE_CONTROL(String control_point){
        //用 Control point UUID 建立通道
        mSpCache.put(WRITE_CHARACTERISTI_UUID_KEY + mDevice.getAddress(), Control_UUID.toString());
        BluetoothDeviceManager.getInstance().bindChannel(mDevice, PropertyType.PROPERTY_WRITE, ServiceUUID, Control_UUID, null);
        mSpCache.remove(WRITE_CHARACTERISTI_UUID_KEY+mDevice.getAddress());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mSpCache.put(WRITE_DATA_KEY + mDevice.getAddress(), control_point.getBytes(StandardCharsets.UTF_8));
        }
        //透過 Cpntrol point UUID 所建立的通道傳送控制指令
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            BluetoothDeviceManager.getInstance().write(mDevice, control_point.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("WRITE control point : " + control_point + " SUCCESS");
        mSpCache.remove(WRITE_DATA_KEY+mDevice.getAddress());
    }//透過 UUID : 2ABA 傳送 Control point
    public static byte[] hexToBytes(String hexString) {

        char[] hex = hexString.toCharArray();
        //轉rawData長度減半
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            //先將hex資料轉10進位數值
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            //將第一個值的二進位值左平移4位,ex: 00001000 => 10000000 (8=>128)
            //然後與第二個值的二進位值作聯集ex: 10000000 | 00001100 => 10001100 (137)
            int value = (high << 4) | low;
            //與FFFFFFFF作補集
            if (value > 127)
                value -= 256;
            //最後轉回byte就OK
            rawData [i] = (byte) value;
        }
        return rawData ;
    }

    /**宣告執行緒*/
    Thread getBLE_address_URI = new getBLEAddress_URI();
    Thread getBLE_address_Ctrl = new getBLEAddress_Ctrl();
    Thread getBLE_address_READ = new getBLEAddress_READ();
    Thread getBLE_address_STORE = new getBLEAddress_STORE();
    Thread priv_hash_Thread_URI = new get_priv_hash_Thread_URI();
    Thread priv_hash_Thread_Ctrl = new get_priv_hash_Thread_Ctrl();
    Thread priv_hash_Thread_READ = new get_priv_hash_Thread_READ();
    Thread DoThread_1 = new connect_Thread_1();
    Thread DoThread_2 = new connect_Thread_2();
    Thread getToken = new gettoken();

    Thread test1 = new test_1();
    Thread test2 = new test_2();
    Thread test3 = new test_3();
    Thread test4 = new test_4();
    Thread test5 = new test_5();
    Thread test = new test_thread();

    /**執行緒內容*/
    class connect_Thread_2 extends Thread{
        public void run(){
            try {
                DoThread_1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("Start get Address");
            getBLE_address_URI.start();
            try {
                getBLE_address_URI.join();
                System.out.println("Get Address 1 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getBLE_address_Ctrl.start();
            try {
                getBLE_address_Ctrl.join();
                System.out.println("Get Address 2 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getBLE_address_READ.start();
            try {
                getBLE_address_READ.join();
                System.out.println("Get Address 3 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getBLE_address_STORE.start();
            try {
                getBLE_address_STORE.join();
                System.out.println("Get Address 4 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressbar.setVisibility(View.INVISIBLE);
                    Loading.setVisibility(View.INVISIBLE);
                    Transaction.setEnabled(true);
                    Transaction.setVisibility(View.VISIBLE);
                    Status.setText("Connected");
                    System.out.println("Set Transaction Btn on \n Set Progress Bar INVISIBLE");
                }
            });
        }
    }//執行 Get Address的執行緒
    class connect_Thread_1 extends Thread{
        public void run(){
            delay(1000);
            priv_hash_Thread_URI.start();
            try {
                priv_hash_Thread_URI.join();
                System.out.println("priv_hash 1 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            priv_hash_Thread_Ctrl.start();
            try {
                priv_hash_Thread_Ctrl.join();
                System.out.println("priv hash 2 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            priv_hash_Thread_READ.start();
            try {
                priv_hash_Thread_READ.join();
                System.out.println("priv hash 3 finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            getToken.start();
            System.out.println("Start getToken");
            delay(1000);
        }
    }//執行 Get priv_hash 及 Get Token 的執行緒

    class gettoken extends Thread{
        public void run(){
            TOKEN = getTOKEN();
            delay(1000);
            if(TOKEN.equals("account exist")){
                System.out.println("Delete exist account");
                Web3Control.Delete_Token_onWeb3(priv_hash_store);
                delay(1000);
                TOKEN = getTOKEN();
            }else if(TOKEN.equals("Authorization failed")){
                System.out.println("Authorization failed : Do again");
                Web3Control.Delete_Token_onWeb3(priv_hash_store);
                delay(1000);
                TOKEN = getTOKEN();
            }
            System.out.println("Your TOKEN is : " + TOKEN);
            String[] extra = TOKEN.split("xx");
            KEY = hexToBytes(extra[0]);
            System.out.println("Key is : "+ KEY+"  "+KEY.length);
            IV = hexToBytes(extra[1]);
            System.out.println("IV is : "+IV+"  "+IV.length);
        }
    }//向Web3 取得Token 的執行緒

    class getBLEAddress_URI extends Thread{
        public void run(){
            BLE_URI(Address);
            delay(1000);
        }
    }//向BLE 取得 Address的執行緒(1. 傳送 URI)
    class getBLEAddress_Ctrl extends Thread{
        public void run(){
            delay(1000);
            BLE_CONTROL(Get2ABA);
        }
    }//向BLE 取得 Address的執行緒(2. 傳送Control Point)
    class getBLEAddress_READ extends Thread{
        public void run(){
            delay(1000);
            BLE_READ();
        }
    }//向BLE 取得 Address的執行緒(3. 讀取 Body)
    class getBLEAddress_STORE extends Thread{
        public void run(){
            delay(1000);
            Address_store = DeviceMirror.Get_value();
            System.out.println("BLE address is : " + Address_store);
        }
    }//向BLE 取得 Address的執行緒(4. 儲存 Body 的值)

    class get_priv_hash_Thread_URI extends Thread{
        public void run(){
            if(BluetoothDeviceManager.getInstance().isConnected(mDevice)){
                BLE_URI(priv_hash_BLE);
            }else{
                System.out.println("BLE DISCONNECT");
            }
        }
    }//向BLE 取得priv_hash的執行緒(1. 傳送 URI)
    class get_priv_hash_Thread_Ctrl extends Thread{
        public void run(){
            delay(1000);
            BLE_CONTROL(Get2ABA);
        }
    }//向BLE 取得priv_hash的執行緒(2. 傳送Control Point)
    class get_priv_hash_Thread_READ extends Thread{
        public void run(){
            delay(1000);
            BLE_READ();
            delay(1000);
            priv_hash_store = DeviceMirror.Get_value();
            System.out.println("priv_hash_store : " + priv_hash_store);
        }
    }//向BLE 取得priv_hash的執行緒(3. 讀取並儲存 Body 的值)

    class test_thread extends Thread{
        public void run(){
            test1.start();
            try {
                test1.join();
                delay(1000);
                System.out.println("BLE_DATA : "+VALUE_FOR_Test+"-->Finish ");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            test2.start();
            try {
                test2.join();
                delay(1000);
                System.out.println("BLE_RW : "+ Address_store+"-->Finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            test3.start();
            try {
                test3.join();
                delay(1000);
                System.out.println("BLE_URI : ethertxn -->Finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            test4.start();
            try {
                test4.join();
                delay(1000);
                System.out.println("BLE_CONTROL : 3 -->Finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            test5.start();
            try {
                test5.join();
                delay(1000);
                TEST = DeviceMirror.Get_value();
                System.out.println("TXN : "+TEST+"-->Finish");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    class test_1 extends Thread{
        public void run(){
            BLE_DATA(VALUE_FOR_Test);
        }
    }
    class test_2 extends Thread{
        public void run(){
            BLE_RW(Address_store);
        }
    }
    class test_3 extends Thread{
        public void run(){
            BLE_URI("ethertxn");
        }
    }
    class test_4 extends Thread{
        public void run(){
            BLE_CONTROL("3");
        }
    }
    class test_5 extends Thread{
        public void run(){
            BLE_READ();
        }
    }
}
