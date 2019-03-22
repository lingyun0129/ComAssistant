package com.bjw.ComAssistant;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bjw.bean.ComBean;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FirstLaunchActivity extends Activity implements View.OnClickListener, PermissionUtils.PermissionCallbacks {
    private EditText receive_at_cmd_disp;
    private Button btn_clear;
    private SerialControl ComA;
    private DispQueueThread DispQueue;
    private int iRecLines = 0;//����������
    //location
    private LocationManager mLocationManager;
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int REQUEST_PERMISSION_CODE = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_launch);
        receive_at_cmd_disp = (EditText) findViewById(R.id.et_rec_at_cmd);
        btn_clear = (Button) findViewById(R.id.btn_clear);
        btn_clear.setOnClickListener(this);
        DispQueue = new DispQueueThread();
        DispQueue.start();
        openSerialPort();
        //location
        if (!PermissionUtils.hasPermissions(this, permissions)) {
            PermissionUtils.requestPermissions(this, REQUEST_PERMISSION_CODE, permissions);
        } else {
            startLocate();
        }
    }

    private String getAvaliableProvider(LocationManager locationManager) {
        List<String> providersList = locationManager.getProviders(true);
        if (providersList.contains(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;//���綨λ
        } else if (providersList.contains(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;//GPS��λ
        } else {
            Toast.makeText(this, "No Pri", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    private Location getLastKnownLocation() {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return null;
            }
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void startLocate() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkProviderEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (gpsProviderEnabled || networkProviderEnabled) { //GPS�ѿ���
            /**
             * �󶨼���
             * ����1���豸����GPS_PROVIDER��NETWORK_PROVIDER���֣�ǰ����GPS,������GPRS�Լ�WIFI��λ
             * ����2��λ����Ϣ��������.��λ�Ǻ���
             * ����3��λ�ñ仯��С���룺��λ�þ���仯������ֵʱ��������λ����Ϣ
             * ����4������
             * ��ע������2��3���������3��Ϊ0�����Բ���3Ϊ׼������3Ϊ0����ͨ��ʱ������ʱ���£�����Ϊ0������ʱˢ��
             */
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
            //Location location = mLocationManager.getLastKnownLocation(getAvaliableProvider(mLocationManager));
            Location location = getLastKnownLocation();
            if (location == null) {
                return;
                //mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
            Log.e("cly", "��λ��ʽ��" + location.getProvider());
            Log.e("cly", "γ�ȣ�" + location.getLatitude());
            Log.e("cly", "���ȣ�" + location.getLongitude());
            Log.e("cly", "���Σ�" + location.getAltitude());
            Log.e("cly", "ʱ�䣺" + location.getTime());
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            Toast.makeText(this, "Open GPS please!", Toast.LENGTH_SHORT).show();
        }
    }

    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //λ����Ϣ�仯ʱ����
            Log.e("cly", "��λ��ʽ��" + location.getProvider());
            Log.e("cly", "γ�ȣ�" + location.getLatitude());
            Log.e("cly", "���ȣ�" + location.getLongitude());
            Log.e("cly", "���Σ�" + location.getAltitude());
            Log.e("cly", "ʱ�䣺" + location.getTime());
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //GPS״̬�仯ʱ����
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.e("onStatusChanged", "��ǰGPS״̬Ϊ�ɼ�״̬");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.e("onStatusChanged", "��ǰGPS״̬Ϊ��������״̬");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.e("onStatusChanged", "��ǰGPS״̬Ϊ��ͣ����״̬");
                    break;
            }
        }

        public void onProviderEnabled(String provider) {
            //GPS����ʱ����
            Log.e("cly", "onProviderEnabled: ");
        }

        public void onProviderDisabled(String provider) {
            //GPS����ʱ����
            Log.e("cly", "onProviderDisabled: ");
        }
    };

    @Override
    public void onDestroy() {
        //saveAssistData(AssistData);
        closeComPort(ComA);
        super.onDestroy();
    }

    private void openSerialPort() {
        ComA = new SerialControl("/dev/ttyMT0", 9600);
        openComPort(ComA);
    }

    public void onClick(View v) {
        receive_at_cmd_disp.setText("");
        iRecLines = 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    public void onPermissionsAllGranted(int requestCode, List<String> perms, boolean isAllGranted) {
        if (isAllGranted) {
            startLocate();
        }
    }

    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (PermissionUtils.somePermissionPermanentlyDenied(this, perms)) {
            PermissionUtils.showDialogGoToAppSettting(this);
        } else {
            PermissionUtils.showPermissionReason(requestCode, this, permissions, "Need Location Permission");
        }
    }

    //----------------------------------------------------���ڿ�����
    private class SerialControl extends SerialHelper {

        public SerialControl() {
        }

        public SerialControl(String sPort, int iBaudRate) {
            super(sPort, iBaudRate);
        }

        @Override
        protected void onDataReceived(final ComBean ComRecData) {
            //���ݽ�����������ʱ��������̣�����Ῠ��,���ܺ�6410����ʾ�����й�
            //ֱ��ˢ����ʾ��������������ʱ���������ԣ�����������ʾͬ����
            //���̶߳�ʱˢ����ʾ���Ի�ý���������ʾЧ�������ǽ��������ٶȿ�����ʾ�ٶ�ʱ����ʾ���ͺ�
            //����Ч�����-_-���̶߳�ʱˢ���Ժ�һЩ��
            DispQueue.AddQueue(ComRecData);//�̶߳�ʱˢ����ʾ(�Ƽ�)
            String atCMD = new String(ComRecData.bRec);
            if (atCMD.equals(MyATCmd.AT_MV)) {
                sendPortData(ComA, MyATCmd.AT_MVOK);
            } else if (atCMD.equals(MyATCmd.AT_HT)) {
                sendPortData(ComA, MyATCmd.AT_HTOK);
            } else if (atCMD.equals(MyATCmd.AT_LPW)) {
                sendPortData(ComA, MyATCmd.AT_LPWOK);
            }
			/*
			runOnUiThread(new Runnable()//ֱ��ˢ����ʾ
			{
				public void run()
				{
					DispRecData(ComRecData);
				}
			});*/
        }
    }

    //----------------------------------------------------ˢ����ʾ�߳�
    private class DispQueueThread extends Thread {
        private Queue<ComBean> QueueList = new LinkedList<ComBean>();

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                final ComBean ComData;
                while ((ComData = QueueList.poll()) != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            DispRecData(ComData);
                        }
                    });
                    try {
                        Thread.sleep(100);//��ʾ���ܸߵĻ������԰Ѵ���ֵ��С��
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public synchronized void AddQueue(ComBean ComData) {
            QueueList.add(ComData);
        }
    }

    //----------------------------------------------------�رմ���
    private void closeComPort(SerialHelper ComPort) {
        if (ComPort != null) {
            ComPort.stopSend();
            ComPort.close();
        }
    }

    //----------------------------------------------------�򿪴���
    private void openComPort(SerialHelper ComPort) {
        try {
            ComPort.open();
        } catch (SecurityException e) {
            ShowMessage("�򿪴���ʧ��:û�д��ڶ�/дȨ��!");
        } catch (IOException e) {
            ShowMessage("�򿪴���ʧ��:δ֪����!");
        } catch (InvalidParameterException e) {
            ShowMessage("�򿪴���ʧ��:��������!");
        }
    }

    //----------------------------------------------------���ڷ���
    private void sendPortData(SerialHelper ComPort, String sOut) {
        if (ComPort != null && ComPort.isOpen()) {
/*            if (radioButtonTxt.isChecked())
            {*/
            ComPort.sendTxt(sOut);
  /*          }else if (radioButtonHex.isChecked()) {
                ComPort.sendHex(sOut);
            }*/
            Log.i("cly", "send AT CMD=" + sOut);
        }
    }

    //----------------------------------------------------��ʾ��������
    private void DispRecData(ComBean ComRecData) {
        StringBuilder sMsg = new StringBuilder();
        sMsg.append(ComRecData.sRecTime);
        sMsg.append("[");
        sMsg.append(ComRecData.sComPort);
        sMsg.append("]");
/*        if (radioButtonTxt.isChecked())
        {*/
        sMsg.append("[Txt] ");
        sMsg.append(new String(ComRecData.bRec));
/*        }else if (radioButtonHex.isChecked()) {
            sMsg.append("[Hex] ");
            sMsg.append(MyFunc.ByteArrToHex(ComRecData.bRec));
        }*/
        sMsg.append("\r\n");
        receive_at_cmd_disp.append(sMsg);
        iRecLines++;
        if ((iRecLines > 500) /*&& (checkBoxAutoClear.isChecked())*/)//�ﵽ500���Զ����
        {
            receive_at_cmd_disp.setText("");
            iRecLines = 0;
        }
    }

    //------------------------------------------��ʾ��Ϣ
    private void ShowMessage(String sMsg) {
        Toast.makeText(this, sMsg, Toast.LENGTH_SHORT).show();
    }
}
