package com.aravinth.cochat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class chatUtils {

    private static final String TAG = "ChatUtils.java";
    private Context context;
    private final Handler handler;
    ProgressDialog pd;

    private ConnectThread connectThread;
    private AcceptThread acceptThread;
    private ConnectedThread connectedThread;
    private BluetoothAdapter bluetoothAdapter;

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    private int state;

    public chatUtils(Context context, Handler handler)
    {
        this.context = context;
        this.handler = handler;
        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        pd = new ProgressDialog(context);
    }

    public synchronized int getState() {
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
            Log.d(TAG,"Connect thread is not null in start()");
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null)
        {
            Log.d(TAG,"Connected thread is not null in start()");
            connectedThread.cancel();
            connectedThread = null;
        }

        if(acceptThread == null)
        {
            Log.d(TAG,"Accept thread is null");
            acceptThread = new AcceptThread();
            Log.d(TAG,"Starting accept thread");
            acceptThread.start();
            Log.d(TAG,"Accept thread started");
        }


        setState(STATE_LISTEN);
        Log.d(TAG,"State set to listening......");
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
        if(connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }
        setState(STATE_NONE);
    }

    public  void write(byte[] out)
    {
        ConnectedThread connThread;
        synchronized(this)
        {
            if(state != STATE_CONNECTED)
            {
                return;
            }

            connThread = connectedThread;
        }

        connThread.write(out);
    }

    public void connect(BluetoothDevice device)
    {
        if(state == STATE_CONNECTING)
        {
            if(connectThread != null)
            {
                connectThread.cancel();
                connectThread = null;
            }

        }

        if(connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();
        setState(STATE_CONNECTING);
    }

    public class AcceptThread extends Thread {

        private final BluetoothServerSocket serverSocket;

        public AcceptThread()
        {
            Log.d(TAG,"Accept thread constructor initialized");
            BluetoothServerSocket tmp = null;
            try
            {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("COCHAT",MY_UUID);
                Log.d(TAG,"Temp is not null");
            }
            catch (IOException e)
            {
                Log.e(TAG,"Socket's listen method failed",e);
            }
            serverSocket = tmp;
            setState(STATE_LISTEN);
            Log.d(TAG,"server socket is assigned to tmp");
        }

        public void run()
        {
            Log.d(TAG,"In run method of accept thread");
            BluetoothSocket socket = null;
            Log.d(TAG,"socket is initialized with null");
            while(true) //doubt no 1
            {
                Log.d(TAG,"In while loop of accept thread");
                try{
                    Log.d(TAG,"In try block");
                    socket = serverSocket.accept();
                    //Toast.makeText(context,"Server socket connected",Toast.LENGTH_SHORT).show();
                    Log.d(TAG,"Server socket accepted");
                }
                catch (Exception e)
                {
                    Log.d(TAG,"Socket's accept method failed");
                    break;
//                    try
//                    {
//                        Log.d(TAG,"In try block of exception in socket accept method");
//                        socket.close();
//                        Log.d(TAG,"Socket is closed");
//                    }
//                    catch (IOException e1)
//                    {
//                        Log.e(TAG,"Socket's close method failed",e1);
//                    }
                }

                if(socket != null)
                {
                    Log.e(TAG,"Socket is not null in 179 and state is "+state);
                    synchronized (chatUtils.this) {
                        switch (state) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket , socket.getRemoteDevice());
                                break;

                            case STATE_NONE:
                            case STATE_CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Socket's close method failed", e);
                                }
                                break;
                        }
                    }

                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
                //bluetoothSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

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
            setState(STATE_CONNECTING);
        }

        public void run()
        {
            bluetoothAdapter.cancelDiscovery();


            try{
                socket.connect();
                //Toast.makeText(context,"Client socket connected",Toast.LENGTH_SHORT).show();
                Log.d(TAG,"At line 233 succesfully connected!");
            }
            catch(IOException connectException)
            {
                Log.d(TAG,"Unable to connect with socket at line 236");
                //Unable to connect; close the socket and return.
                try{
                    socket.close();
                    Log.d(TAG,"Socket closed at line 257");
                }
                catch(IOException closeException)
                {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                connectionFailed();
                return;
            }


            synchronized (chatUtils.this)
            {
                connectThread = null;
            }

            connected(socket , device);
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
        bundle.putString(HomeActivity.TOAST,"Unable to  connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);
        setState(STATE_NONE);
        chatUtils.this.start();
    }

    private synchronized void connected(BluetoothSocket socket , BluetoothDevice device)
    {
        if(connectThread != null)
        {
            connectThread.cancel();
            connectThread = null;
        }

        if(connectedThread != null)
        {
            connectedThread.cancel();
            connectedThread = null;
        }

        if(acceptThread != null)
        {
            acceptThread.cancel();
            acceptThread = null;
        }
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(HomeActivity.MESSAGE_DEVICENAME);
        Bundle bundle = new Bundle();
        bundle.putString(HomeActivity.DEVICE_NAME,device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e){

            }
            inputStream = tmpIn;
            outputStream = tmpOut;
            setState(STATE_CONNECTED);
        }

        public void run()
        {
            byte[] buffer = new byte[1024];
            int bytes;

            while(state == STATE_CONNECTED)
            {
                try{
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(HomeActivity.MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                }
                catch(IOException e)
                {
                    connectionLost();
                    break;
                }

            }

        }

        public void write(byte[] buffer)
        {
            try{
                outputStream.write(buffer);
                handler.obtainMessage(HomeActivity.MESSAGE_WRITE,-1,-1,buffer).sendToTarget();
            }
            catch(IOException e)
            {

            }
        }

        public void cancel()
        {
            try{
                socket.close();
            }
            catch (IOException e)
            {

            }
        }
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(HomeActivity.MESSAGE_TOAST);
        Bundle b = new Bundle();
        b.putString(HomeActivity.TOAST,"Connection lost");
        message.setData(b);
        handler.sendMessage(message);
        setState(STATE_NONE);

        chatUtils.this.start();
    }
}
