package com.illusionbox.www.chronicle;

import android.content.DialogInterface;
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
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.IOException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    Button play,stop,record,upload;
    private MediaRecorder myAudioRecorder;
    private String outputFile = null;
    private Chronometer myChronometer;
    private MediaPlayer m;
    HttpMultipartUpload uploader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play=(Button)findViewById(R.id.btn_play);
        stop=(Button)findViewById(R.id.btn_stop);
        record=(Button)findViewById(R.id.btn_record);
        upload = (Button) findViewById(R.id.btn_upload);
        myChronometer = (Chronometer) findViewById(R.id.chronometer);

        stop.setEnabled(false);
        play.setEnabled(false);
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";;

        myAudioRecorder=new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        myAudioRecorder.setOutputFile(outputFile);

        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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

                record.setEnabled(false);
                stop.setEnabled(true);
                myChronometer.setBase(SystemClock.elapsedRealtime());
                myChronometer.start();

                Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_LONG).show();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myAudioRecorder.stop();
                myAudioRecorder.release();
                myAudioRecorder  = null;

                stop.setEnabled(false);
                play.setEnabled(true);

                myChronometer.stop();

                Toast.makeText(getApplicationContext(), "Audio recorded successfully",Toast.LENGTH_LONG).show();
            }
        });

        play.setOnClickListener(play_click);
    }

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

            play.setText("Pause");
            play.setOnClickListener(pause_click);

            m.start();
            m.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    play.setText("Play");
                    play.setOnClickListener(play_click);
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
                play.setText("Play");
                play.setOnClickListener(play_click);
                myChronometer.stop();
            } else {
                m.stop();
                play.setText("Play");
                play.setOnClickListener(play_click);
                myChronometer.stop();
            }
        }
    };

    View.OnClickListener upload_click = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            uploader.upload(new URL("172.22.111.123/"));
        }
    };

    private class UploadAudioTask extends AsyncTask<URL, Integer, Long> {
        protected Long doInBackground(URL... urls) {
            int count = urls.length;
            long totalSize = 0;
            for (int i = 0; i < count; i++) {
                totalSize += Downloader.downloadFile(urls[i]);
                publishProgress((int) ((i / (float) count) * 100));
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {
            setProgressPercent(progress[0]);
        }

        protected void onPostExecute(Long result) {
            showDialog("Downloaded " + result + " bytes");
        }
    }
}
