package com.twohe.myapplication;

import android.content.Intent;
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

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        final TextView wyswietlDec = (TextView) findViewById(R.id.view_number_dec);
        final EditText poleIndeks = (EditText) findViewById(R.id.index_number);
        final EditText poleMaska = (EditText) findViewById(R.id.mask_number);

        final Button button = (Button) findViewById(R.id.button);

        //definicja handlera do button
        final View.OnClickListener buttonHandler = new View.OnClickListener() {
            public void onClick(View v) throws NumberFormatException {
                String numer_dec = getString(R.string.label_group_value_dec);
                int wynik = 0;
                int indeks;
                int maska;
                try { //sprawdzamy czy podano numer indeksu
                    indeks = Integer.parseInt(poleIndeks.getText().toString());
                    //Log.v("Wartość indeksu", String.valueOf(indeks));
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_index_number), Toast.LENGTH_SHORT).show();
                    return;
                }

                //sprawdzamy numer maski
                String temp = poleMaska.getText().toString();
                int s = temp.length();
                if (s == 0) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_mask_number), Toast.LENGTH_SHORT).show();
                    return;
                }
                maska = 0;
                for (int i = 0; i < s; ++i) {
                    if (temp.charAt(i) == 0x00) break;
                    if (temp.charAt(i) == '1') {
                        maska <<= 1;
                        maska |= 1;
                    }
                    if (temp.charAt(i) == '0') {
                        maska <<= 1;
                        maska &= 0xFFFFFFFE;
                    }
                }
                //Log.v("Maska", String.valueOf(maska));

                //porownujemy obie cyfry ekstrahujac bity
                int temp_maska = 1;
                temp_maska <<= s - 1;
                //Log.v("Temp maska", String.valueOf(temp_maska));
                int max_ilosc_bitow = 0;

                for (int i = 0; i < s; ++i) {
                    if ((maska & temp_maska) != 0) {
                        //Log.v("Trafiony bit w pozycji: ", String.valueOf(temp_maska));
                        if ((indeks & temp_maska) == (maska & temp_maska)) {
                            wynik <<= 1;
                            wynik |= 1;
                        }
                        if ((indeks & temp_maska) != (maska & temp_maska)) {
                            wynik <<= 1;
                            wynik &= 0xFFFFFFFE;
                        }
                        max_ilosc_bitow += 1;
                    }
                    temp_maska >>= 1;
                }

                //sprawdzamy czy wprowadzona maska byla poprawna
                if (max_ilosc_bitow > 3) {
                    Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_mask_number), Toast.LENGTH_SHORT).show();
                    return;
                }

                //jesli wszystko bylo dobre to wyswietlamy wynik
                wyswietlDec.setText(numer_dec);
                wyswietlDec.append(" " + wynik);

                View current = getCurrentFocus();
                if (current != null) current.clearFocus();
            }
        };

        View.OnFocusChangeListener maskListener = new View.OnFocusChangeListener() {

            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    //Log.i("maskListener", "no focus");
                    String mask = poleMaska.getText().toString();
                    int s = mask.length();
                    StringBuilder sb = new StringBuilder(mask);

                    if (s > 1) {
                        for (int i = 0; i < s; ++i) {
                            if (sb.charAt(0) == '1') break;

                            sb.deleteCharAt(0);
                        }

                        poleMaska.setText(sb.toString());
                    }
                }
            }
        };

        if (button != null) {
            button.setOnClickListener(buttonHandler);
        }

        if (poleMaska != null) {
            poleMaska.setOnFocusChangeListener(maskListener);
        }

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

        if (id == R.id.action_info) {

            Intent intentInfo = new Intent(getApplicationContext(), InfoActivity.class);
            startActivity(intentInfo);

            //Log.i("Menu", "Info");
        }

        return super.onOptionsItemSelected(item);
    }


}
