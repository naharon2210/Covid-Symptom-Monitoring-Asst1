package com.example.covid_sym_monitor;

//Main Activity Class contains all the button listeners and processes done in the main screen of the app.

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import org.opencv.android.OpenCVLoader;
import org.opencv.videoio.VideoCapture;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final int VIDEO_CAPTURE = 101;
    ProgressDialog progressDialog;
    CameraActivity cam_act;
    String folder_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Covid_Sym_Monitor/";
    String vid_name = "heart_rate.mp4";
    String mpjeg_name = "heart_rate_conv_mp.mjpeg";
    String avi_name = "final_heart_rate.avi";
    TextView disp_txt;
    DatabaseActivty db_act;
    double hr = 0.0;
    double resp_rate = 0.0;
    int check_hr_measure = 0;
    int check_resp_rate_measure = 0;

    public static String TAG = "Debug_Main_Activity";

    // Checks if OpenCV Library is loaded correctly
    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV done");
        } else {
            Log.d(TAG, "OpenCV error");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        configure_permissions();


        cam_act = new CameraActivity();
        db_act = new DatabaseActivty();

        progressDialog = new ProgressDialog(this);


        // TextView to represent the instructions and print heart and respiratory rate
        disp_txt = (TextView)findViewById(R.id.textView);
        String start_display = "Instructions: \n 1. Press Measure Heart Rate \n 2. Turn on the Flash \n" +
                "3. Put fingertip on camera";
        disp_txt.setText(start_display);

        // Button handler for measuring heart rate
        Button heart_rate_btn = (Button)findViewById(R.id.hr_btn);
        heart_rate_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                check_hr_measure = 1;
                start_recording_intent();
            }
        });


        // Button Handler for measuring respiratory rate
        Button resp_btn = (Button)findViewById(R.id.resp_btn);
        resp_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                check_resp_rate_measure = 1;
                showProgressDialogWithTitle("Put your phone on Chest, Calculating Respiratory Rate...");
                Intent intent = new Intent(MainActivity.this, AccelerometerService.class);
                startService(intent);  // Calls the Accelerometer Service

            }
        });

        // Button handler for uploading heart and respiratory rate and go to the symptoms logging page
        Button sym_btn = (Button)findViewById(R.id.up_sym_btn);
        sym_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // Before going to the next page check if the heart and respiratory rate is measured or not
                if (check_hr_measure == 1 && check_resp_rate_measure == 1) {
                    db_act.create_database();  // Create the database
                    db_act.create_table();   // Create the table
                    int up_check = db_act.upload_hr_resp_rate(hr, resp_rate); // Insert the values in the database
                    if (up_check == 1) {
                        Toast.makeText(MainActivity.this, "Signs Uploaded Successfully", Toast.LENGTH_LONG).show();
                    }
                    Intent intent = new Intent(MainActivity.this, SymptomsActivity.class);
                    startActivity(intent);
                }
                else
                {
                    Toast.makeText(MainActivity.this,"Please Measure the Heart and Respiratory Rate first.", Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    // Intent to open up the Camere and start recording
    public void start_recording_intent()
    {

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,45);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, VIDEO_CAPTURE);

    }



    // Function to ask the permissions required by the app
    void configure_permissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , 10);
            }
            return;
        }
    }



    // Function to perform action after the camera intent is finished
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VIDEO_CAPTURE) {
            if (resultCode == RESULT_OK )
            {
                // The rest of the code takes the video into the input stream and writes it to the location given in the internal storage
                Log.d("uy","ok res");
                File newfile;
                //data.
                AssetFileDescriptor videoAsset = null;
                FileInputStream in_stream = null;
                OutputStream out_stream = null;
                try {

                    videoAsset = getContentResolver().openAssetFileDescriptor(data.getData(), "r");
                    Log.d("uy","vid ead");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    in_stream = videoAsset.createInputStream();
                    Log.d("uy","in stream");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Log.d("uy","dir");
                Log.d("uy",Environment.getExternalStorageDirectory().getAbsolutePath());
                File dir = new File(folder_path);
                if (!dir.exists())
                {
                    dir.mkdirs();
                    Log.d("uy","mkdir");
                }

                newfile = new File(dir, vid_name);
                Log.d("uy","hr");

                if (newfile.exists()) {
                    newfile.delete();
                }


                try {
                    out_stream = new FileOutputStream(newfile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                byte[] buf = new byte[1024];
                int len;

                while (true) {
                    try
                    {
                        Log.d("uy","try");
                        if (((len = in_stream.read(buf)) > 0))
                        {
                            Log.d("uy","File write");
                            out_stream.write(buf, 0, len);
                        }
                        else
                        {
                            Log.d("uy","else");
                            in_stream.close();
                            out_stream.close();
                            break;
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

                // Function to convert video to avi for processing the heart rate
                convert_video_commands();

                Toast.makeText(this, "Video has been saved to:\n" +
                        data.getData(), Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Video recording cancelled.",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to record video",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


    // Function to convert video to avi for processing the heart rate
    public void convert_video_commands()
    {
        //Loads the ffmpeg library
        FFmpeg ffmpeg = FFmpeg.getInstance(this);
        try {
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

        // If the .mpjep files exist it deletes the older file
        File newfile = new File(folder_path + mpjeg_name);

        if (newfile.exists()) {
            newfile.delete();
        }

        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(new String[]{"-i", folder_path + vid_name, "-vcodec", "mjpeg", folder_path + mpjeg_name}, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart()
                {
                    showProgressDialogWithTitle("Converting to AVI and Measuring Heart Rate");
                }

                @Override
                public void onProgress(String message)
                {

                }

                @Override
                public void onFailure(String message) {
                }

                @Override
                public void onSuccess(String message)
                {

                }

                @Override
                public void onFinish()
                {

                }
            });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }

        // If the .avi file exist it deletes the older file
        File avi_newfile = new File(folder_path + avi_name);

        if (avi_newfile.exists()) {
            avi_newfile.delete();
        }
        try {
            // to execute "ffmpeg -version" command you just need to pass "-version"
            ffmpeg.execute(new String[]{"-i", folder_path + mpjeg_name, "-vcodec", "mjpeg", folder_path + avi_name}, new ExecuteBinaryResponseHandler() {

                @Override
                public void onStart()
                {

                }

                @Override
                public void onProgress(String message)
                {

                }

                @Override
                public void onFailure(String message) {
                }

                @Override
                public void onSuccess(String message)
                {


                }

                @Override
                public void onFinish()
                {

                    while(true)
                    {

                        try {
                            // Calculate the heart rate
                            String heart_rate = cam_act.measure_heart_rate(folder_path, avi_name);
                            if (heart_rate != "" )
                            {
                                // Display the heart rate
                                hr = Double.parseDouble(heart_rate);
                                disp_txt.setText("The Heart Rate is: " + heart_rate + "\n");
                                hideProgressDialogWithTitle();
                                break;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }


                }
            });

        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
        }

    }


    //Function to show the Processing Dialog box
    private void showProgressDialogWithTitle(String substring) {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.setMessage(substring);
        progressDialog.show();
    }


    // Function to hide the processing dialog box
    private void hideProgressDialogWithTitle() {
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.dismiss();
    }


    // Receives the final respiratory rate sent by the Accelerometer service class
    private BroadcastReceiver bReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {

            // Display the respiratory rate
            String output = intent.getStringExtra("success");
            Log.d("Output", output);
            disp_txt.append("Respiratory Rate: " + output);
            resp_rate = Double.parseDouble(output);
            hideProgressDialogWithTitle();

        }
    };

    protected void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(bReceiver, new IntentFilter("message"));
    }

    protected void onPause (){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bReceiver);
    }

}
