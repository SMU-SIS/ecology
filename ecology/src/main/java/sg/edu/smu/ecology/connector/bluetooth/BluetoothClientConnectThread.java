package sg.edu.smu.ecology.connector.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

/**
 * This thread runs while attempting to make an outgoing connection with a server device. The
 * connection either succeeds or fails.
 *
 * @author Anuroop PATTENA VANIYAR
 */
class BluetoothClientConnectThread extends Thread {
    private static final String TAG = BluetoothClientConnectThread.class.getSimpleName();
    private final BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private BluetoothAdapter bluetoothAdapter;
    // UUID which is currently being tried to establish a connection
    private UUID uuidToTry;
    // The list of UUIDs to try
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
                                closeFileDescriptor(bluetoothSocket);
                                bluetoothSocket.close();
                            }
                        } catch (IOException e2) {
                            Log.e(TAG, "unable to close() socket during connection failure", e2);
                        }
                    } else {
                        closeFileDescriptor(bluetoothSocket);
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

    /**
     * Handle the server disconnection. Immediately the client device will start looking for new
     * connections
     */
    void handleServerDisconnection() {
        // To start looking for new connections
        connectedToServer = false;
        if (bluetoothSocketReadWriter != null) {
            bluetoothSocketReadWriter.closeDisconnectedSocket();
        }
    }

    /**
     * Create a thread to manage the newly established bluetooth connection
     *
     * @param socket the connected bluetooth socket
     */
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
                closeFileDescriptor(bluetoothSocket);
                bluetoothSocket.close();
                bluetoothSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Close the file descriptor
     *
     * @param socket the socket in use
     */
    private synchronized void closeFileDescriptor(BluetoothSocket socket) {
        try {
            Field field = BluetoothSocket.class.getDeclaredField("mPfd");
            field.setAccessible(true);
            ParcelFileDescriptor mPfd = (ParcelFileDescriptor) field.get(socket);
            if (mPfd == null) {
                return;
            }

            mPfd.close();
        } catch (Exception e) {
            Log.w(TAG, "LocalSocket could not be cleanly closed.");
        }
    }
}

