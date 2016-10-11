package com.twohe.morri.haszowki;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by TwoHe on 10.07.2016.
 */
public class SettingsActivity extends AppCompatActivity {

    SettingsDataSource db = new SettingsDataSource(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

                if (name != null) {
                    if (name.getText().toString().equals("")) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_name", name.getText().toString());
                }

                if (surname != null) {
                    if (surname.getText().toString().equals("")) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_surname), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_surname", surname.getText().toString());
                }

                if (index != null) {
                    if (index.getText().toString().length() != 6) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_student_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_index", index.getText().toString());
                }

                if (subject != null) {
                    if (subject.getText().toString().equals("")) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_subject), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_subject", subject.getText().toString());
                }

                //Log.d("saveButtonHandler", "Dane zapisane");

                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_data_saved), Toast.LENGTH_SHORT).show();
            }
        };
        if (saveButton != null)
            saveButton.setOnClickListener(saveButtonHandler);

        EditText editTestId = (EditText) findViewById(R.id.subject_value);
        EditText.OnEditorActionListener doneKeyboardButton = new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        };
        if (editTestId != null)
            editTestId.setOnEditorActionListener(doneKeyboardButton);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        db.close();
    }


    void resumeState() {

        SettingsDataSource resume_db = new SettingsDataSource(this);
        resume_db.open();

        EditText editName = (EditText) findViewById(R.id.name_value);
        EditText editSurname = (EditText) findViewById(R.id.surname_value);
        EditText editIndex = (EditText) findViewById(R.id.index_value);
        EditText editSubject = (EditText) findViewById(R.id.subject_value);

        String stringName = resume_db.getSetting("setting_name");
        String stringSurname = resume_db.getSetting("setting_surname");
        String stringIndex = resume_db.getSetting("setting_index");
        String stringSubject = resume_db.getSetting("setting_subject");


        if (editName != null)
            editName.setText(stringName);

        if (editSurname != null)
            editSurname.setText(stringSurname);

        if (editIndex != null)
            editIndex.setText(stringIndex);

        if (editSubject != null)
            editSubject.setText(stringSubject);

        resume_db.close();

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
