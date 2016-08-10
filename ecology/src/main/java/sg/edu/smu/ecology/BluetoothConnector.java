package sg.edu.smu.ecology;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * Created by anurooppv on 8/8/2016.
 */
public class BluetoothConnector implements Connector {
    private final static String TAG = BluetoothConnector.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private final String UUID = "852159da-a17b-4057-983d-830c4537851c";

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket bluetoothSocket = null;
    private OutputStreamWriter outputStreamWriter;
    private BufferedReader reader;
    private Connector.Receiver receiver;

    /**
     * Used to save the application context
     */
    private Context applicationContext;

    @Override
    public void sendMessage(List<Object> message) {

    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void connect(Context context) {
        applicationContext = context;

        // To check if the device supports bluetooth.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Device does not support Bluetooth ");
            return;
        }

        // To enable bluetooth if not already enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult((Activity)applicationContext,enableBtIntent, REQUEST_ENABLE_BT, null);
        }

    }

    @Override
    public void disconnect() {

    }

    @Override
    public boolean isConnected() {
        return false;
    }

    private void fetchPairedDeviceList(){
        // Get the paired devices' list
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Vector<BluetoothSocket> sockets = new Vector<>();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                //createBluetoothSocketStreams(connectBluetoothDevice(device));
                bluetoothSocket = connectBluetoothDevice(device);
            }
        }
    }

    private BluetoothSocket connectBluetoothDevice(BluetoothDevice device) {
        BluetoothSocket socket = null;
        try {
            socket = device.createRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID));
            if(socket != null) {
                socket.connect();
                return socket;
            }else {
                Log.e(TAG, "Error creating socket");
                return null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception creating socket");
            return null;
        }
    }

    private void createBluetoothSocketStreams(BluetoothSocket socket){
        if(socket != null) {
            try {
                OutputStream oStream = socket.getOutputStream();
                InputStream iStream = socket.getInputStream();
                setupReaderWriter(oStream, iStream);
            } catch (IOException e) {
                Log.e(TAG, "Error getting in/out stream");
            }
        }
    }

    private void setupReaderWriter(OutputStream oStream, InputStream iStream) {
        outputStreamWriter = new OutputStreamWriter(oStream);
        reader = new BufferedReader(new InputStreamReader(iStream));
    }
}
