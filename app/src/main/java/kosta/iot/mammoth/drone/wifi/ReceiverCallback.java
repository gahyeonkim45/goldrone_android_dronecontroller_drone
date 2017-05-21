package kosta.iot.mammoth.drone.wifi;

/**
 * Created by kosta on 2016-06-27.
 */
public abstract class ReceiverCallback {
    abstract public void joystickReadData(int lx, int ly, int rx, int ry);
}
