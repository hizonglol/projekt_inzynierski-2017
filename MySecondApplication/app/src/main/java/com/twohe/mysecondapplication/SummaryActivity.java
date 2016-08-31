package com.twohe.mysecondapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

/**
 * Created by morri on 30.07.2016.
 */
public class SummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        /* obsluga toolbar w Info */
        Toolbar infoToolbar = (Toolbar) findViewById(R.id.summaryToolbar);
        if (infoToolbar != null)
            infoToolbar.setTitle(R.string.label_summary_activity);

        Button exitButton = (Button) findViewById(R.id.button_exit);
        Button backToTestButton = (Button) findViewById(R.id.button_back_to_test);

        View.OnClickListener exitButtonHandler = new View.OnClickListener() {
            public void onClick(View v) {

                Intent intentMain = new Intent(getApplicationContext(), MainActivity.class);
                intentMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentMain.putExtra("Exit me", true);
                startActivity(intentMain);
                finish();
            }
        };
        if (exitButton != null)
            exitButton.setOnClickListener(exitButtonHandler);

        View.OnClickListener backToTestButtonHandler = new View.OnClickListener() {
            public void onClick(View v) {

                finish();

                Log.i("Summary", "Going back to tabs");
            }
        };
        if (backToTestButton != null)
            backToTestButton.setOnClickListener(backToTestButtonHandler);

        TextView viewYesAmount = (TextView) findViewById(R.id.yes_answer_number);
        TextView viewNoAmount = (TextView) findViewById(R.id.no_answer_number);
        TextView viewDunnoAmount = (TextView) findViewById(R.id.dunno_answer_number);
        TextView viewQuestionsAmount = (TextView) findViewById(R.id.sum_of_answers_number);

        Bundle b = getIntent().getExtras();
        int amount_of_questions = -1; // or other values
        int amount_of_yes_answers = -1;
        int amount_of_no_answers = -1;
        int amount_of_dunno_answers = -1;
        if(b != null) {
            amount_of_questions = b.getInt("amount_of_questions");
            amount_of_yes_answers = b.getInt("amount_of_yes_answers");
            amount_of_no_answers = b.getInt("amount_of_no_answers");
            amount_of_dunno_answers = b.getInt("amount_of_dunno_answers");
        }

        if (viewQuestionsAmount != null)
            viewQuestionsAmount.setText(String.valueOf(amount_of_questions));

        if (viewYesAmount != null)
            viewYesAmount.setText(String.valueOf(amount_of_yes_answers));

        if (viewNoAmount != null)
            viewNoAmount.setText(String.valueOf(amount_of_no_answers));

        if (viewDunnoAmount != null)
            viewDunnoAmount.setText(String.valueOf(amount_of_dunno_answers));
    }
}