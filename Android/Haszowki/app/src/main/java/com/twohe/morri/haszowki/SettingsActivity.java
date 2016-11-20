package com.twohe.morri.haszowki;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.twohe.morri.tools.InstantAutoComplete;
import com.twohe.morri.tools.SettingsDataSource;


/**
 * Created by TwoHe on 10.07.2016.
 *
 * This file contains class Settings Activity.
 */
public class SettingsActivity extends AppCompatActivity {

    SettingsDataSource db = new SettingsDataSource(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Log.d("On create", "SettingsActivity");

        db.open();

        /* obsluga toolbar w Settings */
        Toolbar settingsToolbar = (Toolbar) findViewById(R.id.settingsToolbar);
        if (settingsToolbar != null)
            settingsToolbar.setTitle(R.string.label_settings_activity);

        editSettings_name = (EditText) findViewById(R.id.editSettings_name);
        editSettings_surname = (EditText) findViewById(R.id.editSettings_surname);
        editSettings_index = (EditText) findViewById(R.id.editSettings_index);
        editSettings_course = (InstantAutoComplete) findViewById(R.id.editSettings_course);
        editSettings_serverAddress = (EditText) findViewById(R.id.editSettings_serverAddress);
        buttonSettings_save = (Button) findViewById(R.id.buttonSettings_save);

        View.OnClickListener saveButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {

                if (editSettings_name != null) {
                    if (editSettings_name.getText().toString().equals("")) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_name), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_name", editSettings_name.getText().toString());
                }

                if (editSettings_surname != null) {
                    if (editSettings_surname.getText().toString().equals("")) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_surname), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_surname", editSettings_surname.getText().toString());
                }

                if (editSettings_index != null) {
                    if (editSettings_index.getText().toString().length() != 6) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_student_number), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_studentNo", editSettings_index.getText().toString());
                }

                if (editSettings_course != null) {
                    if (editSettings_course.getText().toString().equals("")) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_course), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_course", editSettings_course.getText().toString());
                }

                if (editSettings_serverAddress != null) {
                    db.createSetting("setting_serverAddress", editSettings_serverAddress.getText().toString());
                }

                //Log.d("saveButtonHandler", "Dane zapisane");

                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_data_saved), Toast.LENGTH_SHORT).show();
            }
        };
        if (buttonSettings_save != null)
            buttonSettings_save.setOnClickListener(saveButtonHandler);

        EditText.OnEditorActionListener doneKeyboardButton = new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        };
        if (editSettings_serverAddress != null)
            editSettings_serverAddress.setOnEditorActionListener(doneKeyboardButton);

        if (editSettings_course != null) {
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, courses);
            editSettings_course.setAdapter(adapter);
            editSettings_course.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

            View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if(hasFocus){
                        editSettings_course.showDropDown();
                    }else {
                        editSettings_course.dismissDropDown();
                    }
                }
            };
            editSettings_course.setOnFocusChangeListener(focusListener);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        db.close();
    }

    void resumeState() {

        SettingsDataSource resume_db = new SettingsDataSource(this);
        resume_db.open();

        String stringName = resume_db.getSetting("setting_name");
        String stringSurname = resume_db.getSetting("setting_surname");
        String stringStudentNo = resume_db.getSetting("setting_studentNo");
        String stringCourse = resume_db.getSetting("setting_course");
        String stringServerAddress = resume_db.getSetting("setting_serverAddress");


        if (editSettings_name != null)
            editSettings_name.setText(stringName);

        if (editSettings_surname != null)
            editSettings_surname.setText(stringSurname);

        if (editSettings_index != null)
            editSettings_index.setText(stringStudentNo);

        if (editSettings_course != null)
            editSettings_course.setText(stringCourse);

        if (editSettings_serverAddress != null)
            editSettings_serverAddress.setText(stringServerAddress);

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


    EditText editSettings_name;
    EditText editSettings_surname;
    EditText editSettings_index;
    InstantAutoComplete editSettings_course;
    EditText editSettings_serverAddress;
    Button buttonSettings_save;

    private static final String[] courses = new String[] {
            "SCRSK", "OpSys", "SICR", "UnixEZI"
    };
}
