package sg.edu.smu.ecology;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by anurooppv on 13/10/2016.
 */

/**
 * This thread runs during a connection with a remote bluetooth device.
 * It handles all the incoming and outgoing messages.
 */
public class BluetoothConnectedThread extends Thread{
    private static final String TAG = BluetoothConnectedThread.class.getSimpleName();

    private final BluetoothSocket bluetoothSocket;
    private Handler handler;
    private final DataInputStream inputStream;
    private final DataOutputStream outputStream;

    public BluetoothConnectedThread(BluetoothSocket bluetoothSocket, Handler handler) {
        this.bluetoothSocket = bluetoothSocket;
        this.handler = handler;
        // Use temporary objects that are later assigned to input and output streams as they
        // are final
        DataInputStream tempIn = null;
        DataOutputStream tempOut = null;

        // Get the BluetoothSocket input and output streams
        try {
            tempIn = new DataInputStream(bluetoothSocket.getInputStream());
            tempOut = new DataOutputStream(bluetoothSocket.getOutputStream());
        } catch (IOException e) {
            Log.e(TAG, "temp sockets not created", e);
        }

        inputStream = tempIn;
        outputStream = tempOut;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        int bytes;

        while(true) {
            try {
                // Read from the InputStream
                bytes = inputStream.read(buffer);
                handler.obtainMessage(Settings.MESSAGE_READ, bytes, -1, buffer).sendToTarget();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    /**
     * Write to the connected OutStream.
     * @param buffer  The bytes to write
     */
    public void writeData(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

    public void writeInt(int length) {
        try {
            outputStream.writeInt(length);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write int", e);
        }
    }

    // This method is called when the device is disconnected from ecology
    public void onInterrupt(){
        // To indicate that the device is disconnected from ecology
        if(bluetoothSocket.isConnected()) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //writeInt(END_OF_FILE);
        }
    }
}
