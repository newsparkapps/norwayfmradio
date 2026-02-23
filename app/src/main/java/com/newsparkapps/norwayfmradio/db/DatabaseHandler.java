package com.newsparkapps.norwayfmradio.db;

/**
 * Created by Roney on 2/24/2018.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.newsparkapps.norwayfmradio.models.MyFourites;
import com.newsparkapps.norwayfmradio.util.Shoutcast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "favoritesManager";
    private static final String TABLE_MESSAGE = "favorites";
    private static final String MY_FM_LIST = "myfmlist";
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


        String CREATE_MESSAGE_TABLE2 = "CREATE TABLE " + MY_FM_LIST + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_NAME + " TEXT," + KEY_URL + " TEXT," + KEY_IMG + " TEXT)";
        db.execSQL(CREATE_MESSAGE_TABLE2);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGE);
        db.execSQL("DROP TABLE IF EXISTS " + MY_FM_LIST);
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    // Adding new contact
    public void addShoutcast(MyFourites myFourites) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, myFourites.getName());
        values.put(KEY_URL, myFourites.getUrl());
        values.put(KEY_IMG, myFourites.getImg());
        // Inserting Row
        db.insert(TABLE_MESSAGE, null, values);
        db.close();
    }

    public void addShoutcastToList(Shoutcast shoutcast) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_NAME, shoutcast.getName());
        values.put(KEY_URL, shoutcast.getUrl());
        values.put(KEY_IMG, shoutcast.getImage());

        db.insert(MY_FM_LIST, null, values);

        db.close();
    }

    public void addShoutcastToLists(Shoutcast shoutcast) {

        if (shoutcast == null) return;

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", shoutcast.getName());
        values.put("url", shoutcast.getUrl());
        values.put("img", shoutcast.getImage());

        db.insertWithOnConflict(
                MY_FM_LIST,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
        );

        db.close();
    }


    // Getting single contact
    public boolean getFavorite(String name) {
        boolean status = false;

        SQLiteDatabase db = this.getReadableDatabase();
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
        String selectQuery = "SELECT  * FROM " + TABLE_MESSAGE + " ORDER BY " +KEY_ID+ " DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                Shoutcast shoutcast = new Shoutcast();
                shoutcast.setId(Integer.parseInt(cursor.getString(0)));
                shoutcast.setName(cursor.getString(1));
                shoutcast.setUrl(cursor.getString(2));
                shoutcast.setImage(cursor.getString(3));
                mymessagesList.add(shoutcast);
            } while (cursor.moveToNext());
        }
        return mymessagesList;
    }

    // Deleting single contact
    public void deleteMessage(MyFourites myFourites) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGE, KEY_NAME + " = ?",
                new String[] { String.valueOf(myFourites.getName()) });
        db.close();
    }

    public void clearFmList() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(MY_FM_LIST, null, null);
        db.close();
    }

    public List<Shoutcast> getAllFmList() {

        List<Shoutcast> fmList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        String selectQuery = "SELECT * FROM " + MY_FM_LIST +
                " ORDER BY " + KEY_ID + " DESC";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Shoutcast shoutcast = new Shoutcast();
                    shoutcast.setId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
                    shoutcast.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)));
                    shoutcast.setUrl(cursor.getString(cursor.getColumnIndexOrThrow(KEY_URL)));
                    shoutcast.setImage(cursor.getString(cursor.getColumnIndexOrThrow(KEY_IMG)));
                    fmList.add(shoutcast);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        db.close();

        return fmList;
    }
}