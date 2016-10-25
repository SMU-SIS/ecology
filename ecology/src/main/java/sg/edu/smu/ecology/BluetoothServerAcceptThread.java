package sg.edu.smu.ecology;

/**
 * Created by anurooppv on 25/10/2016.
 */

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
 * This thread runs while listening for incoming connections. It runs until all the 7 connections
 * are accepted(or until cancelled).
 */
public class BluetoothServerAcceptThread extends Thread {
    private static final String TAG = BluetoothServerAcceptThread.class.getSimpleName();
    // Name for the SDP record when creating server socket
    private static final String NAME = "EcologyBluetoothConnector";
    private BluetoothServerSocket serverSocket = null;
    private ArrayList<UUID> uuidsList;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocketReadWriter bluetoothSocketReadWriter;
    private ArrayList<BluetoothSocket> socketsList = new ArrayList<BluetoothSocket>();
    private ArrayList<String> devicesAddressesList = new ArrayList<String>();
    private ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private ArrayList<BluetoothSocketReadWriter> bluetoothSocketReadWriters = new ArrayList<>();
    private Handler handler;
    private int clientId = 0;

    public BluetoothServerAcceptThread(BluetoothAdapter bluetoothAdapter, ArrayList<UUID> uuidsList,
                                       Handler handler) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.uuidsList = uuidsList;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.i(TAG, "run method ");
        BluetoothSocket socket = null;
        try {
            // Listen for all the required number of UUIDs
            for (int i = 0; i < uuidsList.size(); i++) {
                Log.i(TAG, "Server Listen " + (i + 1));
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,
                        uuidsList.get(i));
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
        clientId++;
        bluetoothSocketReadWriter = new BluetoothSocketReadWriter(socket, handler, true,
                clientId);
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
        Log.i(TAG, "bluetoothSocketReadWriters-size " + bluetoothSocketReadWriters.size());
        clientId = 0;
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
