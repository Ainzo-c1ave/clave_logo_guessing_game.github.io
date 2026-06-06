package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ToGuess_Activity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PictureAdapter adapter;
    private MyDatabaseHelper myDB;
    private SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_to_guess);

        settingsManager = new SettingsManager(this);
        settingsManager.setup();

        recyclerView = findViewById(R.id.pictureRecyclerView);
        ImageButton btnReset = findViewById(R.id.btnReset);

        myDB = new MyDatabaseHelper(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new PictureAdapter(new ArrayList<>(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        btnReset.setOnClickListener(v -> {
            // 1. Get the current user
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String currentUser = pref.getString("username", "Player");

            // 2. Show a confirmation dialog
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Reset All Progress?")
                    .setMessage(currentUser + ", do you really want to clear all your guessed logos? Remember, this cannot be undone.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // 3. Wipe the DB
                        myDB.resetUserProgress(currentUser);

                        // 4. Refresh the grid (we will update loadLogoGrid next to handle the shuffle)
                        loadLogoGrid();

                        Toast.makeText(this, "Progress Wiped! Good luck!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
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
        loadLogoGrid();
    }

    private void loadLogoGrid() {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = pref.getString("username", "Player");

        // 1. Create a list of numbers from 1 to 67
        List<Integer> logoIndices = new ArrayList<>();
        for (int i = 1; i <= 67; i++) {
            logoIndices.add(i);
        }

        // 2. SHUFFLE: Randomize the order of the numbers
        java.util.Collections.shuffle(logoIndices);

        List<Integer> unguessedList = new ArrayList<>();
        List<Integer> guessedList = new ArrayList<>();

        // 3. Loop through our SHUFFLED numbers
        for (int i : logoIndices) {
            int blankId = getResources().getIdentifier("blank" + i, "drawable", getPackageName());
            int originalId = getResources().getIdentifier("image" + i, "drawable", getPackageName());
            String blankName = "blank" + i;

            if (myDB.isLogoGuessed(currentUser, blankName)) {
                guessedList.add(originalId != 0 ? originalId : blankId);
            } else {
                unguessedList.add(blankId);
            }
        }

        // Combine: Unguessed (Top) + Guessed (Bottom)
        List<Integer> masterList = new ArrayList<>();
        List<Boolean> statusList = new ArrayList<>();

        masterList.addAll(unguessedList);
        for (int i = 0; i < unguessedList.size(); i++) statusList.add(false);

        masterList.addAll(guessedList);
        for (int i = 0; i < guessedList.size(); i++) statusList.add(true);

        adapter.updateData(masterList, statusList);
    }

}