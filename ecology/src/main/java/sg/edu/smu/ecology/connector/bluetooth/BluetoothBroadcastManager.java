package sg.edu.smu.ecology.connector.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A BroadcastReceiver that notifies of important bluetooth events.
 *
 * @author Anuroop PATTENA VANIYAR
 */
class BluetoothBroadcastManager extends BroadcastReceiver {
    private static final String TAG = BluetoothBroadcastManager.class.getSimpleName();
    private BluetoothConnector bluetoothConnector;
    // Whether the bluetooth is enabled or not
    private Boolean bluetoothEnabled = true;

    /**
     * @param bluetoothConnector the connector associated with the receiver
     */
    public BluetoothBroadcastManager(BluetoothConnector bluetoothConnector) {
        this.bluetoothConnector = bluetoothConnector;
    }

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast related to
     * bluetooth events
     *
     * @param context the Context in which the receiver is running.
     * @param intent  the Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        bluetoothConnector.onBluetoothOff();
                        bluetoothEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        bluetoothConnector.addPairedDevices();
                        bluetoothConnector.setupBluetoothConnection();
                        bluetoothEnabled = true;
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                // Disconnection can be either because the bluetooth was turned off or the other
                // device went out of range. Ecology already handles disconnection if bluetooth is
                // manually turned off. So this is for handling disconnections due to devices going
                // out of range.
                if (bluetoothEnabled) {
                    bluetoothConnector.onBluetoothOutOfRange();
                }
                break;
        }
    }
}

