package com.example.obu_v2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.util.Calendar;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 101;
    private final int minTime = 0;
    private final float minDistance = 0;
    private TextView current_gps;
    private TextView history_gps_data;
    private TextView accelerometers;
    private TextView history_acc_data;
    private TextView gyroscopes;
    private TextView history_gyro_data;
    private TextView orientation;
    private TextView history_ori_data;
    private Button button_copy;
    private Button button_start;
    private Button button_freq;

    private LocationManager lc;
    private LocationListener ll;

    private File output_gps;
    private File output_acc;
    private File output_gyro;
    private File output_ori;

    private Timer mTimer;

    private int timer_period;


    // new container
    private double lat, lng, speed, bear;
    private final float[] accelerometerReading = new float[3];
    private final float[] gyroscopeReading = new float[3];
    private final float[] magnetometerReading  = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] linear_accelerometerReading = new float[3];


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION"};

    public static void verifyALLPermissions(Activity activity) {
        int PERMISSIONS_ALL = 1;
        try {

            for(String permission: PERMISSIONS){
                if(ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(activity, PERMISSIONS, PERMISSIONS_ALL);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        current_gps = findViewById(R.id.lblGPS);
        history_gps_data = findViewById(R.id.lblRecord_gps);
        accelerometers = findViewById(R.id.lblAcc);
        history_acc_data = findViewById(R.id.lblRecord_acc);
        gyroscopes = findViewById(R.id.lblgyro);
        history_gyro_data = findViewById(R.id.lblRecord_gyro);
        orientation = findViewById(R.id.lblori);
        history_ori_data = findViewById(R.id.lblRecord_ori);
        button_copy = findViewById(R.id.btn_copy);
        button_start = findViewById(R.id.btn_start);
        button_freq = findViewById(R.id.btn_freq);
        lc = (LocationManager)getSystemService(LOCATION_SERVICE);

        history_acc_data.setMovementMethod(new ScrollingMovementMethod());
        history_gps_data.setMovementMethod(new ScrollingMovementMethod());
        history_gyro_data.setMovementMethod(new ScrollingMovementMethod());
        history_ori_data.setMovementMethod(new ScrollingMovementMethod());

        timer_period = 1000;


        verifyALLPermissions( this );

        // dir & file init
        String root = getExternalFilesDir(null).toString();
        File dir = new File(root + File.separator + "Experiment");
        if(!dir.mkdirs() && !dir.exists()){
            Toast.makeText(MainActivity.this, "Failed to make dir", Toast.LENGTH_SHORT).show();
        }
        String filePath_gps = dir + File.separator + "gps.txt";
        String filePath_acc = dir + File.separator + "acc.txt";
        String filePath_gyro = dir + File.separator + "gyro.txt";
        String filePath_ori = dir + File.separator + "ori.txt";

        try{
            output_acc = new File(filePath_acc);
            output_gps = new File(filePath_gps);
            output_gyro = new File(filePath_gyro);
            output_ori = new File(filePath_ori);
            if(output_acc.exists() || output_gyro.exists() || output_gps.exists()){
                output_gps.delete();
                output_acc.delete();
                output_gyro.delete();
                output_ori.delete();
            }
            output_acc.createNewFile();
            output_gps.createNewFile();
            output_gyro.createNewFile();
            output_ori.createNewFile();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }

        history_gps_data.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                history_gps_data.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        history_ori_data.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                history_ori_data.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        history_acc_data.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                history_acc_data.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }

        });

        history_gyro_data.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                history_gyro_data.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

        button_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmailWithFile();
            }
        });

        button_freq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (timer_period){
                    case 100:
                        button_freq.setText("1HZ");
                        timer_period = 1000;
                        break;
                    case 500:
                        button_freq.setText("10HZ");
                        timer_period = 100;
                        break;
                    case 1000:
                        button_freq.setText("5HZ");
                        timer_period = 500;
                        break;
                    default:
                        break;
                }
            }
        });

        // start button register
        button_start.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String temp = (String) button_start.getText();
                SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                if(temp.equals("Start")){
                    button_freq.setEnabled(false);
                    // register sensor

                    Sensor linear_accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                    Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    Sensor gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
                    sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(MainActivity.this, linear_accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(MainActivity.this, magneticField, SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_FASTEST);

                    // register gps
                    ll = new MyLocationListener();
                    try{
                        // 註冊listener
                        lc.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, ll);
                        lc.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, ll);
                    }catch (SecurityException sex){
                        // Log.e("what", "GPS error" + sex.getMessage());
                        Toast.makeText(MainActivity.this, "Cannot get gps", Toast.LENGTH_SHORT).show();
                    }
                    button_copy.setEnabled(false);
                    button_start.setText("Stop");

                    mTimer = new Timer();
                    setTimerTask();
                }
                else if(temp.equals("Stop")){
                    button_freq.setEnabled(true);
                    // unregister sensor
                    sensorManager.unregisterListener(MainActivity.this);
                    current_gps.setText("");
                    accelerometers.setText("");
                    gyroscopes.setText("");
                    orientation.setText("");

                    // unregister sensor
                    lc.removeUpdates(ll);

                    button_copy.setEnabled(true);
                    button_start.setText("Start");
                    mTimer.cancel();
                }
                else{
                    button_start.setText("Error");
                }
            }
        });
    }

    private void setTimerTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                        // updating gps info
                        long round_time = System.currentTimeMillis();
                        String values = "時間: " + round_time + "\n經度: " + lat + "\n緯度: " + lng + "\n速度: " + speed + " 方向: " + bear;
                        String temp = round_time + " " + lat + " " + lng + " " + speed + " " + bear + "\n";
                        current_gps.setText("Current information:\n" + values);
                        history_gps_data.append(temp);
                        writeToFile(temp, "gps");

                        values = "X-axis: " + linear_accelerometerReading[0] + "\nY-axis: " + linear_accelerometerReading[1] + "\nZ-axis: " + linear_accelerometerReading[2];
                        // String date = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
                        temp = round_time + " " + linear_accelerometerReading[0] + " " + linear_accelerometerReading[1] + " " + linear_accelerometerReading[2] + "\n";
                        //updating acc info
                        accelerometers.setText("Accelerometers\n" + values);
                        history_acc_data.append(temp);
                        writeToFile(temp, "acc");

                        values = "X-axis: " + gyroscopeReading[0] + "\nY-axis: " + gyroscopeReading[1] + "\nZ-axis: " + gyroscopeReading[2];
                        temp = round_time + " " + gyroscopeReading[0] + " " + gyroscopeReading[1] + " " + gyroscopeReading[2] + "\n";
                        gyroscopes.setText("Gyroscopes\n" + values);
                        history_gyro_data.append(temp);
                        writeToFile(temp, "gyro");

                        updateOrientationAngles();
                        values = "Azimuth: " + orientationAngles[0] + "\nPitch: " + orientationAngles[1] + "\nRoll: " + orientationAngles[2] + "\n";
                        temp = round_time + " " + orientationAngles[0] + " " + orientationAngles[1] + " " + orientationAngles[2] + "\n";
                        orientation.setText("Orientation\n" + values);
                        history_ori_data.append(temp);
                        writeToFile(temp, "ori");

                    }
                });
            }
        }, 1000, timer_period);
    }

    @Override
    // After get the permissions
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult){
        if(requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION){
            if(grantResult[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "取得權限取得GPS資訊", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this, "Cannot", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onResume(){ super.onResume(); }
    @Override
    protected void onPause(){ super.onPause(); }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        output_gps.delete();
        output_acc.delete();
        output_gyro.delete();
        mTimer.cancel();
    }
    @Override
    // sensor function
    public void onSensorChanged(SensorEvent event){
        if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION){
            System.arraycopy(event.values, 0, linear_accelerometerReading, 0, linear_accelerometerReading.length);
        }
        else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        }
        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
            System.arraycopy(event.values, 0, gyroscopeReading, 0, gyroscopeReading.length);
        }
        else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }
    }
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy){ }

    private class MyLocationListener implements LocationListener {
        @SuppressLint("SetTextI18n")
        public void onLocationChanged(Location current){
            // String date = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
            if(current != null){
                lng = current.getLongitude();
                lat = current.getLatitude();
                bear =  current.getBearing();
                speed = current.getSpeed() * 3.6;
            }
        }
        public void onProviderDisabled(String provider){}
        public void onProviderEnabled(String provider){}
        public void onStatusChanged(String provider, int status, Bundle extras){}
    }

    // write file
    private void writeToFile(String data, String filename){
        BufferedWriter bufferedWriter;
        try{
            switch (filename){
                case "gps":
                    bufferedWriter = new BufferedWriter(new FileWriter(output_gps, true));
                    bufferedWriter.write(data);
                    bufferedWriter.close();
                    break;
                case "gyro":
                    bufferedWriter = new BufferedWriter(new FileWriter(output_gyro, true));
                    bufferedWriter.write(data);
                    bufferedWriter.close();
                    break;
                case "acc":
                    bufferedWriter = new BufferedWriter(new FileWriter(output_acc, true));
                    bufferedWriter.write(data);
                    bufferedWriter.close();
                    break;
                case "ori":
                    bufferedWriter = new BufferedWriter(new FileWriter(output_ori, true));
                    bufferedWriter.write(data);
                    bufferedWriter.close();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmailWithFile(){
        ArrayList<Uri> uris = new ArrayList<>();
        List<Uri> filePaths = Arrays.asList(FileProvider.getUriForFile(this, getPackageName() + ".provider", output_gps),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_acc),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_gyro),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_ori));
        for(Uri uri: filePaths)
            uris.add(uri);
        Intent i = new Intent(Intent.ACTION_SEND_MULTIPLE);
        i.setType("vnd.android.cursor.dir/email");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"ne6081056@gs.ncku.edu.tw"});
        i.putExtra(Intent.EXTRA_SUBJECT, "Experiment outcome");
        i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        try{
            this.startActivity(Intent.createChooser(i, "Send mail..."));
        }catch (Exception e){
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
