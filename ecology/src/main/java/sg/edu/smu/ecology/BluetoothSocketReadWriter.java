package sg.edu.smu.ecology;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by anurooppv on 13/10/2016.
 */

/**
 * This thread runs during a connection with a remote bluetooth device.
 * It handles all the incoming and outgoing messages.
 */
public class BluetoothSocketReadWriter extends Thread {
    private static final String TAG = BluetoothSocketReadWriter.class.getSimpleName();
    private static final int END_OF_FILE = -1;
    private BluetoothSocket bluetoothSocket;
    private Handler handler;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Boolean interrupted = false;

    public BluetoothSocketReadWriter(BluetoothSocket bluetoothSocket, Handler handler) {
        this.bluetoothSocket = bluetoothSocket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            // Get the BluetoothSocket input and output streams
            inputStream = new DataInputStream(bluetoothSocket.getInputStream());
            outputStream = new DataOutputStream(bluetoothSocket.getOutputStream());

            handler.obtainMessage(Settings.MY_HANDLE, this).sendToTarget();
            while (!interrupted) {
                try {
                    int toRead = inputStream.readInt();
                    int currentRead = 0;

                    // This indicates that the other device is disconnected from ecology
                    if (toRead == END_OF_FILE) {
                        break;
                    }

                    while (currentRead < toRead) {
                        byte[] dataBuffer = new byte[toRead];

                        currentRead += inputStream.read(dataBuffer, currentRead, toRead - currentRead);
                        Log.i(TAG, "buffer " + Arrays.toString(dataBuffer));

                        handler.obtainMessage(Settings.MESSAGE_READ, dataBuffer).sendToTarget();
                    }
                } catch (EOFException e) {
                    Log.i(TAG, "EOFException");
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "Exception during read", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSocketStreams();
            handler.obtainMessage(Settings.SOCKET_CLOSE, null).sendToTarget();
            try {
                if(bluetoothSocket.isConnected()) {
                    bluetoothSocket.close();
                    bluetoothSocket = null;
                    Log.i(TAG, "socket close");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
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
    public void onInterrupt() {
        // To indicate that the device is disconnected from ecology
        if (bluetoothSocket.isConnected()) {
            interrupted = true;
            Log.i(TAG, "interrupted");
            try {
                outputStream.writeInt(END_OF_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            closeSocketStreams();
            try {
                if(bluetoothSocket.isConnected()) {
                    bluetoothSocket.close();
                    bluetoothSocket = null;
                    Log.i(TAG, "socket close");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeSocketStreams(){
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {

            }
            inputStream = null;
        }

        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Exception e) {}
            outputStream = null;
        }
    }
}
