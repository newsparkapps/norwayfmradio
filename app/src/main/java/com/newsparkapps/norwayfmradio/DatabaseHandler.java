package com.newsparkapps.norwayfmradio;

/**
 * Created by Roney on 2/24/2018.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import com.newsparkapps.norwayfmradio.util.Shoutcast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "favoritesManager";

    // Contacts table name
    private static final String TABLE_MESSAGE = "favorites";

    // Contacts Table Columns names
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_URL = "url";
    private static final String KEY_IMG = "img";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MESSAGE_TABLE = "CREATE TABLE " + TABLE_MESSAGE + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_NAME + " TEXT," + KEY_URL + " TEXT," + KEY_IMG + " TEXT)";
        db.execSQL(CREATE_MESSAGE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);

        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new contact
    void addShoutcast(MyFourites myFourites) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, myFourites.getName());
        values.put(KEY_URL, myFourites.getUrl());
        values.put(KEY_IMG, myFourites.getImg());
        // Inserting Row
        db.insert(TABLE_MESSAGE, null, values);
        db.close(); // Closing database connection
    }

    // Getting single contact
    boolean getFavorite(String name) {
        boolean status = false;

        SQLiteDatabase db = this.getReadableDatabase();
        //Cursor cursor = db.rawQuery(query,null);

        Cursor cursor = db.rawQuery("select "+KEY_NAME+" from "
                        + TABLE_MESSAGE + " where " + KEY_NAME + "=?",
                new String[] {name});


        if (cursor != null && cursor.moveToFirst()) {
            String fmname = cursor.getString(cursor.getColumnIndex(KEY_NAME));
            Log.i("roney",fmname);
            if (fmname.equals(name)) {
                status = true;
            } else {
                status = false;
            }
        } else {
            cursor.close();
        }
        return status;

    }

    // Getting All Contacts
    public List<Shoutcast> getAllFourites() {
        List<Shoutcast> mymessagesList= new ArrayList<Shoutcast>();
        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_MESSAGE + " ORDER BY " +KEY_ID+ " DESC";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                Shoutcast shoutcast = new Shoutcast();
                shoutcast.setId(Integer.parseInt(cursor.getString(0)));
                shoutcast.setName(cursor.getString(1));
                shoutcast.setUrl(cursor.getString(2));
                shoutcast.setImage(cursor.getString(3));
                // Adding contact to list
                mymessagesList.add(shoutcast);
            } while (cursor.moveToNext());
        }


        // return contact list
        return mymessagesList;
    }


    // Updating single contact
    public int updateMessage(MyFourites myFourites) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, myFourites.getName());
        values.put(KEY_URL, myFourites.getUrl());
        values.put(KEY_IMG, myFourites.getImg());

        // updating row
        return db.update(TABLE_MESSAGE, values, KEY_ID + " = ?",
                new String[] { String.valueOf(myFourites.getId()) });
    }

    // Deleting single contact
    public void deleteMessage(MyFourites myFourites) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGE, KEY_NAME + " = ?",
                new String[] { String.valueOf(myFourites.getName()) });
        db.close();
    }


    // Getting contacts Count
    public int getMessagesCount() {
        String countQuery = "SELECT  * FROM " + TABLE_MESSAGE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        return cursor.getCount();
    }

}