package com.example.aman.bttest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

public class MainActivity extends Activity  {
    Button b1,b2,b3,b4;
    private BluetoothAdapter BA;
    private Set<BluetoothDevice>pairedDevices;
    ArrayList<BluetoothDevice> devices;
    private ArrayAdapter mArrayAdapter;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    InputStream mmInStream;
    OutputStream mmOutStream;
    String TAG = "com.example.aman.bttest";
    ListView lv;
    String currentItem=null;
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.i(TAG, "in handler");
            super.handleMessage(msg);
            switch(msg.what){
                case SUCCESS_CONNECT:
                    ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    Log.i(TAG,"before cted.start()");
                    String s = "successfully_connected#";
                    connectedThread.write(s.getBytes());
                    // connectedThread.start();
                    Toast.makeText(getApplicationContext(), "CONNECT", Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[])msg.obj;
                    Log.i(TAG,"R data 3");
                    String string = new String(readBuf);
                    Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        b1=(Button)findViewById(R.id.button);
        b2=(Button)findViewById(R.id.button2);
        b3=(Button)findViewById(R.id.button3);
        b4=(Button)findViewById(R.id.button4);

        BA = BluetoothAdapter.getDefaultAdapter();
        lv = (ListView)findViewById(R.id.listView);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //String selected = ((TextView) view.findViewById(R.id.listView)).getText().toString();
                currentItem = String.valueOf(parent.getItemAtPosition(position));

                int a = currentItem.length();
                String s = currentItem.substring(a-17,a);
                BluetoothDevice selectedDevice = devices.get(position);
                //BluetoothDevice device = BA.getRemoteDevice(s);
                ConnectThread t = new ConnectThread(selectedDevice);
                t.start();
                Log.i(TAG,s);
            }
        });
    }

    private void init() {
        devices = new ArrayList<BluetoothDevice>();
    }

    public void on(View v){
        if (!BA.isEnabled()) {
            Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOn, 0);
            Toast.makeText(getApplicationContext(), "Turned on",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Already on", Toast.LENGTH_LONG).show();
        }
    }

    public void off(View v){
        BA.disable();
        Toast.makeText(getApplicationContext(), "Turned off" ,Toast.LENGTH_LONG).show();
    }


    public  void visible(View v){
        Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        startActivityForResult(getVisible, 0);
    }

    public void search(View v)
    {
        final BroadcastReceiver rec;
        final ArrayList list = new ArrayList();
        BA.startDiscovery();
        Log.i(TAG,"search start");
        rec = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                Log.i(TAG,"123");
                String action = intent.getAction();
                //Finding devices
                if (BluetoothDevice.ACTION_FOUND.equals(action))
                {
                    // Get the BluetoothDevice object from the Intent
                    Log.i(TAG,"searching");
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    // Add the name and address to an array adapter to show in a ListView
                    list.add(device.getName() + "\n" + device.getAddress());
                    final ArrayAdapter adapter = new  ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1, list);
                    lv.setAdapter(adapter);
                }
            }

        };

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(rec, filter);
    }


    public void list(View v){
        pairedDevices = BA.getBondedDevices();
        ArrayList list = new ArrayList();

        for(BluetoothDevice bt : pairedDevices) list.add(bt.getName());
        Toast.makeText(getApplicationContext(), "Showing Paired Devices",Toast.LENGTH_SHORT).show();

        final ArrayAdapter adapter = new  ArrayAdapter(this,android.R.layout.simple_list_item_1, list);

        lv.setAdapter(adapter);
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            Log.i(TAG,"1.1");
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                //ParcelUuid list[] = mmDevice.getUuids();
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);

            } catch (IOException e) {
                Log.i(TAG,"0.1");
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            BA.cancelDiscovery();
            Log.i(TAG,"1.2");
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
                Log.i(TAG,"connection success");
            } catch (IOException connectException) {
                Log.i(TAG,"didn't connect ,try again " + connectException);
                try {

                    try {
                        mmSocket = (BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(mmDevice, 1);
                    }catch (Exception e)
                    {
                        Log.i(TAG,"error is : "+ e);
                    }
                    mmSocket.connect();
                    Log.e(TAG,"Connected");
                    //  mmSocket.close();
                } catch (IOException closeException) {
                    Log.i(TAG,"again connection issue");
                }
                //return;  <---- see its requirements
            }
            // Do work to manage the connection (in a separate thread)
            //manageConnectedSocket(mmSocket);
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        //private final InputStream mmInStream;
        //private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            Log.i(TAG,"CT 1");
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                Log.i(TAG,"CT 2");
            } catch (IOException e) {
                Log.i(TAG,"inside connected thread exception");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Log.i(TAG,"CT 3");
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int begin = 0;
            int bytes = 0;
            while (true) {
                try {
                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for(int i = begin; i < bytes; i++) {
                        if(buffer[i] == "#".getBytes()[0]) {
                            mHandler.obtainMessage(MESSAGE_READ, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if(i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }
                } catch (IOException e) {
                    break;
                }
            }
          /*  byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()
            Log.i(TAG,"CT run 2");
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    buffer = new byte[1024];
                    bytes = mmInStream.read(buffer);
                    Log.i(TAG,"CT run 3");
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
            */
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.i(TAG,"write 1");
            } catch (IOException e) {
                Log.i(TAG,"write exception");
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    public void recieveData(View v) {
        byte[] buffer = new byte[1024];
        int begin = 0;
        int bytes = 0;
        while (true) {
            try {
                bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                for(int i = begin; i < bytes; i++) {
                    if(buffer[i] == "#".getBytes()[0]) {
                        mHandler.obtainMessage(MESSAGE_READ, begin, i, buffer).sendToTarget();
                        begin = i + 1;
                        if(i == bytes - 1) {
                            bytes = 0;
                            begin = 0;
                        }
                    }
                }
            } catch (IOException e) {
                Log.i(TAG,"unable to recieve. Check the connection");
                break;
            }
        }
      /*  byte[] buffer;  // buffer store for the stream
        int bytes;
        while (true) {
            try {
                // Read from the InputStream
                Log.i(TAG,"R data");
                buffer = new byte[1024];
                bytes = mmInStream.read(buffer);
                Log.i(TAG,"R data 1");
                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                        .sendToTarget();
                Log.i(TAG,"R data 2");
            } catch (IOException e) {
                    Log.i(TAG,"" + e);
                break;
            }
        }
        */
    }

}

/*  Log.i(TAG,"trying fallback...");
                    Class<?> clazz = mmSocket.getRemoteDevice().getClass();
                    Class<?>[] paramTypes = new Class<?>[] {Integer.TYPE};
                    Method m = clazz.getMethod("createRfcommSocket", paramTypes);
                    Object[] params = new Object[] {Integer.valueOf(1)};
                    mmSocket  = (BluetoothSocket) m.invoke(tmp.getRemoteDevice(), params);
                    Thread.sleep(500);
                    */
// mmSocket = new FallbackBluetoothSocket(mmSocket.getUnderlyingSocket());
//  Thread.sleep(500);