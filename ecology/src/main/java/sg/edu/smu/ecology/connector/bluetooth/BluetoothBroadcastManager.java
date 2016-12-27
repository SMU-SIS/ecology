package sg.edu.smu.ecology.connector.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by anurooppv on 25/10/2016.
 */

/**
 * A BroadcastReceiver that notifies of important bluetooth events.
 */
class BluetoothBroadcastManager extends BroadcastReceiver {
    private static final String TAG = BluetoothBroadcastManager.class.getSimpleName();
    private BluetoothConnector bluetoothConnector;

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
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.i(TAG, "BluetoothEnabled ");
                    bluetoothConnector.addPairedDevices();
                    bluetoothConnector.setupBluetoothConnection();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    break;
            }
        }
    }
}

