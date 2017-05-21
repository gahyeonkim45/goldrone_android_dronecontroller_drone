package kosta.iot.mammoth.drone.wifi;

/**
 * Created by kosta on 2016-06-27.
 */
public abstract class WifiServerBRCallback {
    abstract public void brReceiver(int action, String msg);
}
