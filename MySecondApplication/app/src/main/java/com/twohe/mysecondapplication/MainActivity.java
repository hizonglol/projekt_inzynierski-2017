package com.twohe.mysecondapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    boolean moduloflag = false;

    SettingsDataSource db = new SettingsDataSource(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* check if the app is intended to close */
        if (getIntent().getBooleanExtra("Exit me", false)) {
            finish();
            return; // return to prevent from doing unnecessary stuffs
        }

        //*****************************************************************************************

        db.open();

        //*****************************************************************************************

        Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);

        Button startTestButton = (Button) findViewById(R.id.button_start_test);
        Button exitButton = (Button) findViewById(R.id.button_exit);

        final Button computeButton = (Button) findViewById(R.id.button_compute);
        final View.OnClickListener computeButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {

                moduloflag = false;

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

                if (editWeights != null)
                    stringWeights = editWeights.getText().toString();

                try {
                    for (int i = 0; i < 6; ++i) {
                        digitsIndex[i] = Integer.parseInt(Character.toString(stringIndex.charAt(i)));
                        Log.v("Index", String.valueOf(digitsIndex[i]));
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
                moduloflag = true;

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


                if (!moduloflag) {
                    Toast.makeText(getBaseContext(), "Wylicz poprawną grupę", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intentTabs = new Intent(getApplicationContext(), TabsActivity.class);
                if (isCallable(intentTabs)) {
                    Log.i("Main", "Setting up tabs/navigating to them");
                    startActivity(intentTabs);
                } else if (!isCallable(intentTabs)) {
                    Log.i("Main", "Navigating to tabs");
                    navigateUpTo(intentTabs);
                }
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
