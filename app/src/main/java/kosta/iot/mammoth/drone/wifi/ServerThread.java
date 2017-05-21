package kosta.iot.mammoth.drone.wifi;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import kosta.iot.mammoth.drone.P;


/**
 * Created by kosta on 2016-06-08.
 */
public class ServerThread extends Thread {
    private Socket prevSocket = null;
    private ReceiverCallback cb = null;
    private boolean isRun = true;

    public ServerThread(ReceiverCallback cb){
        this.cb = cb;
    }

    @Override
    public void run() {
        ServerSocket welcomeSocket = null;
        Socket socket = null;
        Receiver receiver = null;

        try {
            welcomeSocket = new ServerSocket(P.PORT);
          //  welcomeSocket.setReuseAddress(true);
//            welcomeSocket.bind(new InetSocketAddress(P.PORT));

            while (isRun) {
                try {
                    socket = welcomeSocket.accept();

                    if (socket != null){//socket.isConnected()) {
                        if (receiver != null)
                            receiver.interrupt();

                        closePrevSocket();
                        receiver = new Receiver(socket, this.cb);
                        receiver.start();
                        prevSocket = socket;
                        Log.d(P.TAG, "Receiver start!!!");
                    }
                }  catch (IOException e) {
                    Log.d(P.TAG, "소켓 accept에러!!!");
                    e.printStackTrace();
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
            Log.d("test", "서버 소켓 에러!!!");
        } finally {
            Log.d(P.TAG, "finally ");
            if (welcomeSocket != null) {
                Log.d(P.TAG, "finally2 ");
                try {
                    if (!welcomeSocket.isClosed())
                        welcomeSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d(P.TAG, "finally catch");
                }
                Log.d(P.TAG, "finally : welcomeSocket.close");
            }

            if (receiver != null)
                receiver.interrupt();
        }
        Log.d(P.TAG, "스레드 종료 ");
    }

    public void closePrevSocket() {
        if (prevSocket != null)
            try {
                prevSocket.close();
                prevSocket=null;
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void stopThread(){
        isRun = false;
        closePrevSocket();
    }
}
