package com.twohe.mysecondapplication;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    boolean canBeginTestFlag = false;

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    SettingsDataSource db = new SettingsDataSource(this);


    // Create object of SharedPreferences.
    SharedPreferences sharedPref;

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

    public boolean isCameraPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* check if the app is intended to close */
        if (getIntent().getBooleanExtra("Exit me", false)) {
            finish();
            return; // return to prevent from doing unnecessary stuffs
        }

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        createAppFolder();

        //*****************************************************************************************

        db.open();

        //*****************************************************************************************

        Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);

        Button startTestButton = (Button) findViewById(R.id.button_start_test);
        Button exitButton = (Button) findViewById(R.id.button_exit);
        Button scanQRButton = (Button) findViewById(R.id.button_qr_code);

        final Button computeButton = (Button) findViewById(R.id.button_compute);
        final View.OnClickListener computeButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {

                canBeginTestFlag = false;

                int digitsIndex[] = new int[6];
                int digitsWeights[] = new int[6];
                int digitsResult[] = new int[6];

                //*******************************************************
                //SPRAWDZAMY TERAZ INDEKS
                //*******************************************************

                EditText editWeights = (EditText) findViewById(R.id.weight_value);
                TextView viewResult = (TextView) findViewById(R.id.result_value);

                String stringIndex = db.getSetting("setting_index");
                String stringWeights = null;

                if (editWeights != null) {
                    stringWeights = editWeights.getText().toString();

                    if (stringWeights.length() != 6) {
                        StringBuilder builderStringWeights = new StringBuilder(stringWeights);
                        while (builderStringWeights.length() < 6) {
                            builderStringWeights.insert(0, "0");
                        }
                        editWeights.setText(builderStringWeights);
                        stringWeights = builderStringWeights.toString();
                    }
                }

                try {
                    for (int i = 0; i < 6; ++i) {
                        try {
                            digitsIndex[i] = Integer.parseInt(Character.toString(stringIndex.charAt(i)));
                            Log.v("Index", String.valueOf(digitsIndex[i]));
                        } catch (NumberFormatException e) {
                            Toast.makeText(getBaseContext(), "Podaj poprawny indeks", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                } catch (StringIndexOutOfBoundsException e) {
                    Toast.makeText(getBaseContext(), "Podaj poprawny indeks", Toast.LENGTH_SHORT).show();
                    return;
                }

                //*******************************************************
                //SPRAWDZAMY TERAZ WAGI
                //*******************************************************

                if (stringWeights != null) {
                    try { //sprawdzamy czy podano numer indeksu
                        for (int i = 0; i < 6; ++i) {
                            digitsWeights[i] = Integer.parseInt(Character.toString(stringWeights.charAt(i)));
                            Log.v("Waga", String.valueOf(digitsWeights[i]));
                        }
                    } catch (StringIndexOutOfBoundsException e) {
                        Toast.makeText(getBaseContext(), "Podaj poprawną wagę", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                int numberResult = 0;
                for (int i = 0; i < 6; ++i) {
                    digitsResult[i] = digitsIndex[i] * digitsWeights[i];
                    numberResult += digitsResult[i];
                    Log.v("Wynik", String.valueOf(digitsResult[i]));
                }


                if (viewResult != null) {
                    String stringResult = "";

                    for (int i = 0; i < 5; ++i) {
                        stringResult += String.valueOf(digitsResult[i]) + " + ";
                    }

                    stringResult += digitsResult[5] + " = " + numberResult;

                    viewResult.setText(stringResult);
                    viewResult.setSelected(true);

                }


                int numberModuloResult = numberResult % 16;
                canBeginTestFlag = true;

                TextView viewModuloResult = (TextView) findViewById(R.id.result_modulo_value);
                String stringModuloResult = null;

                if (numberModuloResult < 10)
                    stringModuloResult = String.valueOf(numberModuloResult);
                else if (numberModuloResult == 10)
                    stringModuloResult = "A";
                else if (numberModuloResult == 11)
                    stringModuloResult = "B";
                else if (numberModuloResult == 12)
                    stringModuloResult = "C";
                else if (numberModuloResult == 13)
                    stringModuloResult = "D";
                else if (numberModuloResult == 14)
                    stringModuloResult = "E";
                else if (numberModuloResult == 15)
                    stringModuloResult = "F";

                if (viewModuloResult != null) {
                    viewModuloResult.setText(stringModuloResult);
                }

                db.createSetting("setting_weights", stringWeights);
                db.createSetting("setting_group", stringModuloResult);
            }
        };
        if (computeButton != null) {
            computeButton.setOnClickListener(computeButtonHandler);
        }

        View.OnClickListener startTestButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {
                    /*
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                            */

                if(!isStoragePermissionGranted()){
                    Toast.makeText(getBaseContext(), "Daj uprawnienia do zapisu!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!canBeginTestFlag) {
                    Toast.makeText(getBaseContext(), "Wylicz poprawną grupę!", Toast.LENGTH_SHORT).show();
                    return;
                }

                EditText editRow = (EditText) findViewById(R.id.hall_row_value);
                EditText editPlace = (EditText) findViewById(R.id.hall_place_value);
                EditText editTestId = (EditText) findViewById(R.id.exam_id_value);
                TextView viewSubjectValue = (TextView) findViewById(R.id.subject_value);

                if (editRow != null) {
                    if (Integer.parseInt(editRow.getText().toString()) < 1) {
                        Toast.makeText(getBaseContext(), "Podaj poprawny rząd!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_hall_row", editRow.getText().toString());
                }

                if (editPlace != null) {
                    if (Integer.parseInt(editPlace.getText().toString()) < 1) {
                        Toast.makeText(getBaseContext(), "Podaj poprawne miejsce!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_hall_place", editPlace.getText().toString());
                }

                if (editTestId != null) {
                    if (editTestId.getText().length() == 0) {
                        Toast.makeText(getBaseContext(), "Wprowadź ID testu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.createSetting("setting_test_id", editTestId.getText().toString());
                }

                if (viewSubjectValue != null) {
                    if (viewSubjectValue.getText().length() < 2) {
                        Toast.makeText(getBaseContext(), "Wprowadź nazwę przedmiotu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                Intent intentTabs = new Intent(getApplicationContext(), TabsActivity.class);
                if (isCallable(intentTabs)) {
                    Log.i("Main", "Setting up tabs/navigating to them");
                    startActivity(intentTabs);
                }/* else if (!isCallable(intentTabs)) {
                    Log.i("Main", "Navigating to tabs");
                    navigateUpTo(intentTabs);
                }*/
            }
        };
        if (startTestButton != null)
            startTestButton.setOnClickListener(startTestButtonHandler);

        View.OnClickListener exitButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {

                Intent intentMain = getIntent();
                intentMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentMain.putExtra("Exit me", true);
                startActivity(intentMain);
                finish();

            }
        };
        if (exitButton != null)
            exitButton.setOnClickListener(exitButtonHandler);


        View.OnClickListener scanQRButtonHandler = new View.OnClickListener() {
            public void onClick(View v) {
                if (isCameraPermissionGranted()) {
                    IntentIntegrator integrator = new IntentIntegrator(MainActivity.this);
                    integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                    integrator.setPrompt("Zeskanuj kod QR");
                    integrator.setCameraId(0);  // Use a specific camera of the device
                    integrator.setBeepEnabled(false);
                    integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES);

                    Log.v("scanQRButtonHandler", "Scanned");
                }
            }
        };
        if (scanQRButton != null)
            scanQRButton.setOnClickListener(scanQRButtonHandler);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EditText editTestWeight = (EditText) findViewById(R.id.weight_value);
        EditText editTestId = (EditText) findViewById(R.id.exam_id_value);

        if (requestCode == IntentIntegrator.REQUEST_CODE && data != null) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null) {
                String contents = scanResult.getContents();
                String[] contentsTable = contents.split("\\s+");
                if (editTestWeight != null)
                    editTestWeight.setText(contentsTable[0]);
                if (editTestId != null)
                    editTestId.setText(contentsTable[1]);
                // handle scan result
                Log.v("Scan result:", contents);
            } else {
                // else continue with any other code you need in the method
                Log.v("MainActivity", "scanResult is null.");
            }
        }
    }

    @Override
    public void onBackPressed() {

        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Wychodzę z aplikacji")
                .setMessage("Czy na pewno chcesz wyjść z aplikacji?")
                .setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.button_no), null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        db.close();
    }


    void resumeState() {

        SettingsDataSource db = new SettingsDataSource(this);
        db.open();


        TextView viewIndex = (TextView) findViewById(R.id.index_value);
        TextView viewSubject = (TextView) findViewById(R.id.subject_value);

        String stringIndex = db.getSetting("setting_index");
        String stringSubject = db.getSetting("setting_subject");

        if (viewIndex != null)
            viewIndex.setText(stringIndex);

        if (viewSubject != null)
            viewSubject.setText(stringSubject);

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

        //now get Editor
        SharedPreferences.Editor editor = sharedPref.edit();
        //put your value
        editor.putBoolean("End test", false);

        //commits your edits
        //editor.commit();
        editor.apply();

        resumeState();
    }

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        Log.v("Ilosc instancji: ", String.valueOf(list.size()));
        return list.size() < 2;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        if (id == R.id.action_settings) {

            Intent intentSettings = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intentSettings);

            Log.i("Menu", "Settings");

            return true;
        }

        if (id == R.id.action_info) {

            Intent intentInfo = new Intent(getApplicationContext(), InfoActivity.class);
            startActivity(intentInfo);

            Log.i("Menu", "Info");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
