package com.twohe.mysecondapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

    int moduloGroup = 0;
    boolean moduloflag = false;

    private boolean isEmpty(EditText etText) {
        return etText.getText().toString().trim().length() == 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* check if the app is intended to close */
        if( getIntent().getBooleanExtra("Exit me", false)){
            finish();
            return; // return to prevent from doing unnecessary stuffs
        }

        //*****************************************************************************************

        SettingsDataSource db = new SettingsDataSource(this);
        db.open();

        db.updateSetting("dupadupa55", "blebl55e");

        //*****************************************************************************************

        Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);

        Button startTestButton = (Button) findViewById(R.id.button_start_test);
        Button exitButton = (Button) findViewById(R.id.button_exit);

        final Button computeButton = (Button) findViewById(R.id.button_compute);
        final View.OnClickListener computeButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {

                int cyfryIndeksu[] = new int[6];
                int wartosciWag[] = new int[6];
                int cyfryWyniku[] = new int[6];

                //*******************************************************
                //SPRAWDZAMY TERAZ INDEKS
                //*******************************************************
                /*
                try {
                    cyfryIndeksu[1] = Integer.parseInt(indexNumber5.getText().toString());
                    Log.v("Wartość", String.valueOf(cyfryIndeksu[1]));
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), "Podaj 2 cyfrę indeksu", Toast.LENGTH_SHORT).show();
                    return;
                }*/

                //*******************************************************
                //SPRAWDZAMY TERAZ WAGI
                //*******************************************************
                /*
                try { //sprawdzamy czy podano numer indeksu
                    wartosciWag[0] = Integer.parseInt(weightNumber6.getText().toString());
                    Log.v("Wartość", String.valueOf(wartosciWag[0]));
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), "Podaj 1 cyfrę indeksu", Toast.LENGTH_SHORT).show();
                    return;
                }*/


                int wynik = 0;
                for (int i = 0; i < 6; ++i) {
                    cyfryWyniku[i] = cyfryIndeksu[i] * wartosciWag[i];
                    wynik += cyfryWyniku[i];
                }

                /*
                if (resultNumber6 != null) {
                    resultNumber6.setText(String.valueOf(cyfryWyniku[0]));
                }*/


                wynik = wynik % 16;
                moduloGroup = wynik;
                moduloflag = true;

                TextView moduloResult = (TextView) findViewById(R.id.result_modulo_value);

                /*
                if (moduloResult != null) {
                    moduloResult.setText(Html.fromHtml("<small>" + getString(R.string.modulo_result) + "</small>" + "<br />" +
                            "<bold>" + String.valueOf(wynik) + "</bold>"));
                }*/

            }
        };
        if (computeButton != null) {
            //computeButton.setOnClickListener(computeButtonHandler);
        }

        View.OnClickListener startTestButtonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {
                    /*
                    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                            */

                /*
                if(!moduloflag){
                    Toast.makeText(getBaseContext(), "Wylicz grupę", Toast.LENGTH_SHORT).show();
                    return;
                }*/

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

                Intent intent = getIntent();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("Exit me", true);
                startActivity(intent);
                finish();

            }
        };
        if (exitButton != null)
            exitButton.setOnClickListener(exitButtonHandler);
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

        if (id == R.id.action_summary) {

            Intent intentSummary = new Intent(getApplicationContext(), SummaryActivity.class);
            startActivity(intentSummary);

            Log.i("Menu", "Info");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
