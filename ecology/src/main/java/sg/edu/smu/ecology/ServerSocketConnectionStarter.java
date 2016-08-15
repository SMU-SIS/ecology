package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by tnnguyen on 28/4/16.
 */
public class ServerSocketConnectionStarter extends Thread {

    private static final String TAG = ServerSocketConnectionStarter.class.getSimpleName();

    ServerSocket socket = null;
    private final int THREAD_COUNT = 10;
    private Handler handler;

    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());

    public ServerSocketConnectionStarter(Handler handler) throws IOException {
        try {
            socket = new ServerSocket(Settings.SERVER_PORT);
            this.handler = handler;
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                Log.d(TAG, "connection attempt");
                pool.execute(new SocketReadWriter(socket.accept(), handler));
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) {
                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
        Log.d(TAG, "done");
    }
}
