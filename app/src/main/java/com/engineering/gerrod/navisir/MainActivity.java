package com.engineering.gerrod.navisir;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity {
    private static final String TAG = "MyActivity";
    public final String ACTION_USB_PERMISSION = "com.engineering.gerrod.navisir.USB_PERMISSION";
  //  Log.v(TAG,ACTION_USB_PERMISSION);
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    int[] distancesArray = new int[10];
    boolean arrayFull = false;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                tvReplace(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, intent.getAction());
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        startButton = (Button) findViewById(R.id.buttonStart);
        sendButton = (Button) findViewById(R.id.buttonSend);
        clearButton = (Button) findViewById(R.id.buttonClear);
        stopButton = (Button) findViewById(R.id.buttonStop);
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textView);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


        new UsbSerialInterface.UsbReadCallback() {
            @Override
            public void onReceivedData(byte[] bytes) {
                //do something with bytes
                String data = null;
                try {
                    data = new String(bytes, "UTF-8");
                    data.concat("/n");
                    tvReplace(textView, data);
                    UpdatingDistanceArray(data);
                    DeterminingChange(textView);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        };

    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public void onClickStart(View view) {

        for(int i = 0; i < 5; i++){
           distancesArray[i] = 0;
       }

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x403)//Arduino Vendor ID
                {
                    Log.d(TAG, "Board and Inputed ID are same");
                    tvAppend(textView, "Board and Inputed ID are same\n");
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    Log.d(TAG,"This is the actually id: " + Integer.toString(device.getVendorId()) );
                    tvAppend(textView, "This is the actually id: " + Integer.toString(device.getVendorId()));
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }


    }

    public void onClickSend(View view) {
        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");

    }

    public void onClickClear(View view) {
        textView.setText(" ");
    }

    private void UpdatingDistanceArray(String sDistance){
        int distance = Integer.parseInt(sDistance);
        if(!arrayFull){
        for (int i=0; i<distancesArray.length; i++) {
            if (distancesArray[i] == 0) {
                distancesArray[i] = distance;
                break;
            }else{
                arrayFull = true;
            }
         }
        }else{
            for (int i = 0; i < distancesArray.length - 1; i++) {
                distancesArray[i] = distancesArray[i+1];
            }
            distancesArray[4] = distance;
        }
    }

    private void DeterminingChange(TextView ftv){
        boolean hasIncreased = false;
        boolean hasDecreased = false;
        boolean noChange = true;
        if(arrayFull){

            for(int i = 0; i < distancesArray.length - 1; i++){
                if((distancesArray[i] - distancesArray[4]) >= 5){
                    hasDecreased = true;
                    noChange = false;
                    break;
                }else if((distancesArray[4] - distancesArray[i]) >= 5){
                    hasIncreased = true;
                    noChange = false;
                    break;
                }else if(((distancesArray[i] - distancesArray[4]) <= 5) || ((distancesArray[4] - distancesArray[i]) <= 5)){
                    noChange = true;
                }
            }
            if(noChange == false && hasDecreased == true){
                ftv.append("The distance has decreased");
            }else if(noChange == false && hasIncreased == true){
                ftv.append("The distance has increased");
            }

        }
    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    private void tvReplace(TextView tv, CharSequence text){
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                ftv.setText(" ");
                ftv.setText(ftext);
            }
        });
    }
   @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}

