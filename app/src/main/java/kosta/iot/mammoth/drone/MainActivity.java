package kosta.iot.mammoth.drone;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import kosta.iot.mammoth.drone.arduino.ArduinoConnection;
import kosta.iot.mammoth.drone.arduino.ArduinoConnectionCallback;
import kosta.iot.mammoth.drone.arduino.ArduinoReadingCallback;
import kosta.iot.mammoth.drone.arduino.ArduinoReadingThread;
import kosta.iot.mammoth.drone.sensor.SensorDataCallback;
import kosta.iot.mammoth.drone.sensor.SensorManager;
import kosta.iot.mammoth.drone.sensor.data.ARPData;
import kosta.iot.mammoth.drone.tools.PIDController;
import kosta.iot.mammoth.drone.tools.data.MotorData;
import kosta.iot.mammoth.drone.wifi.ReceiverCallback;
import kosta.iot.mammoth.drone.wifi.ServerThread;
import kosta.iot.mammoth.drone.wifi.WiFiServerBroadcastReceiver;
import kosta.iot.mammoth.drone.wifi.WifiServerBRCallback;

public class MainActivity extends AppCompatActivity {
    private WifiP2pManager wifiManager;
    private WifiManager wifi;
    private WifiP2pDnsSdServiceRequest serviceRequest;
    private WifiP2pManager.Channel wifichannel;
    private BroadcastReceiver wifiServerReceiver;
    private WifiP2pServiceInfo serviceInfo;
    private IntentFilter wifiServerReceiverIntentFilter;
    private ServerThread serverThread;

    private TextView infoView;
    private TextView joyDataView;
    private Button socketBtn;

    private ArduinoConnection arduinoConnection;
    private SensorManager sensorManager;

    private TextView arduStateTextView = null;
    private TextView azimuthTextView = null;
    private TextView pitchTextView = null;
    private TextView rollTextView = null;
    private TextView receviceView = null;
    private TextView pidView = null;

    private ArduinoReadingThread readingThread;
    private PIDController mPIDController;
    private ARPData aimData;
    private boolean isRunApp = true;

    public int keyData[];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        aimData = new ARPData();
        mPIDController = new PIDController();
        keyData = new int[4];

        arduinoInit();
        serverInit();
        createServerView();
    }

    private void createServerView(){
        this.infoView = (TextView) this.findViewById(R.id.server_info_tv);
        this.joyDataView = (TextView) this.findViewById(R.id.joy_tv);
        this.socketBtn = (Button) this.findViewById(R.id.socket_btn);

        this.socketBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSocketServer();
            }
        });
    }

    private void serverInit(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(!wifi.isWifiEnabled()){
            if(wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED)
                wifi.setWifiEnabled(true);
        }

        wifiManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifichannel = wifiManager.initialize(this, getMainLooper(), null);
        wifiServerReceiver = new WiFiServerBroadcastReceiver(wifiManager, wifichannel, new WifiServerBRCallback(){

            @Override
            public void brReceiver(int action, String msg) {
                switch(action){
                    case WiFiServerBroadcastReceiver.ACT_MSG :
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        break;
                    case WiFiServerBroadcastReceiver.ACT_WIFI_CONNECTED:
                        startSocketServer();
                        break;
                    case WiFiServerBroadcastReceiver.ACT_WIFI_DISCONNECTED:
                        stopSocketThread();
                        wifiDisconnect();
                        break;
                }
            }
        });

        wifiServerReceiverIntentFilter = new IntentFilter();;
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifiServerReceiverIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        registerReceiver(wifiServerReceiver, wifiServerReceiverIntentFilter);
        serverCreate();
    }

    private void serverCreate(){
        Map record = new HashMap<String, String>();
        record.put("listenport", String.valueOf(P.PORT));
        record.put("available", "visible");

        serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_mammoth_", "_presence._tcp", record);
        //서비스 등록
        wifiManager.addLocalService(wifichannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                //ServerActivity.showMessage("addLocalService success");
                Log.d(P.TAG, "addLocalService success");
            }

            @Override
            public void onFailure(int reason) {
                //mCallback.onFailure(reason);
                Log.d(P.TAG, "addLocalService fail ::" + reason);
            }
        });

        //브로드캐스트 되는 서비스 정보 저장용
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override/* Callback includes: * fullDomain: full domain name: e.g "printer._ipp._tcp.local." * record: TXT record dta as a map of key/value pairs. * device: The device running the advertised service. */
            public void onDnsSdTxtRecordAvailable(
                    String fullDomain, Map record, WifiP2pDevice device) {
                Log.d(P.TAG, "DnsSdTxtRecord available -" + record.toString());
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener serviceResponseListener =
                new WifiP2pManager.DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {
                        Log.d(P.TAG, "service res available [" + instanceName + "/type:" + registrationType);    }
                };

        wifiManager.setDnsSdResponseListeners(wifichannel, serviceResponseListener, txtListener);

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();//서비스 검색 요청 추가
        wifiManager.addServiceRequest(wifichannel, serviceRequest, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(P.TAG, "sevice success");
            }

            @Override
            public void onFailure(int code) {
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                Log.d(P.TAG, "sevice fail :: " + code);
            }
        });

        //서비스 검색
        wifiManager.discoverServices(wifichannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFailure(int reason) {}
        });
    }

    private void arduinoInit(){
        azimuthTextView = (TextView) this.findViewById(R.id.azimuth_tv);
        pitchTextView = (TextView) this.findViewById(R.id.pitch_tv);
        rollTextView = (TextView) this.findViewById(R.id.roll_tv);
        arduStateTextView = (TextView) this.findViewById(R.id.adu_tv);
        pidView = (TextView) this.findViewById(R.id.pid_tv);

        receviceView = (TextView) this.findViewById(R.id.rece_tv);
        receviceView.setMovementMethod(new ScrollingMovementMethod());
        receviceView.setVerticalScrollBarEnabled(true);

        arduinoConnection = new ArduinoConnection(this, new ArduinoConnectionCallback(){
            @Override
            public void onRecevice(int action) {
                switch(action){
                    case ArduinoConnection.FIND :
                        arduStateTextView.setText("Connected device : " + arduinoConnection.getConnectedDeviceName());
                        break;
                    case ArduinoConnection.CONNECTED:
                        readingThread = new ArduinoReadingThread(arduinoConnection.getConnectedDriver());
                        readingThread.setReadingCallBack(new ArduinoReadingCallback() {
                            @Override
                            public void readData(final String str) {
                                receviceView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        receviceView.setText("Recevied : " + str);
                                        scroll();
                                    }
                                });
                            }
                        });
                        readingThread.start();
                        break;
                    case ArduinoConnection.DISCONNECTED : break;
                }
            }
        });
        arduinoConnection.init();

        sensorManager = new SensorManager(this);
        sensorManager.setSensorDataCallback(new SensorDataCallback() {
            @Override
            public void readSensorData(int azimuth, int pitch, int roll) {
                if(azimuthTextView != null)
                    azimuthTextView.setText("Azimuth : " + azimuth);
                if(pitchTextView != null)
                    pitchTextView.setText("Pitch : " + pitch);
                if(rollTextView != null)
                    rollTextView.setText("Roll : " + roll);

            }
        });

        new Thread(){
            @Override
            public void run() {
                while(isRunApp){
                    mPIDController.setCurrentState(sensorManager.getARPData());
                    MotorData mData = mPIDController.PIDProcess(pidView);
                    arduinoConnection.sendSignalARP(mData);
                    //arduinoConnection.sendSignalARP_(keyData[1]);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void scroll(){
        if(receviceView == null)
            return;

        if(receviceView.getLineCount() > 200)
            receviceView.setText(" ");

        int scrollamout = receviceView.getLayout().getLineTop(receviceView.getLineCount()) - receviceView.getHeight();
        if (scrollamout*2.5 > receviceView.getHeight())
            receviceView.scrollTo(0, scrollamout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerSensor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterSensor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduinoConnection.cleanArduino();
        if(readingThread != null)
        readingThread.stopThread();
        isRunApp = false;

        stopSocketThread();
        clearWifi();

        wifiServerReceiver.clearAbortBroadcast();
        unregisterReceiver(wifiServerReceiver);
    }

    private void clearWifi(){
        wifiManager.cancelConnect(wifichannel, null);
        wifiManager.clearServiceRequests(wifichannel, null);
        wifiManager.clearLocalServices(wifichannel, null);
        wifiManager.removeGroup(wifichannel, null);
        wifiManager.stopPeerDiscovery(wifichannel, null);
        wifiManager.removeLocalService(wifichannel,serviceInfo,null);
        wifiManager.removeServiceRequest(wifichannel,serviceRequest, null);
    }

    public void wifiDisconnect(){
        wifi.disconnect();
    }

    public void stopSocketThread(){
        if(serverThread != null) {
            setText("서버 연결 off..");
            serverThread.stopThread();
            serverThread = null;
        }
    }

    public void setText(String str){
        if(infoView != null)
            infoView.setText("Server state : " + str);
    }

    public void startSocketServer(){
        if(serverThread != null)
            stopSocketThread();

        setText("소켓 서버 on...");
        serverThread = new ServerThread(new ReceiverCallback(){
            @Override
            public void joystickReadData(final int lx, final int ly, final int rx, final int ry) {
                keyData[0] = lx;
                keyData[1] = ly;
                keyData[2] = rx;
                keyData[3] = ry;

                //수정요망
                //aimData.azimuth = lx;
                aimData.azimuth = 0;
                aimData.pitch = -(ly/10);
                aimData.roll = (int)PIDController.Kt;

                mPIDController.setAimState(aimData,rx);

                joyDataView.post(new Runnable() {
                    @Override
                    public void run() {
                        //joyDataView.setText(lx + "," + ly + "," + rx + "," + ry);
                        joyDataView.setText("keyData - > " + keyData[0] + ", " + keyData[1] + "\nthrottle : " +keyData[2] + "\n"
                                + PIDController.Kp_r+ ", " + PIDController.Ki+ ", " + PIDController.Kd);
                    }
                });
            }
        });
        serverThread.start();

    }
}
