package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor; // Added this import
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;

public class MyDatabaseHelper extends SQLiteOpenHelper {

    private Context context;
    private static final String DATABASE_NAME = "QuizGame.db";
    private static final int DATABASE_VERSION = 4; // Incremented to version 4 to add logo_progress table

    private static final String TABLE_NAME = "my_quiz";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_TITLE = "quiz_title"; // New: To store "Quiz 1", "Quiz 2"
    private static final String COLUMN_TYPE = "input_type";
    private static final String COLUMN_QUESTION = "question";
    private static final String COLUMN_ANSWER = "answer";
    private static final String COLUMN_FILENAME = "file_name";
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";
    private static final String COLUMN_USER_OWNER = "user_owner";
    private static final String TABLE_PROGRESS = "logo_progress";
    private static final String COL_PROGRESS_USER = "username";
    private static final String COL_PROGRESS_LOGO_ID = "logo_id";

    public MyDatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    // Add this to MyDatabaseHelper.java
    public Cursor getQuizById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Look for the specific row the user clicked
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " = " + id, null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Added COLUMN_TITLE to the query
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_OWNER + " TEXT, " + // This links the quiz to the user
                COLUMN_TITLE + " TEXT, " +
                COLUMN_TYPE + " TEXT, " +
                COLUMN_QUESTION + " TEXT, " +
                COLUMN_ANSWER + " TEXT, " +
                COLUMN_FILENAME + " TEXT);";
        db.execSQL(query);

        // Add the Users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT, " +
                COL_PASSWORD + " TEXT);";
        db.execSQL(createUsersTable);

        // Inside onCreate:
        String createProgressTable = "CREATE TABLE " + TABLE_PROGRESS + " (" +
                COL_PROGRESS_USER + " TEXT, " +
                COL_PROGRESS_LOGO_ID + " INTEGER, " +
                "PRIMARY KEY (" + COL_PROGRESS_USER + ", " + COL_PROGRESS_LOGO_ID + "));";
        db.execSQL(createProgressTable);

    }

    // Add this method to check if a user exists
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=? AND " + COL_PASSWORD + "=?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Add this to allow registration (optional but good to have)
    public void addUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASSWORD, password);
        db.insert(TABLE_USERS, null, cv);
    }

    // Add this method to save progress
    // Change 'int logoId' to 'String logoName'
    public void markAsGuessed(String username, String logoName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PROGRESS_USER, username);
        cv.put(COL_PROGRESS_LOGO_ID, logoName); // This will now save "blank1"
        db.insertWithOnConflict(TABLE_PROGRESS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // Add this to check if a logo is guessed
    // Change 'int logoId' to 'String logoName'
    public boolean isLogoGuessed(String username, String logoName) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Update the query to compare strings
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PROGRESS +
                        " WHERE " + COL_PROGRESS_USER + "=? AND " + COL_PROGRESS_LOGO_ID + "=?",
                new String[]{username, logoName});
        boolean guessed = cursor.getCount() > 0;
        cursor.close();
        return guessed;
    }

    public void resetUserProgress(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Only delete rows where the username matches
        db.delete(TABLE_PROGRESS, COL_PROGRESS_USER + "=?", new String[]{username});
        db.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Ensure the Users table is created if it was missed in version 2
            String createUsersTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USERNAME + " TEXT, " +
                    COL_PASSWORD + " TEXT);";
            db.execSQL(createUsersTable);

            // Ensure the owner column is added to the quiz table
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_USER_OWNER + " TEXT");
            } catch (Exception e) {
                // Ignore if the column already exists
            }
        }
        if (oldVersion < 4) {
            // Ensure the logo_progress table is created
            String createProgressTable = "CREATE TABLE IF NOT EXISTS " + TABLE_PROGRESS + " (" +
                    COL_PROGRESS_USER + " TEXT, " +
                    COL_PROGRESS_LOGO_ID + " INTEGER, " +
                    "PRIMARY KEY (" + COL_PROGRESS_USER + ", " + COL_PROGRESS_LOGO_ID + "));";
            db.execSQL(createProgressTable);

            // Ensure missing columns are added to the quiz table
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_TITLE + " TEXT");
            } catch (Exception e) {
                // Ignore if the column already exists
            }
            try {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COLUMN_USER_OWNER + " TEXT");
            } catch (Exception e) {
                // Ignore if the column already exists
            }
        }
    }

    // 1. UPDATE THIS METHOD: Group by Title so we don't get duplicate buttons
    // Add 'String username' here to filter the results
    public Cursor getAllQuizzes(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Only select rows that belong to the logged-in user
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_USER_OWNER + "=? GROUP BY " + COLUMN_TITLE, new String[]{username});
    }

    // 2. ADD THIS BRAND NEW METHOD: Fetches all questions belonging to a specific quiz
    public Cursor getQuizByTitle(String title) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE " + COLUMN_TITLE + "=?", new String[]{title});
    }

    // Update the parameters to include "String title" at the start
    public void addQuizItem(String username, String title, String type, String question, String answer, String fileName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_USER_OWNER, username); // Save the owner here
        cv.put(COLUMN_TITLE, title);
        cv.put(COLUMN_TYPE, type);
        cv.put(COLUMN_QUESTION, question);
        cv.put(COLUMN_ANSWER, answer);
        cv.put(COLUMN_FILENAME, fileName);

        long result = -1;
        try {
            result = db.insert(TABLE_NAME, null, cv);
        } catch (android.database.SQLException e) {
            Log.e("DatabaseError", "Error saving quiz: " + e.getMessage());
        }
        if (result == -1) {
            Toast.makeText(context, "Failed to Save", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Saved: " + title, Toast.LENGTH_SHORT).show();
        }

        close();
    }

    // Helper to see how many rows exist so we can name the next one
    private int getQuizCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // Add this to MyDatabaseHelper.java
    public void updateQuizItem(int id, String question, String answer) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put("question", question);
        cv.put("answer", answer);
        // Update the row where the ID matches
        db.update("my_quiz", cv, "_id=?", new String[]{String.valueOf(id)});
    }

    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE " + COL_USERNAME + "=?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public void deleteQuizByTitle(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        // This wipes every question that belongs to that specific title
        db.delete(TABLE_NAME, COLUMN_TITLE + "=?", new String[]{title});
        db.close();
    }

    public void deleteQuizById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // This deletes one specific row (one question) using its ID
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteUserAccount(String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 1. Delete user from the users table
        db.delete(TABLE_USERS, COL_USERNAME + "=?", new String[]{username});
        // 2. Delete user's progress
        db.delete(TABLE_PROGRESS, COL_PROGRESS_USER + "=?", new String[]{username});
        // 3. Delete user's quizzes
        db.delete(TABLE_NAME, COLUMN_USER_OWNER + "=?", new String[]{username});
        db.close();
    }
}