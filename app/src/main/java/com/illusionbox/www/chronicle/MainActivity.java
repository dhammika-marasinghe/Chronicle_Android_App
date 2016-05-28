package com.illusionbox.www.chronicle;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ImageButton record,upload;
    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    private Chronometer myChronometer;
    private MediaPlayer m;
    private ProgressBar progressBar, recordProgress;
    HttpMultipartUpload uploader;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        record=(ImageButton)findViewById(R.id.btn_record);
        upload = (ImageButton) findViewById(R.id.btn_upload);
        myChronometer = (Chronometer) findViewById(R.id.chronometer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        recordProgress = (ProgressBar) findViewById(R.id.progressBar2);

        upload.setEnabled(false);
        upload.setAlpha(0.5f);
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.mp3";;

        upload.setOnClickListener(upload_click);
        myAudioRecorder=new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        myAudioRecorder.setOutputFile(outputFile);

        myChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                long timeElapsed = SystemClock.elapsedRealtime() - chronometer.getBase();
                recordProgress.setProgress((int)timeElapsed/15);
                if(timeElapsed >= 15000){
                    myAudioRecorder.stop();
                    myAudioRecorder.release();
                    myAudioRecorder  = null;

                    record.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
                    record.setOnClickListener(play_click);

                    myChronometer.stop();

                    upload.setEnabled(true);
                    upload.setAlpha(1.0f);

                    Toast.makeText(getApplicationContext(), "Audio recorded successfully",Toast.LENGTH_LONG).show();
                }
            }
        });

        record.setOnClickListener(record_click);
        Button next = (Button) findViewById(R.id.next);
        next.setOnClickListener(next_click);
    }

    View.OnClickListener next_click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent i = new Intent(MainActivity.this, PlaybackActivity.class);
            startActivity(i);
        }
    };

    View.OnClickListener record_click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            try {
                myAudioRecorder.prepare();
                myAudioRecorder.start();
            }

            catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            record.setOnClickListener(stop_click);
            record.setImageDrawable(getResources().getDrawable(R.drawable.red_button));

            myChronometer.setBase(SystemClock.elapsedRealtime());
            myChronometer.start();

            Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
        }
    };

    View.OnClickListener stop_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            myAudioRecorder.stop();
            myAudioRecorder.release();
            myAudioRecorder  = null;

            record.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
            record.setOnClickListener(play_click);

            myChronometer.stop();

            upload.setEnabled(true);
            upload.setAlpha(1.0f);

            Toast.makeText(getApplicationContext(), "Audio recorded successfully",Toast.LENGTH_LONG).show();
        }
    };

    View.OnClickListener play_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) throws IllegalArgumentException,SecurityException,IllegalStateException {
            if(m == null) {
                m = new MediaPlayer();

                try {
                    m.setDataSource(outputFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    m.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            myChronometer.setBase(SystemClock.elapsedRealtime() - m.getCurrentPosition());
            myChronometer.start();

            record.setImageDrawable(getResources().getDrawable(R.drawable.pause_button));
            record.setOnClickListener(pause_click);

            m.start();
            m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    record.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
                    record.setOnClickListener(play_click);
                    myChronometer.stop();
                }
            });
            Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
        }
    };

    View.OnClickListener pause_click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(m.isPlaying()){
                m.pause();
                record.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
                record.setOnClickListener(play_click);
                myChronometer.stop();
            } else {
                m.stop();
                record.setImageDrawable(getResources().getDrawable(R.drawable.play_button));
                record.setOnClickListener(play_click);
                myChronometer.stop();
            }
        }
    };

    View.OnClickListener upload_click = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new UploadAudioTask().execute("url", outputFile);
            upload.setEnabled(false);
            upload.setAlpha(0.5f);
        }
    };

    void setResponse(String str) {
        TextView txt = (TextView)findViewById(R.id.textView);
        txt.setText(str);
    }

    void setProgressPercent(int progress){
        progressBar.setProgress(progress);
    }

    private class UploadAudioTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... strings) {
            publishProgress(25);
            File file = new File(outputFile);
            String result = "Failed";
            try {
                URL url = new URL("http://172.22.111.136:8080/chronicle-server/upload.php");
                HashMap<String, String> map = new HashMap<>();
                map.put("title","Kakki");
                map.put("user","Pakaya");
                result = new HttpMultipartUpload().upload(url, file, "uploadedFile", map);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            publishProgress(100);
            return result;
        }

        protected void onProgressUpdate(Integer... progress) {
            setProgressPercent(progress[0]);
        }

        protected void onPostExecute(String result) {
            setResponse(result);
            Toast.makeText(context, "Done "+result, Toast.LENGTH_LONG).show();
        }
    }
}
