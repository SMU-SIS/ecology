package sg.edu.smu.ecology;

import android.util.Log;

/**
 * Created by anurooppv on 25/10/2016.
 */

public class BluetoothServerConnector extends BluetoothConnector {
    private static final String TAG = BluetoothServerConnector.class.getSimpleName();

    private BluetoothServerAcceptThread bluetoothServerAcceptThread;

    @Override
    public void setupBluetoothConnection() {
    // Start the thread to listen on a BluetoothServerSocket
        if (bluetoothServerAcceptThread == null) {
            Log.i(TAG, "create accept thread ");
            try {
                bluetoothServerAcceptThread = new BluetoothServerAcceptThread(getBluetoothAdapter(),
                        getUuidsList(), getHandler());
                bluetoothServerAcceptThread.start();
                setServer(true);
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
}
