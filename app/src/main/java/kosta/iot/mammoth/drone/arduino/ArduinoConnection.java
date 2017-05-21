package kosta.iot.mammoth.drone.arduino;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.CdcAcmSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialDriver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import kosta.iot.mammoth.drone.tools.data.MotorData;

/**
 * Created by kosta on 2016-06-21.
 */
public class ArduinoConnection {
    private final int BaudRate = 115200;

    public static final int FIND = 0;
    public static final int CONNECTED = 1;
    public static final int DISCONNECTED = 2;

    private boolean isConnected = false;
    private Context mContext;
    private UsbDevice mDevice = null;
    private UsbSerialDriver mDriver = null;

    private char dataBit[];

    private ArduinoConnectionCallback cb = null;

    public ArduinoConnection(Context context, ArduinoConnectionCallback cb) {
        this.mContext = context;
        this.cb = cb;
        dataBit = new char[8];
    }

    public void init() {
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();

        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            //아두이노 찾기
            if (device.getVendorId() == 10755) {
                isConnected = true;
                mDevice = device;
                //Toast.makeText(mContext, "found arduino :" + device.getDeviceName() + "\n", Toast.LENGTH_SHORT).show();
                if(this.cb != null)
                    this.cb.onRecevice(ArduinoConnection.FIND);
            }
        }

        if (mDevice != null) {
            // mDriver = UsbSerialProber.acquire(manager, mDevice);
            try {
                UsbDeviceConnection conn = manager.openDevice(mDevice);
                mDriver = new CdcAcmSerialDriver(mDevice, conn);

                if (mDriver != null) {

                    mDriver.open();
                    mDriver.setBaudRate(BaudRate);

                    if(this.cb != null)
                        this.cb.onRecevice(ArduinoConnection.CONNECTED);
                }
            } catch (Exception e) {
               // Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                // txt.append("Fatal Error : User has not given permission to current device. \n");
                e.printStackTrace();
            }
        }
    }

    public boolean connectCheck() {
        return isConnected;
    }

    public void sendSignalARP(MotorData mData) {
        /*makeBitData(mData.m1, 0, 1);
        makeBitData(mData.m2, 2, 3);
        makeBitData(mData.m3, 4, 5);
        makeBitData(mData.m4, 6, 7);

        String data = String.valueOf(dataBit);
*/
        String data = mData.m1 + "," + mData.m2 + "," + mData.m3 + "," + mData.m4 + "\n";
        try {
            if (isConnected && mDriver != null) {
                 mDriver.write(data.getBytes(), 1000);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //정수값 2byte화
    private void makeBitData(int target, int upBit, int downBit){
        int cnt = 0;
        char temp;

        dataBit[upBit] = 0;
        dataBit[downBit] = 0;

        while(cnt < 16){
            if(cnt < 8){
                dataBit[downBit] = (char) (dataBit[downBit] << 1);
                dataBit[downBit] |= (target & 1);
            }
            else{
                dataBit[upBit] = (char) (dataBit[upBit] << 1);
                dataBit[upBit] |= (target & 1);
            }
            target >>= 1;
            cnt++;
        }
    }

    private int recData(String data, int upBit, int downBit){
        int result = 0;
        int cnt = 0;
        char d1 = data.charAt(upBit);
        char d2 = data.charAt(downBit);
        while(cnt < 16){
            result <<= 1;

            if(cnt < 8){
                if(d1 % 2 == 1)
                    result |= 1;

                d1 >>= 1;
            }
            else{
                if(d2 % 2 == 1)
                    result |= 1;

                d2 >>= 1;
            }

            cnt++;
        }

        Log.d("mammoth", "=" + result);
        return result;
    }

    public void sendSignalARP_(int mData) {
        String data = mData + "," + mData + "," +
                mData + "," + mData + '\n';

        try {
            if (isConnected && mDriver != null) {
                mDriver.write(data.getBytes(), 1000);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanArduino() {
        isConnected = false;

        try {
            if (mDriver != null)
                mDriver.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UsbSerialDriver getConnectedDriver(){
        return this.mDriver;
    }

    public String getConnectedDeviceName(){
        if(mDevice != null)
            return mDevice.getDeviceName();

        return null;
    }
}