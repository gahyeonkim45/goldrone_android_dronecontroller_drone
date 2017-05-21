package kosta.iot.mammoth.drone.sensor;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import kosta.iot.mammoth.drone.sensor.data.ARPData;
import kosta.iot.mammoth.drone.tools.AverageArray;

/**
 * Created by kosta on 2016-06-23.
 */
public class
SensorManager implements SensorEventListener {
    public final int NO_TEXT_VIEW = 0;

    private Context context;

    private android.hardware.SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor mag;

    private ARPData data;

    float[] m_acc_data = null, m_mag_data = null;
    float[] m_rotation = new float[9];
    float[] m_result_data = new float[3];

    private AverageArray azimuthAvg, pitchAvg, rollAvg;
    private SensorDataCallback cb = null;

    public SensorManager(Context context) {
        this.context = context;
        this.data = new ARPData();

        mSensorManager = (android.hardware.SensorManager) this.context.getSystemService(Service.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        azimuthAvg = new AverageArray(50);
        pitchAvg = new AverageArray(50);
        rollAvg = new AverageArray(50);
    }

    public void setSensorDataCallback(SensorDataCallback cb){
        this.cb = cb;
    }

    public void registerSensor() {
        if (mSensorManager != null) {
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    android.hardware.SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    android.hardware.SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    public void unregisterSensor() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 가속 센서가 전달한 데이터인 경우
            // 수치 데이터를 복사한다.
            m_acc_data = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // 자기장 센서가 전달한 데이터인 경우
            // 수치 데이터를 복사한다.
            m_mag_data = event.values.clone();
        }

        // 데이터가 존재하는 경우
        if (m_acc_data != null && m_mag_data != null) {
            // 가속 데이터와 자기장 데이터로 회전 매트릭스를 얻는다.
            android.hardware.SensorManager.getRotationMatrix(m_rotation, null, m_acc_data, m_mag_data);
            // 회전 매트릭스로 방향 데이터를 얻는다.
            android.hardware.SensorManager.getOrientation(m_rotation, m_result_data);

            //azimuth
            m_result_data[0] = (float) Math.toDegrees(m_result_data[0]);
            if (m_result_data[0] < 0) m_result_data[0] += 360;

            azimuthAvg.add((int) m_result_data[0]);
            pitchAvg.add((int) Math.toDegrees(m_result_data[1]));
            rollAvg.add((int) Math.toDegrees(m_result_data[2]));

            data.azimuth = azimuthAvg.getAverage();
            data.pitch = pitchAvg.getAverage() * -1;
            data.roll = rollAvg.getAverage();

            if(cb != null)
                cb.readSensorData(data.azimuth, data.pitch, data.roll);
        }
    }

    public ARPData getARPData(){
        return this.data;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
