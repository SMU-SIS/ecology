package sg.edu.smu.ecology;

/**
 * Created by anurooppv on 1/3/2017.
 */

import android.os.Handler;
import android.os.HandlerThread;

/**
 * The looper associated with the ecology
 */
class EcologyLooper extends HandlerThread {
    private Handler handler;

    EcologyLooper(String name) {
        super(name);
    }

    /**
     * Prepare the ecology looper handler
     */
    void prepareHandler() {
        // Create a handler to handle the message queue
        handler = new Handler(getLooper());
    }

    /**
     * Get the handler associated with the ecology looper
     *
     * @return the handler associated with the ecology looper
     */
    Handler getHandler() {
        return handler;
    }
}