package com.twohe.mysecondapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

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

    }
}