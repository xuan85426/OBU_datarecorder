package com.example.obu_data_recorder;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
    private Button button_send;
    private Button button_start;
    private Button button_freq;
    private Button button_clean;

    private LocationManager lc;
    private LocationListener ll;

    private File output_gps;
    private File output_acc;
    private File output_gyro;
    private File output_ori;
    private File output_linear_acc;

    private static int VIDEO_REQUEST = 101;
    private Uri videoUri;

    private Timer mTimer;

    private int timer_period;

    private final Object lock = new Object();

    // freq
    private final String[] HZ= {"10HZ", "50HZ", "100HZ", "200HZ"};
    private final int[] freq= {100, 20, 10, 5};
    private int index = 0;


    // new container
    private double lat, lng, speed, bear, alt;
    private final float[] accelerometerReading = new float[3];
    private final float[] gyroscopeReading = new float[3];
    private final float[] magnetometerReading  = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final float[] linear_accelerometerReading = new float[3];


    private static String[] PERMISSIONS = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION"
    };

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

    private void setNewFile(){
        String root = getExternalFilesDir(null).toString();
        File dir = new File(root + File.separator + "Experiment");
        if(!dir.mkdirs() && !dir.exists())
            Toast.makeText(MainActivity.this, "Failed to make dir", Toast.LENGTH_SHORT).show();


//        final long timestamp = Calendar.getInstance().getTimeInMillis();
        final long timestamp = System.currentTimeMillis();
        String filePath_gps = dir + File.separator + timestamp + "_gps.txt";
        String filePath_acc = dir + File.separator + timestamp + "_acc.txt";
        String filePath_gyro = dir + File.separator + timestamp + "_gyro.txt";
        String filePath_ori = dir + File.separator + timestamp + "_ori.txt";
        String filePath_linear_acc = dir + File.separator + timestamp + "_linear_acc.txt";

        try{
            output_acc = new File(filePath_acc);
            output_gps = new File(filePath_gps);
            output_gyro = new File(filePath_gyro);
            output_ori = new File(filePath_ori);
            output_linear_acc = new File(filePath_linear_acc);

            output_acc.createNewFile();
            output_gps.createNewFile();
            output_gyro.createNewFile();
            output_ori.createNewFile();
            output_linear_acc.createNewFile();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setTimerTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new TimerTask() {
                    @Override
                    public void run() {
                    // get lock
                    synchronized (lock){
                        // updating gps info
                        long round_time = System.currentTimeMillis();
                        long temp_time = round_time / 1000;
                        String values = "時間: " + temp_time + "\n經度: " + lat + "\n緯度: " + lng + "\n速度: " + speed + "\n方向: " + bear + "\n高度: " + alt;
                        String temp = round_time + " " + lat + " " + lng + " " + speed + " " + bear + " "+ alt + "\n";
                        current_gps.setText("Current information:\n" + values);
                        history_gps_data.append(temp);
                        writeToFile(temp, "gps");

                        values = "X-axis: " + accelerometerReading[0] + "\nY-axis: " + accelerometerReading[1] + "\nZ-axis: " + accelerometerReading[2];
                        // String date = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
                        temp = round_time + " " + accelerometerReading[0] + " " + accelerometerReading[1] + " " + accelerometerReading[2] + "\n";
                        //updating acc info
                        accelerometers.setText("Accelerometers\n" + values);
                        history_acc_data.append(temp);
                        writeToFile(temp, "acc");

                        temp = round_time + " " + linear_accelerometerReading[0] + " " + linear_accelerometerReading[1] + " " + linear_accelerometerReading[2] + "\n";
                        writeToFile(temp, "linear_acc");

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        current_gps = findViewById(R.id.lbl_GPS);
        history_gps_data = findViewById(R.id.lbl_Record_gps);
        accelerometers = findViewById(R.id.lbl_Acc);
        history_acc_data = findViewById(R.id.lbl_Record_acc);
        gyroscopes = findViewById(R.id.lbl_Gyro);
        history_gyro_data = findViewById(R.id.lbl_Record_gyro);
        orientation = findViewById(R.id.lbl_Ori);
        history_ori_data = findViewById(R.id.lbl_Record_ori);
        button_send = findViewById(R.id.btn_send);
        button_start = findViewById(R.id.btn_start);
        button_freq = findViewById(R.id.btn_freq);
        button_clean = findViewById(R.id.btn_clean);
        lc = (LocationManager)getSystemService(LOCATION_SERVICE);

        current_gps.setMovementMethod(new ScrollingMovementMethod());
        history_acc_data.setMovementMethod(new ScrollingMovementMethod());
        history_gps_data.setMovementMethod(new ScrollingMovementMethod());
        history_gyro_data.setMovementMethod(new ScrollingMovementMethod());
        history_ori_data.setMovementMethod(new ScrollingMovementMethod());

        button_freq.setText(HZ[index]);
        timer_period = freq[index];


        verifyALLPermissions( this );

        // dir & file init
        setNewFile();

        current_gps.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                current_gps.getParent().requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });

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

        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOutFile();
            }
        });

        button_freq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                index = (index += 1) % 3;
                button_freq.setText(HZ[index]);
                timer_period = freq[index];
            }
        });

        button_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                button_clean.setEnabled(false);

                // dir & file init
                setNewFile();
                current_gps.setText("");
                accelerometers.setText("");
                gyroscopes.setText("");
                orientation.setText("");

                history_gps_data.setText("GPS_LOG\n");
                history_acc_data.setText("ACCELERATOR_LOG\n");
                history_gyro_data.setText("GYROSCOPES_LOG\n");
                history_ori_data.setText("ORIENTATION_LOG\n");
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
                    sensorManager.registerListener(MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(MainActivity.this, linear_accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(MainActivity.this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                    sensorManager.registerListener(MainActivity.this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);

                    // register gps
                    ll = new MyLocationListener();
                    try{
                        // register listener
                        lc.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, ll);
                        lc.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTime, minDistance, ll);
                    }catch (SecurityException sex){
                        // Log.e("what", "GPS error" + sex.getMessage());
                        Toast.makeText(MainActivity.this, "Cannot get gps", Toast.LENGTH_SHORT).show();
                    }
                    button_send.setEnabled(false);
                    button_start.setText("Stop");

                    mTimer = new Timer();
                    setTimerTask();
                }
                else if(temp.equals("Stop")){
                    button_freq.setEnabled(true);
                    button_clean.setEnabled(true);
                    // unregister sensor
                    sensorManager.unregisterListener(MainActivity.this);
                    current_gps.setText("");
                    accelerometers.setText("");
                    gyroscopes.setText("");
                    orientation.setText("");

                    // unregister sensor
                    lc.removeUpdates(ll);

                    button_send.setEnabled(true);
                    button_start.setText("Start");
                    mTimer.cancel();
                }
                else{
                    button_start.setText("Error");
                }
            }
        });
    }

    @Override
    protected void onResume(){ super.onResume(); }

    @Override
    protected void onPause(){ super.onPause(); }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mTimer.cancel();
    }

    @Override
    // sensor function
    public void onSensorChanged(SensorEvent event){
        //get lock
        synchronized (lock){
            switch (event.sensor.getType()){
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    System.arraycopy(event.values, 0, linear_accelerometerReading, 0, linear_accelerometerReading.length);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    System.arraycopy(event.values, 0, gyroscopeReading, 0, gyroscopeReading.length);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
                    break;
                default:
                    break;
            }
        }
    }
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }
    public void onAccuracyChanged(Sensor sensor, int accuracy){ }

    public void captureVideo(View view) {
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if(videoIntent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(videoIntent, VIDEO_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == VIDEO_REQUEST && resultCode == RESULT_OK){
            videoUri = data.getData();
        }
    }

    private class MyLocationListener implements LocationListener {
        @SuppressLint("SetTextI18n")
        public void onLocationChanged(Location current){
            // String date = new SimpleDateFormat("HH:mm:ss:SSS").format(new Date());
            if(current != null){
                synchronized (lock){
                    alt = current.getAltitude();
                    lng = current.getLongitude();
                    lat = current.getLatitude();
                    bear =  current.getBearing();
                    speed = current.getSpeed() * 3.6;
                }
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
                case "linear_acc":
                    bufferedWriter = new BufferedWriter(new FileWriter(output_linear_acc, true));
                    bufferedWriter.write(data);
                    bufferedWriter.close();
                default:
                    break;
            }
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendOutFile(){
        ArrayList<Uri> uris = new ArrayList<>();
        List<Uri> filePaths = Arrays.asList(FileProvider.getUriForFile(this, getPackageName() + ".provider", output_gps),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_acc),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_gyro),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_ori),
                FileProvider.getUriForFile(this, getPackageName() + ".provider", output_linear_acc));
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
