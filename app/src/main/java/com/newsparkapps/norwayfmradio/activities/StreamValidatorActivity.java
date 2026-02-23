package com.newsparkapps.norwayfmradio.activities;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.newsparkapps.norwayfmradio.R;
import com.newsparkapps.norwayfmradio.RadioManager;
import com.newsparkapps.norwayfmradio.db.DatabaseHandler;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import java.util.ArrayList;
import java.util.List;

public class StreamValidatorActivity extends AppCompatActivity {
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView resultText;
    private Button startButton;
    private DatabaseHandler db;
    private List<Shoutcast> stationList;
    private List<String> reportList = new ArrayList<>();
    private ExoPlayer player;
    private int currentIndex = 0;
    private Handler handler = new Handler(Looper.getMainLooper());
    private static final int STREAM_TIMEOUT = 10000; // 10 sec
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream_validator);

        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        resultText = findViewById(R.id.resultText);
        startButton = findViewById(R.id.startButton);

        db = new DatabaseHandler(this);
        stationList = db.getAllFmList();

        progressBar.setMax(stationList.size());

        startButton.setOnClickListener(v -> startValidation());
    }

    private void startValidation() {

        RadioManager.with(this).stop(); // Stop main playback

        startButton.setEnabled(false);
        reportList.clear();
        currentIndex = 0;
        progressBar.setProgress(0);
        resultText.setText("");

        testNextStation();
    }

    private void testNextStation() {

        if (currentIndex >= stationList.size()) {
            showFinalReport();
            return;
        }

        Shoutcast station = stationList.get(currentIndex);

        statusText.setText("Checking: " + station.getName());

        if (player != null) {
            player.release();
        }

        player = new ExoPlayer.Builder(this).build();
        player.setVolume(0f);

        String url = station.getUrl();
        MediaItem mediaItem = MediaItem.fromUri(url);

        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        startTime = System.currentTimeMillis();

        player.addListener(new Player.Listener() {

            @Override
            public void onPlaybackStateChanged(int state) {

                if (state == Player.STATE_READY) {

                    long timeTaken = System.currentTimeMillis() - startTime;

                    addSuccessToReport(
                            station.getName(),
                            url,
                            timeTaken
                    );

                    cleanupAndContinue();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {

                addFailureToReport(
                        station.getName(),
                        url,
                        error.getMessage()
                );

                cleanupAndContinue();
            }
        });

        // Timeout
        handler.postDelayed(() -> {

            if (player != null &&
                    player.getPlaybackState() != Player.STATE_READY) {

                addFailureToReport(
                        station.getName(),
                        url,
                        "TIMEOUT"
                );

                cleanupAndContinue();
            }

        }, STREAM_TIMEOUT);
    }

    private void cleanupAndContinue() {

        handler.removeCallbacksAndMessages(null);

        if (player != null) {
            player.release();
            player = null;
        }

        currentIndex++;
        progressBar.setProgress(currentIndex);

        handler.postDelayed(this::testNextStation, 700);
    }

    private void addSuccessToReport(String name, String url, long time) {

        String success =
                "<font color='#2E7D32'>✔ SUCCESS</font><br>" +
                        "<b>" + name + "</b><br>" +
                        " URL: " + url + "<6+br>" +
                        " 0/////////////752Started in: " + time + " ms<br><br>";

        reportList.add(success);
    }

    private void addFailureToReport(String name, String url, String reason) {

        String failure =
                "<font color='#C62828'>✘ FAILED</font><br>" +
                        "<b>" + name + "</b><br>" +
                        " URL: " + url + "<br>" +
                        " Reason: " + reason + "<br><br>";

        reportList.add(failure);
    }

    private void showFinalReport() {

        statusText.setText("Validation Complete");

        StringBuilder fullReport = new StringBuilder();

        for (String entry : reportList) {
            fullReport.append(entry);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resultText.setText(
                    Html.fromHtml(fullReport.toString(),
                            Html.FROM_HTML_MODE_LEGACY)
            );
        }

        startButton.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }
}