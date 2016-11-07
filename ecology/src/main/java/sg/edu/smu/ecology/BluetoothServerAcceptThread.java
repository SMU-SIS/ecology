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
import android.util.SparseArray;

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
    private ArrayList<UUID> disconnectedUuidsList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<String> devicesAddressesList = new ArrayList<String>();
    private ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
    private SparseArray<BluetoothSocketReadWriter> bluetoothSocketReadWritersList = new SparseArray<>();
    private Handler handler;
    private int clientId = 0;
    private SparseArray<UUID> clientUuidList = new SparseArray<>();
    private boolean restartUuidsListening = false;

    public BluetoothServerAcceptThread(BluetoothAdapter bluetoothAdapter, ArrayList<UUID> uuidsList,
                                       Handler handler) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.uuidsList = uuidsList;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.i(TAG, "run method ");
        listenForConnectionRequests(uuidsList);
    }

    /**
     * Listen for incoming connection requests and when one is accepted, provide a connected
     * BluetoothSocket
     */
    private void listenForConnectionRequests(ArrayList<UUID> uuidsList) {
        BluetoothSocket socket;
        try {
            // Listen for all the required number of UUIDs
            for (int i = 0; i < uuidsList.size(); i++) {
                Log.i(TAG, "Server Listen " + (i + 1));
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME,
                        uuidsList.get(i));
                socket = serverSocket.accept();
                if (socket != null) {
                    serverSocket.close();
                    devicesList.add(socket.getRemoteDevice());
                    devicesAddressesList.add(socket.getRemoteDevice().getAddress());
                    createSocketReadWriterThreads(socket, uuidsList.get(i));
                }
            }
            Log.i(TAG, "All eight UUIDs used ");
            restartUuidsListening = true;
        } catch (IOException e) {

        }
    }

    /**
     * Start the SocketReadWriter thread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     **/
    private void createSocketReadWriterThreads(BluetoothSocket socket, UUID uuid) {
        clientId++;
        BluetoothSocketReadWriter bluetoothSocketReadWriter = new BluetoothSocketReadWriter(socket,
                handler, true, clientId);
        bluetoothSocketReadWriter.start();
        // Add each connected thread to an array
        bluetoothSocketReadWritersList.put(clientId, bluetoothSocketReadWriter);
        clientUuidList.put(clientId, uuid);
    }

    public ArrayList<BluetoothDevice> getDevicesList() {
        return devicesList;
    }

    int getNumberOfDevicesConnected() {
        return devicesList.size();
    }

    void handleClientDisconnection(int clientId) {
        disconnectedUuidsList.add(clientUuidList.get(clientId));
        Log.i(TAG, "disconnectedUuidsList " + disconnectedUuidsList);
        updateSocketReadWritersList(clientId);
        if (restartUuidsListening) {
            uuidsList = disconnectedUuidsList;
            listenForConnectionRequests(uuidsList);
            restartUuidsListening = false;
            disconnectedUuidsList.clear();
        }
    }

    private void updateSocketReadWritersList(int clientId) {
        bluetoothSocketReadWritersList.get(clientId).closeDisconnectedSocket();
        bluetoothSocketReadWritersList.remove(clientId);
    }

    @Override
    public void interrupt() {
        super.interrupt();
        Log.i(TAG, "bluetoothSocketReadWriters-size " + bluetoothSocketReadWritersList.size());
        clientId = 0;
        for (int i = 0; i < bluetoothSocketReadWritersList.size(); i++) {
            if (bluetoothSocketReadWritersList.valueAt(i) != null) {
                bluetoothSocketReadWritersList.valueAt(i).onInterrupt();
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
