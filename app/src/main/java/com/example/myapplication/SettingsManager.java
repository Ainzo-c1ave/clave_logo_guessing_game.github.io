package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class SettingsManager {

    private final Activity activity;
    private MediaPlayer mediaPlayer;
    private final SharedPreferences prefs;

    public interface SettingsListener {
        void onSoundVolumeChanged(float volume);
    }

    private SettingsListener listener;

    public SettingsManager(Activity activity) {
        this.activity = activity;
        this.prefs = activity.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
    }

    public void setMediaPlayer(MediaPlayer mp) {
        this.mediaPlayer = mp;
        if (mediaPlayer != null) {
            float vol = prefs.getInt("music_volume", 50) / 100.0f;
            mediaPlayer.setVolume(vol, vol);
        }
    }

    public void setSettingsListener(SettingsListener listener) {
        this.listener = listener;
        if (this.listener != null) {
            this.listener.onSoundVolumeChanged(prefs.getInt("sound_volume", 70) / 100.0f);
        }
    }

    public float getSoundVolume() {
        return prefs.getInt("sound_volume", 70) / 100.0f;
    }

    public void setup() {
        ImageView settingsIcon = activity.findViewById(R.id.settings_icon);
        LinearLayout settingsDropdown = activity.findViewById(R.id.layout_SettingsDropdown);
        SeekBar seekMusic = activity.findViewById(R.id.seekbar_Music);
        SeekBar seekSound = activity.findViewById(R.id.seekbar_Sound);
        Button btnLogout = activity.findViewById(R.id.btnLogout);
        Button btnDeleteAccount = activity.findViewById(R.id.btnDeleteAccount);

        if (settingsIcon == null || settingsDropdown == null) return;

        // Set Initial Progress
        seekMusic.setProgress(prefs.getInt("music_volume", 50));
        seekSound.setProgress(prefs.getInt("sound_volume", 70));

        settingsIcon.setOnClickListener(v -> {
            v.animate().rotationBy(360).setDuration(300).start();
            if (settingsDropdown.getVisibility() == View.GONE) {
                settingsDropdown.setVisibility(View.VISIBLE);
            } else {
                settingsDropdown.setVisibility(View.GONE);
            }
        });

        seekMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float vol = progress / 100.0f;
                if (mediaPlayer != null) mediaPlayer.setVolume(vol, vol);
                MusicManager.updateVolume(activity); // Keep MusicManager in sync
                prefs.edit().putInt("music_volume", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float vol = progress / 100.0f;
                if (listener != null) listener.onSoundVolumeChanged(vol);
                prefs.edit().putInt("sound_volume", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnLogout.setOnClickListener(v -> {
            String currentUser = prefs.getString("username", "Player");
            new AlertDialog.Builder(activity)
                    .setTitle("Logout")
                    .setMessage(currentUser + ", are you sure you want to go back to the login screen?")
                    .setPositiveButton("Yes", (dialog, which) -> performLogout())
                    .setNegativeButton("No", null)
                    .show();
        });

        btnDeleteAccount.setOnClickListener(v -> {
            String currentUser = prefs.getString("username", "Player");
            new AlertDialog.Builder(activity)
                    .setTitle("Delete Account")
                    .setMessage(currentUser + ", are you sure you want to delete your account? This will permanently erase your progress and quizzes!")
                    .setPositiveButton("DELETE PERMANENTLY", (dialog, which) -> {
                        MyDatabaseHelper db = new MyDatabaseHelper(activity);
                        db.deleteUserAccount(currentUser);
                        performLogout();
                        Toast.makeText(activity, "Account Deleted", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        });
    }

    private void performLogout() {
        prefs.edit().putBoolean("isLoggedIn", false).putString("username", "").apply();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}