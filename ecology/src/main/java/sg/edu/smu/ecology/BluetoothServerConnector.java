package sg.edu.smu.ecology;

import android.util.Log;

/**
 * Created by anurooppv on 25/10/2016.
 */

/**
 * This class helps the device to act as a server to establish bluetooth connections with other
 * client devices in the ecology
 */
public class BluetoothServerConnector extends BluetoothConnector implements
        BluetoothConnector.ClientDisconnectionListener {
    private static final String TAG = BluetoothServerConnector.class.getSimpleName();

    private BluetoothServerAcceptThread bluetoothServerAcceptThread;

    @Override
    public void setupBluetoothConnection() {
        // Start the thread to listen on a BluetoothServerSocket
        if (bluetoothServerAcceptThread == null) {
            try {
                bluetoothServerAcceptThread = new BluetoothServerAcceptThread(getBluetoothAdapter(),
                        getUuidsList(), getHandler());
                bluetoothServerAcceptThread.start();
                setServer(true);
                setClientDisconnectionListener(this);
            } catch (Exception e) {
                Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
            }
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if (bluetoothServerAcceptThread != null && !bluetoothServerAcceptThread.isInterrupted()) {
            bluetoothServerAcceptThread.interrupt();
        }
    }

    @Override
    public void handleClientDisconnection(int clientId) {
        bluetoothServerAcceptThread.handleClientDisconnection(clientId);
    }
}
