package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class SocketCreator implements Runnable {
    private static final String TAG = SocketCreator.class.getSimpleName();

    private Handler handler;
    private Socket socket = null;
    private DataOutputStream outputStream;

    public SocketCreator(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            int BUFFER_SIZE = 1024;
            byte[] buffer = new byte[BUFFER_SIZE];
            Log.i(TAG, "EventBroadcaster run");
            handler.obtainMessage(Settings.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    inputStream.readFully(buffer);
                    handler.obtainMessage(Settings.MESSAGE_READ,buffer).sendToTarget();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (EOFException e) {
                    break;
                }catch (IOException e) {
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
