package kosta.iot.mammoth.drone.tools.data;

/**
 * Created by kosta on 2016-06-28.
 */
public class MotorData {
    public int m1;
    public int m2;
    public int m3;
    public int m4;

    public MotorData(){
        m1 = 0;
        m2 = 0;
        m3 = 0;
        m4 = 0;
    }

    public MotorData(int throttle){
        m1 = throttle; //(int)((double)throttle*(1.0f + PIDController.Kp/300f));
        m2 = throttle; //(int)((double)throttle*(1.0f + PIDController.Ki/300f));
        m3 = throttle; //(int)((double)throttle*(1.0f + PIDController.Kd/300f));
        m4 = throttle; //(int)((double)throttle*(1.0f + PIDController.Kt/300f));

    }
}
