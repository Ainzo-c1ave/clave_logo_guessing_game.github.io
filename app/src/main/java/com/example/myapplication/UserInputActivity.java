package com.example.myapplication;

import androidx.core.content.ContextCompat;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import android.media.MediaPlayer;// Add this import
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class UserInputActivity extends AppCompatActivity {

    private LinearLayout layoutTextInput, layoutImageInput, quizListContainer;
    private Button btnPickImage, btnMainAction; // Renamed to match XML

    private EditText etTextQuestion, etTextAnswer, etImageQuestion, etImageAnswer, etQuizTitle;
    private MyDatabaseHelper myDB;
    private String currentType = "";
    private String selectedImageUri = ""; // To store the full path
    private boolean isAdding = false; // To track if we are in "Save" mode or "Add" mode
    private MediaPlayer clickSound; // Add the media player
    private Animation buttonClickAnim; // Add this variable
    private SettingsManager settingsManager;
    private float soundVolume = 1.0f;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        // CHANGE: Copy the image and save the NEW local path
                        String localPath = saveImageToInternalStorage(imageUri);
                        if (localPath != null) {
                            selectedImageUri = localPath;
                            btnPickImage.setText(getFileName(imageUri));
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_input);

        settingsManager = new SettingsManager(this);
        settingsManager.setup();
        settingsManager.setSettingsListener(volume -> soundVolume = volume);
        soundVolume = settingsManager.getSoundVolume();

        // Standard Initializations
        buttonClickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);
        clickSound = MediaPlayer.create(this, R.raw.click_sound);
        myDB = new MyDatabaseHelper(this);

        // Find Views
        quizListContainer = findViewById(R.id.quizListContainer);
        btnMainAction = findViewById(R.id.btnMainAction);
        etQuizTitle = findViewById(R.id.etQuizTitle);
        layoutTextInput = findViewById(R.id.layoutTextInput);
        layoutImageInput = findViewById(R.id.layoutImageInput);
        btnPickImage = findViewById(R.id.btnPickImage);

        etTextQuestion = findViewById(R.id.etTextQuestion);
        etTextAnswer = findViewById(R.id.etTextAnswer);
        etImageQuestion = findViewById(R.id.etImageQuestion);
        etImageAnswer = findViewById(R.id.etImageAnswer);

        refreshQuizList();

        // CLEANED UP: Only ONE listener for btnMainAction
        btnMainAction.setOnClickListener(v -> {
            animateAndPlaySound(v);
            if (!isAdding) {
                showChoiceDialog();
            } else {
                saveDataToDB();
            }
        });

        btnPickImage.setOnClickListener(v -> {
            animateAndPlaySound(v);
            openGallery();
        });
    }

    private void refreshQuizList() {
        quizListContainer.removeAllViews();
        // --- ADD THESE TWO LINES HERE ---
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = pref.getString("username", "DefaultUser");

        // --- UPDATE THIS LINE TO PASS currentUser ---
        Cursor cursor = myDB.getAllQuizzes(currentUser);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                final String quizTitle = cursor.getString(1); // Column 1: Title

                // SAFETY CHECK: Skip buttons that would say "null"
                if (quizTitle == null || quizTitle.equals("null") || quizTitle.isEmpty()) {
                    continue;
                }

                // Inside refreshQuizList() ...
                Button btn = new Button(this);
                btn.setText(quizTitle);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

                // --- NEW MARGIN FIX: Convert 16dp to actual pixels so it spaces out nicely ---
                int marginPx = (int) (16 * getResources().getDisplayMetrics().density);
                params.setMargins(0, 0, 0, marginPx);

                btn.setLayoutParams(params);
                btn.setBackgroundResource(R.drawable.custom_button_bg);

                // --- NEW COLOR FIX: Use main_text_button instead of game_bg ---
                btn.setTextColor(ContextCompat.getColor(this, R.color.game_bg));
                btn.setAllCaps(false);

                btn.setOnClickListener(v -> {
                    animateAndPlaySound(v);
                    Intent intent = new Intent(UserInputActivity.this, QuizActivity.class);
                    intent.putExtra("QUIZ_TITLE", quizTitle);
                    startActivity(intent);
                });

                // Use ONLY this one for the Long Click (Delete the other one)
                btn.setOnLongClickListener(v -> {
                    animateAndPlaySound(v);
                    showGroupDeleteConfirmation(quizTitle);
                    return true;
                });

                quizListContainer.addView(btn);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private void showGroupDeleteConfirmation(String title) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Quiz")
                .setMessage("Do you want to delete the entire \"" + title + "\" quiz?")
                .setPositiveButton("Yes, Delete All", (dialog, which) -> {
                    // You'll need to add deleteQuizByTitle in your DB helper
                    myDB.deleteQuizByTitle(title);
                    refreshQuizList();
                    Toast.makeText(this, "Deleted " + title, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
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
        refreshQuizList(); // Updates the buttons if titles changed or were deleted
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }

    private void saveDataToDB() {
        String title = etQuizTitle.getText().toString().trim();
        String q, a, file = "";

        // --- ADD THESE TWO LINES HERE ---
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String currentUser = pref.getString("username", "DefaultUser");

        // Decide which "Starter" inputs to use
        if (currentType.equals("text")) {
            q = etTextQuestion.getText().toString().trim();
            a = etTextAnswer.getText().toString().trim();
        } else {
            q = etImageQuestion.getText().toString().trim();
            a = etImageAnswer.getText().toString().trim();
            file = selectedImageUri;
        }

        if (title.isEmpty() || q.isEmpty() || a.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        } else {
            myDB.addQuizItem(currentUser, title, currentType, q, a, file);
            resetUI(); // Clear the form
            refreshQuizList();
            Toast.makeText(this, "Quiz Created!", Toast.LENGTH_SHORT).show();
        }
    }

    private void resetUI() {
        isAdding = false;
        btnMainAction.setText("Add Quiz");
        etQuizTitle.setText("");
        etQuizTitle.setVisibility(View.GONE);
        layoutTextInput.setVisibility(View.GONE);
        layoutImageInput.setVisibility(View.GONE);

        // Clear Starter text boxes
        etTextQuestion.setText("");
        etTextAnswer.setText("");
        etImageQuestion.setText("");
        etImageAnswer.setText("");
        selectedImageUri = "";
    }

    private void showChoiceDialog() {
        String[] options = {"Text Question", "Image Question"};
        new AlertDialog.Builder(this)
                .setTitle("Select Type")
                .setItems(options, (dialog, which) -> {
                    isAdding = true; // Set state to "Adding"
                    btnMainAction.setText("Save Item"); // Change button text
                    etQuizTitle.setVisibility(View.VISIBLE); // Show title field

                    if (which == 0) {
                        currentType = "text";
                        layoutTextInput.setVisibility(View.VISIBLE);
                        layoutImageInput.setVisibility(View.GONE);
                    } else {
                        currentType = "image";
                        layoutImageInput.setVisibility(View.VISIBLE);
                        layoutTextInput.setVisibility(View.GONE);
                    }
                }).show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            // Create a unique name for the image
            String fileName = "quiz_img_" + System.currentTimeMillis() + ".jpg";

            // Open the original image and a new local file
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = openFileOutput(fileName, MODE_PRIVATE);

            // Copy the data
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            // Return the absolute path to the new local file
            return getFileStreamPath(fileName).getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private void showSingleDeleteConfirmation(int quizId, String title) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Quiz")
                .setMessage("Do you really want to delete \"" + title + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    myDB.deleteQuizById(quizId);
                    refreshQuizList(); // Refresh to remove the button from screen
                    Toast.makeText(this, "Deleted " + title, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    // New helper method to handle both animation and sound
    private void animateAndPlaySound(View view) {
        if (buttonClickAnim != null) {
            view.startAnimation(buttonClickAnim);
        }
        playClickSound();
    }

    private void playClickSound() {
        if (clickSound != null) {
            // Reset to beginning in case they click fast
            clickSound.setVolume(soundVolume, soundVolume);
            clickSound.seekTo(0);
            clickSound.start();
        }
    }
}