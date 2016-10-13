package com.twohe.morri.haszowki;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

/**
 * Created by morri on 10.07.2016.
 *
 * Models an activity where information about this app are viewed.
 * It shows version of current application build.
 */
public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        /* obsluga toolbar w Info */
        infoToolbar = (Toolbar) findViewById(R.id.infoToolbar);
        if (infoToolbar != null)
            infoToolbar.setTitle(R.string.label_info_activity);

    }

    Toolbar infoToolbar;
}
