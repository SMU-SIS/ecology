package sg.edu.smu.ecology;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by anurooppv on 29/11/2016.
 */

/**
 * This class saves the sync data that need to be synced across the connected devices in the ecology
 */
public class DataSync {
    private static final String TAG = DataSync.class.getSimpleName();
    private final static int INITIAL_SYNC_MESSAGE_INDICATOR = 0;
    private final static int DATA_SYNC_MESSAGE_INDICATOR = 1;
    /**
     * Notified when the data has changed.
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
    private Map<Object, Object> dataSyncValues = new HashMap<>();
    /**
     * Whether this is the sync data reference or not
     */
    private boolean isDataSyncReference;

    /**
     * Whether data is currently synchronized with the data reference or not.
     */
    private boolean isSynchronized = false;

    DataSync(Connector connector, SyncDataChangeListener dataChangeListener,
             boolean isDataSyncReference) {
        this.connector = connector;
        this.dataChangeListener = dataChangeListener;
        this.isDataSyncReference = isDataSyncReference;
    }

    /**
     * To request for the current sync data from the reference
     */
    private void requestDataSynchronization() {
        if (!isDataSyncReference) {
            EcologyMessage message = new EcologyMessage(Collections.<Object>singletonList(
                    INITIAL_SYNC_MESSAGE_INDICATOR));
            message.setTargetType(EcologyMessage.TARGET_TYPE_SERVER);
            connector.onMessage(message);
        }
    }

    /**
     * Sets the sync data value
     *
     * @param key   the key paired to the sync data
     * @param value the new data
     */
    public void setData(Object key, Object value) {
        Object oldValue = dataSyncValues.get(key);
        dataSyncValues.put(key, value);
        // Check if old value is not same as the new value
        if (oldValue != value) {
            EcologyMessage message = new EcologyMessage(Arrays.asList(key, value,
                    DATA_SYNC_MESSAGE_INDICATOR));
            message.setTargetType(EcologyMessage.TARGET_TYPE_BROADCAST);
            connector.onMessage(message);
            dataChangeListener.onDataUpdate(key, value, oldValue);
        }
    }

    /**
     * Get the sync data
     *
     * @param key the key paired to the sync data
     * @return the corresponding sync data
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

        if (messageIndicator == DATA_SYNC_MESSAGE_INDICATOR) {
            onDataSyncMessage(message);
        } else if (messageIndicator == INITIAL_SYNC_MESSAGE_INDICATOR) {
            onInitialDataSyncMessage(message);
        }
    }

    /**
     * This method is called when the device is not connected to the data reference device
     */
    void onConnected(){
        requestDataSynchronization();
    }

    /**
     * This method is called when the device is disconnected from the data reference device
     */
    void onDisconnected(){
        isSynchronized = false;
    }

    /**
     * When the initial data sync message is received
     *
     * @param msg the received message
     */
    private void onInitialDataSyncMessage(EcologyMessage msg) {
        if (isDataSyncReference) {
            EcologyMessage message = new EcologyMessage(Arrays.asList(dataSyncValues,
                    INITIAL_SYNC_MESSAGE_INDICATOR));
            message.setTargetType(EcologyMessage.TARGET_TYPE_SPECIFIC);
            message.setTargets(Collections.singletonList(msg.getSource()));
            connector.onMessage(message);
        } else {
            saveInitialSyncData((Map<?, ?>) msg.fetchArgument());
        }
    }

    /**
     * To save the initial sync data received from the reference.
     *
     * @param syncData the initial sync data received from the reference
     */
    private void saveInitialSyncData(Map<?, ?> syncData) {
        dataSyncValues.clear();
        for (Object key : syncData.keySet()) {
            Object newValue = syncData.get(key);
            Object oldValue = dataSyncValues.get(key);
            dataSyncValues.put(key, newValue);
            dataChangeListener.onDataUpdate(key, newValue, oldValue);
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
