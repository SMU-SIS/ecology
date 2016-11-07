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
import android.util.SparseArray;

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
 * Created by anurooppv on 25/10/2016.
 */

abstract class BluetoothConnector implements Connector, Handler.Callback {
    private final static String TAG = BluetoothConnector.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    // Buffer size to be allocated to the IoBuffer - message byte array size is different from this
    private static final int BUFFER_SIZE = 1024;
    // Seven randomly-generated UUIDs. These must match on both server and client.
    private static final ArrayList<UUID> uuidsList = new ArrayList<>(Arrays.asList(
            java.util.UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"),
            java.util.UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"),
            java.util.UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"),
            java.util.UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"),
            java.util.UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"),
            java.util.UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"),
            java.util.UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0")));

    // To listen to certain events of bluetooth
    private final IntentFilter intentFilter = new IntentFilter();
    private BluetoothAdapter bluetoothAdapter;
    private Connector.Receiver receiver;
    private List<BluetoothDevice> pairedDevicesList = new Vector<>();
    private Handler handler = new Handler(this);
    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;
    private IoBuffer ioBuffer;
    private BluetoothBroadcastManager bluetoothBroadcastManager;
    private ArrayList<BluetoothSocketReadWriter> bluetoothSocketReadWritersList = new ArrayList<>();
    private SparseArray<BluetoothSocketReadWriter> clientList = new SparseArray<>();
    private boolean isServer = false;
    //Used to save the activity context
    private Context context;
    private ServerDisconnectionListener serverDisconnectionListener;
    private ClientDisconnectionListener clientDisconnectionListener;

    @Override
    public void sendMessage(List<Object> message) {
        ioBuffer = IoBuffer.allocate(BUFFER_SIZE);
        for (int i = 0; i < bluetoothSocketReadWritersList.size(); i++) {
            encodeMessage(message);
            writeData(bluetoothSocketReadWritersList.get(i));
        }
    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public void connect(Context context) {
        this.context = context;

        // To check if the device supports bluetooth.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Log.i(TAG, "Device does not support Bluetooth ");
            return;
        }

        addIntentActionsToFilter();
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
    }

    @Override
    public boolean isConnected() {
        return onConnectorConnected;
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

                // If the device is a server, the received message will be forwarded to other
                // connected clients
                if (isServer) {
                    forwardMessage(data, msg.arg1);
                }
                break;

            case Settings.SOCKET_SERVER:
                Log.d(TAG, "Connected as a server to a device");
                Object obj = msg.obj;

                updateClientsList((BluetoothSocketReadWriter) obj, msg.arg1);
                addSocketReadWriterObject((BluetoothSocketReadWriter) obj);

                onConnectorConnected = true;
                receiver.onConnectorConnected();
                break;

            case Settings.SOCKET_CLIENT:
                Log.i(TAG, "Connected as a client to a device");
                Object object = msg.obj;

                addSocketReadWriterObject((BluetoothSocketReadWriter) object);

                onConnectorConnected = true;
                receiver.onConnectorConnected();
                break;

            case Settings.SOCKET_CLOSE:
                Log.d(TAG, "Socket Close");
                onConnectorConnected = false;

                Object disconnectedObj = msg.obj;
                removeSocketReadWriterObject((BluetoothSocketReadWriter) disconnectedObj);

                receiver.onConnectorDisconnected();

                if (isServer) {
                    updateClientsList((BluetoothSocketReadWriter) disconnectedObj, msg.arg1);
                    if (clientDisconnectionListener != null) {
                        clientDisconnectionListener.handleClientDisconnection(msg.arg1);
                    }
                } else {
                    if (serverDisconnectionListener != null) {
                        serverDisconnectionListener.resetClientConnectThread();
                    }
                }

                break;
        }
        return true;
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    protected List<BluetoothDevice> getPairedDevicesList() {
        return pairedDevicesList;
    }

    protected Handler getHandler() {
        return handler;
    }

    /**
     * Will be set to true if the device is a server
     *
     * @param server the boolean value denoting the device type
     */
    protected void setServer(boolean server) {
        isServer = server;
    }

    protected void setServerDisconnectionListener(ServerDisconnectionListener
                                                          serverDisconnectionListener) {
        this.serverDisconnectionListener = serverDisconnectionListener;
    }

    protected void setClientDisconnectionListener(ClientDisconnectionListener
                                                          clientDisconnectionListener) {
        this.clientDisconnectionListener = clientDisconnectionListener;
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
    }

    /**
     * Add Intent actions to match against.
     */
    private void addIntentActionsToFilter() {
        // add the intents that the broadcast receiver should check for
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    /**
     * Get the UUIDs list
     *
     * @return the UUID list
     */
    ArrayList<UUID> getUuidsList() {
        return uuidsList;
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

    /**
     * Write data to be published
     *
     * @param bluetoothSocketReadWriter thread responsible for data read and write
     */
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

    /**
     * When a server receives a message from a client, it is forwarded to rest of the connected
     * clients
     *
     * @param message  the message received
     * @param clientId the client id of the client from which the message was received
     */
    private void forwardMessage(List<Object> message, int clientId) {
        ioBuffer = IoBuffer.allocate(BUFFER_SIZE);
        for (int i = 0; i < bluetoothSocketReadWritersList.size(); i++) {
            if (!(clientList.get(clientId).equals(bluetoothSocketReadWritersList.get(i)))) {
                encodeMessage(message);
                writeData(bluetoothSocketReadWritersList.get(i));
                Log.i(TAG, "Message forwarding...");
            }
        }
    }

    /**
     * This method updates the list accordingly when a client gets connected or disconnected.
     *
     * @param bluetoothSocketReadWriter the thread associated with the client
     * @param clientId                  the unique id of the client
     */
    private void updateClientsList(BluetoothSocketReadWriter bluetoothSocketReadWriter,
                                   int clientId) {
        if (clientList.get(clientId) != null) {
            clientList.remove(clientId);
            Log.i(TAG, "Removed from clients list " + clientList.size());
        } else {
            clientList.put(clientId, bluetoothSocketReadWriter);
            Log.i(TAG, "Added to clients list " + clientList.size());
        }
    }

    /**
     * This method adds the associated thread when a new connection is established
     *
     * @param bluetoothSocketReadWriter the thread responsible for message read and write
     */
    private void addSocketReadWriterObject(BluetoothSocketReadWriter bluetoothSocketReadWriter) {
        bluetoothSocketReadWritersList.add(bluetoothSocketReadWriter);
        Log.i(TAG, "bluetoothSocketReadWritersList " + bluetoothSocketReadWritersList.size());
    }

    /**
     * This method removes the associated thread when a connection is disconnected
     *
     * @param bluetoothSocketReadWriter the thread responsible for message read and write
     */
    private void removeSocketReadWriterObject(BluetoothSocketReadWriter bluetoothSocketReadWriter) {
        bluetoothSocketReadWritersList.remove(bluetoothSocketReadWriter);
        Log.i(TAG, "bluetoothSocketReadWritersList " + bluetoothSocketReadWritersList.size());
    }

    public abstract void setupBluetoothConnection();

    /**
     * Interface to listen to server disconnection
     */
    protected interface ServerDisconnectionListener {

        /**
         * Resets the client connect thread so that it starts looking for a new server
         */
        public void resetClientConnectThread();
    }

    /**
     * Interface to listen to client disconnections
     */
    protected interface ClientDisconnectionListener {

        /**
         * Handle the client disconnection
         * @param clientId the client that got disconnected
         */
        public void handleClientDisconnection(int clientId);
    }
}
