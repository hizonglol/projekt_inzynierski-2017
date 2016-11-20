package com.twohe.morri.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.Method;

/**
 * Created by morri on 16.10.2016.
 *
 * This file contains class IncomingCallReceiver.
 * It is being used to reject incoming calls.
 */

public class IncomingCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPrefICR = PreferenceManager.getDefaultSharedPreferences(context);

        TelephonyManager telephonyManagerICR = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> c = Class.forName(telephonyManagerICR.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephonyManagerICR);
            Bundle bundle = intent.getExtras();
            String phoneNumber = bundle.getString("incoming_number");
            //Log.e("INCOMING", phoneNumber);
            if (phoneNumber != null) {
                telephonyService.silenceRinger();
                telephonyService.endCall();
                //Log.e("HANG UP", phoneNumber);

                /*
                SharedPreferences.Editor editor = sharedPrefICR.edit();
                editor.putBoolean("Call handled", true);
                editor.putBoolean("Call handle changed", true);
                editor.apply();
                */
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
