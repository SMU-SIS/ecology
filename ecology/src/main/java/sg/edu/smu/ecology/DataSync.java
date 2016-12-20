package sg.edu.smu.ecology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anurooppv on 29/11/2016.
 */

/**
 * This class saves the sync data that need to be synced across the connected devices in the ecology
 */
public class DataSync {
    private static final String TAG = DataSync.class.getSimpleName();
    // Notified when the data has changed.
    private final SyncDataChangeListener dataChangeListener;
    /**
     * To store the sync data as a key value pair
     */
    private Map<Object, Object> dataSyncValues = new HashMap<>();
    // Notify when a message needs to be forwarded to the corresponding DataSyncs in the other
    // devices of the ecology.
    private Connector connector;

    DataSync(Connector connector, SyncDataChangeListener dataChangeListener) {
        this.connector = connector;
        this.dataChangeListener = dataChangeListener;
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
        connector.onMessage(new ArrayList<>(Arrays.asList(key, value)));
        dataChangeListener.onDataUpdate(key, value, oldValue);
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
    void onMessage(List<Object> message) {
        Object key = message.get(0);
        Object oldValue = dataSyncValues.get(key);
        Object newValue = message.get(1);

        dataSyncValues.put(key, newValue);
        dataChangeListener.onDataUpdate(key, newValue, oldValue);
    }

    interface Connector {
        void onMessage(List<Object> message);
    }

    interface SyncDataChangeListener {
        void onDataUpdate(Object dataId, Object newValue, Object oldValue);
    }
}
