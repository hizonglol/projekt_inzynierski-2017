package com.twohe.mysecondapplication;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Created by TwoHe on 10.07.2016.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final SettingsDataSource db = new SettingsDataSource(this);
        db.open();

        /* obsluga toolbar w Settings */
        Toolbar settingsToolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        if (settingsToolbar != null)
            settingsToolbar.setTitle(R.string.label_settings_activity);

        Button saveButton = (Button) findViewById(R.id.button_save);
        View.OnClickListener saveButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {

                EditText name = (EditText) findViewById(R.id.name_value);
                EditText surname = (EditText) findViewById(R.id.surname_value);
                EditText index = (EditText) findViewById(R.id.index_value);
                EditText subject = (EditText) findViewById(R.id.subject_value);

                if (name != null)
                    db.createSetting("setting_name", name.getText().toString());

                if(surname != null)
                    db.createSetting("setting_surname", surname.getText().toString());

                if(index != null)
                    db.createSetting("setting_index", index.getText().toString());

                if(subject != null)
                    db.createSetting("setting_subject", subject.getText().toString());

                Log.d("Settings", "Dane zapisane");
            }
        };
        if (saveButton != null)
            saveButton.setOnClickListener(saveButtonHandler);

    }


    void resumeState(){

        SettingsDataSource db = new SettingsDataSource(this);
        db.open();

        EditText editName = (EditText) findViewById(R.id.name_value);
        EditText editSurname = (EditText) findViewById(R.id.surname_value);
        EditText editIndex = (EditText) findViewById(R.id.index_value);
        EditText editSubject = (EditText) findViewById(R.id.subject_value);

        String stringName = db.getSetting("setting_name");
        String stringSurname = db.getSetting("setting_surname");
        String stringIndex = db.getSetting("setting_index");
        String stringSubject = db.getSetting("setting_subject");


        if (editName != null)
            editName.setText(stringName);

        if(editSurname != null)
            editSurname.setText(stringSurname);

        if(editIndex != null)
            editIndex.setText(stringIndex);

        if(editSubject != null)
            editSubject.setText(stringSubject);

        db.close();

    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        resumeState();
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        resumeState();
    }

}
