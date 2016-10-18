package sg.edu.smu.ecology;

/**
 * Created by anurooppv on 13/10/2016.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This thread runs while attempting to make an outgoing connection
 * with a device. The connection either succeeds or fails.
 */
public class BluetoothConnectThread extends Thread {
    private static final String TAG = BluetoothConnectThread.class.getSimpleName();
    private final BluetoothSocket bluetoothSocket;
    private final BluetoothDevice bluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;
    private UUID tempUuid;
    private ArrayList<UUID> mUuidsList;
    private BluetoothConnectedThread bluetoothConnectedThread;
    private Handler handler;

    public BluetoothConnectThread(BluetoothAdapter bluetoothAdapter, BluetoothDevice device,
                                  UUID uuidToTry, ArrayList<UUID> mUuidsList, Handler handler) {
        this.bluetoothAdapter = bluetoothAdapter;
        bluetoothDevice = device;
        BluetoothSocket tempSocket = null;
        tempUuid = uuidToTry;
        this.mUuidsList = mUuidsList;
        this.handler = handler;

        // Get a BluetoothSocket for a connection with the
        // given BluetoothDevice
        try {
            tempSocket = device.createRfcommSocketToServiceRecord(uuidToTry);
        } catch (IOException e) {
            Log.e(TAG, "create() failed", e);
        }
        bluetoothSocket = tempSocket;
    }

    @Override
    public void run() {
        // Always cancel discovery because it will slow down a connection
        bluetoothAdapter.cancelDiscovery();
        // Make a connection to the BluetoothSocket
        try {
            Log.i(TAG, "connect ");
            // This is a blocking call and will only return on a
            // successful connection or an exception
            bluetoothSocket.connect();
        } catch (IOException e) {
            Log.i(TAG, "IO Excpetion ");
            if (tempUuid.toString().contentEquals(mUuidsList.get(6).toString())) {
                //connectionFailed();
            }
            // Close the socket
            try {
                bluetoothSocket.close();
            } catch (IOException e2) {
                Log.e(TAG, "unable to close() socket during connection failure", e2);
            }
            return;
        }

        // Start the connected thread
        connected(bluetoothSocket);
    }

    private void connected(BluetoothSocket socket) {
        bluetoothConnectedThread = new BluetoothConnectedThread(socket, handler);
        bluetoothConnectedThread.start();
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if (bluetoothConnectedThread != null) {
            bluetoothConnectedThread.onInterrupt();
        }
    }
}
