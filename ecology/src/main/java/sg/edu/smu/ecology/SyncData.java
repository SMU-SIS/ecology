package sg.edu.smu.ecology;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by anurooppv on 29/11/2016.
 */

/**
 * This class saves the sync data that need to be synced across the connected devices in the ecology
 */
class SyncData {
    private static final String TAG = SyncData.class.getSimpleName();
    /**
     * To store the sync data as a key value pair
     */
    private Map<Object, Object> dataSyncValues = new HashMap<>();

    /**
     * Sets the sync data value
     *
     * @param key   the key paired to the sync data
     * @param value the data
     */
    void setDataSyncValue(Object key, Object value) {
        dataSyncValues.put(key, value);
    }

    /**
     * Get the sync data
     *
     * @param key the key paired to the sync data
     * @return the corresponding sync data
     */
    Object getDataSyncValue(Object key) {
        return dataSyncValues.get(key);
    }
}
