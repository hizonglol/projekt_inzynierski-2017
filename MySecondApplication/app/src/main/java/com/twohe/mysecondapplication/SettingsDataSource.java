package com.twohe.mysecondapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by morri on 23.08.2016.
 */
public class SettingsDataSource {

    // Database fields
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] allColumns = {
            DBHelper.COLUMN_SETTING,
            DBHelper.COLUMN_SETTING_VALUE};

    public SettingsDataSource(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /* updates existing setting */
    public int updateSetting(String setting, String settingValue) {

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SETTING_VALUE, settingValue);

        return database.update(DBHelper.TABLE_SETTINGS,
                values, DBHelper.COLUMN_SETTING + " = ?",
                new String[]{setting});
    }

    /* creates setting or updates it if it exists */
    public Boolean createSetting(String setting, String setting_value) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SETTING, setting);
        values.put(DBHelper.COLUMN_SETTING_VALUE, setting_value);

        try {
            database.insertOrThrow(DBHelper.TABLE_SETTINGS, null, values);
        } catch (SQLiteConstraintException e) {
            Log.d("DB", "Setting already exists. Updating...");
            updateSetting(setting, setting_value);
        }

        return true;
    }

    /* gets setting. If setting does not exist, creates setting and returns space */
    public String getSetting(String setting) {
        String setting_value = " ";

        Cursor cursor = database.query(DBHelper.TABLE_SETTINGS,
                allColumns, DBHelper.COLUMN_SETTING + "=?", new String[]{setting}, null, null, null, null);

        if (cursor != null) {

            if (cursor.getCount() < 1) {
                createSetting(setting, " ");
                cursor.close();
                return setting_value;
            }

            cursor.moveToFirst();
            setting_value = cursor.getString(1);


            cursor.close();
        }

        // make sure to close the cursor
        return setting_value;
    }

    /* deletes setting. I dunno why I made this method */
    public void deleteSetting(String setting) {
        System.out.println("Deleted setting: " + setting);
        database.delete(DBHelper.TABLE_SETTINGS, DBHelper.COLUMN_SETTING
                + " = ?", new String[]{setting});
    }
}
