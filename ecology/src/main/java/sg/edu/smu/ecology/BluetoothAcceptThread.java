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
 * This thread runs while listening for incoming connections. It runs until a connection is
 * accepted(or until cancelled).
 */
public class BluetoothAcceptThread extends Thread {
    private static final String TAG = BluetoothAcceptThread.class.getSimpleName();
    // Name for the SDP record when creating server socket
    private static final String NAME = "EcologyBluetoothConnector";
    private static final int MAX_NUMBER_OF_BLUETOOTH_CONNECTIONS = 7;
    BluetoothServerSocket serverSocket = null;
    private ArrayList<UUID> mUuids;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnectedThread bluetoothConnectedThread;
    private ArrayList<BluetoothSocket> socketsList = new ArrayList<BluetoothSocket>();
    private ArrayList<String> devicesAddressesList = new ArrayList<String>();
    private ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private ArrayList<BluetoothConnectedThread> bluetoothConnectedThreads;
    private Handler handler;

    public BluetoothAcceptThread(BluetoothAdapter bluetoothAdapter, ArrayList<UUID> mUuids,
                                 Handler handler) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.mUuids = mUuids;
        this.handler = handler;
    }

    public void run() {
        Log.i(TAG, "run method ");
        BluetoothSocket socket = null;
        try {
            // Listen for all 7 UUIDs
            for (int i = 0; i < MAX_NUMBER_OF_BLUETOOTH_CONNECTIONS; i++) {
                Log.i(TAG, "Server Listen "+(i+1));
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, mUuids.get(i));
                socket = serverSocket.accept();
                if (socket != null) {
                    socketsList.add(socket);
                    devicesList.add(socket.getRemoteDevice());
                    devicesAddressesList.add(socket.getRemoteDevice().getAddress());
                    createConnectedThreads(socket);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "accept() failed", e);
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     **/
    private void createConnectedThreads(BluetoothSocket socket) {
        bluetoothConnectedThread = new BluetoothConnectedThread(socket, handler);
        bluetoothConnectedThread.start();
        // Add each connected thread to an array
        bluetoothConnectedThreads.add(bluetoothConnectedThread);
    }

    public ArrayList<BluetoothDevice> getDevicesList() {
        return devicesList;
    }

    public int getNumberOfDevicesConnected() {
        return devicesList.size();
    }

    @Override
    public void interrupt() {
        super.interrupt();

        for (int i = 0; i < bluetoothConnectedThreads.size(); i++) {
            if (bluetoothConnectedThreads.get(i) != null) {
                bluetoothConnectedThreads.get(i).onInterrupt();
            }
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "server socket close failed", e);
        }
    }
}
