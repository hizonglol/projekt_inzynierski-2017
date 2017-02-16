package com.twohe.morri.tools;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Created by morri on 13.02.2017.
 *
 * This file contains class SecureTextView
 */

public class SecureTextView extends TextView {

    ComponentCallbacks2 SecureMemoryBoss;

    public SecureTextView(Context context) {
        super(context);
    }

    public void assignMemoryBoss(ComponentCallbacks2 boss){
        SecureMemoryBoss = boss;
    }

    @Override
    public boolean onFilterTouchEventForSecurity(MotionEvent event) {
        if ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) == MotionEvent.FLAG_WINDOW_IS_OBSCURED) {
            Log.d("SecureTextView", "is obscured");

            if (SecureMemoryBoss != null)
                SecureMemoryBoss.onTrimMemory(0);

            return false;
        }
        return super.onFilterTouchEventForSecurity(event);
    }
}
