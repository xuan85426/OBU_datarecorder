package com.example.obu_v2;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


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
    // Old version
    private String gps_data;
    private String accelerometers_data;
    private String gyroscopes_data;
    private String return_data;

    /*
    private FileOutputStream gps_output = null;
    private FileOutputStream sensor_output = null;
    private FileOutputStream gyroscopes_output = null;
     */

//    private FileInputStream test = null;


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

        // file open
        /*
        try {
            gps_output = openFileOutput("gps.txt", Context.MODE_PRIVATE);
            sensor_output = openFileOutput("accelerometers.txt", Context.MODE_PRIVATE);
            gyroscopes_output = openFileOutput("gyroscopes.txt", Context.MODE_PRIVATE);

            gps_output.write("GPS\n".getBytes());
            sensor_output.write("Accelerometers\n".getBytes());
            gyroscopes_output.write("Gyroscopes\n".getBytes());

            Toast.makeText(MainActivity.this, "File open!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
         */

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
                StringBuffer data = new StringBuffer();
                /*
                try {
                    test = new FileInputStream("gps.txt");
                    BufferedReader reader = new BufferedReader( new InputStreamReader(test, "utf-8"));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        Toast.makeText(MainActivity.this, line, Toast.LENGTH_SHORT).show();
                        break;
                    }
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                }
                 */
                // Old version using clipboard
//                return_data += "GPS\n" + gps_data + "\nACCELEROMETERS\n" + accelerometers_data + "\nGYROSCOPES\n" + gyroscopes_data;
//                ClipboardManager myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
//                ClipData myClip = ClipData.newPlainText("text", return_data);
//                if(myClipboard!= null)
//                    myClipboard.setPrimaryClip(myClip);
//                Toast.makeText(MainActivity.this, "Copy!", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_EMAIL, new String[]{"ne6080156@ns.ncku.edu.tw"});
                i.putExtra(Intent.EXTRA_SUBJECT, "Experiment outcome");
                i.putExtra(Intent.EXTRA_TEXT, history_gps_data.getText() + "\n" +
                        history_acc_data.getText() + "\n" + history_gyro_data.getText());
                try{
                    startActivity(Intent.createChooser(i, "Send mail..."));
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT);
                }
            }
        });

        // start button register
        button_start.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String temp = (String) button_start.getText();
                if(temp.equals("Start")){
                    // init strings
                    // Old version
                    gps_data = "";
                    accelerometers_data = "";
                    gyroscopes_data = "";
                    return_data = "";


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
        //file close
        /*
        try {
            gps_output.close();
            sensor_output.close();
            gyroscopes_output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

         */

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
            //Old version Q: String may full
//            accelerometers_data += rightNow.getTimeInMillis() + String.valueOf(event.values[0]) + " " +
//                    String.valueOf(event.values[1]) + " " +String.valueOf(event.values[2]) + "\n";
            history_acc_data.append(temp);
        }
        if(event.sensor.equals(gyro)){
            gyroscopes.setText("Gyroscopes\n" + values);
            // Old version Q: String may full
//            gyroscopes_data += rightNow.getTimeInMillis() + String.valueOf(event.values[0]) + " " +
//                    String.valueOf(event.values[1]) + " " +String.valueOf(event.values[2]) + "\n";
            history_gyro_data.append(temp);
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
                // Toast.makeText(MainActivity.this, "location changed", Toast.LENGTH_SHORT).show();
                // time format: Return the UTC time of this fix, in milliseconds since January 1, 1970.
                current_gps.setText("Current information: \n時間: " + rightNow.getTimeInMillis() +
                        "\n經度: "+ lat + "\n緯度: " + lng + "\n速度: " + speed);
                history_gps_data.append(temp);
                // Old version Q: String may full
//                gps_data += rightNow.getTimeInMillis() + " " + lat + " " + lng + " " + speed + "\n";

            }
        }
        public void onProviderDisabled(String provider){}
        public void onProviderEnabled(String provider){}
        public void onStatusChanged(String provider, int status, Bundle extras){}
    }

}
