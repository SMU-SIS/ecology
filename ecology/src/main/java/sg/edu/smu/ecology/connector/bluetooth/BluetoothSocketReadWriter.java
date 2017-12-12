/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package sg.edu.smu.ecology.connector.bluetooth;

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
 *
 * @author Anuroop PATTENA VANIYAR
 */
class BluetoothSocketReadWriter extends Thread {
    private static final String TAG = BluetoothSocketReadWriter.class.getSimpleName();
    private static final int END_OF_FILE = -1;
    private BluetoothSocket bluetoothSocket;
    private Handler handler;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private int clientId = 0;

    /**
     * Constructor used in {@link BluetoothClientConnectThread} by a {@link BluetoothClientConnector}
     * instance
     *
     * @param bluetoothSocket the connected bluetooth socket
     * @param handler         to handle the messages
     */
    BluetoothSocketReadWriter(BluetoothSocket bluetoothSocket, Handler handler) {
        this.bluetoothSocket = bluetoothSocket;
        this.handler = handler;
    }

    /**
     * Constructor used in {@link BluetoothServerAcceptThread} by a {@link BluetoothServerConnector}
     * instance
     *
     * @param bluetoothSocket the connected bluetooth socket
     * @param handler         to handle the messages
     * @param clientId        the id of the connected client
     */
    BluetoothSocketReadWriter(BluetoothSocket bluetoothSocket, Handler handler, int clientId) {
        this.bluetoothSocket = bluetoothSocket;
        this.handler = handler;
        this.clientId = clientId;
    }

    @Override
    public void run() {
        try {
            // Get the BluetoothSocket input and output streams
            inputStream = new DataInputStream(bluetoothSocket.getInputStream());
            outputStream = new DataOutputStream(bluetoothSocket.getOutputStream());

            handler.obtainMessage(BluetoothConnector.SOCKET_CONNECTED, clientId, 0, this).
                    sendToTarget();

            while (true) {
                try {
                    // Get the length of the data
                    int toRead = inputStream.readInt();
                    int currentRead = 0;

                    // This indicates that the other device is disconnected from ecology
                    if (toRead == END_OF_FILE) {
                        handler.obtainMessage(BluetoothConnector.SOCKET_CLOSE, clientId, 0,
                                this).sendToTarget();
                        break;
                    }

                    byte[] dataBuffer = new byte[toRead];
                    while (currentRead < toRead) {
                        currentRead += inputStream.read(dataBuffer, currentRead, toRead -
                                currentRead);
                    }
                    Log.i(TAG, "buffer " + Arrays.toString(dataBuffer));
                    handler.obtainMessage(BluetoothConnector.MESSAGE_RECEIVED, clientId, 0,
                            dataBuffer).sendToTarget();
                } catch (IOException e) {
                    // This signals that there is a disconnection
                    handler.obtainMessage(BluetoothConnector.SOCKET_CLOSE, clientId, 0,
                            this).sendToTarget();
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
    void writeData(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }

    /**
     * Write the length of bytes to be written
     *
     * @param length the length of the bytes
     */
    void writeInt(int length) {
        try {
            outputStream.writeInt(length);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write int", e);
        }
    }

    /**
     * This method is called when the device disconnects from the ecology
     */
    void onInterrupt() {
        // To indicate that the device is disconnected from ecology
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
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
     * This method is called when a connected device gets disconnected
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
