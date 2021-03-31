package com.aravinth.cochat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;


public class chatUtils {

    private static final String TAG = "ChatUtils.java";
    private Context context;
    private final Handler handler;

    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private BluetoothAdapter bluetoothAdapter;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final UUID MY_UUID = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00d");

    private int state;

    public chatUtils(Context context, Handler handler)
    {
        this.context = context;
        this.handler = handler;
        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(HomeActivity.MESSAGE_STATE_CHANGED,state,-1).sendToTarget();
    }

    public synchronized void start()
    {
        if(connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        if(acceptThread == null)
        {
            acceptThread = new AcceptThread();
            acceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    public synchronized void stop()
    {
        if(connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        if(acceptThread != null)
        {
            acceptThread.cancel();
            acceptThread = null;
        }
        setState(STATE_NONE);
    }

    public void connect(BluetoothDevice device)
    {
        if(state == STATE_CONNECTING)
        {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public class AcceptThread extends Thread {

        private final BluetoothServerSocket serverSocket;
        private BluetoothSocket bluetoothSocket;

        public AcceptThread()
        {
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("COCHAT",MY_UUID);
            }
            catch (IOException e)
            {
                Log.e(TAG,"Socket's listen method failed",e);
            }
            serverSocket = tmp;
        }

        public void run()
        {
            bluetoothSocket = null;
            while(true)
            {
                try{
                    bluetoothSocket = serverSocket.accept();
                }
                catch (IOException e)
                {
                    Log.e(TAG,"Socket's accept method failed",e);
                    try
                    {
                        bluetoothSocket.close();
                    }
                    catch (IOException e1)
                    {
                        Log.e(TAG,"Socket's close method failed",e1);
                    }
                }

                if(bluetoothSocket != null)
                {
                    switch (state)
                    {
                        case STATE_LISTEN:
                        case STATE_CONNECTING:
                            connect(bluetoothSocket.getRemoteDevice());
                            break;

                        case STATE_NONE:
                        case STATE_CONNECTED:
                            try{
                                bluetoothSocket.close();
                            }
                            catch(IOException e)
                            {
                                Log.e(TAG,"Socket's close method failed",e);
                            }
                            break;
                    }
                    //manageMyConnectedSocket(bluetoothSocket);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        //e.printStackTrace();
                        Log.e(TAG,"Socket's conection method failed",e);
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
                bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;
        private BluetoothAdapter bluetoothAdapter;

        public ConnectThread(BluetoothDevice device)
        {
            this.device = device;
            BluetoothSocket tmp = null;

            try{
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            }
            catch (IOException e)
            {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            socket = tmp;
        }

        public void run()
        {
            //bluetoothAdapter.cancelDiscovery();

            try{
                socket.connect();
            }
            catch(IOException connectException)
            {
                //Unable to connect; close the socket and return.
                try{
                    socket.close();
                }
                catch(IOException closeException)
                {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                connectionFailed();
                return;
            }
            //manageMyConnectedSocket(bluetoothSocket);

            synchronized (chatUtils.this)
            {
                connectThread = null;
            }

            connected(device);
        }

        public void cancel()
        {
            try{
                socket.close();
            }
            catch (IOException e)
            {
                Log.e("Closing socket failed",e.toString());
            }
        }
    }

    private synchronized void connectionFailed()
    {
        Message message = handler.obtainMessage(HomeActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(HomeActivity.TOAST,"Can't connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        chatUtils.this.start();
    }

    private synchronized void connected(BluetoothDevice device)
    {
        if(connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        Message message = handler.obtainMessage(HomeActivity.MESSAGE_DEVICENAME);
        Bundle bundle = new Bundle();
        bundle.putString(HomeActivity.DEVICE_NAME,device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
