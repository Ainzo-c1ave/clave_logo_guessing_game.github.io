package com.example.myapplication;

import android.content.Context;
import android.media.MediaPlayer;
import android.content.SharedPreferences;

import java.util.Random;

public class MusicManager {
    private static MediaPlayer mediaPlayer;
    private static int currentMusicResId = -1;

    public static void prepareMusic(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        
        // Only shuffle if music is not already playing or prepared
        if (currentMusicResId == -1) {
            int[] musicFiles = {R.raw.ballin8_know_me, R.raw.music_2};
            currentMusicResId = musicFiles[new Random().nextInt(musicFiles.length)];
        }
    }

    public static void startMusic(Context context) {
        if (currentMusicResId == -1) {
            prepareMusic(context);
        }

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.getApplicationContext(), currentMusicResId);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                updateVolume(context);
            }
        }

        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public static void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public static void updateVolume(Context context) {
        if (mediaPlayer != null) {
            SharedPreferences prefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            float volume = prefs.getInt("music_volume", 50) / 100.0f;
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public static void resetMusicSelection() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        currentMusicResId = -1;
    }

    public static MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}