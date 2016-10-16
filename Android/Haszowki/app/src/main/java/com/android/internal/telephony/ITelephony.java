package com.android.internal.telephony;

/**
 * Created by morri on 16.10.2016.
 */

public interface ITelephony {

    boolean endCall();

    void answerRingingCall();

    void silenceRinger();

}
