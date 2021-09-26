package com.example.covid_sym_monitor;

// Class to get the Accelerometer sensor data and process to compute the respiratory rate.

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.FocusFinder;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AccelerometerService extends Service implements SensorEventListener {
    private SensorManager accelerometermanage;
    private Sensor sense_accelerometer;
    double accelerometervalueX[]= new double[1280];
    double accelerometervalueY[]= new double[1280];
    double accelerometervalueZ[]= new double[1280];
    int index = 0;
    int k=0;
    Bundle b;
    Algorithms algos = new Algorithms();
    public static String TAG = "Debug_Acc_Service";


    String data_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Covid_Sym_Monitor/";
    String file_name = "data_breathe.csv";
    //ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

    public AccelerometerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Start the service
    @Override
    public void onCreate()
    {
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        accelerometermanage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sense_accelerometer = accelerometermanage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometermanage.registerListener(this, sense_accelerometer,SensorManager.SENSOR_DELAY_NORMAL);

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Toast.makeText(this, "registered listener", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    // Log the axis values from the accelerometer
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor my_sensor = sensorEvent.sensor;
        if(my_sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            index++;
            accelerometervalueX[index] = sensorEvent.values[0];
            accelerometervalueY[index] = sensorEvent.values[1];
            accelerometervalueZ[index] = sensorEvent.values[2];

            if (index>=1279){
                index = 0;
                Toast.makeText(this, "Started to File", Toast.LENGTH_LONG).show();
                accelerometermanage.unregisterListener(this);
                File output_file = new File(data_path + file_name);
                if (output_file.exists()) {
                    output_file.delete();
                }

                // Write the csv data contaning the axis values
                write_csv_breathe();
            }
        }
    }

    // Function to write the csv data
    public void write_csv_breathe() {
        Toast.makeText(this, "Started Writing File", Toast.LENGTH_LONG).show();
        File output = new File(data_path + file_name);
        FileWriter data_Output = null;
        try{
            data_Output = new FileWriter(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i=0; i<1280;i++){
            try{
                data_Output.append(accelerometervalueX[i]+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i=0; i<1280;i++){
            try{
                data_Output.append(accelerometervalueY[i]+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (int i=0; i<1280;i++){
            try{
                data_Output.append(accelerometervalueZ[i]+"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(this, "Done Writing", Toast.LENGTH_LONG).show();
        }


        // Reading the values from the csv
        HashMap<String, List<Double>> hashMap = (HashMap<String, List<Double>>) readFromCSV();

        int mov_period = 50;

        // Calculating the moving average and peak detection
        List<Double> values_x = hashMap.get("x");
        List<Double> avg_data_x = algos.calc_mov_avg(mov_period, values_x);
        int peak_counts_X = algos.count_zero_crossings_thres(avg_data_x);

        List<Double> values_y = hashMap.get("y");
        List<Double> avg_data_y = algos.calc_mov_avg(mov_period, values_y);
        int peak_counts_Y = algos.count_zero_crossings_thres(avg_data_y);

        List<Double> values_z = hashMap.get("z");
        List<Double> avg_data_z = algos.calc_mov_avg(mov_period, values_z);
        int peak_counts_Z = algos.count_zero_crossings_thres(avg_data_z);


        //String s = " " + peak_counts_X/2 + " " + peak_counts_Y/2 + " " + peak_counts_Z/2;
        String resp_rate_val = ""+peak_counts_Y/2;
        // Sending back the received value to the main activity
        sendBroadcast(resp_rate_val);
        Log.d(TAG, resp_rate_val);

        //sendBroadcast("Done writing");

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Function to send the broadcast message to the main activity
    private void sendBroadcast (String success){
        Intent intent = new Intent ("message"); //put the same message as in the filter you used in the activity when registering the receiver
        intent.putExtra("success", success);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    // Function to read the csv
    public Map<String, List<Double>> readFromCSV(){
        List<Double> values = new ArrayList<Double>();
        try {
            File file = new File(data_path+file_name);
            InputStream inputStream = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String csvLine;
                while ((csvLine = reader.readLine()) != null)
                {
                    values.add(Double.parseDouble(csvLine));
                }
            }
            catch (IOException e){
                Log.d(TAG, "File IO Error found");
            }
        }
        catch(FileNotFoundException e)
        {
            Log.d(TAG, "File Not found");
        }

        List<Double> values_x = new ArrayList<Double>();
        List<Double> values_y = new ArrayList<Double>();
        List<Double> values_z = new ArrayList<Double>();

        int iter = (int) values.size()/3;

        for (int i=0; i< iter; i++)
        {
            Double val = values.get(i);
            values_x.add(val);
        }

        for (int i=iter; i< (2*iter); i++)
        {
            Double val = values.get(i);
            values_y.add(val);
        }

        for (int i=(2*iter); i< (3*iter); i++)
        {
            Double val = values.get(i);
            values_z.add(val);
        }

        Map<String, List<Double>> hashMap = new HashMap();
        hashMap.put("x", values_x);
        hashMap.put("y", values_y);
        hashMap.put("z", values_z);

        return hashMap;
    }


}
