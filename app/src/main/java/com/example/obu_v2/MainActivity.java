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
    private Button button_copy;
    private Button button_start;

    private LocationManager lc;
    private LocationListener ll;

    private SensorManager sm;
    private Sensor acc;
    private Sensor gyro;

    private File output_gps;
    private File output_acc;
    private File output_gyro;

    private Timer mTimer;

    private int timer_period;

    // updating container
    private double lat, lng, speed;
    private double acc_x, acc_y, acc_z;
    private double gyro_x, gyro_y, gyro_z;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        current_gps = findViewById(R.id.lblOutput);
        history_gps_data = findViewById(R.id.lblRecord);
        accelerometers = findViewById(R.id.lblAcc);
        history_acc_data = findViewById(R.id.lblRecord_acc);
        gyroscopes = findViewById(R.id.lblgyro);
        history_gyro_data = findViewById(R.id.lblRecord_gyro);
        button_copy = findViewById(R.id.btn_copy);
        button_start = findViewById(R.id.btn_start);
        lc = (LocationManager)getSystemService(LOCATION_SERVICE);

        history_acc_data.setMovementMethod(new ScrollingMovementMethod());
        history_gps_data.setMovementMethod(new ScrollingMovementMethod());
        history_gyro_data.setMovementMethod(new ScrollingMovementMethod());


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

        try{
            output_acc = new File(filePath_acc);
            output_gps = new File(filePath_gps);
            output_gyro = new File(filePath_gyro);
            if(output_acc.exists() || output_gyro.exists() || output_gps.exists()){
                output_gps.delete();
                output_acc.delete();
                output_gyro.delete();
            }
            output_acc.createNewFile();
            output_gps.createNewFile();
            output_gyro.createNewFile();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }

        button_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmailWithFile();
            }
        });

        // start button register
        button_start.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String temp = (String) button_start.getText();
                if(temp.equals("Start")){
                    // register sensor
                    sm = (SensorManager) getSystemService(SENSOR_SERVICE);
                    acc = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                    gyro = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
                    sm.registerListener(MainActivity.this, acc, SensorManager.SENSOR_DELAY_NORMAL);
                    sm.registerListener(MainActivity.this, gyro, SensorManager.SENSOR_DELAY_NORMAL);
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
                    // unregister sensor
                    sm.unregisterListener(MainActivity.this);
                    current_gps.setText("");
                    accelerometers.setText("");
                    gyroscopes.setText("");

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
                // updating gps info
                long roundtime = System.currentTimeMillis();
                String temp = System.currentTimeMillis() + " " + lat + " " + lng + " " + speed + "\n";
                current_gps.setText("Current information: \n時間: " + roundtime +
                        "\n經度: "+ lng + "\n緯度: " + lat + "\n速度: " + speed);
                history_gps_data.append(temp);
                writeToFile(temp, "gps");

                String values = "X-axis: " + acc_x + "\n" +
                                    "Y-axis: " + acc_y + "\n" +
                                    "Z-axis: " + acc_z;
                // String date = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
                temp = roundtime + " " + acc_x + " " + acc_y + " " + acc_z + "\n";
                //updating acc info
                accelerometers.setText("Accelerometers\n" + values);
                history_acc_data.append(temp);
                writeToFile(temp, "acc");

                values = "X-axis: " + gyro_x + "\n" +
                        "Y-axis: " + gyro_y + "\n" +
                        "Z-axis: " + gyro_z;
                temp = roundtime + " " + gyro_x + " " + gyro_y + " " + gyro_z + "\n";
                gyroscopes.setText("Gyroscopes\n" + values);
                history_gyro_data.append(temp);
                writeToFile(temp, "gyro");
            }
        }, 1000, 1000);
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
        if(event.sensor.equals(acc)){
            acc_x = event.values[0];
            acc_y = event.values[1];
            acc_z = event.values[2];
        }
        if(event.sensor.equals(gyro)){
            gyro_x = event.values[0];
            gyro_y = event.values[1];
            gyro_z = event.values[2];
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy){ }

    private class MyLocationListener implements LocationListener {
        @SuppressLint("SetTextI18n")
        public void onLocationChanged(Location current){
            // String date = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
            if(current != null){
                lng = current.getLongitude();
                lat = current.getLatitude();
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
                default:
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private String readFromFile(String filename){ // unused
        String ret = "";
        try {
            InputStream inputStream = this.openFileInput(filename);
            if(inputStream != null){
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while((receiveString = bufferedReader.readLine()) != null){
                    stringBuilder.append(receiveString);
                }
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }catch (Exception e){
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
        return ret;
    }

    private void sendEmailWithFile(){
        ArrayList<Uri> uris = new ArrayList<>();
        List<Uri> filePaths = Arrays.asList(FileProvider.getUriForFile(this, getPackageName() + ".provider", output_gps),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_acc),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_gyro));
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


// useless code


// get GPS permission
//        if(!lc.isProviderEnabled(LocationManager.GPS_PROVIDER)){
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("定位管理")
//                    .setMessage("GPS not enable.\n" + "please enable")
//                    .setPositiveButton("啟用",
//                            new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    Intent i = new Intent(
//                                            Settings.ACTION_LOCALE_SETTINGS);
//                                    startActivity(i);
//                                }
//                            })
//                    .setNegativeButton("不啟用", null).create().show();
//        }
//        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
//        }
//

