package sg.edu.smu.ecology;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by anurooppv on 13/10/2016.
 */

/**
 * This thread runs while listening for incoming connections. It runs until all the 7 connections
 * are accepted(or until cancelled).
 */
public class BluetoothServerAcceptThread extends Thread {
    private static final String TAG = BluetoothServerAcceptThread.class.getSimpleName();
    // Name for the SDP record when creating server socket
    private static final String NAME = "EcologyBluetoothConnector";
    private static final int MAX_NUMBER_OF_BLUETOOTH_CONNECTIONS = 7;
    private BluetoothServerSocket serverSocket = null;
    private ArrayList<UUID> mUuids;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocketReadWriter bluetoothSocketReadWriter;
    private ArrayList<BluetoothSocket> socketsList = new ArrayList<BluetoothSocket>();
    private ArrayList<String> devicesAddressesList = new ArrayList<String>();
    private ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private ArrayList<BluetoothSocketReadWriter> bluetoothSocketReadWriters = new ArrayList<>();
    private Handler handler;

    public BluetoothServerAcceptThread(BluetoothAdapter bluetoothAdapter, ArrayList<UUID> mUuids,
                                       Handler handler) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.mUuids = mUuids;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.i(TAG, "run method ");
        BluetoothSocket socket = null;
        try {
            // Listen for all 7 UUIDs
            for (int i = 0; i < MAX_NUMBER_OF_BLUETOOTH_CONNECTIONS; i++) {
                Log.i(TAG, "Server Listen " + (i + 1));
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, mUuids.get(i));
                socket = serverSocket.accept();
                if (socket != null) {
                    serverSocket.close();
                    socketsList.add(socket);
                    devicesList.add(socket.getRemoteDevice());
                    devicesAddressesList.add(socket.getRemoteDevice().getAddress());
                    createSocketReadWriterThreads(socket);
                }
            }
        } catch (IOException e) {

        }
    }

    /**
     * Start the SocketReadWriter thread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     **/
    private void createSocketReadWriterThreads(BluetoothSocket socket) {
        bluetoothSocketReadWriter = new BluetoothSocketReadWriter(socket, handler);
        bluetoothSocketReadWriter.start();
        // Add each connected thread to an array
        bluetoothSocketReadWriters.add(bluetoothSocketReadWriter);
    }

    public ArrayList<BluetoothDevice> getDevicesList() {
        return devicesList;
    }

    int getNumberOfDevicesConnected() {
        return devicesList.size();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        Log.i(TAG, "bluetoothSocketReadWriters-size "+bluetoothSocketReadWriters.size());
        for (int i = 0; i < bluetoothSocketReadWriters.size(); i++) {
            if (bluetoothSocketReadWriters.get(i) != null) {
                bluetoothSocketReadWriters.get(i).onInterrupt();
            }
        }

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "server socket close failed", e);
        }
    }
}
