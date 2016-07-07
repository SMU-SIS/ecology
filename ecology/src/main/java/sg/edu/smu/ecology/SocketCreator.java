package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class SocketCreator implements Runnable {
    private static final String TAG = SocketCreator.class.getSimpleName();

    private Handler handler;
    private Socket socket = null;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final int BUFFER_SIZE = 150;

    public SocketCreator(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytes;
            Log.i(TAG, "EventBroadcaster run");
            handler.obtainMessage(Settings.MY_HANDLE, this).sendToTarget();
            while (true) {
                try {
                    Log.i(TAG, "buffer "+ Arrays.toString(buffer));
                    bytes = inputStream.read(buffer);
                    if (bytes == -1) {
                        break;
                    }
                    handler.obtainMessage(Settings.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "Exception during read", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void write(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write", e);
        }
    }
}
