package sg.edu.smu.ecology;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by anurooppv on 25/10/2016.
 */

public class BluetoothClientConnector extends BluetoothConnector implements
        BluetoothConnector.ServerDisconnectionListener {
    private static final String TAG = BluetoothClientConnector.class.getSimpleName();

    private BluetoothClientConnectThread bluetoothClientConnectThread;
    private ArrayList<BluetoothClientConnectThread> clientConnectThreadsList = new ArrayList<>();
    private ClientConnectionListener clientConnectionListener = new ClientConnectionListener() {
        @Override
        public void clientConnectedToServer(BluetoothClientConnectThread
                                                    bluetoothClientConnectThread) {
            Log.i(TAG, "Client connected to server");
            // Set the client connect thread connected to the server
            setBluetoothClientConnectThread(bluetoothClientConnectThread);
            // Interrupt other client connect threads that are no longer required
            if (clientConnectThreadsList.size() > 0) {
                interruptOtherClientConnectThreads();
            }
        }
    };

    /**
     * This sets up connection requests to all the paired devices
     */
    @Override
    public void setupBluetoothConnection() {
        // Among the paired devices, any device can be the server. So try connecting to each and
        // every paired device
        for (int i = 0; i < getPairedDevicesList().size(); i++) {
            BluetoothClientConnectThread bluetoothClientConnectThread = new
                    BluetoothClientConnectThread(getBluetoothAdapter(),
                    getPairedDevicesList().get(i), getUuidsList(), getHandler(),
                    clientConnectionListener);
            bluetoothClientConnectThread.start();
            // Save the connect threads list
            clientConnectThreadsList.add(bluetoothClientConnectThread);
        }
        Log.i(TAG, "clientConnectThreadsList size " + clientConnectThreadsList.size());
        setServerDisconnectionListener(this);
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if (bluetoothClientConnectThread != null && !bluetoothClientConnectThread.isInterrupted()) {
            bluetoothClientConnectThread.interrupt();
        }

        if (clientConnectThreadsList.size() > 0) {
            for (int i = 0; i < clientConnectThreadsList.size(); i++) {
                if (!clientConnectThreadsList.get(i).isInterrupted()) {
                    clientConnectThreadsList.get(i).interrupt();
                }
            }
        }
    }

    @Override
    public void handleServerDisconnection() {
        bluetoothClientConnectThread.handleServerDisconnection();
    }


    private void setBluetoothClientConnectThread(BluetoothClientConnectThread
                                                         bluetoothClientConnectThread) {
        this.bluetoothClientConnectThread = bluetoothClientConnectThread;
    }

    /**
     * Interrupt other client connect threads which tried to set up a connection with a server
     * device
     */
    private void interruptOtherClientConnectThreads() {
        Log.i(TAG, "interruptOtherClientConnectThreads");
        for (int i = 0; i < clientConnectThreadsList.size(); i++) {
            if (!clientConnectThreadsList.get(i).equals(bluetoothClientConnectThread)) {
                if (!clientConnectThreadsList.get(i).isInterrupted()) {
                    clientConnectThreadsList.get(i).interrupt();
                }
            }
        }
        clientConnectThreadsList.clear();
    }

    /**
     * Interface to listen to client connections to a server
     */
    interface ClientConnectionListener {

        /**
         * This method is called when a client gets connected to the server
         *
         * @param bluetoothClientConnectThread the client connect thread that got connected
         */
        public void clientConnectedToServer(BluetoothClientConnectThread
                                                    bluetoothClientConnectThread);
    }
}
