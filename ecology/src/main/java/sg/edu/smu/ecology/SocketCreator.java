package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

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

            Log.i(TAG, "EventBroadcaster run");
            handler.obtainMessage(Settings.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    int toRead = inputStream.readInt();
                    int currentRead = 0;

                    while(currentRead < toRead){
                        byte[] dataBuffer = new byte[toRead];

                        currentRead += inputStream.read(dataBuffer, currentRead, toRead - currentRead);
                        Log.i(TAG, "buffer "+ Arrays.toString(dataBuffer));

                        handler.obtainMessage(Settings.MESSAGE_READ, dataBuffer).sendToTarget();
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

    public void writeData(byte[] buffer) {
        try {
            outputStream.write(buffer);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write data", e);
        }
    }

    public void writeInt(int length){
        try {
            outputStream.writeInt(length);
        }catch (IOException e) {
            Log.e(TAG, "Exception during write int", e);
        }
    }
}
