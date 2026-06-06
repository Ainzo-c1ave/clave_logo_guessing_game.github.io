package com.example.myapplication;

import androidx.core.content.ContextCompat;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.text.TextWatcher;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private LinearLayout quizContentContainer;
    private MyDatabaseHelper myDB;
    private Button btnFinish, btnEdit, btnAddMore;
    private boolean isEditMode = false;
    private boolean isAddingNew = false;
    private View newBlankItemView;
    private List<String> correctAnswers = new ArrayList<>();
    private List<EditText> userInputs = new ArrayList<>();

    private String tempNewItemUri = "";
    private ImageView lastClickedImageView;
    private Animation buttonClickAnim;
    private MediaPlayer clickSound;
    private SettingsManager settingsManager;
    private float soundVolume = 1.0f;

    private String currentQuizType = "text";
    private String currentQuizTitle = "";
    private String currentUser = "DefaultUser";

    private final ActivityResultLauncher<Intent> quizImagePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null && lastClickedImageView != null) {
                        String localPath = saveImageToInternalStorage(imageUri);
                        if (localPath != null) {
                            tempNewItemUri = localPath;
                            lastClickedImageView.setImageURI(Uri.fromFile(new java.io.File(localPath)));
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        settingsManager = new SettingsManager(this);
        settingsManager.setup();
        settingsManager.setSettingsListener(volume -> soundVolume = volume);
        soundVolume = settingsManager.getSoundVolume();

        buttonClickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);
        clickSound = MediaPlayer.create(this, R.raw.click_sound);

        quizContentContainer = findViewById(R.id.quizContentContainer);
        btnFinish = findViewById(R.id.btnFinish);
        btnEdit = findViewById(R.id.btnEdit);
        btnAddMore = findViewById(R.id.btnAddMore);
        myDB = new MyDatabaseHelper(this);

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "DefaultUser");

        // Receive the Title from the Home Screen
        currentQuizTitle = getIntent().getStringExtra("QUIZ_TITLE");

        if (currentQuizTitle != null && !currentQuizTitle.isEmpty()) {
            loadQuizData(currentQuizTitle);
        }

        btnAddMore.setOnClickListener(v -> {
            animateAndPlaySound(v);
            if (!isAddingNew) {
                isAddingNew = true;
                isEditMode = true;
                btnEdit.setText("Cancel");
                btnFinish.setText("Save");
                addNewBlankItem();
            }
        });

        btnEdit.setOnClickListener(v -> {
            animateAndPlaySound(v);
            if (isAddingNew) {
                quizContentContainer.removeView(newBlankItemView);
                isAddingNew = false;
            }
            isEditMode = !isEditMode;
            if (isEditMode) enterEditMode();
            else exitEditMode();
        });

        btnFinish.setOnClickListener(v -> {
            animateAndPlaySound(v);
            if (isEditMode) {
                saveChanges();
            } else {
                checkScore();
            }
        });
    }

    private void animateAndPlaySound(View view) {
        if (buttonClickAnim != null) view.startAnimation(buttonClickAnim);
        if (clickSound != null) {
            clickSound.setVolume(soundVolume, soundVolume);
            clickSound.seekTo(0);
            clickSound.start();
        }
    }

    private void addNewBlankItem() {
        newBlankItemView = getLayoutInflater().inflate(R.layout.quiz_item, quizContentContainer, false);

        EditText etQ = newBlankItemView.findViewById(R.id.itemQuestionEdit);
        EditText etA = newBlankItemView.findViewById(R.id.itemAnswerInput);
        TextView tvQ = newBlankItemView.findViewById(R.id.itemQuestion);
        ImageView ivImg = newBlankItemView.findViewById(R.id.itemImage);

        tvQ.setVisibility(View.GONE);
        etQ.setVisibility(View.VISIBLE);
        etQ.setHint("Enter Question");
        // Update the Question input color:
        etQ.setTextColor(ContextCompat.getColor(this, R.color.box_text));

        // Update the Answer input color (inside the if statement):
        if (etA != null) {
            etA.setHint("Enter Answer");
            etA.setEnabled(true);
            etA.setTextColor(ContextCompat.getColor(this, R.color.box_text));
        }

        if (currentQuizType.equalsIgnoreCase("image")) {
            ivImg.setVisibility(View.VISIBLE);
            ivImg.setImageResource(R.drawable.add_image);
            ivImg.setOnClickListener(v -> {
                animateAndPlaySound(v);
                lastClickedImageView = ivImg;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                quizImagePicker.launch(intent);
            });
        } else {
            ivImg.setVisibility(View.GONE);
        }

        quizContentContainer.addView(newBlankItemView);

        quizContentContainer.post(() -> {
            ScrollView scrollView = (ScrollView) quizContentContainer.getParent();
            if (scrollView != null) scrollView.fullScroll(View.FOCUS_DOWN);
        });
    }

    // FIXED: Parameter is now String title
    private void loadQuizData(String title) { // Fixed: Changed int id to String title
        Cursor cursor = myDB.getQuizByTitle(title);
        quizContentContainer.removeAllViews();
        correctAnswers.clear();
        userInputs.clear();

        List<QuizItemData> list = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            currentQuizType = cursor.getString(2);
            do {
                list.add(new QuizItemData(
                        cursor.getInt(0),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5)
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }

        // --- THE SHUFFLE ---
        java.util.Collections.shuffle(list);

        for (QuizItemData item : list) {
            View itemView = getLayoutInflater().inflate(R.layout.quiz_item, quizContentContainer, false);
            itemView.setTag(item.id);

            TextView tvQ = itemView.findViewById(R.id.itemQuestion);
            EditText etQEdit = itemView.findViewById(R.id.itemQuestionEdit);
            EditText etAns = itemView.findViewById(R.id.itemAnswerInput);
            ImageView ivImg = itemView.findViewById(R.id.itemImage);

            // Change these 3 lines inside the loadQuizData loop:
            tvQ.setTextColor(ContextCompat.getColor(this, R.color.primary_ui));
            etQEdit.setTextColor(ContextCompat.getColor(this, R.color.box_text));
            etAns.setTextColor(ContextCompat.getColor(this, R.color.box_text));

            tvQ.setText(item.question);
            if (item.imagePath != null && !item.imagePath.isEmpty()) {
                ivImg.setVisibility(View.VISIBLE);
                ivImg.setImageURI(Uri.fromFile(new java.io.File(item.imagePath)));
            } else {
                ivImg.setVisibility(View.GONE);
            }

            correctAnswers.add(item.answer); // Stays synced with shuffled list
            userInputs.add(etAns);

            itemView.setOnLongClickListener(v -> {
                animateAndPlaySound(v);
                showItemDeleteConfirmation(item.id, item.question);
                return true;
            });

            quizContentContainer.addView(itemView);

            etAns.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Put original background back
                    itemView.setBackgroundResource(R.drawable.white_rounded_bg);
                    // Put original tint back
                    itemView.setBackgroundTintList(ContextCompat.getColorStateList(QuizActivity.this, R.color.box_bg));
                }

                @Override
                public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void showItemDeleteConfirmation(int rowId, String question) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Question")
                .setMessage("Remove this question?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    myDB.deleteQuizById(rowId);

                    // FIXED: Change currentQuizId to currentQuizTitle
                    loadQuizData(currentQuizTitle);

                    Toast.makeText(this, "Question removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkScore() {
        int score = 0;
        int total = correctAnswers.size();

        // 1. EVALUATION PHASE (Colors Only, No Clearing!)
        for (int i = 0; i < total; i++) {
            View itemView = quizContentContainer.getChildAt(i);
            String userGuess = normalize(userInputs.get(i).getText().toString());
            String actualAnswer = normalize(correctAnswers.get(i));

            if (userGuess.equals(actualAnswer)) {
                score++;
                itemView.setBackgroundResource(R.drawable.white_rounded_bg);
                itemView.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.box_bg));
            } else {
                itemView.setBackgroundResource(R.drawable.error_item_bg);
                itemView.setBackgroundTintList(null); // Let the red shine!

                Animation shake = AnimationUtils.loadAnimation(this, R.anim.button_click);
                itemView.startAnimation(shake);
            }
        }

        // 2. SHOW DIALOG PHASE
        new AlertDialog.Builder(this)
                .setTitle("Quiz Results")
                .setMessage("You scored " + score + " out of " + total + "!")
                .setCancelable(false) // Forces them to click Finish or Try Again
                .setPositiveButton("Finish", (dialog, which) -> finish())
                .setNegativeButton("Try Again", (dialog, which) -> {

                    boolean focusSet = false; // Tracker to find the FIRST mistake

                    // 3. AUTO-CLEAR & FOCUS PHASE
                    for (int i = 0; i < total; i++) {
                        String userGuess = normalize(userInputs.get(i).getText().toString());
                        String actualAnswer = normalize(correctAnswers.get(i));

                        if (!userGuess.equals(actualAnswer)) {
                            // It's a wrong answer! Clear it.
                            // (Note: This safely triggers the TextWatcher to remove the red glow)
                            userInputs.get(i).setText("");

                            // Put the cursor in the VERY FIRST wrong box we find
                            if (!focusSet) {
                                userInputs.get(i).requestFocus();
                                focusSet = true;
                            }
                        }
                    }

                    // Scroll to the top so they can start fresh
                    ScrollView scrollView = (ScrollView) quizContentContainer.getParent();
                    if (scrollView != null) scrollView.fullScroll(View.FOCUS_UP);

                    Toast.makeText(this, "Wrong answers cleared. Good luck!", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void enterEditMode() {
        btnEdit.setText("Cancel");
        btnFinish.setText("Save");
        for (int i = 0; i < quizContentContainer.getChildCount(); i++) {
            View itemView = quizContentContainer.getChildAt(i);
            TextView tvQ = itemView.findViewById(R.id.itemQuestion);
            EditText etQEdit = itemView.findViewById(R.id.itemQuestionEdit);
            EditText etAns = itemView.findViewById(R.id.itemAnswerInput);

            if (tvQ != null && etQEdit != null) {
                tvQ.setVisibility(View.GONE);
                etQEdit.setVisibility(View.VISIBLE);
                etQEdit.setText(tvQ.getText());
            }
            if (etAns != null) {
                // When editing, show the real answer in the box
                etAns.setText(correctAnswers.get(i));
            }
        }
    }

    private void exitEditMode() {
        isEditMode = false;
        isAddingNew = false;
        btnEdit.setText("Edit");
        btnFinish.setText("Finish");
        for (int i = 0; i < quizContentContainer.getChildCount(); i++) {
            View itemView = quizContentContainer.getChildAt(i);
            TextView tvQ = itemView.findViewById(R.id.itemQuestion);
            EditText etQEdit = itemView.findViewById(R.id.itemQuestionEdit);
            EditText etAns = itemView.findViewById(R.id.itemAnswerInput);

            if (tvQ != null && etQEdit != null) {
                tvQ.setVisibility(View.VISIBLE);
                etQEdit.setVisibility(View.GONE);
            }
            if (etAns != null) {
                etAns.setText(""); // Clear for the quiz
            }
        }
    }

    private void saveChanges() {
        if (isAddingNew) {
            EditText etQ = newBlankItemView.findViewById(R.id.itemQuestionEdit);
            EditText etA = newBlankItemView.findViewById(R.id.itemAnswerInput);
            String q = etQ.getText().toString().trim();
            String a = etA.getText().toString().trim();

            if (!q.isEmpty() && !a.isEmpty()) {
                myDB.addQuizItem(currentUser, currentQuizTitle, currentQuizType, q, a, tempNewItemUri);
                tempNewItemUri = "";
                loadQuizData(currentQuizTitle);
                exitEditMode();
            } else {
                Toast.makeText(this, "Fill in all fields!", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Loop through all items to save updates
            for (int i = 0; i < quizContentContainer.getChildCount(); i++) {
                View itemView = quizContentContainer.getChildAt(i);
                if (itemView.getTag() != null) {
                    int rowId = (int) itemView.getTag();
                    EditText etQ = itemView.findViewById(R.id.itemQuestionEdit);
                    EditText etA = itemView.findViewById(R.id.itemAnswerInput);

                    // ADD .trim() HERE to keep the database clean
                    String cleanQ = etQ.getText().toString().trim();
                    String cleanA = etA.getText().toString().trim();

                    myDB.updateQuizItem(rowId, cleanQ, cleanA);
                }
            }
            Toast.makeText(this, "All Changes Saved!", Toast.LENGTH_SHORT).show();
            loadQuizData(currentQuizTitle);
            exitEditMode();
        }
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            String fileName = "quiz_img_" + System.currentTimeMillis() + ".jpg";
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            java.io.FileOutputStream outputStream = openFileOutput(fileName, MODE_PRIVATE);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
            return getFileStreamPath(fileName).getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String normalize(String text) {
        if (text == null) return "";
        // toLowerCase handles case sensitivity
        // trim() handles the accidental spaces at the end
        // replaceAll("[\\s\\-]", "") handles spaces or dashes inside the name
        return text.toLowerCase().trim().replaceAll("[\\s\\-]", "");
    }




    private static class QuizItemData {
        int id;
        String question, answer, imagePath;
        QuizItemData(int id, String question, String answer, String imagePath) {
            this.id = id;
            this.question = question;
            this.answer = answer;
            this.imagePath = imagePath;
        }
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }
    }
}