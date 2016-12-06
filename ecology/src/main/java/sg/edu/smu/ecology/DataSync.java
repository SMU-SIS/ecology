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

    /**
     * To store the sync data as a key value pair
     */
    private Map<Object, Object> dataSyncValues = new HashMap<>();
    private Connector connector;

    private SyncDataChangeListener syncDataChangeListener = new SyncDataChangeListener() {
        @Override
        public void onDataUpdate(List<Object> message) {
            dataSyncValues.put(message.get(0), message.get(1));
        }
    };

    DataSync(Connector connector) {
        this.connector = connector;
    }

    SyncDataChangeListener getSyncDataChangeListener() {
        return syncDataChangeListener;
    }

    /**
     * Sets the sync data value
     *
     * @param key   the key paired to the sync data
     * @param value the data
     */
    public void setData(Object key, Object value) {
        dataSyncValues.put(key, value);
        connector.onMessage(new ArrayList<Object>(Arrays.asList(key, value)));
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

    interface Connector {
        void onMessage(List<Object> message);
    }

    interface SyncDataChangeListener {
        void onDataUpdate(List<Object> message);
    }

}
