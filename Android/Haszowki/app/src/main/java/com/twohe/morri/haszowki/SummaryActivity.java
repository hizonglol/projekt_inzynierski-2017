package com.twohe.morri.haszowki;

import android.content.ComponentCallbacks2;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by morri on 30.07.2016.
 */
public class SummaryActivity extends AppCompatActivity {

    SharedPreferences sharedPref;

    boolean wasInBackground = false;

    public class MemoryBoss implements ComponentCallbacks2 {
        @Override
        public void onConfigurationChanged(final Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(final int level) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                // We're in the Background
                wasInBackground = true;
            }
            // you might as well implement some memory cleanup here and be a nice Android dev.
        }
    }

    Thread thread = new Thread() {
        @Override
        public void run() {
            try {
                Thread.sleep(3500); // As I am using LENGTH_LONG in Toast
                SummaryActivity.this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        Log.d("On create", "SummaryActivity");

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SummaryActivity.MemoryBoss mMemoryBoss;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mMemoryBoss = new SummaryActivity.MemoryBoss();
            registerComponentCallbacks(mMemoryBoss);
        }

        /* obsluga toolbar w Info */
        Toolbar infoToolbar = (Toolbar) findViewById(R.id.summaryToolbar);
        if (infoToolbar != null)
            infoToolbar.setTitle(R.string.label_summary_activity);

        Button exitButton = (Button) findViewById(R.id.buttonMain_exitApp);
        Button backToTestButton = (Button) findViewById(R.id.button_back_to_test);

        View.OnClickListener exitButtonHandler = new View.OnClickListener() {
            public void onClick(View v) {

                /*
                Intent intentMain = new Intent(getApplicationContext(), MainActivity.class);
                intentMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentMain.putExtra("Exit me", true);
                startActivity(intentMain);
                */
                //now get Editor
                SharedPreferences.Editor editor = sharedPref.edit();
                //put your value
                editor.putBoolean("End test", true);
                //commits your edits
                //editor.commit();
                editor.apply();
                finish();
            }
        };
        if (exitButton != null)
            exitButton.setOnClickListener(exitButtonHandler);

        View.OnClickListener backToTestButtonHandler = new View.OnClickListener() {
            public void onClick(View v) {

                finish();
            }
        };
        if (backToTestButton != null)
            backToTestButton.setOnClickListener(backToTestButtonHandler);

        TextView viewYesAmount = (TextView) findViewById(R.id.yes_answer_number);
        TextView viewNoAmount = (TextView) findViewById(R.id.no_answer_number);
        TextView viewDunnoAmount = (TextView) findViewById(R.id.dunno_answer_number);
        TextView viewQuestionsAmount = (TextView) findViewById(R.id.sum_of_answers_number);

        Bundle b = getIntent().getExtras();

        if (b != null) {
            fileYesAnswers = b.getInt("tabs_fileYesAnswers");
            fileNoAnswers = b.getInt("tabs_fileNoAnswers");
            fileDunnoAnswers = b.getInt("tabs_fileDunnoAnswers");
            serverYesAnswers = b.getInt("tabs_serverYesAnswers");
            serverNoAnswers = b.getInt("tabs_serverNoAnswers");
            serverDunnoAnswers = b.getInt("tabs_serverDunnoAnswers");
        }

        if (viewQuestionsAmount != null)
            viewQuestionsAmount.setText(String.valueOf(fileYesAnswers + fileNoAnswers + fileDunnoAnswers));

        if (viewYesAmount != null)
            viewYesAmount.setText(String.valueOf(serverYesAnswers));

        if (viewNoAmount != null)
            viewNoAmount.setText(String.valueOf(serverNoAnswers));

        if (viewDunnoAmount != null)
            viewDunnoAmount.setText(String.valueOf(serverDunnoAnswers));

        checkErrors();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (wasInBackground) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.message_you_quit_test))
                    .setMessage(getResources().getString(R.string.message_test_ended))
                    .setCancelable(false)
                    .show();


            //now get Editor
            SharedPreferences.Editor editor = sharedPref.edit();
            //put your value
            editor.putBoolean("End test", true);
            //commits your edits
            //editor.commit();
            editor.apply();

            thread.start();
        }
    }

    private void checkErrors() {
        boolean ifError = false;

        StringBuilder errorMessage = new StringBuilder(getResources().getString(R.string.message_answers_error));
        errorMessage.append("\n");

        if (fileYesAnswers > serverYesAnswers) {
            String yesErrorPattern = getResources().getString(R.string.message_yes_answers_error);
            String yesError = String.format(yesErrorPattern, fileYesAnswers - serverYesAnswers);
            errorMessage.append(yesError);
            errorMessage.append(".");
            ifError = true;
        }

        if (fileNoAnswers > serverNoAnswers) {
            if (errorMessage.charAt(errorMessage.length() - 1) == '.') {
                errorMessage.deleteCharAt(errorMessage.length() - 1);
                errorMessage.append(", ").append("\n");
            }

            String noErrorPattern = getResources().getString(R.string.message_no_answers_error);
            String noError = String.format(noErrorPattern, fileNoAnswers - serverNoAnswers);
            errorMessage.append(noError);
            errorMessage.append(".");
            ifError = true;
        }

        if (fileDunnoAnswers > serverDunnoAnswers) {
            if (errorMessage.charAt(errorMessage.length() - 1) == '.') {
                errorMessage.deleteCharAt(errorMessage.length() - 1);
                errorMessage.append(", ").append("\n");
            }

            String dunnoErrorPattern = getResources().getString(R.string.message_dunno_answers_error);
            String dunnoError = String.format(dunnoErrorPattern, fileDunnoAnswers - serverDunnoAnswers);
            errorMessage.append(dunnoError);
            errorMessage.append(".");
            ifError = true;
        }

        errorMessage.append("\n").append("\n");
        errorMessage.append(getResources().getString(R.string.message_what_to_do));

        if (ifError)
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.message_answers_error_title))
                    .setMessage(errorMessage.toString())
                    .setNegativeButton(getResources().getString(R.string.button_ok), null)
                    .show();

    }

    int fileYesAnswers = -1;
    int fileNoAnswers = -1;
    int fileDunnoAnswers = -1;
    int serverYesAnswers = -1;
    int serverNoAnswers = -1;
    int serverDunnoAnswers = -1;
}