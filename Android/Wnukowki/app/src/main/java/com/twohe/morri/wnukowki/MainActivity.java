package com.twohe.morri.wnukowki;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by morri on 03.07.2016.
 * <p>
 * Models an activity where user can compute decimal value of group
 * typing his decimal student number and binary mask. Mask is applied on
 * binary form of student number with AND logic. Result is viewed
 * in decimal form.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Initializes variables with viewed layout and sets up listeners.
     *
     * @param savedInstanceState bundle with dynamic instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        Association of elements from layout with local variables
         */
        toolbarMain = (Toolbar) findViewById(R.id.toolbarMain);
        viewMain_result = (TextView) findViewById(R.id.viewMain_result);
        editMain_studNum = (EditText) findViewById(R.id.editMain_studNum);
        editMain_mask = (EditText) findViewById(R.id.editMain_mask);
        buttonMain_compute = (Button) findViewById(R.id.buttonMain_compute);

        if (toolbarMain != null)
            setSupportActionBar(toolbarMain);

        if (buttonMain_compute != null)
            buttonMain_compute.setOnClickListener(buttonMainCallback);

        if (editMain_mask != null)
            editMain_mask.setOnFocusChangeListener(maskListener);

    }

    /**
     * Inflates the menu. Adds items to the action bar if it is present.
     *
     * @param menu interface for managing the items in a menu
     * @return you must return true for the menu to be displayed; if you return false it will not be shown
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up buttonMain_compute, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_info) {

            Intent intentInfo = new Intent(getApplicationContext(), InfoActivity.class);
            startActivity(intentInfo);
        }

        return super.onOptionsItemSelected(item);
    }

    TextView viewMain_result;
    EditText editMain_studNum;
    EditText editMain_mask;
    Button buttonMain_compute;
    Toolbar toolbarMain;

    /**
     * Callback for clicking a button.
     * Computes all the stuff with putting mask on binary student number
     * and applies the result on viewMain_result.
     */
    View.OnClickListener buttonMainCallback = new View.OnClickListener() {
        public void onClick(View v) throws NumberFormatException {
            String resultLabel = getString(R.string.label_group_value_dec);
            int result = 0;
            int studentNumber;
            int maskInt;
            try {
                studentNumber = Integer.parseInt(editMain_studNum.getText().toString());
            } catch (NumberFormatException e) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_index_number), Toast.LENGTH_SHORT).show();
                return;
            }

            String maskString = editMain_mask.getText().toString();
            int maskLength = maskString.length();
            if (maskLength == 0) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_give_mask_number), Toast.LENGTH_SHORT).show();
                return;
            }
            maskInt = 0;
            for (int i = 0; i < maskLength; ++i) {
                if (maskString.charAt(i) == 0x00) break;
                if (maskString.charAt(i) == '1') {
                    maskInt <<= 1;
                    maskInt |= 1;
                }
                if (maskString.charAt(i) == '0') {
                    maskInt <<= 1;
                    maskInt &= 0xFFFFFFFE;
                }
            }

            int temp_maskInt = 1;
            temp_maskInt <<= maskLength - 1;
            int foundBitOccurences = 0;

            for (int i = 0; i < maskLength; ++i) {
                if ((maskInt & temp_maskInt) != 0) {
                    if ((studentNumber & temp_maskInt) == (maskInt & temp_maskInt)) {
                        result <<= 1;
                        result |= 1;
                    }
                    if ((studentNumber & temp_maskInt) != (maskInt & temp_maskInt)) {
                        result <<= 1;
                        result &= 0xFFFFFFFE;
                    }
                    foundBitOccurences += 1;
                }
                temp_maskInt >>= 1;
            }

            if (foundBitOccurences > 3) {
                Toast.makeText(getBaseContext(), getResources().getString(R.string.message_invalid_mask_number), Toast.LENGTH_SHORT).show();
                return;
            }

            viewMain_result.setText(resultLabel);
            viewMain_result.append(" " + result);

            View current = getCurrentFocus();
            if (current != null) current.clearFocus();
        }
    };

    /**
     * Callback for focus change.
     * Checks whether editMain_mask has no focus and then removes
     * leading zeros in student number.
     */
    View.OnFocusChangeListener maskListener = new View.OnFocusChangeListener() {

        public void onFocusChange(View v, boolean hasFocus) {
            if (!hasFocus) {
                String mask = editMain_mask.getText().toString();
                int maskLength = mask.length();
                StringBuilder modifableMask = new StringBuilder(mask);

                if (maskLength > 1) {
                    for (int i = 0; i < maskLength; ++i) {
                        if (modifableMask.charAt(0) == '0')
                            modifableMask.deleteCharAt(0);
                        else break;
                    }

                    editMain_mask.setText(modifableMask.toString());
                }
            }
        }
    };

}
