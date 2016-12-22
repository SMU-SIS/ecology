package sg.edu.smu.ecology.connector.bluetooth;

/**
 * Created by anurooppv on 25/10/2016.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This thread runs while listening for incoming client connection requests. It runs until all the
 * 7 connections are accepted(or until cancelled).
 */
class BluetoothServerAcceptThread extends Thread {
    private static final String TAG = BluetoothServerAcceptThread.class.getSimpleName();
    // Name for the SDP record when creating server socket
    private static final String NAME = "EcologyBluetoothConnector";
    private BluetoothServerSocket serverSocket = null;
    private List<UUID> uuidsList;
    private List<UUID> disconnectedUuidsList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private Map<Integer, BluetoothSocketReadWriter> bluetoothSocketReadWritersList = new HashMap<>();
    private Handler handler;
    private int clientId = 0;
    private SparseArray<UUID> clientUuidList = new SparseArray<>();
    private boolean restartUuidsListening = false;

    BluetoothServerAcceptThread(BluetoothAdapter bluetoothAdapter, List<UUID> uuidsList,
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
    private void listenForConnectionRequests(List<UUID> uuidsList) {
        BluetoothSocket socket;
        Integer index = 0;
        try {
            // Listen for all the required number of UUIDs
            for (UUID uuid : uuidsList) {
                Log.i(TAG, "Server Listen " + index++);
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, uuid);
                socket = serverSocket.accept();
                if (socket != null) {
                    serverSocket.close();
                    createSocketReadWriterThreads(socket, uuid);
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
                handler, clientId);
        bluetoothSocketReadWriter.start();
        // Add each connected thread to an array
        bluetoothSocketReadWritersList.put(clientId, bluetoothSocketReadWriter);
        clientUuidList.put(clientId, uuid);
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
        for (BluetoothSocketReadWriter bluetoothSocketReadWriter :
                bluetoothSocketReadWritersList.values()) {
            if (bluetoothSocketReadWriter != null) {
                bluetoothSocketReadWriter.onInterrupt();
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
