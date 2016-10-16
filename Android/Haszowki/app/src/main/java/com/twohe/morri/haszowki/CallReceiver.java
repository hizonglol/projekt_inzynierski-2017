package com.twohe.morri.haszowki;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.twohe.morri.reusable.receivers.PhonecallReceiver;

import java.util.Date;

/**
 * Created by morri on 14.10.2016.
 *
 * dd
 */

public class CallReceiver extends PhonecallReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);


    }

    @Override
    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        Log.d("onIncomingCallReceived", "handled");
    }

    @Override
    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
        Log.d("onIncomingCallAnswered", "handled");
        //
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        Log.d("onIncomingCallEnded", "handled");
        //
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
        Log.d("onOutgoingCallStarted", "handled");
        //
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
        Log.d("onOutgoingCallEnded", "handled");
        //
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start)
    {
        Log.d("onMissedCall", "handled");
        //
    }

}