package com.twohe.morri.tools;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by morri on 23.08.2016.
 */
public class DBHelper extends SQLiteOpenHelper {


    public static final String TABLE_SETTINGS = "settings";
    public static final String COLUMN_SETTING = "setting";
    public static final String COLUMN_SETTING_VALUE = "setting_value";

    private static final String DATABASE_NAME = "settings.dataBaseMain";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_SETTINGS + "( " + COLUMN_SETTING
            + " text not null unique, " + COLUMN_SETTING_VALUE
            + " text);";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE);
        Log.d(this.getClass().getName(), "Utworzono baze danych");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

}