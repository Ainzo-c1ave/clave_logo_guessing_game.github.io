package com.example.myapplication;

import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button; // Added
import android.widget.EditText; // Added
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GuessActivity extends AppCompatActivity {

    // 1. DECLARE VARIABLES HERE (Class Scope)
    // This allows both onCreate and checkUserGuess to see them
    private HashMap<Integer, String> answerKey;
    private EditText editTextGuessInput;
    private int selectedImageId;
    private Button buttonGuess;
    private Animation buttonClickAnim;
    private MediaPlayer clickSound;
    private List<Integer> allImages;
    private int currentImageIndex = 0;
    private ImageView imageGuess; // Move this to class level so we can update it easily
    private MyDatabaseHelper myDB;
    private SettingsManager settingsManager;
    private float soundVolume = 1.0f;

    private void setupAnswers() {
        answerKey = new HashMap<>();
        answerKey.put(R.drawable.blank1, "ABS-CBN");
        answerKey.put(R.drawable.blank2, "China Bank");
        answerKey.put(R.drawable.blank3, "Andoks");
        answerKey.put(R.drawable.blank4, "MangInasal");
        answerKey.put(R.drawable.blank5, "BangkoSentral");
        answerKey.put(R.drawable.blank6, "SM");
        answerKey.put(R.drawable.blank7, "BPI");
        answerKey.put(R.drawable.blank8, "Caltex");
        answerKey.put(R.drawable.blank9, "CebuPacific");
        answerKey.put(R.drawable.blank10, "Chowking");
        answerKey.put(R.drawable.blank11, "DA");
        answerKey.put(R.drawable.blank12, "DOT");
        answerKey.put(R.drawable.blank13, "Dunkin' Donuts");
        answerKey.put(R.drawable.blank14, "EnchantedKingdom");
        answerKey.put(R.drawable.blank15, "Globe");
        answerKey.put(R.drawable.blank16, "GMA");
        answerKey.put(R.drawable.blank17, "Goldilocks");
        answerKey.put(R.drawable.blank18, "GreenWich");
        answerKey.put(R.drawable.blank19, "Jollibee");
        answerKey.put(R.drawable.blank20, "KCO");
        answerKey.put(R.drawable.blank21, "KFC");
        answerKey.put(R.drawable.blank22, "Knorr");
        answerKey.put(R.drawable.blank23, "Lucky Me");
        answerKey.put(R.drawable.blank24, "5 Star");
        answerKey.put(R.drawable.blank25, "Magic Sarap");
        answerKey.put(R.drawable.blank26, "McDonalds");
        answerKey.put(R.drawable.blank27, "Meralco");
        answerKey.put(R.drawable.blank28, "Mercury Drug Store");
        answerKey.put(R.drawable.blank29, "Metrobank");
        answerKey.put(R.drawable.blank30, "Mototrade");
        answerKey.put(R.drawable.blank31, "PCSO");
        answerKey.put(R.drawable.blank32, "Petron");
        answerKey.put(R.drawable.blank33, "PhilHealth");
        answerKey.put(R.drawable.blank34, "PLDT");
        answerKey.put(R.drawable.blank35, "PNB");
        answerKey.put(R.drawable.blank36, "PSBank");
        answerKey.put(R.drawable.blank37, "PureGold");
        answerKey.put(R.drawable.blank38, "RCBC");
        answerKey.put(R.drawable.blank39, "Red Horse Beer");
        answerKey.put(R.drawable.blank40, "Robinsons");
        answerKey.put(R.drawable.blank41, "Security Bank");
        answerKey.put(R.drawable.blank42, "Shell");
        answerKey.put(R.drawable.blank43, "Smart");
        answerKey.put(R.drawable.blank44, "SSS");
        answerKey.put(R.drawable.blank45, "STI");
        answerKey.put(R.drawable.blank46, "SUN");
        answerKey.put(R.drawable.blank47, "TNT");
        answerKey.put(R.drawable.blank48, "TesDa");
        answerKey.put(R.drawable.blank49, "Unilever");
        answerKey.put(R.drawable.blank50, "AMA");
        answerKey.put(R.drawable.blank51, "Bingo Plus");
        answerKey.put(R.drawable.blank52, "Champion");
        answerKey.put(R.drawable.blank53, "Cherry Mobile");
        answerKey.put(R.drawable.blank54, "Converge");
        answerKey.put(R.drawable.blank55, "DITO");
        answerKey.put(R.drawable.blank56, "Guanzon");
        answerKey.put(R.drawable.blank57, "Kuya J");
        answerKey.put(R.drawable.blank58, "Nature Spring");
        answerKey.put(R.drawable.blank59, "Nova");
        answerKey.put(R.drawable.blank60, "Orocan");
        answerKey.put(R.drawable.blank61, "PAGCOR");
        answerKey.put(R.drawable.blank62, "Piattos");
        answerKey.put(R.drawable.blank63, "Rusi");
        answerKey.put(R.drawable.blank64, "Standard");
        answerKey.put(R.drawable.blank65, "Tender Juicy");
        answerKey.put(R.drawable.blank66, "Zest-O");
        answerKey.put(R.drawable.blank67, "Victory Liner");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guess);

        setupAnswers();
        myDB = new MyDatabaseHelper(this);

        settingsManager = new SettingsManager(this);
        settingsManager.setup();
        settingsManager.setSettingsListener(volume -> soundVolume = volume);
        soundVolume = settingsManager.getSoundVolume();

        // 1. Initialize class-level variables (No "ImageView" or "Button" at the start)
        imageGuess = findViewById(R.id.image_Guess);
        editTextGuessInput = findViewById(R.id.editTextGuessInput);
        buttonGuess = findViewById(R.id.buttonGuess);

        // 2. Prepare the list
        allImages = new ArrayList<>();
        for (int i = 1; i <= 67; i++) {
            int resId = getResources().getIdentifier("blank" + i, "drawable", getPackageName());
            if (resId != 0) allImages.add(resId);
        }
        java.util.Collections.shuffle(allImages);

        // 3. Get the image from Intent AND find its position in our shuffled list
        selectedImageId = getIntent().getIntExtra("SELECTED_IMAGE", 0);
        if (selectedImageId != 0) {
            imageGuess.setImageResource(selectedImageId);
            // This ensures the next random image is actually the "next" one in the list
            currentImageIndex = allImages.indexOf(selectedImageId);
        }

        // 4. Effects
        buttonClickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);
        clickSound = MediaPlayer.create(this, R.raw.click_sound);

        buttonGuess.setOnClickListener(v -> {
            animateAndPlaySound(v);
            checkUserGuess();
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void checkUserGuess() {
        // 1. Get the raw input
        String rawInput = editTextGuessInput.getText().toString().trim();

        // 2. Clean/Normalize the user input
        String cleanInput = normalize(rawInput);

        if (answerKey.containsKey(selectedImageId)) {
            // 3. Get and clean the correct answer from your HashMap
            String correctAnswer = answerKey.get(selectedImageId);
            String cleanAnswer = normalize(correctAnswer);

            if (cleanInput.equals(cleanAnswer)) {
                SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                String currentUser = pref.getString("username", "Player");

                // --- PUT THE NEW CODE HERE ---
                // 1. Get the name of the image (e.g., "blank1") instead of its ID number
                String imageName = getResources().getResourceEntryName(selectedImageId);

                // 2. Save that name to the database
                myDB.markAsGuessed(currentUser, imageName);

                // 3. Reveal logic (stays the same)
                String originalName = imageName.replace("blank", "image");
                int originalResId = getResources().getIdentifier(originalName, "drawable", getPackageName());
                imageGuess.setImageResource(originalResId);

                // 1. Get the correct name from your HashMap using the selected ID
                String logoName = answerKey.get(selectedImageId);
                // 2. Update the Toast to show the specific name
                Toast.makeText(this, "Correct! It's " + logoName + "!", Toast.LENGTH_SHORT).show();

                imageGuess.postDelayed(() -> {
                    loadNextRandomImage();
                }, 2000);
            } else {
                Toast.makeText(this, "Wrong! Try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error: Answer not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNextRandomImage() {
        currentImageIndex++;

        // Check if we still have images left in the shuffled list
        if (currentImageIndex < allImages.size()) {
            // Update the ID to the next one in the list
            selectedImageId = allImages.get(currentImageIndex);

            // Update the UI
            ImageView imageGuess = findViewById(R.id.image_Guess); // Ensure this is accessible
            imageGuess.setImageResource(selectedImageId);

            // Clear the input field for the new round
            editTextGuessInput.setText("");
        } else {
            // If they finished all logos
            Toast.makeText(this, "Congratulations! You finished all logos!", Toast.LENGTH_LONG).show();
            finish(); // Close the activity and go back to the menu
        }
    }

    // Add this at the bottom of GuessActivity class
    private String normalize(String text) {
        if (text == null) return "";
        // 1. Convert to lowercase
        // 2. replaceAll("[\\s\\-]", "") removes all spaces (\\s) and dashes (\\-)
        return text.toLowerCase().replaceAll("[\\s\\-]", "");
    }

    private void animateAndPlaySound(View view) {
        // Start the scale animation (the bounce)
        if (buttonClickAnim != null) {
            view.startAnimation(buttonClickAnim);
        }
        // Play the click sound
        if (clickSound != null) {
            clickSound.setVolume(soundVolume, soundVolume);
            clickSound.seekTo(0); // Reset to start in case user clicks fast
            clickSound.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pauseMusic();
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.startMusic(this);
        settingsManager.setMediaPlayer(MusicManager.getMediaPlayer());
    }

}
