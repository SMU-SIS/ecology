package sg.edu.smu.ecology;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by anurooppv on 1/8/2016.
 * This class is used to get Ecology instance required to connect and send messages to other devices
 * <p>
 * This service is started when the device is connected to the ecology and is stopped when the device
 * disconnects from ecology
 */
public class EcologyService extends Service {

    private static final String TAG = EcologyService.class.getSimpleName();
    private static Ecology ecology;

    // Initial connection to the ecology - returns the ecology instance
    public static Ecology connect(EcologyConfig config, Context context) {
        // Ecology can be only connected once
        if (ecology != null) {
            throw new EcologyAlreadyConnectedException("The ecology has already been connected.");
        }

        EcologyConnection ecologyConnection = new EcologyConnection();

        // Adds a core connector to the ecology connection.
        for (Connector coreConnector : config.getCoreConnectors()) {
            ecologyConnection.addCoreConnector(coreConnector);
        }

        // Adds a dependent connector to the ecology connection.
        for (Connector dependentConnector : config.getDependentConnectors()) {
            ecologyConnection.addDependentConnector(dependentConnector);
        }

        ecology = new Ecology(ecologyConnection);
        // Connect to the ecology
        ecology.connect(context);

        return ecology;
    }

    // Returns the ecology instance once the ecology is connected
    public static Ecology getEcology() {
        return ecology;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

    // Disconnects from the ecology
    public static void disconnect(Context context) {
        ecology.disconnect(context);
        ecology = null;
    }

    protected static class EcologyAlreadyConnectedException extends RuntimeException {
        public EcologyAlreadyConnectedException(String message) {
            super(message);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
