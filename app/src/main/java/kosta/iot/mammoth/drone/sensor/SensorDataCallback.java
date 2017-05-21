package kosta.iot.mammoth.drone.sensor;

/**
 * Created by kosta on 2016-06-27.
 */
public abstract class SensorDataCallback {
    public abstract void readSensorData(int azimuth, int pitch, int roll);
}
