package com.vise.bledemo.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.vise.baseble.core.DeviceMirror;
import com.vise.baseble.model.BluetoothLeDevice;
import com.vise.bledemo.R;
import com.vise.bledemo.common.BluetoothDeviceManager;
import com.vise.xsnow.cache.SpCache;

public class LoginActivity extends AppCompatActivity {
    public static BluetoothLeDevice mDevice;
    public static SpCache mSpCache;
    public static String password_use;

    private EditText password;
    private Button Sign_up_BTN;
    private Button Login_BTN;
    Intent next;

    String callback = "empty";

    @Override
    protected void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        password = findViewById(R.id.PSW);
        Login_BTN = findViewById(R.id.login);

        mDevice = getIntent().getParcelableExtra("extra_device");
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        BluetoothDeviceManager.getInstance().connect(mDevice);
        //連線設備
        while (!BluetoothDeviceManager.getInstance().isConnected(mDevice)){
            BluetoothDeviceManager.getInstance().connect(mDevice);
            middlePlace.delay(500);
        }
        Toast.makeText(this, "Connect", Toast.LENGTH_SHORT).show();

        mSpCache = new SpCache(this);

        main();
    }

    public void main(){
        Login_URI_Thread.start();
        //登入樹梅派
        Login_BTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PSW_DATA_login_Thread.start();
                try {
                    PSW_DATA_login_Thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                final AlertDialog.Builder Check = new AlertDialog.Builder(LoginActivity.this);
                Check.setTitle("Check the password!");
                Check.setMessage("Is the password correct : \n"+password.getText().toString());
                Check.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        postDATA.start();
                        try {
                            postDATA.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        middlePlace.delay(10000);

                        TellInfo_Thread.start();
                        /*final AlertDialog.Builder Read = new AlertDialog.Builder(LoginActivity.this);
                        Read.setTitle("Tips");
                        Read.setMessage("Please wait a few seconds");
                        Read.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                readBODY.start();
                                try {
                                    readBODY.join();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                while(callback.equals("empty")){
                                    readBODY.start();
                                }
                                System.out.println("BLE call back : "+callback);

                                if(callback.equals("Login success")){
                                    //登入成功
                                    next = new Intent(LoginActivity.this,middlePlace.class);
                                    next.putExtra(middlePlace.EXTRA_DEVICE,mDevice);
                                    startActivity(next);
                                }else if(callback.equals("Password error")){
                                    Login_URI_Thread.start();

                                }else if(callback.equals("Address not exists")){
                                    //尚未創建帳戶
                                }
                            }
                        });
                        AlertDialog TellInfo1 = Read.create();
                        TellInfo1.setCancelable(false);
                        TellInfo1.show();*/
                    }
                });
                Check.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        password.setText("");
                        BluetoothDeviceManager.getInstance().disconnect(mDevice);
                        middlePlace.delay(1000);
                        BluetoothDeviceManager.getInstance().connect(mDevice);
                        Check.setCancelable(true);
                    }
                });
                AlertDialog TellInfo = Check.create();
                TellInfo.setCancelable(false);
                TellInfo.show();

                middlePlace.delay(500);
            }
        });
    }

    /**執行緒*/
    Thread Login_URI_Thread = new Login_URI();
    Thread PSW_DATA_login_Thread = new PSW_DATA_login();
    Thread postDATA = new Ctrl_point();
    Thread readBODY = new READ_BODY();
    Thread TellInfo_Thread = new testThread();

    class PSW_DATA_login extends Thread{
        public void run(){
            password_use = password.getText().toString();
            middlePlace.BLE_DATA(password_use,mSpCache);
        }
    }
    class Login_URI extends Thread{
        public void run(){
            middlePlace.BLE_URI("login",mSpCache);
        }
    }
    class Ctrl_point extends Thread{
        public void run(){
            middlePlace.BLE_CONTROL("3",mSpCache);
        }
    }
    class READ_BODY extends Thread{
        public void run(){
            middlePlace.BLE_READ();
            middlePlace.delay(1000);
            callback = DeviceMirror.Get_value();
        }
    }

    class testThread extends Thread{
        public void run(){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final AlertDialog.Builder Read = new AlertDialog.Builder(LoginActivity.this);
                    Read.setTitle("Tips");
                    Read.setMessage("Please wait a few seconds");
                    Read.setPositiveButton("Next", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            readBODY.start();
                            try {
                                readBODY.join();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            while(callback.equals("empty")){
                                readBODY.start();
                            }
                            System.out.println("BLE call back : "+callback);

                            if(callback.equals("Login success")){
                                //登入成功
                                next = new Intent(LoginActivity.this,middlePlace.class);
                                next.putExtra(middlePlace.EXTRA_DEVICE,mDevice);
                                startActivity(next);
                            }else if(callback.equals("Password error")){
                                Login_URI_Thread.start();

                            }else if(callback.equals("Address not exists")){
                                //尚未創建帳戶
                            }
                        }
                    });
                    AlertDialog TellInfo1 = Read.create();
                    TellInfo1.setCancelable(false);
                    TellInfo1.show();
                }
            });
        }
    }
}
