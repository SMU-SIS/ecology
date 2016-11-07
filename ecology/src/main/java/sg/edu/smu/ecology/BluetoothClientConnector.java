package sg.edu.smu.ecology;

/**
 * Created by anurooppv on 25/10/2016.
 */

public class BluetoothClientConnector extends BluetoothConnector implements
        BluetoothConnector.ServerDisconnectionListener {
    private static final String TAG = BluetoothClientConnector.class.getSimpleName();

    private BluetoothClientConnectThread bluetoothClientConnectThread;

    @Override
    public void setupBluetoothConnection() {
        bluetoothClientConnectThread = new BluetoothClientConnectThread(getBluetoothAdapter(),
                getPairedDevicesList().get(0), getUuidsList(), getHandler());
        bluetoothClientConnectThread.start();
        setServerDisconnectionListener(this);
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if (bluetoothClientConnectThread != null && !bluetoothClientConnectThread.isInterrupted()) {
            bluetoothClientConnectThread.interrupt();
        }
    }

    @Override
    public void handleServerDisconnection() {
        bluetoothClientConnectThread.handleServerDisconnection();
    }
}
