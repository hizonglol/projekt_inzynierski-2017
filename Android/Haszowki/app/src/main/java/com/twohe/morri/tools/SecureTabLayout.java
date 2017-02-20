package com.twohe.morri.tools;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by morri on 13.02.2017.
 *
 * This file contains class SecureTabLayout
 */

public class SecureTabLayout extends android.support.design.widget.TabLayout {

    ComponentCallbacks2 SecureMemoryBoss;

    public SecureTabLayout(Context context) {
        super(context);
    }
    public SecureTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SecureTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void assignMemoryBoss(ComponentCallbacks2 boss){
        SecureMemoryBoss = boss;
    }

    @Override
    public boolean onFilterTouchEventForSecurity(MotionEvent event) {
        if ((event.getFlags() & MotionEvent.FLAG_WINDOW_IS_OBSCURED) == MotionEvent.FLAG_WINDOW_IS_OBSCURED) {
            Log.d("SecureTabLayout", "is obscured");

            if (SecureMemoryBoss != null)
                SecureMemoryBoss.onTrimMemory(0);

            return false;
        }
        return super.onFilterTouchEventForSecurity(event);
    }
}
