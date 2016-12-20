package com.twohe.morri.haszowki;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dd.CircularProgressButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.safetynet.SafetyNet;
import com.twohe.morri.tools.SettingsDataSource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Created by morri.
 * <p>
 * This file contains class Settings Activity.
 */
public class SecurityCheckActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;
    static String minAppVersion;
    static String maxAppVersion;
    static String cipheringKey;
    private CircularProgressButton progressButtonSecurityCheck_validation;
    private CircularProgressButton progressButtonSecurityCheck_serverConnection;
    private CircularProgressButton progressButtonSecurityCheck_appConfiguration;
    private Button buttonSecurityCheck_continue;
    private Button buttonSecurityCheck_abort;
    private serverConnectionCheckTask serverCheckTask;
    private appConfigurationTask configurationTask;
    private SettingsDataSource databaseSecurityCheck;
    private AlertDialog alertDialog;
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
    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    private SharedPreferences sharedPrefSecurity;

    /**
     * Checks if server with given serverAddress is reachable
     *
     * @param context       of application
     * @param serverAddress address of checked server
     * @return true if server is reachable, false if not
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

    /**
     * Downloads configuration file from server
     *
     * @param context               of application
     * @param serverDocumentAddress address of configuration file
     * @return true if downloaded configuration, false if not
     */
    public static boolean downloadedConfiguration(Context context, String serverDocumentAddress) {
        // First, check we have any sort of connectivity
        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
        boolean downloadedConfiguration = false;

        if (netInfo != null && netInfo.isConnected()) {
            // Some sort of connection is open, check if server is reachable
            try {
                URL url = new URL(serverDocumentAddress);
                URLConnection conn = url.openConnection();

                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser configurationParser = xmlFactoryObject.newPullParser();
                configurationParser.setInput(conn.getInputStream(), null);

                int event = configurationParser.getEventType();
                while (event != XmlPullParser.END_DOCUMENT) {
                    String name = configurationParser.getName();
                    switch (event) {
                        case XmlPullParser.START_TAG:
                            break;

                        case XmlPullParser.END_TAG:
                            if (name.equals("version")) {
                                minAppVersion = configurationParser.getAttributeValue(null, "min");
                                maxAppVersion = configurationParser.getAttributeValue(null, "max");
                            } else if (name.equals("key")) {
                                cipheringKey = configurationParser.getAttributeValue(null, "rsa");
                            }
                            break;
                    }
                    event = configurationParser.next();
                }

                downloadedConfiguration = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return downloadedConfiguration;
    }

    /**
     * @param savedInstanceState instance state of created activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_check);

        Log.d("On create", "SecurityCheckActivity");

        sharedPrefSecurity = PreferenceManager.getDefaultSharedPreferences(this);
        databaseSecurityCheck = new SettingsDataSource(this);
        databaseSecurityCheck.open();

        serverConnectionSuccessful = false;

        progressButtonSecurityCheck_validation = (CircularProgressButton) findViewById(R.id.progressButtonSecurityCheck_validation);
        progressButtonSecurityCheck_serverConnection = (CircularProgressButton) findViewById(R.id.progressButtonSecurityCheck_serverConnection);
        progressButtonSecurityCheck_appConfiguration = (CircularProgressButton) findViewById(R.id.progressButtonSecurityCheck_appConfiguration);
        buttonSecurityCheck_continue = (Button) findViewById(R.id.buttonSecurityCheck_continue);
        buttonSecurityCheck_abort = (Button) findViewById(R.id.buttonSecurityCheck_abort);

        initializeSpinning();

        serverCheckTask = new serverConnectionCheckTask();
        serverCheckTask.execute();
        configurationTask = new appConfigurationTask();
        configurationTask.execute();

        buttonSecurityCheck_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (serverConnectionSuccessful && appConfigurationSuccessful && validationSuccessful) {

                    SharedPreferences.Editor editor = sharedPrefSecurity.edit();
                    editor.putString("Key", cipheringKey);
                    editor.apply();

                    Intent intentTabs = new Intent(getApplicationContext(), TabsActivity.class);
                    startActivity(intentTabs);
                    finish();
                } else
                    retryChecks();

            }
        });

        buttonSecurityCheck_abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serverCheckTask.cancel(true);
                configurationTask.cancel(true);

                try {
                    if (alertDialog != null && alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                finish();
            }
        });

    }

    /**
     * Used to execute check tasks.
     * Restarts spinners.
     */
    private void retryChecks() {
        if (!serverConnectionSuccessful) {
            serverCheckTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_serverConnection);
            serverCheckTask = new serverConnectionCheckTask();
            serverCheckTask.execute();
        }

        if (!appConfigurationSuccessful || !validationSuccessful) {
            configurationTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_appConfiguration);
            restartSpinning(progressButtonSecurityCheck_validation);
            configurationTask = new appConfigurationTask();
            configurationTask.execute();
        }
    }

    /**
     * USE IT FOR SAFETYNET
     *
     * @param connectionHint not yet described
     */
    @Override
    public void onConnected(Bundle connectionHint) {
    }

    /**
     * USE IT FOR SAFETYNET
     *
     * @param cause not yet described
     */
    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    /**
     * USE IT FOR SAFETYNET
     *
     * @param result not yet described
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
     * USE IT FOR SAFETYNET
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
     * Used to check if these values are not null.
     * If they're not null then checks if they're not equal "".
     *
     * @param minVersion minimal version
     * @param maxVersion maximal version
     * @param rsaKey     ciphering key
     * @return true if proper, false if not
     */
    private boolean isConfigurationProper(String minVersion, String maxVersion, String rsaKey) {

        if (minVersion == null) {
            alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.label_attention))
                    .setMessage(getResources().getString(R.string.message_missing_min_version))
                    .setPositiveButton(getResources().getString(R.string.button_ok), null)
                    .show();
            return false;
        } else {
            if (minVersion.equals("")) {
                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage(getResources().getString(R.string.message_missing_min_version))
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
                return false;
            }
        }

        if (maxVersion == null) {
            alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.label_attention))
                    .setMessage(getResources().getString(R.string.message_missing_max_version))
                    .setPositiveButton(getResources().getString(R.string.button_ok), null)
                    .show();
            return false;
        } else {
            if (maxVersion.equals("")) {
                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage(getResources().getString(R.string.message_missing_max_version))
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
                return false;
            }
        }

        if (rsaKey == null) {
            alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.label_attention))
                    .setMessage(getResources().getString(R.string.message_missing_key))
                    .setPositiveButton(getResources().getString(R.string.button_ok), null)
                    .show();
            return false;
        } else {
            if (rsaKey.equals("")) {
                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage(getResources().getString(R.string.message_missing_key))
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
                return false;
            } else if (!isRSAkeyValid()) {
                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage(getResources().getString(R.string.message_wrong_key_abort_test))
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether application version higher or equal than minimal version,
     * and lower or equal than maximal version
     *
     * @param minVersion minimal app version
     * @param maxVersion maximal app version
     * @return true if acceptable, false if not
     */
    private boolean isAppVersionAcceptable(String minVersion, String maxVersion) {

        String appVersion = getResources().getString(R.string.version_value);

        int[] minIntVersion = new int[3];
        int[] maxIntVersion = new int[3];
        int[] intAppVersion = new int[3];

        for (int i = 0; i < 3; ++i) {
            minIntVersion[i] = Integer.valueOf(minVersion.split("\\.")[i]);
            maxIntVersion[i] = Integer.valueOf(maxVersion.split("\\.")[i]);
            intAppVersion[i] = Integer.valueOf(appVersion.split("\\.")[i]);
        }

        if (intAppVersion[0] < minIntVersion[0] || intAppVersion[0] > maxIntVersion[0]) {
            return false;
        } else if (intAppVersion[0] > minIntVersion[0] && intAppVersion[0] < maxIntVersion[0]) {
            return true;
        } else if (intAppVersion[0] == minIntVersion[0] && intAppVersion[0] < maxIntVersion[0]) {
            if (intAppVersion[1] < minIntVersion[1]) {
                return false;
            } else if (intAppVersion[1] > minIntVersion[1]) {
                return true;
            } else if (intAppVersion[1] == minIntVersion[1]) {
                if (intAppVersion[2] < minIntVersion[2]) {
                    return false;
                } else if (intAppVersion[2] >= minIntVersion[2]) {
                    return true;
                }
            }
        } else if (intAppVersion[0] > minIntVersion[0] && intAppVersion[0] == maxIntVersion[0]) {
            if (intAppVersion[1] > maxIntVersion[1]) {
                return false;
            } else if (intAppVersion[1] < maxIntVersion[1]) {
                return true;
            } else if (intAppVersion[1] == maxIntVersion[1]) {
                if (intAppVersion[2] > maxIntVersion[2]) {
                    return false;
                } else if (intAppVersion[2] <= maxIntVersion[2]) {
                    return true;
                }
            }
        } else if (intAppVersion[0] == minIntVersion[0] && intAppVersion[0] == maxIntVersion[0]) {
            if (intAppVersion[1] < minIntVersion[1] || intAppVersion[1] > maxIntVersion[1]) {
                return false;
            } else if (intAppVersion[1] > minIntVersion[1] && intAppVersion[1] < maxIntVersion[1]) {
                return true;
            } else if (intAppVersion[1] > minIntVersion[1] && intAppVersion[1] == maxIntVersion[1]) {
                if (intAppVersion[2] > maxIntVersion[2]) {
                    return false;
                } else if (intAppVersion[2] <= maxIntVersion[2]) {
                    return true;
                }
            } else if (intAppVersion[1] == minIntVersion[1] && intAppVersion[1] < maxIntVersion[1]) {
                if (intAppVersion[2] < minIntVersion[2]) {
                    return false;
                } else if (intAppVersion[2] >= minIntVersion[2]) {
                    return true;
                }
            } else if (intAppVersion[1] == minIntVersion[1] && intAppVersion[1] == maxIntVersion[1]) {

                if (intAppVersion[2] < minIntVersion[2] || intAppVersion[2] > maxIntVersion[2]) {
                    return false;
                } else if (intAppVersion[2] >= minIntVersion[2] && intAppVersion[2] <= maxIntVersion[2]) {
                    return true;
                }
            }
        }

        Log.d("termination", "total");
        return false;
    }

    /**
     * Checks if RSA key is valid or not by trying to cipher text
     *
     * @return true i valid, false if not
     */
    private boolean isRSAkeyValid() {

        String text = "Blebleble";

        try {
            byte[] keyBytes = Base64.decode(cipheringKey.getBytes(), Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey key = keyFactory.generatePublic(spec);
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            new String(cipher.doFinal(text.getBytes("ISO-8859-1")), "ISO-8859-1");

            return true;

        } catch (InvalidKeyException e) {
            e.printStackTrace();
            return false;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (BadPaddingException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Being used to run server reachability check.
     */
    private class serverConnectionCheckTask extends AsyncTask<String, String, String> {

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
            if (stringDbServerUrl.length() > 1) {
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
            if (result.equals("success")) {
                spinnerSuccessful(progressButtonSecurityCheck_serverConnection);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_continue));
                serverConnectionSuccessful = true;
            } else if (result.equals("failure")) {
                spinnerUnsuccessful(progressButtonSecurityCheck_serverConnection);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                serverConnectionSuccessful = false;
            }
        }
    }

    /**
     * Generates random string containing characters safe for URL requests.
     * String is shortened to 4 characters and all characters are changed to lower case characters.
     *
     * @param length length of string (has to be between 1 and 8)
     * @return shortened random string
     */
    private String generateSessionID(int length) {
        SecureRandom randomizer = new SecureRandom();
        byte bytes[] = new byte[10];
        randomizer.nextBytes(bytes);
        String base64 = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP);

        return base64.substring(0, length).toLowerCase();
    }

    /**
     * Being used to download app configuration.
     */
    private class appConfigurationTask extends AsyncTask<String, String, String> {

        /**
         * Used to download configuration data from server.
         *
         * @param urls execution parameters
         * @return success if successful, failure if unsuccessful
         */
        @Override
        protected String doInBackground(String... urls) {

            StringBuilder sbServerQuery = new StringBuilder();
            String divider = "&";

            String stringDbServerUrl = databaseSecurityCheck.getSetting("setting_serverAddress");
            String stringServerUrl = getResources().getString(R.string.server_address);
            if (stringDbServerUrl.length() > 1) {
                stringServerUrl = stringDbServerUrl;
            }
            String sessionSecurityID = generateSessionID(8);
            databaseSecurityCheck.createSetting("setting_sessionSecurityID", sessionSecurityID);

            stringServerUrl = stringServerUrl
                    .concat(databaseSecurityCheck.getSetting("setting_course"))
                    .concat("/")
                    .concat(databaseSecurityCheck.getSetting("setting_test_id"))
                    .concat(".xml")
                    .toLowerCase();

            stringServerUrl = stringServerUrl.replace(" ", "");

            sbServerQuery.append(stringServerUrl).append("?");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS", Locale.getDefault());
            Date now = new Date();
            String XMLtimestamp = formatter.format(now);
            databaseSecurityCheck.createSetting("setting_XMLtimestamp", XMLtimestamp);

            String studentNo = databaseSecurityCheck.getSetting("setting_studentNo");
            String course = databaseSecurityCheck.getSetting("setting_course");
            String testId = databaseSecurityCheck.getSetting("setting_test_id");
            String hall_row = databaseSecurityCheck.getSetting("setting_hall_row");
            String hall_seat = databaseSecurityCheck.getSetting("setting_hall_seat");
            String name = databaseSecurityCheck.getSetting("setting_name");
            String surname = databaseSecurityCheck.getSetting("setting_surname");
            String vector = databaseSecurityCheck.getSetting("setting_vector");
            String group = databaseSecurityCheck.getSetting("setting_group");
            String question_no = "";
            String answer = "";
            String sessionID = "";

            sbServerQuery.append("student_no=").append(studentNo).append(divider);
            sbServerQuery.append("course=").append(course).append(divider);
            sbServerQuery.append("test_id=").append(testId).append(divider);
            sbServerQuery.append("hall_row=").append(hall_row).append(divider);
            sbServerQuery.append("hall_seat=").append(hall_seat).append(divider);
            sbServerQuery.append("group=").append(group).append(divider);
            sbServerQuery.append("timestamp=").append(XMLtimestamp).append(divider);
            sbServerQuery.append("question_no=").append(question_no).append(divider);
            sbServerQuery.append("answer=").append(answer).append(divider);
            sbServerQuery.append("vector=").append(vector).append(divider);
            sbServerQuery.append("version=").append(getResources().getString(R.string.version_value)).append(divider);
            sbServerQuery.append("session_id=").append(sessionID).append(divider);
            sbServerQuery.append("name=").append(name).append(divider);
            sbServerQuery.append("surname=").append(surname).append(divider);
            sbServerQuery.append("session_id2=").append(sessionSecurityID);


            Log.d("Adres z xml", sbServerQuery.toString().toLowerCase());

            if (downloadedConfiguration(getApplicationContext(), sbServerQuery.toString().toLowerCase()))
                return "success";
            else
                return "failure";
        }

        /**
         * Handles result of doInBackground.
         * Checks what was the result of configuration.
         * If it was successful then notifies about it and changes buttonSecurityCheck_continue
         * text to appropriate one.
         * If it was unsuccessful then
         *
         * @param result of check
         */
        @Override
        protected void onPostExecute(String result) {
            if (result.equals("success")) {

                spinnerSuccessful(progressButtonSecurityCheck_appConfiguration);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_continue));
                appConfigurationSuccessful = true;

                if (!isConfigurationProper(minAppVersion, maxAppVersion, cipheringKey)) {
                    spinnerUnsuccessful(progressButtonSecurityCheck_appConfiguration);
                    spinnerUnsuccessful(progressButtonSecurityCheck_validation);
                    buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                    appConfigurationSuccessful = false;
                    validationSuccessful = false;
                    return;
                }

                if (isAppVersionAcceptable(minAppVersion, maxAppVersion)) {
                    spinnerSuccessful(progressButtonSecurityCheck_validation);
                    buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_continue));
                    validationSuccessful = true;
                } else {
                    spinnerUnsuccessful(progressButtonSecurityCheck_validation);
                    buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                    validationSuccessful = false;

                    String announcePattern = getResources().getString(R.string.message_invalid_app_version);
                    String announcement = String.format(announcePattern, getResources().getString(R.string.version_value), minAppVersion, maxAppVersion);

                    alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getResources().getString(R.string.label_attention))
                            .setMessage(announcement)
                            .setPositiveButton(getResources().getString(R.string.button_ok), null)
                            .show();
                }
            } else if (result.equals("failure")) {
                spinnerUnsuccessful(progressButtonSecurityCheck_appConfiguration);
                spinnerUnsuccessful(progressButtonSecurityCheck_validation);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                appConfigurationSuccessful = false;
                validationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Błąd połączenia internetowego lub niewłaściwe ID testu!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            }
        }
    }
}
