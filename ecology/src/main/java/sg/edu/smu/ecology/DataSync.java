package sg.edu.smu.ecology;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by anurooppv on 29/11/2016.
 */

/**
 * This class can be used to synchronize any data across the connected devices in the ecology.
 */
public class DataSync {
    private static final String TAG = DataSync.class.getSimpleName();
    /**
     * Routing ID for data sync messages
     */
    private final static int DATA_SYNC_MESSAGE = 0;
    /**
     * Routing ID for data sync message request
     */
    private final static int DATA_SYNC_REQUEST = 1;
    /**
     * Routing ID for data sync message response
     */
    private final static int DATA_SYNC_RESPONSE = 2;

    /**
     * Notify when the sync data has changed.
     */
    private final SyncDataChangeListener dataChangeListener;
    /**
     * Notify when a message needs to be forwarded to the corresponding DataSyncs in the other
     * devices of the ecology.
     */
    private final Connector connector;
    /**
     * To store the sync data as a key value pair
     */
    private Map<Object, Object> dataSyncValues = new ConcurrentHashMap<>();
    /**
     * Whether this device is the sync data reference or not
     */
    private boolean isReference;

    /**
     * Ecology instance
     */
    private Ecology ecology;

    /**
     * Whether data is currently synchronized with the data reference or not.
     */
    private boolean isSynchronized = false;

    /**
     * Creates a new Data Sync Object that can be used to sync data across the connected devices
     * part of the ecology.
     *
     * @param connector          to notify when a message needs to be forwarded to other devices
     * @param dataChangeListener to notify when there is a change in sync data value
     * @param isReference        if this device should be the data reference or not
     * @param ecology            the ecology that this device is part of
     */
    DataSync(Connector connector, SyncDataChangeListener dataChangeListener, boolean isReference,
             Ecology ecology) {
        this.connector = connector;
        this.dataChangeListener = dataChangeListener;
        this.isReference = isReference;
        this.ecology = ecology;
    }

    /**
     * Set the sync data value
     *
     * @param key   the key paired to the sync data
     * @param value the new data
     */
    public void setData(final Object key, final Object value) {
        final Object oldValue = dataSyncValues.get(key);
        dataSyncValues.put(key, value);
        // Check if old value is not same as the new value
        if (oldValue != value) {
            ecology.getHandler().post(new Runnable() {
                @Override
                public void run() {
                    EcologyMessage message = new EcologyMessage(Arrays.asList(key, value,
                            DATA_SYNC_MESSAGE));
                    message.setTargetType(EcologyMessage.TARGET_TYPE_BROADCAST);
                    connector.onMessage(message);
                    dataChangeListener.onDataUpdate(key, value, oldValue);
                }
            });
        }
    }

    /**
     * Get the sync data value corresponding to the key.
     *
     * @param key the key paired to the sync data
     * @return the corresponding sync data value
     */
    public Object getData(Object key) {
        return dataSyncValues.get(key);
    }

    /**
     * Handle message from the corresponding DataSync instances in the other devices of the ecology.
     *
     * @param message the content of the message
     */
    void onMessage(EcologyMessage message) {
        Integer messageIndicator = (Integer) message.fetchArgument();

        switch (messageIndicator) {
            case DATA_SYNC_MESSAGE:
                onDataSyncMessage(message);
                break;

            case DATA_SYNC_REQUEST:
                sendRefSyncData(message.getSource());
                break;

            case DATA_SYNC_RESPONSE:
                saveRefSyncData((Map<?, ?>) message.fetchArgument());
                break;
        }
    }

    /**
     * This method is called when this device is connected to the data reference device.
     */
    void onConnected() {
        // Once the connection is established, data synchronization is requested.
        requestDataSynchronization();
    }

    /**
     * This method is called when the device is disconnected from the data reference device
     */
    void onDisconnected() {
        isSynchronized = false;
    }

    /**
     * To request for the current sync data from the reference
     */
    private void requestDataSynchronization() {
        if (!isReference) {
            EcologyMessage message = new EcologyMessage(Collections.<Object>singletonList(
                    DATA_SYNC_REQUEST));
            message.setTargetType(EcologyMessage.TARGET_TYPE_SERVER);
            connector.onMessage(message);
        }
    }

    /**
     * To check if the data sync is currently synchronized with the data reference or not
     *
     * @return true if data sync is synchronized with the data reference
     */
    boolean isSynchronized() {
        return isSynchronized;
    }

    /**
     * Clear the current sync data
     */
    void clear() {
        for (Object key : dataSyncValues.keySet()) {
            Object oldValue = dataSyncValues.get(key);
            dataChangeListener.onDataUpdate(key, null, oldValue);
        }
        dataSyncValues.clear();
    }

    /**
     * Send the current reference sync data when a request for the same is received
     *
     * @param deviceId the device id of the requester
     */
    private void sendRefSyncData(String deviceId) {
        EcologyMessage message = new EcologyMessage(Arrays.asList(dataSyncValues,
                DATA_SYNC_RESPONSE));
        message.setTargetType(EcologyMessage.TARGET_TYPE_SPECIFIC);
        message.setTargets(Collections.singletonList(deviceId));
        connector.onMessage(message);
    }

    /**
     * To save the current reference sync data received from the reference.
     *
     * @param refSyncData the reference sync data received from the reference
     */
    private void saveRefSyncData(Map<?, ?> refSyncData) {
        for (Object key : refSyncData.keySet()) {
            Object newValue = refSyncData.get(key);
            Object oldValue = null;

            // Check if the key in initial sync data is already present in data sync
            if (dataSyncValues.containsKey(key)) {
                oldValue = dataSyncValues.get(key);
            }

            dataSyncValues.put(key, newValue);
            dataChangeListener.onDataUpdate(key, newValue, oldValue);
        }
        // Remove the data corresponding to the keys that are not present in the initial data
        // sync
        Iterator<Map.Entry<Object, Object>> iterator = dataSyncValues.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Object, Object> entry = iterator.next();
            if (!refSyncData.containsKey(entry.getKey())) {
                Object key = entry.getKey();
                Object oldValue = dataSyncValues.get(key);
                iterator.remove();
                dataChangeListener.onDataUpdate(key, null, oldValue);
            }
        }

        isSynchronized = true;
    }

    /**
     * When a data sync message is received
     *
     * @param message the received message
     */
    private void onDataSyncMessage(EcologyMessage message) {
        Object newValue = message.fetchArgument();
        Object key = message.fetchArgument();
        Object oldValue = dataSyncValues.get(key);

        dataSyncValues.put(key, newValue);
        dataChangeListener.onDataUpdate(key, newValue, oldValue);
    }

    interface Connector {
        void onMessage(EcologyMessage message);
    }

    interface SyncDataChangeListener {
        void onDataUpdate(Object dataId, Object newValue, Object oldValue);
    }
}
