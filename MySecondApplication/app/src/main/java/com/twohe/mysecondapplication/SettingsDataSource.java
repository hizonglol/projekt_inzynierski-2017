package com.twohe.mysecondapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by morri on 23.08.2016.
 */
public class SettingsDataSource {


    // Database fields
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private String[] allColumns = {DBHelper.COLUMN_ID,
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

    public Setting createSetting(String setting, String setting_value) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SETTING, setting);
        values.put(DBHelper.COLUMN_SETTING_VALUE, setting_value);

        long insertId = database.insert(DBHelper.TABLE_SETTINGS, null,
                values);

        Cursor cursor = database.query(DBHelper.TABLE_SETTINGS,
                allColumns, DBHelper.COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        Setting newSetting = cursorToSetting(cursor);
        cursor.close();
        return newSetting;
    }

    public void deleteSetting(Setting setting) {
        long id = setting.getId();
        System.out.println("Deleted setting: " + id);
        database.delete(DBHelper.TABLE_SETTINGS, DBHelper.COLUMN_ID
                + " = " + id, null);
    }

    public List<Setting> getAllSettings() {
        List<Setting> settings = new ArrayList<Setting>();

        Cursor cursor = database.query(DBHelper.TABLE_SETTINGS,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Setting setting = cursorToSetting(cursor);
            settings.add(setting);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return settings;
    }

    private Setting cursorToSetting(Cursor cursor) {
        Setting setting = new Setting();
        setting.setId(cursor.getLong(0));
        setting.setSetting(cursor.getString(1));
        setting.setSettingValue(cursor.getString(2));
        return setting;
    }
}
