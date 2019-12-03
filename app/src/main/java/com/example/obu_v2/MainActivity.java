package com.example.obu_v2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;


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

    private Calendar rightNow;


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

        // get GPS permission
        if(!lc.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("定位管理")
                    .setMessage("GPS not enable.\n" + "please enable")
                    .setPositiveButton("啟用",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent i = new Intent(
                                            Settings.ACTION_LOCALE_SETTINGS);
                                    startActivity(i);
                                }
                            })
                    .setNegativeButton("不啟用", null).create().show();
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
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
                    // init file
                    createEmptyFile();
                    // register sensor
                    sm = (SensorManager) getSystemService(SENSOR_SERVICE);
                    acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
                }
                else{
                    button_start.setText("Error");
                }
            }
        });


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
    }
    @Override
    // sensor function
    public void onSensorChanged(SensorEvent event){
        String values = "X-axis: " + String.valueOf(event.values[0]) + "\n" +
                "Y-axis: " + String.valueOf(event.values[1]) + "\n" +
                "Z-axis: " + String.valueOf(event.values[2]);
        rightNow = Calendar.getInstance();
        String temp = rightNow.getTimeInMillis()+ " " + String.valueOf(event.values[0]) + " " +
                String.valueOf(event.values[1]) + " " +String.valueOf(event.values[2]) + "\n";
        if(event.sensor.equals(acc)){
            accelerometers.setText("Accelerometers\n" + values);
            history_acc_data.append(temp);
            writeToFile(temp, "acc.txt");
        }
        if(event.sensor.equals(gyro)){
            gyroscopes.setText("Gyroscopes\n" + values);
            history_gyro_data.append(temp);
            writeToFile(temp, "gyro.txt");
        }
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy){ } //unchanged
    ///
    private class MyLocationListener implements LocationListener {
        @SuppressLint("SetTextI18n")
        public void onLocationChanged(Location current){
            double lat, lng, speed;
            rightNow = Calendar.getInstance();
            if(current != null){
                lat = current.getLatitude();
                lng = current.getLongitude();
                speed = current.getSpeed();
                String temp = current.getTime() + " " + lat + " " + lng + " " + speed + "\n";
                // time format: Return the UTC time of this fix, in milliseconds since January 1, 1970.
                current_gps.setText("Current information: \n時間: " + rightNow.getTimeInMillis() +
                        "\n經度: "+ lat + "\n緯度: " + lng + "\n速度: " + speed);
                history_gps_data.append(temp);
                // write file
                writeToFile(temp, "gps.txt");
            }
        }
        public void onProviderDisabled(String provider){}
        public void onProviderEnabled(String provider){}
        public void onStatusChanged(String provider, int status, Bundle extras){}
    }

    private void createEmptyFile(){
        try {
            OutputStreamWriter cleaning1 = new OutputStreamWriter(this.openFileOutput("gps.txt", Context.MODE_PRIVATE));
            cleaning1.write("");
            cleaning1.close();
            OutputStreamWriter cleaning2 = new OutputStreamWriter(this.openFileOutput("acc.txt", Context.MODE_PRIVATE));
            cleaning2.write("");
            cleaning2.close();
            OutputStreamWriter cleaning3 = new OutputStreamWriter(this.openFileOutput("gyro.txt", Context.MODE_PRIVATE));
            cleaning3.write("");
            cleaning3.close();

        } catch (Exception e){
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    // write file
    private void writeToFile(String data, String filename){
        try{
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput(filename, Context.MODE_APPEND));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private String readFromFile(String filename){
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
        File gps_filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "gps.txt");
        File acc_filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "acc.txt");
        File gyro_filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "gyro.txt");
        Uri path1 = FileProvider.getUriForFile(this, getPackageName() + ".provider", gps_filelocation);
        Uri path2 = FileProvider.getUriForFile(this, getPackageName() + ".provider", acc_filelocation);
        Uri path3 = FileProvider.getUriForFile(this, getPackageName() + ".provider", gyro_filelocation);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("vnd.android.cursor.dir/email");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"ne6080156@ns.ncku.edu.tw"});
        i.putExtra(Intent.EXTRA_SUBJECT, "Experiment outcome");
        i.putExtra(Intent.EXTRA_STREAM, path1);
        i.putExtra(Intent.EXTRA_STREAM, path2);
        i.putExtra(Intent.EXTRA_STREAM, path3);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try{
            startActivity(Intent.createChooser(i, "Send mail..."));
        }catch (Exception e){
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
