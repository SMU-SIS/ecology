package sg.edu.smu.ecology;

import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by anurooppv on 29/11/2016.
 */

class SyncData {
    private Map<Object, Object> dataSyncValues = new HashMap<>();

    void setDataSyncValue(Object key, Object value){
        dataSyncValues.put(key, value);
    }

    Object getDataSyncValue(Object key){
        return dataSyncValues.get(key);
    }
}
