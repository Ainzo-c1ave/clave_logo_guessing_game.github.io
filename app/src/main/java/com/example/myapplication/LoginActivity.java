package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private boolean isRegisterMode = false;

    EditText editUser, editPass;
    Button btnLogin;
    MyDatabaseHelper db;
    TextView txtRegister;
    SettingsManager settingsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        settingsManager = new SettingsManager(this);
        settingsManager.setup();

        // 1. Session Check
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        if (pref.getBoolean("isLoggedIn", false)) {
            MusicManager.prepareMusic(this);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // 2. Initialize Views
        db = new MyDatabaseHelper(this);
        editUser = findViewById(R.id.editUsername);
        editPass = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        // 3. Set Initial State
        updateRegisterLink("Create Account");

        // 4. Main Action Button (Login/Register)
        btnLogin.setOnClickListener(v -> {
            String user = editUser.getText().toString().trim();
            String pass = editPass.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isRegisterMode) {
                if (db.checkUserExists(user)) {
                    Toast.makeText(this, "Username already taken!", Toast.LENGTH_SHORT).show();
                } else {
                    db.addUser(user, pass);
                    Toast.makeText(this, "Account Created! Welcome, " + user, Toast.LENGTH_SHORT).show();
                    loginSuccess(user, true);
                }
            } else {
                if (db.checkUser(user, pass)) {
                    loginSuccess(user, false);
                } else {
                    Toast.makeText(this, "Wrong username or password.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 5. Toggle Mode Link
        txtRegister.setOnClickListener(v -> {
            isRegisterMode = !isRegisterMode;

            if (isRegisterMode) {
                btnLogin.setText("REGISTER");
                updateRegisterLink("Back to Login");
            } else {
                btnLogin.setText("LOGIN");
                updateRegisterLink("Create Account");
            }

            editUser.setText("");
            editPass.setText("");
            editUser.requestFocus();
        });
    }

    // Helper to keep the underline style consistent
    private void updateRegisterLink(String text) {
        android.text.SpannableString content = new android.text.SpannableString(text);
        content.setSpan(new android.text.style.UnderlineSpan(), 0, text.length(), 0);
        txtRegister.setText(content);
    }

    private void loginSuccess(String username, boolean isNewAccount) {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().putBoolean("isLoggedIn", true).apply();
        pref.edit().putString("username", username).apply();

        MusicManager.resetMusicSelection();
        MusicManager.prepareMusic(this);

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("IS_NEW_ACCOUNT", isNewAccount);
        startActivity(intent);
        finish();
    }
}