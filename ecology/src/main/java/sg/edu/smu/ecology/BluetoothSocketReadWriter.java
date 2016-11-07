package sg.edu.smu.ecology;

/**
 * Created by anurooppv on 25/10/2016.
 */

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

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
    private Boolean isServer = false;
    private int clientId;

    protected BluetoothSocketReadWriter(BluetoothSocket bluetoothSocket, Handler handler) {
        this.bluetoothSocket = bluetoothSocket;
        this.handler = handler;
    }

    protected BluetoothSocketReadWriter(BluetoothSocket bluetoothSocket, Handler handler,
                                        Boolean isServer, int clientId) {
        this.bluetoothSocket = bluetoothSocket;
        this.handler = handler;
        this.isServer = isServer;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try {
            // Get the BluetoothSocket input and output streams
            inputStream = new DataInputStream(bluetoothSocket.getInputStream());
            outputStream = new DataOutputStream(bluetoothSocket.getOutputStream());

            if (isServer) {
                handler.obtainMessage(Settings.SOCKET_SERVER, clientId, 0, this).sendToTarget();
            } else {
                handler.obtainMessage(Settings.SOCKET_CLIENT, this).sendToTarget();
            }
            while (true) {
                try {
                    int toRead = inputStream.readInt();
                    int currentRead = 0;

                    // This indicates that the other device is disconnected from ecology
                    if (toRead == END_OF_FILE) {
                        if (isServer) {
                            handler.obtainMessage(Settings.SOCKET_CLOSE, clientId, 0,
                                    this).sendToTarget();
                        } else {
                            handler.obtainMessage(Settings.SOCKET_CLOSE, this).sendToTarget();
                        }
                        break;
                    }

                    byte[] dataBuffer = new byte[toRead];
                    while (currentRead < toRead) {
                        currentRead += inputStream.read(dataBuffer, currentRead, toRead -
                                currentRead);
                    }
                    Log.i(TAG, "buffer " + Arrays.toString(dataBuffer));
                    if (isServer) {
                        handler.obtainMessage(Settings.MESSAGE_READ, clientId, 0,
                                dataBuffer).sendToTarget();
                    } else {
                        handler.obtainMessage(Settings.MESSAGE_READ, dataBuffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        } catch (IOException e) {
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

    /**
     * This method is called when the device disconnects from the ecology
     */
    public void onInterrupt() {
        // To indicate that the device is disconnected from ecology
        if (bluetoothSocket.isConnected()) {
            Log.i(TAG, "interrupted");
            try {
                outputStream.writeInt(END_OF_FILE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (bluetoothSocket.isConnected()) {
                    bluetoothSocket.close();
                    bluetoothSocket = null;
                    Log.i(TAG, "socket close");
                }
            } catch (IOException e) {
            }
        }
        interrupt();
    }

    /**
     * This method is called when a client disconnects from the server
     */
    void closeDisconnectedSocket() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
                bluetoothSocket = null;
                Log.i(TAG, "socket close");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        interrupt();
    }
}
