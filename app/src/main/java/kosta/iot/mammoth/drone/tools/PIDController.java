package kosta.iot.mammoth.drone.tools;

import android.util.Log;
import android.widget.TextView;

import kosta.iot.mammoth.drone.sensor.data.ARPData;
import kosta.iot.mammoth.drone.tools.data.MotorData;

/**
 * Created by kosta on 2016-06-28.
 */
public class PIDController {

    public static double Kp_r = 0;
    public static double Kp_p = 0;

    public static double Ki = 0;
    public static double Kd = 0;
    public static double Kt = 0;
    //75~78

    //roll PID 寃뚯씤
    private final double rollKp = 1;
    private final double rollKi = 1;
    private final double rollKd = 1;

    //roll PID 寃뚯씤
    private final double pitchKp = 173;
    private final double pitchKi = 72;
    private final double pitchKd = 203;

    private final int MOTOR_MAX = 179; //紐⑦꽣 理쒕? 異쒕젰 蹂댁젙
    private ARPData currentState;  //?꾩옱媛곷룄
    private ARPData aimState;   //紐⑺몴媛곷룄

    //2以?P?쒖뼱??蹂?붾웾 ???
    private AverageArray pErrorAngle_roll;
    private AverageArray pErrorAngle_pitch;

    //I?쒖뼱??蹂?붾웾 ???
    private AverageArray pIAngle_roll;

    //D?쒖뼱???듦?媛????
    private AverageArray prevAngle_roll;
    private AverageArray prevAngle_pitch;

    //I?쒖뼱??踰꾪띁
    private double bufErrRoll = 0;
    private double bufErrPitch = 0;

    private double prevErrorRoll = 0;

    private int throttle;  //紐⑦꽣 異쒕젰 (?ъ슜???낅젰)

    public PIDController() {
        this.currentState = new ARPData();
        this.aimState = new ARPData();

        this.pErrorAngle_roll = new AverageArray(5);
        this.pIAngle_roll = new AverageArray(150);
        this.prevAngle_roll = new AverageArray(10);

        this.prevAngle_pitch = new AverageArray(5);
        this.pErrorAngle_pitch = new AverageArray(5);

        this.throttle = 0;
    }

    public void setCurrentState(ARPData data) {
        this.currentState = data;
    }

    public void setAimState(ARPData data, int throttle) {
        this.aimState = data;
        this.throttle = throttle;
    }

    private int test = 0;
    MotorData data;

    public MotorData PIDProcess(final TextView info) {
        data = new MotorData(this.throttle*5);
        double error_roll = aimState.roll - currentState.roll;//aimState.pitch + this.pErrorAngle_pitch.getAverage();
        double control_roll = getRollPID(error_roll);

        if (error_roll > 0) {
            data.m1 += (Math.abs((int)(control_roll)));
            data.m2 += (Math.abs((int)(control_roll)));
        } else if (error_roll < 0) {
            data.m3 += (Math.abs((int)(control_roll)));
            data.m4 += (Math.abs((int)(control_roll)));
        }


    //pitch PID control
        double error_pitch = aimState.pitch - currentState.pitch;//aimState.pitch + this.pErrorAngle_pitch.getAverage();
        double control_pitch = getPitchPID(error_pitch);
//        control = 0;
        if (error_pitch > 0) {
            data.m1 += (Math.abs((int)(control_pitch)));
            data.m4 += (Math.abs((int)(control_pitch)));
        } else if (error_pitch < 0) {
            data.m2 += (Math.abs((int)(control_pitch)));
            data.m3 += (Math.abs((int)(control_pitch)));
        }

        //?댁쟾?듦? ???
        this.prevAngle_pitch.add(currentState.pitch);

        info.post(new Runnable() {
            @Override
            public void run() {
                info.setText("motor1 : " + data.m1 +
                        "\nmotor2 : " + data.m2 +
                        "\nmotor3 : " + data.m3 +
                        "\nmotor4 : " + data.m4);
            }
        });
        return data;
    }

    private double getRollPID(double error){
        double P, I, D;   // p?? i?? d??
        double control;

        //22
//        P = error * error * (Kp/500);//error * (Kp * 0.01);//P?곗궛
//        if(error < 0)
//            P *= -1;


        Log.e("getRollPID",""+Kp_r);

        P = error * (Kp_r * 0.01);
        bufErrRoll += (error * 0.1);
        I = (Ki/1000) * bufErrRoll;
        D = (Kd *0.0001f) * (error - prevErrorRoll) / 0.1;  //D?곗궛
        control = (int) (P + I + D);

        prevErrorRoll = error;

        return control;
    }

    private double getPitchPID(double error){
        double P, I, D;   // p?? i?? d??
        double control;


        Log.e("getPitchPID",""+Kp_p);

        this.pErrorAngle_roll.add(aimState.roll - currentState.roll);


        P = error * (Kp_p / 100.0f);  //P?곗궛
        bufErrPitch += (error / 10);
        I = (Ki / 2000.0f) * bufErrPitch; //I?곗궛
        D = (Kd / 20.0f) * (this.prevAngle_pitch.getRate());  //D?곗궛
        control = (int) (P + I + D);

        return control;
    }
}
