package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by tnnguyen on 28/4/16.
 */
public class MemberSocketHandler extends Thread {
    private static final String TAG = MemberSocketHandler.class.getSimpleName();

    private Handler handler;
    private InetAddress address;
    private SocketCreator socketCreator;

    public MemberSocketHandler(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.setReuseAddress(true);
            socket.bind(null);
            Log.d(TAG, "MemberSocketHandler run EventBroadcaster ");
            socket.connect(new InetSocketAddress(address.getHostAddress(), Settings.SERVER_PORT), Settings.TIME_OUT);
            socketCreator = new SocketCreator(socket, handler);
            new Thread(socketCreator).start();
        } catch (IOException e) {
            e.printStackTrace();
            try {
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return;
        }
    }
}
