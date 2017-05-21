package kosta.iot.mammoth.drone.wifi;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import kosta.iot.mammoth.drone.P;
import kosta.iot.mammoth.drone.tools.PIDController;


/**
 * Created by kosta on 2016-06-13.
 */
public class Receiver extends Thread {
    private Socket socket;
    public static int data[];
    private ReceiverCallback cb = null;

    public Receiver(Socket socket, ReceiverCallback cb) {
        this.socket = socket;
        this.cb = cb;

        data = new int[4];
    }

    @Override
    public void run() {
        BufferedReader networkReader = null;
        String strBuf = null;
        int i;

        try {
            networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Log.d(P.TAG, "Receiver run!!!");
            while (!Thread.currentThread().isInterrupted()) {
                strBuf = networkReader.readLine();
                if (strBuf != null)
                    //getXY(strBuf);
                    getXY_test(strBuf);
                else
                    break;

                if(!socket.isConnected()) {
                    socket.close();
                    break;
                }

                Thread.sleep(10);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(P.TAG, "IOException:" + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            Log.d(P.TAG, "finally :");
            if (networkReader != null) {
                try {
                    networkReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(P.TAG, "finally : networkReader");
            }
            if (socket != null) {
                try {
                    socket.shutdownInput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(P.TAG, "finally : socket");
            }
        }
        Log.d(P.TAG, "리시버 종료!!!");
    }

    private void getXY(String str) {
        String buf = "";
        int cnt = 0, size = str.length();
        char ch;

        if (str == null)
            return;

        for (int i = 0; i < size; i++) {
            ch = str.charAt(i);
            if (ch == ',') {
                data[cnt++] = Integer.valueOf(buf);
                buf = "";
            } else {
                buf += ch;
                if (i == size - 1)
                    data[cnt++] = Integer.valueOf(buf);
            }
        }

        if (cnt < 3)
            return;

        if(this.cb != null)
            this.cb.joystickReadData(data[0], data[1], data[2], data[3]);
    }

    private void getXY_test(String str) {

        Log.e("getXY_test",str);

        if (str == null)
            return;

        String buf = "";
        int cnt = 0, size = str.length();
        char ch;

        if (str == null)
            return;

        for (int i = 0; i < size; i++) {
            ch = str.charAt(i);
            if (ch == ',') {
                Log.e("cnt!!",""+buf);

                if(cnt == 0) {
                    data[0] = Integer.valueOf(buf);
                }
                else if(cnt == 1) {
                    data[1] = Integer.valueOf(buf);
                }
                if(cnt == 2) {
                    data[2] = Integer.valueOf(buf);
                    data[3] = 0;
                }
                else if(cnt == 3) {
                    PIDController.Kp_r = Double.valueOf(buf);
                }
                else if(cnt == 4) {
                    PIDController.Ki = Double.valueOf(buf);
                }
                else if(cnt == 5) {
                    PIDController.Kd = Double.valueOf(buf);
                }
                else if(cnt == 6){
                    PIDController.Kp_p = Double.valueOf(buf);
                }
                cnt++;
                buf = "";
            } else {
                buf += ch;
            }
        }

        PIDController.Kt= Double.valueOf(buf);

        if(this.cb != null)
            this.cb.joystickReadData(data[0], data[1], data[2], data[3]);
    }
}
