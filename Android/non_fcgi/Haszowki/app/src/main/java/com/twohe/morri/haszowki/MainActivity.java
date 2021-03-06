package com.twohe.morri.haszowki;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.twohe.morri.tools.IncomingCallReceiver;
import com.twohe.morri.tools.InstantAutoComplete;
import com.twohe.morri.tools.SettingsDataSource;

import java.io.File;

/**
 * Created by morri on 30.07.2016.
 * <p>
 * This file contains class Main Activity.
 */
@SuppressWarnings("FieldCanBeLocal")
public class MainActivity extends AppCompatActivity {

    /**
     * Initializes variables with viewed layout and sets up listeners.
     * Creates database handler and initializes canBeginTest flag with false value.
     *
     * @param savedInstanceState bundle with dynamic instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("On create", "MainActivity");

        checkIfCloseRequested();
        canBeginTestFlag = false;

        createAppFolder();

        sharedPrefMain = PreferenceManager.getDefaultSharedPreferences(this);

        disableIncomingCallReceiver();

        databaseMain = new SettingsDataSource(this);

        toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        buttonMain_startTest = (Button) findViewById(R.id.buttonMain_startTest);
        buttonMain_exitApp = (Button) findViewById(R.id.buttonMain_exitApp);
        buttonMain_scanQR = (Button) findViewById(R.id.buttonMain_scanQR);
        editMain_vector = (EditText) findViewById(R.id.editMain_vector);
        viewMain_result = (TextView) findViewById(R.id.viewMain_result);
        buttonMain_compute = (Button) findViewById(R.id.buttonMain_compute);
        editMain_testID = (InstantAutoComplete) findViewById(R.id.editMain_testID);
        viewMain_moduloResult = (TextView) findViewById(R.id.editMain_moduloResult);
        editMain_hallRow = (EditText) findViewById(R.id.editMain_hallRow);
        editMain_hallSeat = (EditText) findViewById(R.id.editMain_hallSeat);
        viewMain_course = (TextView) findViewById(R.id.viewMain_course);

        databaseMain.open();
        setSupportActionBar(toolbarMain);


        if (buttonMain_compute != null)
            buttonMain_compute.setOnClickListener(computeButtonHandler);

        if (buttonMain_startTest != null)
            buttonMain_startTest.setOnClickListener(startTestButtonHandler);

        if (buttonMain_exitApp != null)
            buttonMain_exitApp.setOnClickListener(exitButtonHandler);

        if (buttonMain_scanQR != null)
            buttonMain_scanQR.setOnClickListener(scanQRButtonHandler);

        if (editMain_testID != null)
            editMain_testID.setOnEditorActionListener(doneKeyboardButton);

        instantiateIncomingCallReceiver();
    }

    /**
     * Checks if there is parameter "Exit me" with true value.
     * Exits intent if there is. Does nothing if there is not or is with false value.
     */
    private void checkIfCloseRequested() {

        if (getIntent().getBooleanExtra("Exit me", false)) {
            finish();
        }
    }

    /**
     * Checks if host system is Marshmallow or higher.
     * Then checks permission for writing external memory.
     * If permission is not granted, makes request and asks to grant permission.
     *
     * @return true if permission has been granted, false if not
     */
    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("isStoragePermGranted", "Permission is granted");
                return true;
            } else {

                Log.v("isStoragePermGranted", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("isStoragePermGranted", "Permission is granted");
            return true;
        }
    }

    /**
     * Checks if host system is Marshmallow or higher.
     * Then checks permission for using camera.
     * If permission is not granted, makes request and asks to grant permission.
     *
     * @return true if permission has been granted, false if not
     */
    private boolean isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("isCameraPermGranted", "Permission is granted");
                return true;
            } else {

                Log.v("isCameraPermGranted", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("isCameraPermGranted", "Permission is granted");
            return true;
        }
    }

    /**
     * Checks if host system is Marshmallow or higher.
     * Then checks permission for using telephone.
     * If permission is not granted, makes request and asks to grant permission.
     *
     * @return true if permission has been granted, false if not
     */
    private boolean isTelephonyPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("isTelephonyPermGranted", "Permission is granted");
                return true;
            } else {

                Log.v("isTelephonyPermGranted", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("isTelephonyPermGranted", "Permission is granted");
            return true;
        }
    }

    /**
     * Used to create folder for tests data.
     * <p>
     * Checks if external memoery write permission has been granted.
     * Afterwards creates folder for tests files.
     *
     * @return true if folder creation successful, false if not
     */
    private boolean createAppFolder() {

        isStoragePermissionGranted();

        File folder = new File(Environment.getExternalStorageDirectory() + "/Haszowki");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success) {
            Log.v("createAppFolder", "successful");
            //MediaScannerConnection.scanFile(this, new String[] { folder.getAbsolutePath() }, null, null);
        } else {
            Log.v("createAppFolder", "unsuccessful");
        }

        return success;
    }

    /**
     * Used to handle data received from OR code scanner.
     * <p>
     * Checks if scanResult is valid. Weights vector cannot be negative.
     * Updates editMain_vector and editMain_testID with received data.
     * Performs computing if reading successful.
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param data        An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IntentIntegrator.REQUEST_CODE && data != null) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null) {
                String contents = scanResult.getContents();
                String[] contentsTable = contents.split("\\s+");

                try {
                    if (Integer.parseInt(contentsTable[0]) < 0) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_wrong_qr_button), Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_wrong_qr_button), Toast.LENGTH_SHORT).show();
                    return;
                }

                canBeginTestFlag = false;
                if (editMain_vector != null)
                    editMain_vector.setText(contentsTable[0]);
                if (editMain_testID != null)
                    editMain_testID.setText(contentsTable[1]);
                buttonMain_compute.performClick();
            }
        }
    }

    /**
     * Used to make a check if user really wants to press back button.
     */
    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.message_i_quit_app))
                .setMessage(getResources().getString(R.string.message_do_you_want_to_quit_app))
                .setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.button_no), null)
                .show();
    }

    /**
     * Used to close database when this activity is being destroyed.
     * It also disables Incoming Call Receiver.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        disableIncomingCallReceiver();
        databaseMain.close();
    }

    /**
     * Called everytime when this activity is being re-initialized.
     * Always after onCreate() and before onPostCreate().
     *
     * @param savedInstanceState the data most recently supplied in onSaveInstanceState(Bundle).
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        resumeState();
    }

    /**
     * Called everytime when this activity comes back from background.
     * <p>
     * Resets shared preferences to prevent TabsActivity closing itself.
     * Loads all the fields on this activity with saved data by calling
     * resumeState(). It also disables Incoming Call Receiver.
     */
    @Override
    public void onResume() {
        super.onResume();

        disableIncomingCallReceiver();

        /*
        Reset sharedPrefMain to prevent closing TabsActivity itself
         */
        SharedPreferences.Editor editor = sharedPrefMain.edit();
        editor.putBoolean("End test", false);
        editor.apply();

        resumeState();
    }

    /**
     * Used to disable IncomingCallReceiver that rejects any incoming calls
     */
    private void disableIncomingCallReceiver() {

        SharedPreferences.Editor editor = sharedPrefMain.edit();
        editor.putBoolean("Rejecting enabled", false);
        editor.apply();
    }

    /**
     * Used to launch IncomingCallReceiver
     */
    private void instantiateIncomingCallReceiver(){

        PackageManager pm = MainActivity.this.getPackageManager();
        ComponentName componentName = new ComponentName(MainActivity.this, IncomingCallReceiver.class);
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Is being used to restore two significant fields in this activity:
     * student number and course name.
     * <p>
     * It also creates suggestions for test id by concatenating lowered course name
     * with numbers from 0 to 10.
     * TYPE_TEXT_FLAG_AUTO_COMPLETE is used to get rid of autocomplete suggestions
     * <p>
     * To avoid database leakage this method uses it's own SettingsDataSource.
     */
    private void resumeState() {

        SettingsDataSource db = new SettingsDataSource(this);
        db.open();


        TextView viewStudentNo = (TextView) findViewById(R.id.viewMain_studentNo);
        TextView viewCourse = (TextView) findViewById(R.id.viewMain_course);

        String stringStudentNo = db.getSetting("setting_studentNo");
        String stringCourse = db.getSetting("setting_course");

        if (viewStudentNo != null)
            viewStudentNo.setText(stringStudentNo);

        if (viewCourse != null)
            viewCourse.setText(stringCourse);

        if (editMain_testID != null && stringCourse.length() > 0) {

            String possibleTests[] = new String[10];
            String stringCourseFormatted = stringCourse.toLowerCase();
            stringCourseFormatted = stringCourseFormatted.replace(" ", "");

            for (Integer i = 0; i < 10; ++i) {
                String onePossibleTestName = stringCourseFormatted.concat(i.toString());
                possibleTests[i] = onePossibleTestName;
            }

            ArrayAdapter<String> adapter =
                    new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, possibleTests);
            editMain_testID.setAdapter(adapter);
            editMain_testID.setInputType(InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE);

            View.OnFocusChangeListener focusListener = new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        editMain_testID.showDropDown();
                    } else {
                        editMain_testID.dismissDropDown();
                    }
                }
            };
            editMain_testID.setOnFocusChangeListener(focusListener);
        }

        db.close();
    }

    /**
     * Inflates the menu. Adds items to the action bar if it is present.
     *
     * @param menu interface for managing the items in a menu
     * @return you must return true for the menu to be displayed; if you return false it will not be shown
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles action bar item clicks.
     *
     * @param item interface for direct access to a previously created menu item
     * @return false to allow normal menu processing to proceed, true to consume it here
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intentSettings = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intentSettings);
            return true;
        }

        if (id == R.id.action_info) {
            Intent intentInfo = new Intent(getApplicationContext(), InfoActivity.class);
            startActivity(intentInfo);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback for clicking buttonMain_compute.
     * <p>
     * Does all procedures with group computation.
     * Updates corresponding editTexts with computed data.
     * Changes flag canBeginTest into true if computation has been completed.
     */
    View.OnClickListener computeButtonHandler = new View.OnClickListener() {
        public void onClick(View v) throws NumberFormatException {

            canBeginTestFlag = false;

            int digitsStudentNo[] = new int[6];
            int digitsVector[] = new int[6];
            int digitsResult[] = new int[6];

            String stringStudentNo = databaseMain.getSetting("setting_studentNo");
            String stringVector;

            try {
                stringVector = checkTestVector();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                return;
            }

            if (parseStudentNo(stringStudentNo, digitsStudentNo)) return;

            if (parseTestVector(stringVector, digitsVector)) return;

            int integerResult = computeIntegerResult(digitsStudentNo, digitsVector, digitsResult);

            assembleStringResultAndView(digitsResult, integerResult);

            convertIntegerToStringModuloAndView(integerResult);

            canBeginTestFlag = true;
        }
    };

    /**
     * Checks if test vector is longer than zero.
     * Checks if test vector is not equal to 0.
     * If any of upper conditions are true then throws exception and resets fields.
     * <p>
     * Checks test vector taken from editMain_vector for too short length and fills front of it
     * with zeros until there are 6 digits total.
     * Updates editMain_vector with checked vector if needed.
     *
     * @return string with test vector
     */
    private String checkTestVector() throws IllegalArgumentException {
        StringBuilder builderStringVector = new StringBuilder("");

        if (editMain_vector != null) {
            if (editMain_vector.getText().toString().length() == 0) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_vector_number), Toast.LENGTH_SHORT).show();
                resetResults();
                throw new IllegalArgumentException(getResources().getString(R.string.message_give_proper_vector_number));
            }
            if (Integer.parseInt(editMain_vector.getText().toString()) == 0) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_vector_cant_be_zero), Toast.LENGTH_SHORT).show();
                resetResults();
                throw new IllegalArgumentException(getResources().getString(R.string.message_vector_cant_be_zero));
            }

            builderStringVector.append(editMain_vector.getText().toString());

            if (builderStringVector.length() != 6) {
                while (builderStringVector.length() < 6) {
                    builderStringVector.insert(0, "0");
                }
                editMain_vector.setText(builderStringVector.toString());
            }
        }

        databaseMain.createSetting("setting_vector", builderStringVector.toString());

        return builderStringVector.toString();
    }

    /**
     * Parses string contained in stringStudentNo into table of digits called digitsStudentNo.
     * Handles exceptions if something goes wrong with parsing.
     *
     * @param stringStudentNo table containing student number
     * @param digitsStudentNo table containing digits of student number
     * @return false if everything goes well, true if catches an exception
     */
    private boolean parseStudentNo(String stringStudentNo, int[] digitsStudentNo) {
        try {
            for (int i = 0; i < 6; ++i) {
                try {
                    digitsStudentNo[i] = Integer.parseInt(Character.toString(stringStudentNo.charAt(i)));
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_student_number), Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_student_number), Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    /**
     * Parses string contained in stringVector into table of digits.
     * Handles exceptions if something goes wrong with parsing.
     *
     * @param stringVector string containing test vector
     * @param digitsVector table containing digits of test vector
     * @return false if everything goes well, true if catches an exception
     */
    private boolean parseTestVector(String stringVector, int[] digitsVector) {
        try {
            if (stringVector != null) {
                for (int i = 0; i < 6; ++i) {
                    try {
                        digitsVector[i] = Integer.parseInt(Character.toString(stringVector.charAt(i)));
                    } catch (NumberFormatException e) {
                        Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_vector_number), Toast.LENGTH_SHORT).show();
                        return true;
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_vector_number), Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    /**
     * Computes digitsResult by multiplying digitsStudentNo fields with corresponding digitsVector fields.
     * Sums up fields from digitsResult and puts into integerResult.
     *
     * @param digitsStudentNo table containing digits of student number
     * @param digitsVector    table containing digits of test vector
     * @param digitsResult    table containing digits of the result
     * @return sum of all digitsResult fields
     */
    private int computeIntegerResult(int[] digitsStudentNo, int[] digitsVector, int[] digitsResult) {
        int integerResult = 0;

        for (int i = 0; i < 6; ++i) {
            digitsResult[i] = digitsStudentNo[i] * digitsVector[i];
            integerResult += digitsResult[i];
        }

        return integerResult;
    }

    /**
     * Assembles result string and puts it into viewMain_result
     *
     * @param digitsResult  table of each digit from student number multiplied by corresponded vector digit
     * @param integerResult sum of all fields from digitResult
     */
    private void assembleStringResultAndView(int[] digitsResult, int integerResult) {
        String stringResult = "";

        for (int i = 0; i < 5; ++i) {
            stringResult += String.valueOf(digitsResult[i]) + " + ";
        }

        stringResult += digitsResult[5] + " = " + integerResult;

        if (viewMain_result != null) {
            viewMain_result.setText(stringResult);
            viewMain_result.setSelected(true);
        }
    }

    /**
     * Converts group from integer to string modulo result (0-F)
     * and puts it into viewMain_moduloResult.
     * It also updates corresponding key-pair in database.
     *
     * @param integerResult computed group that is in a range from 0 to 15
     */
    private void convertIntegerToStringModuloAndView(Integer integerResult) {

        integerResult = integerResult % 16;

        String stringModuloResult = null;

        if (integerResult < 10)
            stringModuloResult = String.valueOf(integerResult);
        else if (integerResult == 10)
            stringModuloResult = "A";
        else if (integerResult == 11)
            stringModuloResult = "B";
        else if (integerResult == 12)
            stringModuloResult = "C";
        else if (integerResult == 13)
            stringModuloResult = "D";
        else if (integerResult == 14)
            stringModuloResult = "E";
        else if (integerResult == 15)
            stringModuloResult = "F";

        if (viewMain_moduloResult != null) {
            viewMain_moduloResult.setText(stringModuloResult);
        }

        databaseMain.createSetting("setting_group", stringModuloResult);
    }

    /**
     * Used to reset fields:
     * viewMain_result
     * viewMain_moduloResult
     */
    private void resetResults() {
        viewMain_result.setText("");
        viewMain_moduloResult.setText("");
    }

    /**
     * Callback for clicking buttonMain_startTest.
     * <p>
     * Checks if app has been granted permission to write to external storage.
     * Checks if process of computing group number has been succeeded.
     * Checks if all editable fields consists proper data.
     * Checks if app has been granted permission to telephone.
     * If all checks are passed, starts TabsActivity.
     */
    View.OnClickListener startTestButtonHandler = new View.OnClickListener() {
        public void onClick(View v) throws NumberFormatException {

            if (!isOnline()) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_no_internet_connection), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isStoragePermissionGranted()) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_writing_permission), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!canBeginTestFlag) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_compute_group), Toast.LENGTH_SHORT).show();
                return;
            }

            if (checkEditableValues())
                return;

            if (!isTelephonyPermissionGranted())
                return;

            Intent intentSecurityCheck = new Intent(getApplicationContext(), SecurityCheckActivity.class);
            startActivity(intentSecurityCheck);
        }
    };

    /**
     * Checks whether app has network access.
     *
     * @return true if online, false if not
     */
    public boolean isOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Checks if row and seat is higher than 0.
     * If it is then it proceeds.
     * <p>
     * Checks if row and seat equal 0 or <-1.
     * If it is then it stops operation.
     * <p>
     * Checks if row and seat is equal -1.
     * If it is then it informs that student is beggining test without
     * giving row and seat.
     * <p>
     * Checks if row == -1 with seat != -1 or row != -1 with seat == -1.
     * If it is true then stops operation.
     * <p>
     * Checks if testId contains at least one character.
     * Checks if course name contains at least two characters.
     *
     * @return false if checks are passed, true if any check has been failed
     */
    private boolean checkEditableValues() {
        int hallRow = 0;
        int hallSeat = 0;

        if (editMain_hallRow != null) {
            try {
                hallRow = Integer.parseInt(editMain_hallRow.getText().toString());
                if (hallRow > 0) {
                    databaseMain.createSetting("setting_hall_row", editMain_hallRow.getText().toString());
                } else if (hallRow == 0) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_row), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (hallRow < -1) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_row), Toast.LENGTH_SHORT).show();
                    return true;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_type_row), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return true;
            }
        }

        if (editMain_hallSeat != null) {
            try {
                hallSeat = Integer.parseInt(editMain_hallSeat.getText().toString());
                if (hallSeat > 0) {
                    databaseMain.createSetting("setting_hall_seat", editMain_hallSeat.getText().toString());
                } else if (hallSeat == 0) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_seat), Toast.LENGTH_SHORT).show();
                    return true;
                } else if (hallSeat < -1) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_seat), Toast.LENGTH_SHORT).show();
                    return true;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_type_seat), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return true;
            }
        }

        if (hallRow == -1 && hallSeat == -1) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.message_going_to_test_without_position), Toast.LENGTH_SHORT).show();
            databaseMain.createSetting("setting_hall_row", editMain_hallRow.getText().toString());
            databaseMain.createSetting("setting_hall_seat", editMain_hallSeat.getText().toString());
        } else if (hallRow == -1 && hallSeat != -1 || hallRow != -1 && hallSeat == -1) {
            Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_row_and_seat), Toast.LENGTH_SHORT).show();
            return true;
        }

        if (editMain_testID != null) {
            if (editMain_testID.getText().length() == 0) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_type_ID_name), Toast.LENGTH_SHORT).show();
                return true;
            }
            databaseMain.createSetting("setting_test_id", editMain_testID.getText().toString());
        }

        if (viewMain_course != null) {
            if (viewMain_course.getText().length() < 2) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_type_course_name), Toast.LENGTH_SHORT).show();
                return true;
            }
        }

        try {
            if (editMain_vector != null) {
                if (Integer.parseInt(editMain_vector.getText().toString()) == 0) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_vector_cant_be_zero), Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_proper_vector_number), Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    /**
     * Callback for clicking buttonMain_exitApp.
     * <p>
     * Launches second main intent with cleaning the rest of intents,
     * gives it parameter "Exit me" to force quit it on it's onCreate
     * and finishes current intent.
     */
    View.OnClickListener exitButtonHandler = new View.OnClickListener() {
        public void onClick(View v) throws NumberFormatException {

            Intent intentMain = getIntent();
            intentMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intentMain.putExtra("Exit me", true);
            startActivity(intentMain);
            finish();

        }
    };

    /**
     * Callback for clicking buttonMain_scanQR.
     * <p>
     * Configures scan of QR code and launches it.
     */
    View.OnClickListener scanQRButtonHandler = new View.OnClickListener() {
        public void onClick(View v) {
            if (isCameraPermissionGranted()) {
                IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setPrompt(getResources().getString(R.string.message_scan_qr_code));
                integrator.setCameraId(0);
                integrator.setBeepEnabled(false);
                integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);
            }
        }
    };

    /**
     * Callback for clicking keys on soft keyboard while editing editMain_testID.
     * <p>
     * If DONE key has been clicked then it hides soft keyboard and returns true.
     * Otherwise it returns false.
     */
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


    //variables and classes
    private boolean canBeginTestFlag;
    private SettingsDataSource databaseMain;
    private SharedPreferences sharedPrefMain;
    private Toolbar toolbarMain;
    private Button buttonMain_startTest;
    private Button buttonMain_exitApp;
    private Button buttonMain_scanQR;
    private InstantAutoComplete editMain_testID;
    private EditText editMain_vector;
    private TextView viewMain_result;
    private Button buttonMain_compute;
    private TextView viewMain_moduloResult;
    private EditText editMain_hallRow;
    private EditText editMain_hallSeat;
    private TextView viewMain_course;
}
