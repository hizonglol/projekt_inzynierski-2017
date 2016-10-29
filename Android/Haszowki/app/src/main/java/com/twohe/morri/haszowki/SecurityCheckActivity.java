package com.twohe.morri.haszowki;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.dd.CircularProgressButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.safetynet.SafetyNet;
import com.twohe.morri.tools.SettingsDataSource;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This activity is being used to check application
 */
public class SecurityCheckActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /**
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_check);

        databaseSecurityCheck = new SettingsDataSource(this);
        databaseSecurityCheck.open();

        serverConnectionSuccessful = false;

        progressButtonSecurityCheck_validation = (CircularProgressButton) findViewById(R.id.progressButtonSecurityCheck_validation);
        progressButtonSecurityCheck_serverConnection = (CircularProgressButton) findViewById(R.id.progressButtonSecurityCheck_serverConnection);
        progressButtonSecurityCheck_appConfiguration = (CircularProgressButton) findViewById(R.id.progressButtonSecurityCheck_appConfiguration);
        buttonSecurityCheck_continue = (Button) findViewById(R.id.buttonSecurityCheck_continue);
        buttonSecurityCheck_abort = (Button) findViewById(R.id.buttonSecurityCheck_abort);

        initializeSpinning();

        serverCheckTaskSecurityCheck = new serverCheckTask();
        serverCheckTaskSecurityCheck.execute();

        progressButtonSecurityCheck_validation.setProgress(0);
        progressButtonSecurityCheck_appConfiguration.setProgress(0);

        buttonSecurityCheck_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (serverConnectionSuccessful) {
                    Intent intentTabs = new Intent(getApplicationContext(), TabsActivity.class);
                    startActivity(intentTabs);
                    finish();
                }
                else
                    retryChecks();

            }
        });

        buttonSecurityCheck_abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    private void retryChecks(){
        if (!serverConnectionSuccessful){
            restartSpinning(progressButtonSecurityCheck_serverConnection);
            serverCheckTaskSecurityCheck = new serverCheckTask();
            serverCheckTaskSecurityCheck.execute();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    /**
     *
     * @param result
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // TODO Auto-generated method stub

        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                result.startResolutionForResult(this, // your activity
                        RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent. Return to
                // default
                // state and attempt to connect to get an updated
                // ConnectionResult.
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }

    }

    /**
     *
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(SafetyNet.API)
                .addConnectionCallbacks(this)
                .build();
    }

    /**
     * Used to initialize spinning of all three spinners.
     */
    private void initializeSpinning() {
        progressButtonSecurityCheck_validation.setProgress(1);
        progressButtonSecurityCheck_validation.setIndeterminateProgressMode(true);
        progressButtonSecurityCheck_serverConnection.setProgress(1);
        progressButtonSecurityCheck_serverConnection.setIndeterminateProgressMode(true);
        progressButtonSecurityCheck_appConfiguration.setProgress(1);
        progressButtonSecurityCheck_appConfiguration.setIndeterminateProgressMode(true);
    }

    /**
     * Restarts button and makes it spinning again.
     *
     * @param button that will be affected
     */
    private void restartSpinning(CircularProgressButton button) {
        button.setIndeterminateProgressMode(true); // turn on indeterminate progress
        button.setProgress(0); // set progress > 0 & < 100 to display indeterminate progress
        button.setProgress(1); // set progress > 0 & < 100 to display indeterminate progress
    }

    /**
     * Set spinner to successful
     *
     * @param button that will be affected
     */
    private void spinnerSuccessful(CircularProgressButton button) {
        button.setProgress(0);
        button.setProgress(100);
    }

    /**
     * Set spinner to unsuccessful
     *
     * @param button that will be affected
     */
    private void spinnerUnsuccessful(CircularProgressButton button) {
        button.setProgress(0);
        button.setProgress(-1);
    }

    /**
     * Being used to run server reachability check.
     */
    private class serverCheckTask extends AsyncTask<String, String, String> {

        /**
         * Used to launch server reachability.
         *
         * @param urls execution parameters
         * @return success if successful, failure if unsuccessful
         */
        @Override
        protected String doInBackground(String... urls) {

            String stringDbServerUrl = databaseSecurityCheck.getSetting("setting_serverAddress");
            String stringServerUrl = getResources().getString(R.string.server_address);
            if (stringDbServerUrl.length() > 1){
                stringServerUrl = stringDbServerUrl;
            }

            if (isReachable(getApplicationContext(), stringServerUrl))
                return "success";
            else
                return "failure";
        }

        /**
         * Handles result of doInBackground
         *
         * @param result of check
         */
        @Override
        protected void onPostExecute(String result) {
            if(result.equals("success")) {
                spinnerSuccessful(progressButtonSecurityCheck_serverConnection);
                buttonSecurityCheck_continue.setText("Kontynuuj");
                serverConnectionSuccessful = true;
            }
            else if (result.equals("failure")) {
                spinnerUnsuccessful(progressButtonSecurityCheck_serverConnection);
                buttonSecurityCheck_continue.setText("Spróbuj ponownie");
                serverConnectionSuccessful = false;
            }
        }
    }

    /**
     * Checks if server with given serverAddress is reachable
     *
     * @param context of application
     * @param serverAddress address of checked server
     * @return
     */
    public static boolean isReachable(Context context, String serverAddress) {
        // First, check we have any sort of connectivity
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        boolean isReachable = false;

        if (netInfo != null && netInfo.isConnected()) {
            // Some sort of connection is open, check if server is reachable
            try {
                URL url = new URL(serverAddress);
                HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
                urlc.setRequestProperty("User-Agent", "Android Application");
                urlc.setRequestProperty("Connection", "close");
                urlc.setConnectTimeout(5 * 1000); //5 sek
                urlc.connect();
                isReachable = (urlc.getResponseCode() == 200);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return isReachable;
    }

    private CircularProgressButton progressButtonSecurityCheck_validation;
    private CircularProgressButton progressButtonSecurityCheck_serverConnection;
    private CircularProgressButton progressButtonSecurityCheck_appConfiguration;
    private Button buttonSecurityCheck_continue;
    private Button buttonSecurityCheck_abort;
    private serverCheckTask serverCheckTaskSecurityCheck;
    private SettingsDataSource databaseSecurityCheck;

    /*
    * A flag indicating that app validation was successful.
    */
    private boolean validationSuccessful;
    /*
    * A flag indicating that server is reachable.
    */
    private boolean serverConnectionSuccessful;
    /*
    * A flag indicating that app configuration went successful.
    */
    private boolean appConfigurationSuccessful;
    /*
    * A flag indicating that a PendingIntent is in progress and prevents us
    * from starting further intents.
    */
    private boolean mIntentInProgress;
    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;
    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
}
