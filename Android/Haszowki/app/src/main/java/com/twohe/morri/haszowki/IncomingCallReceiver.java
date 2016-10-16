package com.twohe.morri.haszowki;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

/**
 * Created by morri on 16.10.2016.
 *
 * dd
 */

public class IncomingCallReceiver extends BroadcastReceiver {
    private ITelephony telephonyService;

    @Override
    public void onReceive(Context context, Intent intent) {

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(tm);
            Bundle bundle = intent.getExtras();
            String phoneNumber = bundle.getString("incoming_number");
            //Log.e("INCOMING", phoneNumber);
            if (phoneNumber != null) {
                telephonyService.silenceRinger();
                telephonyService.endCall();
                //Log.e("HANG UP", phoneNumber);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
