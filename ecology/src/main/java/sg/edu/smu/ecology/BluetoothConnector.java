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
    // The room used for sending device id exchange and device id disconnection events.
    private static final String DEFAULT_ROOM = "room";
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
    private String deviceId;
    // To store the device ids of connected devices.
    private SparseArray<String> deviceIdsList = new SparseArray<>();

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
    public void connect(Context context, String deviceId) {
        this.context = context;
        this.deviceId = deviceId;

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
            case Settings.MESSAGE_RECEIVED:
                Log.d(TAG, " MESSAGE RECEIVED");
                onMessageReceived(msg);
                break;

            case Settings.CLIENT_CONNECTED:
                Log.d(TAG, "A client device has been connected");
                onClientConnected(msg);
                break;

            case Settings.CONNECTED_TO_A_SERVER:
                Log.i(TAG, "Connected as a client to a device");
                onConnectedToAServer(msg);
                break;

            case Settings.SOCKET_CLOSE:
                Log.d(TAG, "Socket Close");
                onSocketClose(msg);
                break;
        }
        return true;
    }

    /**
     * When a message is received from another device in the ecology
     *
     * @param msg the message received
     */
    private void onMessageReceived(Message msg) {
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

        // If the device is a server, the received message will be forwarded to other
        // connected clients
        if (isServer) {
            if (eventTypeReceived != null && eventTypeReceived.equals(Settings.DEVICE_ID_EXCHANGE)) {
                deviceIdsList.put(msg.arg1, (String) data.get(data.size() - 3));
                Log.i(TAG, "deviceIdList " + deviceIdsList.clone());
                // Add the internal integer id so that it's referenced the same in all the
                // devices
                data.add(0, msg.arg1);
            }
            forwardMessage(data, msg.arg1);
        }

        passMessageToReceiver(eventTypeReceived, data);
    }

    /**
     * When a client device gets connected
     *
     * @param msg the message received
     */
    private void onClientConnected(Message msg) {
        Object obj = msg.obj;

        updateClientsList((BluetoothSocketReadWriter) obj, msg.arg1);
        addSocketReadWriterObject((BluetoothSocketReadWriter) obj);

        onConnectorConnected = true;
        // Pass the server device Id to the connected client device. Also add the internal
        // integer id so that it's referenced the same in all the devices
        sendMessageToClient(new ArrayList<Object>(Arrays.asList(0, deviceId,
                Settings.DEVICE_ID_EXCHANGE, DEFAULT_ROOM)), msg.arg1);

        // To notify the new client about the already connected client devices in the ecology
        sendConnectedClientsIds(msg.arg1);
    }

    /**
     * When a device gets connected to a server
     *
     * @param msg the message received
     */
    private void onConnectedToAServer(Message msg) {
        Object object = msg.obj;

        addSocketReadWriterObject((BluetoothSocketReadWriter) object);

        onConnectorConnected = true;
        // Send the client device Id to the server device
        sendMessage(new ArrayList<Object>(Arrays.asList(deviceId, Settings.DEVICE_ID_EXCHANGE,
                DEFAULT_ROOM)));
    }

    /**
     * When a socket gets closed
     *
     * @param msg the message received
     */
    private void onSocketClose(Message msg) {
        onConnectorConnected = false;

        Object disconnectedObj = msg.obj;
        removeSocketReadWriterObject((BluetoothSocketReadWriter) disconnectedObj);

        if (isServer) {
            // A client device has been disconnected
            receiver.onDeviceDisconnected(deviceIdsList.get(msg.arg1));
            // To notify other connected client devices in the ecology
            forwardMessage(new ArrayList<Object>(Arrays.asList(deviceIdsList.get(msg.arg1),
                    Settings.DEVICE_DISCONNECTED, DEFAULT_ROOM)), msg.arg1);
            // Update the connected devices list
            deviceIdsList.remove(msg.arg1);
            updateClientsList((BluetoothSocketReadWriter) disconnectedObj, msg.arg1);
            if (clientDisconnectionListener != null) {
                clientDisconnectionListener.handleClientDisconnection(msg.arg1);
            }
        } else {
            if (serverDisconnectionListener != null) {
                serverDisconnectionListener.handleServerDisconnection();
            }
            for (int i = 0; i < deviceIdsList.size(); i++) {
                receiver.onDeviceDisconnected(deviceIdsList.get(deviceIdsList.keyAt(i)));
            }
            deviceIdsList.clear();
        }
    }

    /**
     * Pass the received message to receiver
     *
     * @param eventTypeReceived the event type received
     * @param data              the data received
     */
    private void passMessageToReceiver(String eventTypeReceived, List<Object> data) {
        switch (eventTypeReceived) {
            case Settings.DEVICE_DISCONNECTED:
                String receivedDeviceId = (String) data.get(data.size() - 3);
                receiver.onDeviceDisconnected(receivedDeviceId);

                for (int i = 0; i < deviceIdsList.size(); i++) {
                    if (deviceIdsList.get(deviceIdsList.keyAt(i)).equals(receivedDeviceId)) {
                        deviceIdsList.delete(deviceIdsList.keyAt(i));
                    }
                }
                break;

            case Settings.DEVICE_ID_EXCHANGE:
                if (!isServer) {
                    deviceIdsList.put((Integer) data.get(data.size() - 4),
                            (String) data.get(data.size() - 3));
                    Log.i(TAG, "deviceIdList " + deviceIdsList);
                }
                receiver.onDeviceConnected((String) data.get(data.size() - 3));

            default:
                receiver.onMessage(data);
                break;
        }
    }

    /**
     * Send the device Ids of already connected client devices to the newly connected client device
     *
     * @param clientId the client Id of the new client device
     */
    private void sendConnectedClientsIds(int clientId) {
        for (int i = 0; i < deviceIdsList.size(); i++) {
            if (deviceIdsList.keyAt(i) != clientId) {
                encodeMessage(new ArrayList<Object>(Arrays.asList(deviceIdsList.keyAt(i),
                        deviceIdsList.get(deviceIdsList.keyAt(i)), Settings.DEVICE_ID_EXCHANGE,
                        DEFAULT_ROOM)));
                writeData(clientList.get(clientId));
            }
        }
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
     * Send message to a particular client device
     *
     * @param message  the message to be sent
     * @param clientId the client id of the destination device
     */
    private void sendMessageToClient(List<Object> message, int clientId) {
        ioBuffer = IoBuffer.allocate(BUFFER_SIZE);
        for (int i = 0; i < bluetoothSocketReadWritersList.size(); i++) {
            if ((clientList.get(clientId).equals(bluetoothSocketReadWritersList.get(i)))) {
                encodeMessage(message);
                writeData(bluetoothSocketReadWritersList.get(i));
            }
        }
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
         * Handle the server disconnection so that it starts looking for new server connection
         */
        public void handleServerDisconnection();
    }

    /**
     * Interface to listen to client disconnections
     */
    protected interface ClientDisconnectionListener {

        /**
         * Handle the client disconnection
         *
         * @param clientId the client that got disconnected
         */
        public void handleClientDisconnection(int clientId);
    }
}
