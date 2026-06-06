package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast; // Added for popups

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private SoundPool soundPool;
    private int clickSoundId;
    private boolean isLoaded = false;
    private float soundVolume = 1.0f;
    private SettingsManager settingsManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- Welcome Animation ---
        TextView txtWelcome = findViewById(R.id.txtWelcomeBack);
        boolean isNewAccount = getIntent().getBooleanExtra("IS_NEW_ACCOUNT", false);

        settingsManager = new SettingsManager(this);
        settingsManager.setup();
        settingsManager.setSettingsListener(volume -> soundVolume = volume);
        soundVolume = settingsManager.getSoundVolume();
        
        MusicManager.startMusic(this);
        settingsManager.setMediaPlayer(MusicManager.getMediaPlayer());

        if (!isNewAccount) {
            SharedPreferences welcomePrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = welcomePrefs.getString("username", "Player");

            txtWelcome.setText("Welcome back, " + username + "!");

            txtWelcome.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(800)
                    .setStartDelay(500)
                    .withEndAction(() -> {
                        txtWelcome.animate()
                                .alpha(0f)
                                .setStartDelay(3000)
                                .setDuration(1000)
                                .start();
                    })
                    .start();
        } else {
            txtWelcome.setVisibility(View.GONE);
        }

        // --- UI Setup ---
        Button btnPlay = findViewById(R.id.btn_Play);
        Button btnQuizzMode = findViewById(R.id.btn_QuizzMode);

        // --- 2. SoundPool Setup ---
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME) // Changed from SONIFICATION
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        clickSoundId = soundPool.load(this, R.raw.click_sound, 1);

        // Visual "Popups" (Toasts) for debugging
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                isLoaded = true;
            }
        });

        btnPlay.setOnClickListener(v -> {
            applyClickEffects(v, clickSoundId);
            startActivity(new Intent(MainActivity.this, ToGuess_Activity.class));
        });

        btnQuizzMode.setOnClickListener(v -> {
            applyClickEffects(v, clickSoundId);
            startActivity(new Intent(MainActivity.this, UserInputActivity.class));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void applyClickEffects(View view, int soundId) {
        Log.d("EnzoDebug", "Attempting to play sound ID: " + soundId);

        if (soundPool != null) {
            int streamId = soundPool.play(soundId, soundVolume, soundVolume, 1, 0, 1.0f);
            if (streamId == 0) {
                Log.e("SoundPool", "Sound failed to play! Check stream limits.");
            }
        }

        // Always keep the animation so the UI feels responsive
        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() ->
                view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
        ).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.startMusic(this);
        settingsManager.setMediaPlayer(MusicManager.getMediaPlayer());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private void performLogout() {
        // 1. Clear the "Session"
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.putString("username", "");
        editor.apply();

        MusicManager.resetMusicSelection();

        // 2. Go back to Login
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // These flags clear the activity "stack" so they can't click "Back" to enter the game again
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        Toast.makeText(this, "Logged Out Successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}