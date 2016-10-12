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
public class SocketReadWriter implements Runnable {
    private static final String TAG = SocketReadWriter.class.getSimpleName();

    private static final int END_OF_FILE = -1;
    private Handler handler;
    private Socket socket = null;
    private DataOutputStream outputStream;

    public SocketReadWriter(Socket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
    }

    // This method is called when the device is disconnected from ecology
    public void onInterrupt(){
        // To indicate that the device is disconnected from ecology
        if(!socket.isClosed()) {
            writeInt(END_OF_FILE);
        }
    }

    @Override
    public void run() {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            outputStream = new DataOutputStream(socket.getOutputStream());

            Log.i(TAG, "Socket read/writer started");
            handler.obtainMessage(Settings.MY_HANDLE, this).sendToTarget();

            while (true) {
                try {
                    int toRead = inputStream.readInt();
                    int currentRead = 0;

                    // This indicates that the other device is disconnected from ecology
                    if(toRead == END_OF_FILE){
                        break;
                    }

                    while (currentRead < toRead) {
                        byte[] dataBuffer = new byte[toRead];

                        currentRead += inputStream.read(dataBuffer, currentRead, toRead - currentRead);
                        Log.i(TAG, "buffer " + Arrays.toString(dataBuffer));

                        handler.obtainMessage(Settings.MESSAGE_READ, dataBuffer).sendToTarget();
                    }
                } catch (EOFException e) {
                    Log.i(TAG, "EOFException");
                    break;
                } catch (IOException e) {
                    Log.e(TAG, "Exception during read", e);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                Log.i(TAG, "socket close");

                handler.obtainMessage(Settings.SOCKET_CLOSE, null).sendToTarget();
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

    public void writeInt(int length) {
        try {
            outputStream.writeInt(length);
        } catch (IOException e) {
            Log.e(TAG, "Exception during write int", e);
        }
    }
}
