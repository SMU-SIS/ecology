package sg.edu.smu.ecology;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import static android.support.v4.app.ActivityCompat.startActivityForResult;


/**
 * Created by anurooppv on 8/8/2016.
 */
public class BluetoothConnector implements Connector, Handler.Callback {
    private final static String TAG = BluetoothConnector.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MAX_NUMBER_OF_BLUETOOTH_CONNECTIONS = 7;
    // Buffer size to be allocated to the IoBuffer - message byte array size is different from this
    private static final int BUFFER_SIZE = 1024;
    private BluetoothAdapter mBluetoothAdapter;
    private Connector.Receiver receiver;
    private List<BluetoothDevice> pairedDevicesList = new Vector<>();
    private ArrayList<UUID> uuidsList;
    private Handler handler = new Handler(this);
    private boolean isServer = false;
    private BluetoothAcceptThread bluetoothAcceptThread;
    private BluetoothConnectThread bluetoothConnectThread;
    private BluetoothConnectedThread bluetoothConnectedThread;
    private ArrayList<BluetoothConnectedThread> bluetoothConnectedThreads = new ArrayList<>();
    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;
    private IoBuffer ioBuffer;
    // The thread responsible for initializing the connection.
    private Thread connectionStarter = null;
    /**
     * Used to save the application context
     */
    private Context applicationContext;

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void connect(Context context) {
        uuidsList = new ArrayList<>();
        // 7 randomly-generated UUIDs. These must match on both server and client.
        uuidsList.add(java.util.UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"));
        uuidsList.add(java.util.UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"));
        uuidsList.add(java.util.UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"));
        uuidsList.add(java.util.UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"));
        uuidsList.add(java.util.UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"));
        uuidsList.add(java.util.UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"));
        uuidsList.add(java.util.UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));

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
            startActivityForResult((Activity) applicationContext, enableBtIntent,
                    REQUEST_ENABLE_BT, null);
        }

        addPairedDevices();

        Log.i(TAG, "isServer " + isServer);

        if (isServer) {
            // Start the thread to listen on a BluetoothServerSocket
            if (bluetoothAcceptThread == null) {
                Log.i(TAG, "create accept thread ");
                bluetoothAcceptThread = new BluetoothAcceptThread(mBluetoothAdapter, uuidsList,
                        handler);
                connectionStarter = bluetoothAcceptThread;
                connectionStarter.start();
            }
        } else {
            for (int i = 0; i < pairedDevicesList.size(); i++) {
                // Create a new thread and attempt to connect to each UUID one-by-one.
                    try {
                        bluetoothConnectThread = new BluetoothConnectThread(mBluetoothAdapter,
                                pairedDevicesList.get(i), uuidsList, handler);
                        connectionStarter = bluetoothConnectThread;
                        connectionStarter.start();
                    } catch (Exception e) {
                    }
            }
        }
    }

    @Override
    public void disconnect() {
        onConnectorConnected = false;
        Log.i(TAG, "disconnected ");
        if (connectionStarter != null && !connectionStarter.isInterrupted()) {
            connectionStarter.interrupt();
        }
    }

    @Override
    public boolean isConnected() {
        return onConnectorConnected;
    }

    private void addPairedDevices() {
        // Get the paired devices' list
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesList.add(device);
            }
        }
        Log.i(TAG, "paired devices " + pairedDevicesList.toString());
    }

    @Override
    public void sendMessage(List<Object> message) {
        ioBuffer = IoBuffer.allocate(BUFFER_SIZE);
        if (isServer) {
            for (int i = 0; i < bluetoothAcceptThread.getNumberOfDevicesConnected(); i++) {
                encodeMessage(message);
                writeData(bluetoothConnectedThreads.get(i));
            }
        } else if (bluetoothConnectedThread != null) {
            encodeMessage(message);
            writeData(bluetoothConnectedThread);
        }
    }

    /**
     * Encode the message to be sent
     *
     * @param message the message to be encoded
     */
    private void encodeMessage(List<Object> message) {
        DataEncoder dataEncoder = new DataEncoder();
        MessageData messageData = new MessageData();

        for (int i = 0; i < message.size(); i++) {
            messageData.addArgument(message.get(i));
        }

        try {
            dataEncoder.encodeMessage(messageData, ioBuffer);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }
    }

    private void writeData(BluetoothConnectedThread bluetoothConnectedThread) {
        // To store the length of the message
        int length = ioBuffer.position();

        // Contains the whole IoBuffer
        byte[] eventData = ioBuffer.array();

        // Actual message data is retrieved
        byte[] eventDataToSend = Arrays.copyOfRange(eventData, 0, length);

        // Write length of the data first
        bluetoothConnectedThread.writeInt(length);

        // Write the byte data
        bluetoothConnectedThread.writeData(eventDataToSend);
        ioBuffer.clear();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Settings.MESSAGE_READ:
                Log.d(TAG, " MESSAGE_READ");
                byte[] readBuf = (byte[]) msg.obj;

                DataDecoder dataDecoder = new DataDecoder();

                MessageData messageData = dataDecoder.convertMessage(readBuf, readBuf.length);

                List<Object> data;
                data = messageData.getArguments();
                Log.i(TAG, "data " + data);
                String eventTypeReceived = null;

                try {
                    eventTypeReceived = (String) data.get(data.size() - 2);
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, " eventType " + eventTypeReceived);

                receiver.onMessage(data);
                break;

            case Settings.MY_HANDLE:
                Log.d(TAG, " MY HANDLE");
                Object obj = msg.obj;
                if (isServer) {
                    addConnectedThreadObjects((BluetoothConnectedThread) obj);
                } else {
                    setConnectedThreadObject((BluetoothConnectedThread) obj);
                }

                onConnectorConnected = true;

                receiver.onConnectorConnected();
                break;

            case Settings.SOCKET_CLOSE:
                Log.d(TAG, "Socket Close");
                onConnectorConnected = false;

                receiver.onConnectorDisconnected();

                if(bluetoothConnectThread != null){
                    bluetoothConnectThread.setConnectedToServer(false);
                }
                break;
        }
        return true;
    }

    private void addConnectedThreadObjects(BluetoothConnectedThread bluetoothConnectedThread) {
        bluetoothConnectedThreads.add(bluetoothConnectedThread);
    }

    private void setConnectedThreadObject(BluetoothConnectedThread bluetoothConnectedThread) {
        this.bluetoothConnectedThread = bluetoothConnectedThread;
    }
}
