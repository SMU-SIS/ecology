package sg.edu.smu.ecology;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    // Seven randomly-generated UUIDs. These must match on both server and client.
    private static final ArrayList<String> uuidsList = new ArrayList<>(Arrays.asList(
            "b7746a40-c758-4868-aa19-7ac6b3475dfc",
            "2d64189d-5a2c-4511-a074-77f199fd0834",
            "e442e09a-51f3-4a7b-91cb-f638491d1412",
            "a81d6504-4536-49ee-a475-7d96d09439e4",
            "aa91eab1-d8ad-448e-abdb-95ebba4a9b55",
            "4d34da73-d0a4-4f40-ac38-917e0a9dee97",
            "5e14d4df-9c8a-4db7-81e4-c937564c86e0"));

    // Buffer size to be allocated to the IoBuffer - message byte array size is different from this
    private static final int BUFFER_SIZE = 1024;
    // To listen to certain events of bluetooth
    private final IntentFilter intentFilter = new IntentFilter();
    private BluetoothAdapter bluetoothAdapter;
    private Connector.Receiver receiver;
    private List<BluetoothDevice> pairedDevicesList = new Vector<>();
    private Handler handler = new Handler(this);
    private boolean isServer;
    private BluetoothServerAcceptThread bluetoothServerAcceptThread;
    private BluetoothClientConnectThread bluetoothClientConnectThread;
    private ArrayList<BluetoothClientConnectThread> clientConnectThreadList = new ArrayList<>();
    private BluetoothSocketReadWriter bluetoothSocketReadWriter;
    private ArrayList<BluetoothSocketReadWriter> bluetoothSocketReadWritersList = new ArrayList<>();
    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;
    private IoBuffer ioBuffer;
    private ArrayList<UUID> uuidsRequiredList = new ArrayList<>();
    private BluetoothBroadcastManager bluetoothBroadcastManager;
    /**
     * Used to save the activity context
     */
    private Context context;

    public BluetoothConnector(Boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void connect(Context context) {
        this.context = context;
        Log.i(TAG, "isServer " + isServer);

        // To check if the device supports bluetooth.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Device does not support Bluetooth ");
            return;
        }

        addIntentActionsToFliter();
        // To notify about various events occurring with respect to the bluetooth connection
        bluetoothBroadcastManager = new BluetoothBroadcastManager(this);
        // Register the broadcast receiver with the intent values to be matched
        context.registerReceiver(bluetoothBroadcastManager, intentFilter);

        // To enable bluetooth if not already enabled.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult((Activity) context, enableBtIntent,
                    REQUEST_ENABLE_BT, null);
        } else {
            // Bluetooth is already enabled on the device
            addPairedDevices();
            setupBluetoothConnection();
        }
    }

    @Override
    public void disconnect() {
        onConnectorConnected = false;
        Log.i(TAG, "disconnected ");

        context.unregisterReceiver(bluetoothBroadcastManager);

        if (bluetoothServerAcceptThread != null && !bluetoothServerAcceptThread.isInterrupted()) {
            bluetoothServerAcceptThread.interrupt();
        }

        for (int i = 0; i < clientConnectThreadList.size(); i++) {
            if (clientConnectThreadList.get(i) != null && !clientConnectThreadList.get(i).isInterrupted()) {
                clientConnectThreadList.get(i).interrupt();
            }
        }
    }

    @Override
    public boolean isConnected() {
        return onConnectorConnected;
    }

    /**
     * Add the required number of UUIDs needed for setting up the bluetooth connection
     *
     * @param numberOfUuidsRequired number of UUIDs required
     */
    private void addUuids(int numberOfUuidsRequired) {
        for (int i = 0; i < numberOfUuidsRequired; i++) {
            uuidsRequiredList.add(java.util.UUID.fromString(uuidsList.get(i)));
        }
        Log.i(TAG, "Number of UUIDs added " + uuidsRequiredList.size());
    }

    /**
     * Add Intent actions to match against.
     */
    private void addIntentActionsToFliter() {
        // add the intents that the broadcast receiver should check for
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    /**
     * Get the UUIDs list
     *
     * @return the UUID list
     */
    private ArrayList<UUID> getUuidsRequiredList() {
        return uuidsRequiredList;
    }

    /**
     * Add all the paired devices
     */
    void addPairedDevices() {
        // Get the paired devices' list
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesList.add(device);
            }
        }
        Log.i(TAG, "paired devices " + pairedDevicesList.toString());

        addUuids(pairedDevicesList.size());
    }

    /**
     * Setup the connection based on the device type - server or client
     */
    void setupBluetoothConnection() {
        // Check if the device is a server or a client
        if (isServer) {
            // Start the thread to listen on a BluetoothServerSocket
            if (bluetoothServerAcceptThread == null) {
                Log.i(TAG, "create accept thread ");
                try {
                    bluetoothServerAcceptThread = new BluetoothServerAcceptThread(bluetoothAdapter,
                            getUuidsRequiredList(), handler);
                    bluetoothServerAcceptThread.start();
                } catch (Exception e) {
                    Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
                }
            }
            // Start threads to connect as clients
            for (int i = 0; i < pairedDevicesList.size(); i++) {
                createClientConnectionThreads(pairedDevicesList.get(i));
            }

        } else {
            // Create threads to connect as clients and attempt to connect to each UUID one-by-one.
            for (int i = 0; i < pairedDevicesList.size(); i++) {
                createClientConnectionThreads(pairedDevicesList.get(i));
            }
        }
    }

    private void createClientConnectionThreads(BluetoothDevice device) {
        bluetoothClientConnectThread = new BluetoothClientConnectThread(bluetoothAdapter,
                device, getUuidsRequiredList(), handler);
        bluetoothClientConnectThread.start();
        clientConnectThreadList.add(bluetoothClientConnectThread);
        Log.i(TAG, "clientConnectThreadList " + clientConnectThreadList.size());
    }

    @Override
    public void sendMessage(List<Object> message) {
        ioBuffer = IoBuffer.allocate(BUFFER_SIZE);
        if (isServer) {
            for (int i = 0; i < bluetoothServerAcceptThread.getNumberOfDevicesConnected(); i++) {
                encodeMessage(message);
                writeData(bluetoothSocketReadWritersList.get(i));
            }
        } else if (bluetoothSocketReadWriter != null) {
            encodeMessage(message);
            writeData(bluetoothSocketReadWriter);
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

    private void writeData(BluetoothSocketReadWriter bluetoothSocketReadWriter) {
        // To store the length of the message
        int length = ioBuffer.position();

        // Contains the whole IoBuffer
        byte[] eventData = ioBuffer.array();

        // Actual message data is retrieved
        byte[] eventDataToSend = Arrays.copyOfRange(eventData, 0, length);

        // Write length of the data first
        bluetoothSocketReadWriter.writeInt(length);

        // Write the byte data
        bluetoothSocketReadWriter.writeData(eventDataToSend);
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

            case Settings.SOCKET_SERVER:
                Log.d(TAG, "Connected as a server to a device");
                Object obj = msg.obj;
                addSocketReadWriterObjects((BluetoothSocketReadWriter) obj);
                onConnectorConnected = true;

                receiver.onConnectorConnected();
                break;

            case Settings.SOCKET_CLIENT:
                Log.i(TAG, "Connected as a client to a device");
                Object object = msg.obj;
                setSocketReadWriterObject((BluetoothSocketReadWriter) object);
                break;

            case Settings.SOCKET_CLOSE:
                Log.d(TAG, "Socket Close");
                onConnectorConnected = false;

                receiver.onConnectorDisconnected();

                if (bluetoothClientConnectThread != null) {
                    bluetoothClientConnectThread.setConnectedToServer(false);
                }
                break;
        }
        return true;
    }

    // When the device is a server
    private void addSocketReadWriterObjects(BluetoothSocketReadWriter bluetoothSocketReadWriter) {
        bluetoothSocketReadWritersList.add(bluetoothSocketReadWriter);
        Log.i(TAG, "bluetoothSocketReadWritersList " + bluetoothSocketReadWritersList.size());
    }

    // When the device is a client
    private void setSocketReadWriterObject(BluetoothSocketReadWriter bluetoothSocketReadWriter) {
        this.bluetoothSocketReadWriter = bluetoothSocketReadWriter;
    }
}
