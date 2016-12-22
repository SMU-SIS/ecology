package sg.edu.smu.ecology.connector.bluetooth;

/**
 * Created by anurooppv on 25/10/2016.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * This thread runs while attempting to make an outgoing connection with a server device. The
 * connection either succeeds or fails.
 */
class BluetoothClientConnectThread extends Thread {
    private static final String TAG = BluetoothClientConnectThread.class.getSimpleName();
    private final BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    private UUID uuidToTry;
    private List<UUID> uuidsList;
    private BluetoothSocketReadWriter bluetoothSocketReadWriter;
    private Handler handler;
    private int numberOfAttempts = 0;
    // To record the status of the connection
    private boolean connectedToServer = false;
    private boolean threadInterrupted = false;
    private BluetoothClientConnector.ClientConnectionListener clientConnectionListener;

    BluetoothClientConnectThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device,
                                 List<UUID> uuidsList, Handler handler,
                                 BluetoothClientConnector.ClientConnectionListener
                                         clientConnectionListener) {
        this.bluetoothAdapter = bluetoothAdapter;
        bluetoothDevice = device;
        this.uuidsList = uuidsList;
        this.handler = handler;
        this.clientConnectionListener = clientConnectionListener;
        uuidToTry = uuidsList.get(numberOfAttempts);
    }

    @Override
    public void run() {
        // Always cancel discovery because it will slow down a connection
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        // Try connecting till the connection is setup
        while (!threadInterrupted) {
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
                    if (bluetoothSocket != null) {
                        bluetoothSocket.connect();
                        Log.i(TAG, "connected ");
                    }

                    connectedToServer = true;
                    // Start the connected thread
                    createSocketReadWriter(bluetoothSocket);
                    // Notify bluetooth client connector
                    clientConnectionListener.clientConnectedToServer(this);
                    // Reset the values
                    numberOfAttempts = 0;
                    uuidToTry = uuidsList.get(numberOfAttempts);
                } catch (IOException e) {
                    if (connectedToServer) {
                        // Close the socket
                        try {
                            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                                bluetoothSocket.close();
                            }
                        } catch (IOException e2) {
                            Log.e(TAG, "unable to close() socket during connection failure", e2);
                        }
                    } else {
                        Log.i(TAG, "Connection attempt " + (numberOfAttempts + 1) + " failed");
                        if (uuidToTry.toString().contentEquals(uuidsList.get(uuidsList.size() - 1).
                                toString())) {
                            numberOfAttempts = 0;
                        } else {
                            numberOfAttempts++;
                        }
                        uuidToTry = uuidsList.get(numberOfAttempts);
                    }
                }
            }
        }
        Log.i(TAG, "Done ");
    }

    void handleServerDisconnection() {
        connectedToServer = false;
        if (bluetoothSocketReadWriter != null) {
            bluetoothSocketReadWriter.closeDisconnectedSocket();
        }
    }

    private void createSocketReadWriter(BluetoothSocket socket) {
        bluetoothSocketReadWriter = new BluetoothSocketReadWriter(socket, handler);
        bluetoothSocketReadWriter.start();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        threadInterrupted = true;
        if (connectedToServer) {
            if (bluetoothSocketReadWriter != null) {
                Log.i(TAG, "Interrupted");
                bluetoothSocketReadWriter.onInterrupt();
            }
        } else {
            try {
                bluetoothSocket.close();
                bluetoothSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
