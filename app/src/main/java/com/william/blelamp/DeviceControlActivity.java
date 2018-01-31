package com.william.blelamp;

/**
 * Created by gionee on 18-1-31.
 */

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;




/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements ColorPicker.OnColorChangedListener {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    private ColorPicker picker;
    private SeekBar lightnessBar;

    private Button button;
    private TextView text;
    public int myColor;
    public String myHexColor;
    public String mySubHexColor_R,mySubHexColor_G,mySubHexColor_B,mySubHexColor;
    public int myIntColor_R,myIntColor_G,myIntColor_B;
    public String myFormatStrColor_R,myFormatStrColor_G,myFormatStrColor_B;
    public int lightness = 10;

    EditText edtSend;
    ScrollView svResult;
    Button btnSend;
    Button select;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            Log.e(TAG, "mBluetoothLeService is okay");
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {  //连接成功
                Log.e(TAG, "Only gatt, just wait");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //断开连接
                mConnected = false;
                invalidateOptionsMenu();
                btnSend.setEnabled(false);
                clearUI();
            }else if(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) //可以开始干活了
            {
                mConnected = true;
                mDataField.setText("");
                ShowDialog();
                btnSend.setEnabled(true);
                Log.e(TAG, "In what we need");
                invalidateOptionsMenu();
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { //收到数据
                Log.e(TAG, "RECV DATA");
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null) {
                    if (mDataField.length() > 500)
                        mDataField.setText("");
                    mDataField.append(data);
                    svResult.post(new Runnable() {
                        public void run() {
                            svResult.fullScroll(ScrollView.FOCUS_DOWN);
                        }
                    });
                }
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {                                        //初始化
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mDataField = (TextView) findViewById(R.id.data_value);
        edtSend = (EditText) this.findViewById(R.id.edtSend);
        edtSend.setText("数据格式：RGB111222333");
        svResult = (ScrollView) this.findViewById(R.id.svResult);

        btnSend = (Button) this.findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new ClickEvent());
        btnSend.setEnabled(false);

        picker = (ColorPicker) findViewById(R.id.picker);
        lightnessBar = (SeekBar) findViewById(R.id.lightnessBar);
        lightnessBar.setProgress(lightness);
        lightnessBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            //int progress = 1;
            //停止拖动的时候
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }
            //表示进度条该开始拖动，开始拖动时候触发的操作
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                // TODO Auto-generated method stub
                //this.progress = progress;
                lightness = progress;

            }
        });

        button = (Button) findViewById(R.id.button1);
        text = (TextView) findViewById(R.id.textView1);


        picker.setOnColorChangedListener(this);

        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mConnected) {
                    text.setTextColor(picker.getColor());
                    picker.setOldCenterColor(picker.getColor());
                    myColor = picker.getColor();
                    myHexColor = Integer.toHexString(myColor);
                    //text.setText(String.valueOf(myColor));
                    mySubHexColor = myHexColor.substring(2);
                    Log.e(this.toString(), myHexColor + "**" + mySubHexColor
                            + "*******************");
                    mySubHexColor_R = myHexColor.substring(2, 4);
                    myIntColor_R = Integer.parseInt(mySubHexColor_R, 16);
                    /***
                     * format  %Index$ 0 补“0” 3d 保留三位整数
                     */
                    myFormatStrColor_R = String.format("%1$03d",
                            Integer.parseInt(mySubHexColor_R, 16)*lightness/10);
                    mySubHexColor_G = myHexColor.substring(4, 6);
                    myIntColor_G = Integer.parseInt(mySubHexColor_G, 16);
                    myFormatStrColor_G = String.format("%1$03d", myIntColor_G*lightness/10);
                    mySubHexColor_B = myHexColor.substring(6, 8);
                    myIntColor_B = Integer.parseInt(mySubHexColor_B, 16);
                    myFormatStrColor_B = String.format("%1$03d", myIntColor_B*lightness/10);

                    //				int testInt = Integer.parseInt(mySubHexColor_R, 16);
                    //				Log.e(this.toString(), testInt+"*************************");
                    //text.setText("RGB"+Integer.numberOfLeadingZeros(myIntColor_R)+" "+myIntColor_G+" "+myIntColor_B);
                    text.setText("RGB" + myFormatStrColor_R
                            + myFormatStrColor_G + myFormatStrColor_B);
                    edtSend.setText("RGB" + myFormatStrColor_R
                            + myFormatStrColor_G + myFormatStrColor_B);
                    mBluetoothLeService.WriteValue("RGB000000000");
                    mBluetoothLeService.WriteValue("RGB" + myFormatStrColor_R
                            + myFormatStrColor_G + myFormatStrColor_B);
                    mBluetoothLeService.WriteValue("RGB" + myFormatStrColor_R
                            + myFormatStrColor_G + myFormatStrColor_B);
                }else{
                    Toast.makeText(DeviceControlActivity.this,"请先连接到灯泡！", Toast.LENGTH_SHORT).show();

                }



            }


        });



        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        Log.d(TAG, "Try to bindService=" + bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE));

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //this.unregisterReceiver(mGattUpdateReceiver);
        //unbindService(mServiceConnection);
        if(mBluetoothLeService != null)
        {
            mBluetoothLeService.close();
            mBluetoothLeService = null;
        }
        Log.d(TAG, "We are in destroy");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.gatt_services, menu);
//        if (mConnected) {
//            menu.findItem(R.id.menu_connect).setVisible(false);
//            menu.findItem(R.id.menu_disconnect).setVisible(true);
//        } else {
//            menu.findItem(R.id.menu_connect).setVisible(true);
//            menu.findItem(R.id.menu_disconnect).setVisible(false);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {                              //点击按钮
        switch(item.getItemId()) {
//            case R.id.menu_connect:
//                mBluetoothLeService.connect(mDeviceAddress);
//                return true;
//            case R.id.menu_disconnect:
//                mBluetoothLeService.disconnect();
//                return true;
            case android.R.id.home:
                if(mConnected)
                {
                    mBluetoothLeService.disconnect();
                    mConnected = false;
                }
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ShowDialog()
    {
        Toast.makeText(this, "连接成功，现在可以正常通信！", Toast.LENGTH_SHORT).show();
    }

    // 按钮事件
    class ClickEvent implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == btnSend) {
                if(!mConnected) return;

                if (edtSend.length() < 1) {
                    Toast.makeText(DeviceControlActivity.this, "请输入要发送的内容", Toast.LENGTH_SHORT).show();
                    return;
                }
                mBluetoothLeService.WriteValue("RGB000000000");
                mBluetoothLeService.WriteValue(edtSend.getText().toString());
                mBluetoothLeService.WriteValue(edtSend.getText().toString());

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm.isActive())
                    imm.hideSoftInputFromWindow(edtSend.getWindowToken(), 0);
                //todo Send data
            }
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {                        //注册接收的事件
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        return intentFilter;
    }

    @Override
    public void onColorChanged(int color) {
        //gives the color when it's changed.
        picker.getColor();
    }
}

