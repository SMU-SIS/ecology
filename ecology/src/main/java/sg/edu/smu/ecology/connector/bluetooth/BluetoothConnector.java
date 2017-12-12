package sg.edu.smu.ecology.connector.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import sg.edu.smu.ecology.EcologyMessage;
import sg.edu.smu.ecology.connector.Connector;
import sg.edu.smu.ecology.encoding.MessageDecoder;
import sg.edu.smu.ecology.encoding.MessageEncoder;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * @author Anuroop PATTENA VANIYAR
 */
abstract class BluetoothConnector implements Connector, Handler.Callback {
    private final static String TAG = BluetoothConnector.class.getSimpleName();

    private static final int REQUEST_ENABLE_BT = 1;
    // Used for client-server message handler
    static final int MESSAGE_RECEIVED = 0x400 + 1;
    static final int SOCKET_CONNECTED = 0x400 + 2;
    static final int SOCKET_CLOSE = 0x400 + 3;
    static final int DEVICE_READY = 0x400 + 4;

    // Seven randomly-generated UUIDs. These must match on both server and client.
    private static final List<UUID> uuidsList = Arrays.asList(
            UUID.fromString("b7746a40-c758-4868-aa19-7ac6b3475dfc"),
            UUID.fromString("2d64189d-5a2c-4511-a074-77f199fd0834"),
            UUID.fromString("e442e09a-51f3-4a7b-91cb-f638491d1412"),
            UUID.fromString("a81d6504-4536-49ee-a475-7d96d09439e4"),
            UUID.fromString("aa91eab1-d8ad-448e-abdb-95ebba4a9b55"),
            UUID.fromString("4d34da73-d0a4-4f40-ac38-917e0a9dee97"),
            UUID.fromString("5e14d4df-9c8a-4db7-81e4-c937564c86e0"));
    // An Id used to route connector messages
    private static final int CONNECTOR_MESSAGE_ID = 0;
    // An Id used to route receiver messages
    private static final int RECEIVER_MESSAGE_ID = 1;
    // To listen to certain events of bluetooth
    private final IntentFilter intentFilter = new IntentFilter();
    // Represents the local device Bluetooth adapter.
    private BluetoothAdapter bluetoothAdapter;
    private Connector.Receiver receiver;
    // To store the list of paired bluetooth devices
    private List<BluetoothDevice> pairedDevicesList = new Vector<>();
    private Handler handler = new Handler(this);
    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;
    private BluetoothBroadcastManager bluetoothBroadcastManager;
    //Used to save the activity context
    private Context context;
    // Device Id of this device
    private String deviceId;
    // To store the device ids(user generated) of all the connected devices.
    private Map<Integer, String> deviceIdsList = new HashMap<>();
    // Message encoder to encode the message into byte arrays before sending it.
    private final MessageEncoder messageEncoder = new MessageEncoder();
    // Message decoder to decode byte arrays into messages.
    private final MessageDecoder messageDecoder = new MessageDecoder();

    /**
     * Send message to all the connected devices in the ecology
     *
     * @param message the message to be sent
     */
    @Override
    public void sendMessage(EcologyMessage message) {
        message.addArgument(RECEIVER_MESSAGE_ID);

        doSendMessage(message, getBluetoothSocketReadWriterList(message.getTargetType(),
                message.getTargets()));
    }

    /**
     * Send a connector message
     *
     * @param message                    the message to be sent
     * @param bluetoothSocketReadWriters thread list of destination devices
     */
    void sendConnectorMessage(EcologyMessage message,
                              Collection<BluetoothSocketReadWriter> bluetoothSocketReadWriters) {
        message.addArgument(CONNECTOR_MESSAGE_ID);
        doSendMessage(message, bluetoothSocketReadWriters);
    }

    private void doSendMessage(EcologyMessage message, Collection<BluetoothSocketReadWriter>
            bluetoothSocketReadWriters) {
        byte[] encodedMessageData = encodeMessage(message);

        for (BluetoothSocketReadWriter bluetoothSocketReadWriter : bluetoothSocketReadWriters) {
            writeData(bluetoothSocketReadWriter, encodedMessageData);
        }
    }

    /**
     * Connect to the ecology
     *
     * @param context  the activity context
     * @param deviceId the id of th device
     */
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
            if(context instanceof Activity) {
                startActivityForResult((Activity) context, enableBtIntent,
                        REQUEST_ENABLE_BT, null);
            }
        } else {
            // Bluetooth is already enabled on the device
            addPairedDevices();
            setupBluetoothConnection();
        }
    }

    /**
     * Disconnect from the ecology
     */
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

    /**
     * Handle the incoming messages
     *
     * @param msg the received message
     * @return if the message was handled or not
     */
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MESSAGE_RECEIVED:
                Log.d(TAG, " MESSAGE RECEIVED");
                onMessageReceived(msg);
                break;

            case SOCKET_CONNECTED:
                onConnectorConnected = true;
                onDeviceConnected(msg);
                break;

            case SOCKET_CLOSE:
                onConnectorConnected = false;
                Log.d(TAG, "Socket Close");
                onDeviceDisconnected(msg);
                break;

            case DEVICE_READY:
                getReceiver().onConnected();
                break;
        }
        return true;
    }

    /**
     * Gets the {@link sg.edu.smu.ecology.connector.Connector.Receiver} instance of this connector
     *
     * @return the {@link sg.edu.smu.ecology.connector.Connector.Receiver} instance of this connector
     */
    Receiver getReceiver() {
        return receiver;
    }

    /**
     * Sets the {@link sg.edu.smu.ecology.connector.Connector.Receiver} instance of this connector
     *
     * @param receiver the {@link sg.edu.smu.ecology.connector.Connector.Receiver} instance of this
     *                 connector
     */
    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Get the list of device ids of connected devices in the ecology
     *
     * @return the list of device ids of connected devices in the ecology
     */
    Map<Integer, String> getDeviceIdsList() {
        return deviceIdsList;
    }

    /**
     * When a message is received from another device in the ecology
     *
     * @param msg the message received
     */
    private void onMessageReceived(Message msg) {
        byte[] readBuf = (byte[]) msg.obj;

        EcologyMessage message;
        message = messageDecoder.decode(readBuf);
        Log.i(TAG, "data " + message.getArguments());

        // Fetch the routing id of the received message
        Integer receivedMessageId = (Integer) message.fetchArgument();

        // Check if the received data is a connector message or a receiver message
        if (receivedMessageId.equals(CONNECTOR_MESSAGE_ID)) {
            onConnectorMessage(msg, message);
        } else if (receivedMessageId.equals(RECEIVER_MESSAGE_ID)) {
            onReceiverMessage(msg, message);
        }
    }

    /**
     * When a receiver message is received
     *
     * @param msg         the message received
     * @param messageData the decoded data
     */
    void onReceiverMessage(Message msg, EcologyMessage messageData) {
        getReceiver().onMessage(messageData);
    }

    /**
     * Get the {@link BluetoothAdapter} instance of this device
     *
     * @return the {@link BluetoothAdapter} instance of this device
     */
    BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    /**
     * Get the list of {@link BluetoothDevice} currently paired to this device
     *
     * @return the list of {@link BluetoothDevice} currently paired to this device
     */
    List<BluetoothDevice> getPairedDevicesList() {
        return pairedDevicesList;
    }

    /**
     * Get the {@link Handler} instance used for handling messages
     *
     * @return the {@link Handler} instance
     */
    Handler getHandler() {
        return handler;
    }

    /**
     * Get the device id of this device
     *
     * @return the device id of this device
     */
    String getDeviceId() {
        return deviceId;
    }

    /**
     * Add all devices that are currently paired with this device
     */
    void addPairedDevices() {
        // Get the paired devices' list
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                if (!pairedDevicesList.contains(device)) {
                    pairedDevicesList.add(device);
                }
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
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
    }

    /**
     * Get the UUIDs list used for setting up a bluetooth connection
     *
     * @return the UUID list
     */
    List<UUID> getUuidsList() {
        return uuidsList;
    }

    /**
     * Encode the message to be sent
     *
     * @param message the message to be encoded
     * @return the encoded message
     */
    private byte[] encodeMessage(EcologyMessage message) {
        try {
            return messageEncoder.encode(message);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    /**
     * Write data to be published
     *
     * @param bluetoothSocketReadWriter thread responsible for data read and write
     * @param encodedMessage            the encoded message ready to be sent
     */
    private void writeData(BluetoothSocketReadWriter bluetoothSocketReadWriter,
                           byte[] encodedMessage) {
        if (bluetoothSocketReadWriter != null) {
            // Write length of the data first
            bluetoothSocketReadWriter.writeInt(encodedMessage.length);

            // Write the byte data
            bluetoothSocketReadWriter.writeData(encodedMessage);
        }
    }

    public abstract void setupBluetoothConnection();

    public abstract Collection<BluetoothSocketReadWriter> getBluetoothSocketReadWriterList(
            Integer targetType, List<String> targets);

    public abstract void onDeviceConnected(Message msg);

    public abstract void onDeviceDisconnected(Message msg);

    public abstract void onConnectorMessage(Message msg, EcologyMessage messageData);

    public abstract void onBluetoothOff();

    public abstract void onBluetoothOutOfRange();
}
