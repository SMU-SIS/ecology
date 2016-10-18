package sg.edu.smu.ecology;

/**
 * Created by anurooppv on 13/10/2016.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. The connection either succeeds or fails.
 */
public class BluetoothConnectThread extends Thread {
    private static final String TAG = BluetoothConnectThread.class.getSimpleName();
    private final BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private UUID uuidToTry;
    private ArrayList<UUID> mUuidsList;
    private BluetoothConnectedThread bluetoothConnectedThread;
    private Handler handler;
    private int numberOfAttempts = 0;
    // To record the status of the connection
    private boolean connectedToServer = false;

    public BluetoothConnectThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device,
                                  ArrayList<UUID> mUuidsList, Handler handler) {
        this.bluetoothAdapter = bluetoothAdapter;
        bluetoothDevice = device;
        this.mUuidsList = mUuidsList;
        this.handler = handler;
        uuidToTry = mUuidsList.get(numberOfAttempts);
    }

    public void setConnectedToServer(boolean connectedToServer) {
        this.connectedToServer = connectedToServer;
    }

    @Override
    public void run() {
        // Always cancel discovery because it will slow down a connection
        bluetoothAdapter.cancelDiscovery();
        // Try connecting till the connection is setup
        while (!isInterrupted()) {
            if (!connectedToServer) {
                // Make a connection to the BluetoothSocket
                try {
                    // If not connected to server, try to connect
                    Log.i(TAG, "connect attempt " + (numberOfAttempts + 1));
                    Log.i(TAG, "UUID " + uuidToTry);
                    // Get a BluetoothSocket for a connection with the given BluetoothDevice
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuidToTry);
                    } catch (IOException e) {
                        Log.e(TAG, "create failed", e);
                    }
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    bluetoothSocket.connect();
                    Log.i(TAG, "connected ");
                    // When connected, set this to true
                    connectedToServer = true;
                    // Start the connected thread
                    connected(bluetoothSocket);
                    numberOfAttempts = 0;
                    uuidToTry = mUuidsList.get(numberOfAttempts);
                } catch (IOException e) {
                    Log.i(TAG, "IO Exception attempt " + (numberOfAttempts + 1) + " failed");
                    if (uuidToTry.toString().contentEquals(mUuidsList.get(6).toString())) {
                        numberOfAttempts = 0;
                    } else {
                        numberOfAttempts++;
                    }
                    uuidToTry = mUuidsList.get(numberOfAttempts);
                }
            }
        }
        Log.i(TAG, "Done ");
    }

    private void connected(BluetoothSocket socket) {
        bluetoothConnectedThread = new BluetoothConnectedThread(socket, handler);
        bluetoothConnectedThread.start();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        Log.i(TAG, "Interrupted");
        if (bluetoothConnectedThread != null) {
            bluetoothConnectedThread.onInterrupt();
        }
        // Close the socket
        try {
            bluetoothSocket.close();
            Log.i(TAG, "All attempts done. Socket closed ");
        } catch (IOException e2) {
            Log.e(TAG, "unable to close() socket during connection failure", e2);
        }
    }
}
