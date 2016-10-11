package com.twohe.morri.wnukowki;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by morri on 03.07.2016.
 *
 * Models an activity where information about this app is viewed.
 * It shows version of current application build.
 */
public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        toolbarInfo = (Toolbar) findViewById(R.id.toolbarInfo);
        if (toolbarInfo != null)
            toolbarInfo.setTitle(R.string.label_info_activity);

    }

    Toolbar toolbarInfo;
}