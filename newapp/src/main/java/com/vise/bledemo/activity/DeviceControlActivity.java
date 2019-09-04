package com.vise.bledemo.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.bledemo.R;
import com.vise.bledemo.common.BluetoothDeviceManager;
import com.vise.log.ViseLog;
import com.vise.xsnow.cache.SpCache;
import com.vise.xsnow.event.BusManager;

import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.UUID;
/**
 * BLE Device control for Transaction UI
 * From middlePlace
 **/
public class DeviceControlActivity extends AppCompatActivity {

    public static BluetoothLeDevice mDevice = LoginActivity.mDevice;

    public static SpCache mSpCache;

    private TextView balance_title;
    private TextView balance;
    private TextView ReceiverAddress;
    private TextView gasPricevalue;
    private TextView gasvalue;
    private EditText mValue;
    private Button qrbtn;
    private Button CheckBtn;
    private Button SendBtn;
    private Button Balance_update;
    private String Nonce;
    private String Balance;
    private String Nonce_encrypted;
    private String Balance_encrypted;
    private String Address;
    private String Token;
    private String priv_hash;
    private SeekBar Gas_Seekbar;
    private SeekBar GasPrice_Seekbar;

    UUID ServiceUUID = UUID.fromString("00001823-0000-1000-8000-00805f9b34fb"); //1823
    UUID URI_UUID = UUID.fromString("00002ab6-0000-1000-8000-00805f9b34fb");    //2AB6
    UUID DATA_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");   //1001
    UUID RW_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");     //1000
    UUID Control_UUID = UUID.fromString("00002aba-0000-1000-8000-00805f9b34fb");//2ABA   1:get  3:post
    UUID Body_UUID = UUID.fromString("00002ab9-0000-1000-8000-00805f9b34fb");//2AB9
    UUID Status_UUID = UUID.fromString("00002abb-0000-1000-8000-00805f9b34fb");//2ABB  讀取若為200則成功

    URI Balance_Web3 = URI.create("http://192.168.50.20:5000/balance");
    URI Nonce_Web3 = URI.create("http://192.168.50.20:5000/nonce");
    URI transaction_Web3 = URI.create("http://192.168.50.20:5000/transaction");

    static int i;

    String Post2ABA = "3";
    String publickey = "publickey";
    String response_Web3;
    String TXN_TO_Web3 = "empty";
    String VALUE_FOR_TXN;
    String ResultOfTransaction;
    String encrypted;
    String Address_encrypted;
    String TXN_TO_Web3_encrypted;
    static String result;

    byte[] KEY = new byte[16];
    byte[] IV = new byte[16];

    StringEntity Balance_change;
    StringEntity Nonce_change;
    StringEntity TXN_change;

    JSONObject BalanceJson;
    JSONObject NonceJson;
    JSONObject TransactionJson;

    Intent next;

    @Override
    protected void onDestroy(){
        super.onDestroy();
        BluetoothDeviceManager.getInstance().disconnect(mDevice);
    }//關閉頁面後斷開與 BLE 的連線

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        BusManager.getBus().register(this);
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        /**將前一頁面請求的資訊傳到這個頁面*/
        Intent intent = this.getIntent();
        Bundle bundle = getIntent().getExtras();
        Address = bundle.getString("Address");
        priv_hash = bundle.getString("priv_hash");
        Token = bundle.getString("Token");
        KEY = bundle.getByteArray("KEY");
        IV = bundle.getByteArray("IV");
        System.out.println("Get Address :"+Address+"  Get priv_hash :"+priv_hash+"   Get Token :"+Token);

        //先將Address加密 稍後要 post 給 Server
        encrypted_ADDRESS_Thread.start();
        try {
            encrypted_ADDRESS_Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //連線設備
        while (!BluetoothDeviceManager.getInstance().isConnected(mDevice)){
            BluetoothDeviceManager.getInstance().connect(mDevice);
        }

        mSpCache = new SpCache(this);

        Do_txn_URI_Thread.start();//進入頁面先傳送 ethertxn 的 URI 給 BLE
        get_Balance_Thread.start();//向 Web3 Server 取得 Balance
        try {
            get_Balance_Thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        get_Nonce_Thread.start();//向 Web3 Server 取得 Nonce

        init();
    }

    /**接收掃描QRcode的值並將結果存到Receiver Address*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (!BluetoothDeviceManager.getInstance().isConnected(mDevice)) {
            System.out.println("Out of connect");
            new AlertDialog.Builder(DeviceControlActivity.this)
                    .setTitle("與藍芽設備連線中斷")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent next = new Intent(DeviceControlActivity.this,DeviceScanActivity.class);
                            startActivity(next);
                            finish();
                        }
                    }).show();
        }else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                String getContents = result.getContents();
                if (getContents == null) {
                    Toast.makeText(this, "You cancelled the scanning", Toast.LENGTH_SHORT).show();
                } else {
                    ReceiverAddress.setText("");
                    ReceiverAddress.setText(getContents);//將QRcode掃描結果放置address框框
                    Do_txn_RW_Thread.start();
                    try {
                        Do_txn_RW_Thread.join();
                        System.out.println("Finish RW Thread!!");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void init() {
        //TextView
        balance_title = findViewById(R.id.Balance);
        balance = findViewById(R.id.Balance_word);
        gasvalue = findViewById(R.id.Gas_Value);
        gasPricevalue = findViewById(R.id.GasPrice_Value);
        ReceiverAddress = findViewById(R.id.Receiver_addr);
        //EditText
        mValue = findViewById(R.id.Value);
        //SeekBar
        Gas_Seekbar = findViewById(R.id.Gas_seekBar);
        GasPrice_Seekbar = findViewById(R.id.GasPrice_seekBar);
        //Button
        Balance_update = findViewById(R.id.Balance_reset);
        qrbtn = findViewById(R.id.qrcodebtn);
        CheckBtn = findViewById(R.id.CheckBtn);
        SendBtn = findViewById(R.id.SendBtn);
        //Get from last Activity
        mDevice = getIntent().getParcelableExtra(middlePlace.EXTRA_DEVICE);
        /**按鍵更新 Balance值*/
        Balance_update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                get_Balance_Thread.start();
            }
        });
        final Activity Device_Control_transaction = this;

        Gas_Seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String x = String.valueOf(progress);
                gasvalue.setText(x);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });//拖曳條 控制 Gas

        GasPrice_Seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String t = String.valueOf(progress);
                gasPricevalue.setText(t);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });//拖曳條 控制 Gas Price

        /**掃描QRCode 按鍵*/
        qrbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentIntegrator integrator = new IntentIntegrator(Device_Control_transaction);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);  //QRcode內容設定
                integrator.setPrompt("Scan");
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(false);
                integrator.initiateScan();
            }
        });

        /**將資訊打包成TXN*/
        CheckBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBtn.setEnabled(false);
                mValue.setEnabled(false);
                Gas_Seekbar.setEnabled(false);
                GasPrice_Seekbar.setEnabled(false);
                int a, b;
                a = Integer.parseInt(gasvalue.getText().toString());
                b = Integer.parseInt(gasPricevalue.getText().toString());
                if (a == 0 || b == 0) {
                    AlertDialog.Builder Info = new AlertDialog.Builder(DeviceControlActivity.this);
                    Info.setTitle("Warning!");
                    Info.setMessage("Gas or GasPrice can not be 0 !!!");
                    Info.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Gas_Seekbar.setEnabled(true);
                            GasPrice_Seekbar.setEnabled(true);
                        }
                    });
                    AlertDialog TellInfo = Info.create();
                    TellInfo.show();
                } else{
                        /**將 Value Gas GasPrice 打包*/
                        VALUE_FOR_TXN = mValue.getText() + "," + Nonce + "," + gasPricevalue.getText() + "," + gasvalue.getText();  //value nonce gasprice gas
                        ViseLog.i("Value for Txn : " + VALUE_FOR_TXN);
                        System.out.println(VALUE_FOR_TXN);
                        Do_txn_DATA_Thread.start();
                        try {
                            Do_txn_DATA_Thread.join();
                            middlePlace.delay(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        AlertDialog.Builder builder = new AlertDialog.Builder(DeviceControlActivity.this);
                        builder.setTitle("Remind");
                        builder.setMessage("Is the info correct?");
                        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            middlePlace.delay(1000);
                            Do_txn_CTRL_Thread.start();
                            try {
                                Do_txn_CTRL_Thread.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            SendBtn.setEnabled(true);
                        }
                        });
                        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        CheckBtn.setEnabled(true);
                        mValue.setEnabled(true);
                        Gas_Seekbar.setEnabled(true);
                        GasPrice_Seekbar.setEnabled(true);
                        mValue.setText("");
                        }
                        });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                }
            }
        });

        /**傳送交易請求並進入到下一個頁面*/
        SendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Do_txn_READ_Thread.start();
                try {
                    Do_txn_READ_Thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(TXN_TO_Web3.equals("empty")){
                    Do_txn_READ_Thread.start();
                }

                post_transaction.start(); //傳送交易請求的執行緒
                try {
                    post_transaction.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (ResultOfTransaction.equals("Successfully")){
                    AlertDialog.Builder Success = new AlertDialog.Builder(DeviceControlActivity.this);
                    Success.setTitle("Transaction Succeed");
                    Success.setMessage("This transaction is Succeed\n Back to Home page");
                    Success.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BluetoothDeviceManager.getInstance().disconnect(mDevice);
                            next = new Intent(DeviceControlActivity.this,middlePlace.class);
                            next.putExtra(middlePlace.EXTRA_DEVICE,mDevice);
                            startActivity(next);
                        }
                    });
                    AlertDialog TellInfo = Success.create();
                    TellInfo.show();
                }else{
                    System.out.println("Failed Transaction");
                    AlertDialog.Builder Failed = new AlertDialog.Builder(DeviceControlActivity.this);
                    Failed.setTitle("Transaction Failed");
                    Failed.setMessage("Result : \n"+ResultOfTransaction);
                    Failed.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            BluetoothDeviceManager.getInstance().disconnect(mDevice);
                            next = new Intent(DeviceControlActivity.this,middlePlace.class);
                            next.putExtra(middlePlace.EXTRA_DEVICE,mDevice);
                            startActivity(next);
                        }
                    });
                    AlertDialog TellInfo = Failed.create();
                    TellInfo.show();
                }
            }
        });
        CheckBtn.setEnabled(true);
    }

    /**函式*/
    public void Check_EditText(){
        if (mValue.getText().equals(null) && gasvalue.getText().equals(null) && gasPricevalue.getText().equals(null)){
            System.out.println("No word in editbox");
            Check_EditText();
        }else{
            System.out.println("mValue : "+mValue.getText()+"\n mGas : "+gasvalue.getText()+"\n mGasPrice : "+gasPricevalue.getText());
            System.out.println("word are in Editbox");
            CheckBtn.setEnabled(true);
        }
    }//若內容填完則啟用按鍵
    public String Get_Nonce_from_Web3(){
        NonceJson = new JSONObject();
        try {
            NonceJson.put("id",priv_hash);
            NonceJson.put("token",Token);
            NonceJson.put("data",Address_encrypted);
            try {
                Nonce_change = new StringEntity(NonceJson.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Web3Control.Post_to_Web3(Nonce_Web3,Nonce_change);
        response_Web3 = Web3Control.get_response();
        System.out.println("Nonce : " + response_Web3);
        return response_Web3;
    }//取得Nonce 的函式
    public String Get_Balance_from_Web3(){
        BalanceJson = new JSONObject();
        try {
            BalanceJson.put("id",priv_hash);
            BalanceJson.put("token",Token);
            BalanceJson.put("data",Address_encrypted);
            try {
                Balance_change = new StringEntity(BalanceJson.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Web3Control.Post_to_Web3(Balance_Web3,Balance_change);
        response_Web3 = Web3Control.get_response();
        System.out.println("Balance : " + response_Web3);
        return response_Web3;

    }//取得Balance的函式
    public String post_transaction_to_Web3(){
        TransactionJson = new JSONObject();
        try {
            TransactionJson.put("id",priv_hash);
            TransactionJson.put("token",Token);
            TransactionJson.put("data",TXN_TO_Web3_encrypted);
            try {
                TXN_change = new StringEntity(TransactionJson.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Web3Control.Post_to_Web3(transaction_Web3,TXN_change);
        response_Web3 = Web3Control.get_response();
        System.out.println("Result : " + response_Web3);
        return response_Web3;
    }//將東西post 到Web3的函式

    /**宣告執行緒*/
    Thread encrypted_ADDRESS_Thread = new encrypted_address();

    Thread get_Nonce_Thread = new get_Nonce_Thread_From_Web3();
    Thread get_Balance_Thread = new get_Balance_From_Web3();

    Thread Do_txn_URI_Thread = new DOTXN_URI();
    Thread Do_txn_DATA_Thread = new DOTXN_DATA();
    Thread Do_txn_RW_Thread = new DOTXN_RW();
    Thread Do_txn_CTRL_Thread = new DOTXN_CTRL();
    Thread Do_txn_READ_Thread = new DOTXN_READ();

    Thread post_transaction = new POST_Transaction_To_Web3();

    /**執行緒內容*/
    class encrypted_address extends Thread{
        public void run(){
            try {
                Address_encrypted = security.Encrypt(Address,KEY,IV);
                System.out.println("Address_encrypted : "+Address_encrypted);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class get_Balance_From_Web3 extends Thread{
        public void run(){
            Balance_encrypted = Get_Balance_from_Web3();
            System.out.println("Get Balance encrypted : "+Balance_encrypted+" <END>");
            try {
                Balance = security.Decrypt(Balance_encrypted,KEY,IV);
                System.out.println("Get Balance decrypted : "+Balance+" <END>");
            } catch (Exception e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    balance.setText(Balance);
                }
            });
        }
    }//取得Balance的執行緒
    class get_Nonce_Thread_From_Web3 extends Thread{
        public void run(){
            Nonce_encrypted = Get_Nonce_from_Web3();
            System.out.println("Get Nonce_encrypted : "+ Nonce_encrypted+" <END>");
            try {
                Nonce = security.Decrypt(Nonce_encrypted,KEY,IV);
                System.out.println("Get Nonce_decrypted : "+ Nonce+" <END>");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }//取得Nonce 的執行緒

    class DOTXN_URI extends Thread{
        public void run(){
            middlePlace.BLE_URI("ethertxn",mSpCache);
            middlePlace.delay(500);
        }
    }
    class DOTXN_DATA extends Thread{
        public void run(){
            middlePlace.BLE_DATA(VALUE_FOR_TXN,mSpCache);
        }
    }
    class DOTXN_RW extends Thread{
        public void run(){
            String Data = LoginActivity.password_use+","+ReceiverAddress.getText().toString();
            middlePlace.BLE_RW(Data,mSpCache);
            System.out.println("Data Password & Address to BLE : "+Data);
        }
    }
    class DOTXN_CTRL extends Thread{
        public void run(){
            middlePlace.BLE_CONTROL("3",mSpCache);
        }
    }
    class DOTXN_READ extends Thread{
        public void run(){
            middlePlace.BLE_READ();
            middlePlace.delay(1000);
            TXN_TO_Web3 = DeviceMirror.Get_value();
            System.out.println("TXN Get from BLE : "+TXN_TO_Web3);
            try {
                TXN_TO_Web3_encrypted = security.Encrypt(TXN_TO_Web3,KEY,IV);
                System.out.println("TXN to Web3 encrypted : " +TXN_TO_Web3_encrypted+" END");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class POST_Transaction_To_Web3 extends Thread{
        public void run(){
            System.out.println("In Thread : Transaction");
            ResultOfTransaction = post_transaction_to_Web3();
        }
    }//將東西post到Web3的執行緒
}