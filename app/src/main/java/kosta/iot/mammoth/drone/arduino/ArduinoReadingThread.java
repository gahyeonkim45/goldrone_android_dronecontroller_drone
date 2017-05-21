package kosta.iot.mammoth.drone.arduino;

import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.io.IOException;

/**
 * Created by kosta on 2016-06-27.
 */
public class ArduinoReadingThread extends Thread {
    private byte[] data = new byte[24];
    private String buffer;
    private boolean isRun = true;
    private UsbSerialDriver mDriver;

    private ArduinoReadingCallback readingCallback = null;

    public ArduinoReadingThread(UsbSerialDriver mDriver){
        this.mDriver = mDriver;
    }

    public void stopThread(){
        this.isRun = false;
    }

    public void setReadingCallBack(ArduinoReadingCallback cb){
        this.readingCallback = cb;
    }

    @Override
    public void run() {
        while (isRun) {
            try {
                int numBytesRead = mDriver.read(data, 1000);

                Thread.sleep(100);

                if (numBytesRead >= 0) {
                    String buf = new String(data);

                    if (buf.substring(0, 2).equals("@:")) {
                        buffer = buf.substring(2);

                        if(readingCallback != null)
                            readingCallback.readData(buffer);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
