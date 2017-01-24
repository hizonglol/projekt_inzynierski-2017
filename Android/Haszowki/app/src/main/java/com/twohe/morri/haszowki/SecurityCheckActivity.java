package com.twohe.morri.haszowki;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.dd.CircularProgressButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.twohe.morri.tools.SettingsDataSource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by morri.
 * <p>
 * This file contains class Settings Activity.
 */
public class SecurityCheckActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    //ACTIVITY TAG
    private static final String TAG = "SecurityCheck";
    //SOME DATA:
    static String minAppVersion;
    static String maxAppVersion;
    static String cipheringKey;
    static String mResult;
    static String validationStatus;
    static String confirmationStatus;
    byte[] nonce;
    //LAYOUT OBJECTS:
    private CircularProgressButton progressButtonSecurityCheck_validation;
    private CircularProgressButton progressButtonSecurityCheck_serverConnection;
    private CircularProgressButton progressButtonSecurityCheck_appConfiguration;
    private Button buttonSecurityCheck_continue;
    private Button buttonSecurityCheck_abort;
    private AlertDialog alertDialog;
    //ASYNC TASKS:
    private serverConnectionCheckTask serverCheckTask;
    private appConfigurationTask configurationTask;
    private appNonceDownloadTask nonceDownloadTask;
    private appResponseUploadTask responseUploadTask;
    private appValidationTask validationTask;
    private settingsConfirmationTask confirmationTask;
    //FLAGS:
    //validation was successful.
    private boolean appValidationSuccessful;
    //server is reachable.
    private boolean serverConnectionSuccessful;
    //app configuration went successful.
    private boolean appConfigurationSuccessful;
    //nonce was downloaded successfully
    private boolean nonceDownloadSuccessful;
    //connecting to google api went successful
    private boolean googleApiSuccessful;
    //uploading response went successful
    private boolean responseUploadSuccessful;
    //confirmation settings went successful
    private boolean confirmationSuccessful;

    /* Used to access app's database */
    private SettingsDataSource databaseSecurityCheck;
    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;
    /* App shared preferences. */
    private SharedPreferences sharedPrefSecurity;
    /* Used to add custom SSL certificate. */
    private static SSLContext SSLContext;


    /**
     * @param savedInstanceState instance state of created activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_check);

        Log.d("On create", "SecurityCheckActivity");

        loadCertificate();

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

        buttonSecurityCheck_continue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(String.valueOf(serverConnectionSuccessful), String.valueOf(appConfigurationSuccessful));
                Log.d(String.valueOf(nonceDownloadSuccessful), String.valueOf(googleApiSuccessful));

                if (serverConnectionSuccessful && appConfigurationSuccessful && nonceDownloadSuccessful && googleApiSuccessful && responseUploadSuccessful) {

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
                if (serverCheckTask != null)
                    serverCheckTask.cancel(true);
                if (configurationTask != null)
                    configurationTask.cancel(true);
                if (nonceDownloadTask != null)
                    nonceDownloadTask.cancel(true);
                if (responseUploadTask != null)
                    responseUploadTask.cancel(true);
                if (validationTask != null)
                    validationTask.cancel(true);

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
     * Method loading certificate.
     *
     * @return true if something went wrong, false if everything is okay
     */
    private boolean loadCertificate() {

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            InputStream witoldInput = new BufferedInputStream(getResources().openRawResource(R.raw.witold));
            Certificate witold;

            InputStream morriInput = new BufferedInputStream(getResources().openRawResource(R.raw.morri));
            Certificate morri;

            witold = cf.generateCertificate(witoldInput);
            //System.out.println("witold=" + ((X509Certificate) witold).getSubjectDN());
            witoldInput.close();

            morri = cf.generateCertificate(morriInput);
            morriInput.close();

            // Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("witold", witold);
            keyStore.setCertificateEntry("morri", morri);

            // Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            SSLContext = SSLContext.getInstance("TLS");
            SSLContext.init(null, tmf.getTrustManagers(), null);
        } catch (CertificateException e) {
            e.printStackTrace();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return true;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return true;
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return true;
        }

        return false;
    }

    /**
     * Used to execute check tasks.
     * Restarts spinners.
     */
    private void retryChecks() {
        if (!serverConnectionSuccessful) {
            if (serverCheckTask != null)
                serverCheckTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_serverConnection);
            serverCheckTask = new serverConnectionCheckTask();
            serverCheckTask.execute();
        } else if (!nonceDownloadSuccessful) {
            if (nonceDownloadTask != null)
                nonceDownloadTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_validation);
            nonceDownloadTask = new appNonceDownloadTask();
            nonceDownloadTask.execute();
        } else if (!googleApiSuccessful) {
            restartSpinning(progressButtonSecurityCheck_validation);
            sendSafetyNetRequest();
        } else if (!responseUploadSuccessful) {
            if (responseUploadTask != null)
                responseUploadTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_validation);
            responseUploadTask = new appResponseUploadTask();
            responseUploadTask.execute();
        }/* else if (!appValidationSuccessful) {
            if (validationTask != null)
                validationTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_validation);
            validationTask = new appValidationTask();
            validationTask.execute();
        }*/ else if (!appConfigurationSuccessful) {
            if (configurationTask != null)
                configurationTask.cancel(true);
            restartSpinning(progressButtonSecurityCheck_appConfiguration);
            configurationTask = new appConfigurationTask();
            configurationTask.execute();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Error occurred during connection to Google Play Services that could not be
        // automatically resolved.
        Log.e(TAG,
                "Error connecting to Google Play Services." + connectionResult.getErrorMessage());
        spinnerUnsuccessful(progressButtonSecurityCheck_validation);
        buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
        googleApiSuccessful = false;
    }

    /**
     * USE IT FOR SAFETYNET
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(SafetyNet.API)
                .enableAutoManage(this, this)
                .addOnConnectionFailedListener(this)
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

    private void spinnerIndeterminate(CircularProgressButton button) {
        button.setIndeterminateProgressMode(true);
        button.setProgress(0);
    }

    // VALIDATION STUFF

    /**
     * Being used to run server reachability check.
     */
    private class serverConnectionCheckTask extends AsyncTask<String, String, String> {

        /**
         * Checks if server with given serverAddress is reachable
         *
         * @param context       of application
         * @param serverAddress address of checked server
         * @return true if server is reachable, false if not
         */
        private boolean isReachable(Context context, String serverAddress) {
            // First, check we have any sort of connectivity
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            boolean reachable = false;

            if (netInfo != null && netInfo.isConnected()) {
                // Some sort of connection is open, check if server is reachable
                try {
                    URL url = new URL(serverAddress);
                    HttpsURLConnection urlc = (HttpsURLConnection) url.openConnection();
                    urlc.setSSLSocketFactory(SSLContext.getSocketFactory());
                    urlc.setRequestProperty("User-Agent", "Android Application");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(5 * 1000); //5 sek
                    urlc.connect();
                    reachable = (urlc.getResponseCode() == 200);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return reachable;
        }

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
                serverConnectionSuccessful = true;

                nonceDownloadTask = new appNonceDownloadTask();
                nonceDownloadTask.execute();
            } else if (result.equals("failure")) {
                spinnerUnsuccessful(progressButtonSecurityCheck_serverConnection);
                spinnerIndeterminate(progressButtonSecurityCheck_validation);
                spinnerIndeterminate(progressButtonSecurityCheck_appConfiguration);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                serverConnectionSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Serwer jest nieosiągalny! Sprawdź połączenie internetowe oraz adres serwera!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            }
        }
    }

    private class appNonceDownloadTask extends AsyncTask<String, String, String> {

        /**
         * Downloads configuration file from server
         *
         * @param context               of application
         * @param serverVerifierAddress address of verifier
         * @return true if downloaded configuration, false if not
         */
        private boolean nonceDownloaded(Context context, String serverVerifierAddress) {
            // First, check we have any sort of connectivity
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            boolean downloaded = false;

            if (netInfo != null && netInfo.isConnected()) {
                // Some sort of connection is open, check if server is reachable
                try {
                    URL url = new URL(serverVerifierAddress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setSSLSocketFactory(SSLContext.getSocketFactory());

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
                                if (name.equals("nonce")) {
                                    String stringNonce = configurationParser.getAttributeValue(null, "bytes");

                                    nonce = Base64.decode(stringNonce.getBytes(), Base64.DEFAULT);
                                }
                                break;
                        }
                        event = configurationParser.next();
                    }

                    downloaded = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return downloaded;
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
            String sessionID = generateSessionID(6);
            databaseSecurityCheck.createSetting("setting_sessionSecurityID", sessionSecurityID);
            databaseSecurityCheck.createSetting("setting_sessionID", sessionID);

            stringServerUrl = stringServerUrl
                    .concat("fcgi-bin")
                    .concat("/")
                    .concat("hasz_serwer")
                    .concat("/")
                    .concat("verify.fcgi").toLowerCase();

            stringServerUrl = stringServerUrl.replace(" ", "");

            sbServerQuery.append(stringServerUrl).append("?");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS", Locale.getDefault());
            Date now = new Date();
            String timestamp = formatter.format(now);

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

            sbServerQuery.append("student_no=").append(studentNo).append(divider);
            sbServerQuery.append("course=").append(course).append(divider);
            sbServerQuery.append("test_id=").append(testId).append(divider);
            sbServerQuery.append("hall_row=").append(hall_row).append(divider);
            sbServerQuery.append("hall_seat=").append(hall_seat).append(divider);
            sbServerQuery.append("group=").append(group).append(divider);
            sbServerQuery.append("timestamp=").append(timestamp).append(divider);
            sbServerQuery.append("question_no=").append(question_no).append(divider);
            sbServerQuery.append("answer=").append(answer).append(divider);
            sbServerQuery.append("vector=").append(vector).append(divider);
            sbServerQuery.append("version=").append(getResources().getString(R.string.version_value)).append(divider);
            sbServerQuery.append("session_id=").append(sessionID).append(divider);
            sbServerQuery.append("name=").append(name).append(divider);
            sbServerQuery.append("surname=").append(surname).append(divider);
            sbServerQuery.append("session_id2=").append(sessionSecurityID);

            if (nonceDownloaded(getApplicationContext(), sbServerQuery.toString()))
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

                nonceDownloadSuccessful = true;
                buildGoogleApiClient();
                sendSafetyNetRequest();

            } else if (result.equals("failure")) {

                spinnerUnsuccessful(progressButtonSecurityCheck_validation);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                nonceDownloadSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Anons nie został pobrany z serwera! Spróbuj ponownie!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            }
        }
    }

    private void sendSafetyNetRequest() {
        Log.i(TAG, "Sending SafetyNet API request.");

         /*
        Create a nonce for this request.
        The nonce is returned as part of the response from the
        SafetyNet API. Here we append the string to a number of random bytes to ensure it larger
        than the minimum 16 bytes required.
        Read out this value and verify it against the original request to ensure the
        response is correct and genuine.
        NOTE: A nonce must only be used once and a different nonce should be used for each request.
        As a more secure option, you can obtain a nonce from your own server using a secure
        connection. Here in this sample, we generate a String and append random bytes, which is not
        very secure. Follow the tips on the Security Tips page for more information:
        https://developer.android.com/training/articles/security-tips.html#Crypto
         */
        // TODO(developer): Change the nonce generation to include your own, used once value,
        // ideally from your remote server.

        // Call the SafetyNet API asynchronously. The result is returned through the result callback.
        SafetyNet.SafetyNetApi.attest(mGoogleApiClient, nonce)
                .setResultCallback(new ResultCallback<SafetyNetApi.AttestationResult>() {

                    @Override
                    public void onResult(SafetyNetApi.AttestationResult result) {
                        Status status = result.getStatus();
                        if (status.isSuccess()) {
                            /*
                             Successfully communicated with SafetyNet API.
                             Use result.getJwsResult() to get the signed result data. See the server
                             component of this sample for details on how to verify and parse this
                             result.
                             */
                            mResult = result.getJwsResult();
                            Log.d(TAG, "Success! SafetyNet result:\n" + mResult + "\n");

                            /*
                             TODO: Change it after validation is written
                             */
                            googleApiSuccessful = true;
                            responseUploadTask = new appResponseUploadTask();
                            responseUploadTask.execute();


                        } else {
                            // An error occurred while communicating with the service.
                            Log.d(TAG, "ERROR! " + status.getStatusCode() + " " + status
                                    .getStatusMessage());
                            mResult = null;
                            spinnerUnsuccessful(progressButtonSecurityCheck_validation);
                            buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                            googleApiSuccessful = false;
                        }
                    }
                });
    }

    private class appResponseUploadTask extends AsyncTask<String, String, String> {

        private boolean responseUploaded(Context context, String serverDocumentAddress, String postData) {
            // First, check we have any sort of connectivity
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            boolean uploaded = false;

            if (netInfo != null && netInfo.isConnected()) {

                try {
                    byte[] postDataBytes = postData.getBytes("UTF-8");
                    URL url = new URL(serverDocumentAddress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setSSLSocketFactory(SSLContext.getSocketFactory());
                    conn.setInstanceFollowRedirects(false);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "text/plain");
                    conn.setRequestProperty("charset", "utf-8");
                    conn.setRequestProperty("Content-Length", Integer.toString(postDataBytes.length));
                    conn.setUseCaches(false);
                    conn.setReadTimeout(15000);
                    conn.setConnectTimeout(15000);
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(postData);

                    writer.flush();
                    writer.close();
                    os.close();
                    int responseCode = conn.getResponseCode();

                    //DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
                    //wr.write(postDataBytes);

                    Log.d("Upload status", String.valueOf(responseCode));

                    uploaded = true;
                } catch (IOException e) {
                    e.printStackTrace();
                    uploaded = false;
                }
            }

            return uploaded;
        }

        /**
         * Used to download configuration data from server.
         *
         * @param params execution parameters
         * @return success if successful, failure if unsuccessful
         */
        @Override
        protected String doInBackground(String... params) {

            StringBuilder sbServerQuery = new StringBuilder();
            String divider = "&";

            String stringDbServerUrl = databaseSecurityCheck.getSetting("setting_serverAddress");
            String stringServerUrl = getResources().getString(R.string.server_address);
            if (stringDbServerUrl.length() > 1) {
                stringServerUrl = stringDbServerUrl;
            }

            stringServerUrl = stringServerUrl
                    .concat("fcgi-bin")
                    .concat("/")
                    .concat("hasz_serwer")
                    .concat("/")
                    .concat("verify.fcgi").toLowerCase();

            stringServerUrl = stringServerUrl.replace(" ", "");

            sbServerQuery.append(stringServerUrl).append("?");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS", Locale.getDefault());
            Date now = new Date();
            String timestamp = formatter.format(now);

            String studentNo = databaseSecurityCheck.getSetting("setting_studentNo");
            String course = databaseSecurityCheck.getSetting("setting_course");
            String testId = databaseSecurityCheck.getSetting("setting_test_id");
            String hall_row = databaseSecurityCheck.getSetting("setting_hall_row");
            String hall_seat = databaseSecurityCheck.getSetting("setting_hall_seat");
            String name = databaseSecurityCheck.getSetting("setting_name");
            String surname = databaseSecurityCheck.getSetting("setting_surname");
            String vector = databaseSecurityCheck.getSetting("setting_vector");
            String group = databaseSecurityCheck.getSetting("setting_group");
            String sessionSecurityID = databaseSecurityCheck.getSetting("setting_sessionSecurityID");
            String sessionID = databaseSecurityCheck.getSetting("setting_sessionID");
            String question_no = "";
            String answer = "";
            String stringNonce = Base64.encodeToString(nonce, Base64.DEFAULT);

            sbServerQuery.append("student_no=").append(studentNo).append(divider);
            sbServerQuery.append("course=").append(course).append(divider);
            sbServerQuery.append("test_id=").append(testId).append(divider);
            sbServerQuery.append("hall_row=").append(hall_row).append(divider);
            sbServerQuery.append("hall_seat=").append(hall_seat).append(divider);
            sbServerQuery.append("group=").append(group).append(divider);
            sbServerQuery.append("timestamp=").append(timestamp).append(divider);
            sbServerQuery.append("question_no=").append(question_no).append(divider);
            sbServerQuery.append("answer=").append(answer).append(divider);
            sbServerQuery.append("vector=").append(vector).append(divider);
            sbServerQuery.append("version=").append(getResources().getString(R.string.version_value)).append(divider);
            sbServerQuery.append("session_id=").append(sessionID).append(divider);
            sbServerQuery.append("name=").append(name).append(divider);
            sbServerQuery.append("surname=").append(surname).append(divider);
            sbServerQuery.append("session_id2=").append(sessionSecurityID).append(divider);
            sbServerQuery.append("nonce=").append(stringNonce);

            if (responseUploaded(getApplicationContext(), sbServerQuery.toString(), mResult))
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

                spinnerSuccessful(progressButtonSecurityCheck_validation);
                responseUploadSuccessful = true;
                configurationTask = new appConfigurationTask();
                configurationTask.execute();

            } else if (result.equals("failure")) {

                spinnerUnsuccessful(progressButtonSecurityCheck_validation);
                responseUploadSuccessful = false;
            }
        }
    }

    //TODO ten fragment jeszcze nie działa
    private class appValidationTask extends AsyncTask<String, String, String> {

        private int appValidated(Context context, String serverDocumentAddress) {
            // First, check we have any sort of connectivity
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            int validated = -1;

            if (netInfo != null && netInfo.isConnected()) {
                // Some sort of connection is open, check if server is reachable
                try {
                    URL url = new URL(serverDocumentAddress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setSSLSocketFactory(SSLContext.getSocketFactory());

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
                                if (name.equals("validation")) {
                                    validationStatus = configurationParser.getAttributeValue(null, "status");
                                }
                                break;
                        }
                        event = configurationParser.next();
                    }

                    if (validationStatus.equals("success")) validated = 0;
                    else if(validationStatus.equals("processed")) validated = 1;
                    else if(validationStatus.equals("attestation_missing")) validated = 2;
                } catch (Exception e) {
                    e.printStackTrace();
                    validated = -2;
                }
            }

            return validated;
        }

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

            stringServerUrl = stringServerUrl
                    .concat("fcgi-bin")
                    .concat("/")
                    .concat("hasz_serwer")
                    .concat("/")
                    .concat("verify.fcgi").toLowerCase();

            stringServerUrl = stringServerUrl.replace(" ", "");

            sbServerQuery.append(stringServerUrl).append("?");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS", Locale.getDefault());
            Date now = new Date();
            String timestamp = formatter.format(now);

            String studentNo = databaseSecurityCheck.getSetting("setting_studentNo");
            String course = databaseSecurityCheck.getSetting("setting_course");
            String testId = databaseSecurityCheck.getSetting("setting_test_id");
            String hall_row = databaseSecurityCheck.getSetting("setting_hall_row");
            String hall_seat = databaseSecurityCheck.getSetting("setting_hall_seat");
            String name = databaseSecurityCheck.getSetting("setting_name");
            String surname = databaseSecurityCheck.getSetting("setting_surname");
            String vector = databaseSecurityCheck.getSetting("setting_vector");
            String group = databaseSecurityCheck.getSetting("setting_group");
            String sessionSecurityID = databaseSecurityCheck.getSetting("setting_sessionSecurityID");
            String sessionID = databaseSecurityCheck.getSetting("setting_sessionID");
            String question_no = "";
            String answer = "";

            sbServerQuery.append("student_no=").append(studentNo).append(divider);
            sbServerQuery.append("course=").append(course).append(divider);
            sbServerQuery.append("test_id=").append(testId).append(divider);
            sbServerQuery.append("hall_row=").append(hall_row).append(divider);
            sbServerQuery.append("hall_seat=").append(hall_seat).append(divider);
            sbServerQuery.append("group=").append(group).append(divider);
            sbServerQuery.append("timestamp=").append(timestamp).append(divider);
            sbServerQuery.append("question_no=").append(question_no).append(divider);
            sbServerQuery.append("answer=").append(answer).append(divider);
            sbServerQuery.append("vector=").append(vector).append(divider);
            sbServerQuery.append("version=").append(getResources().getString(R.string.version_value)).append(divider);
            sbServerQuery.append("session_id=").append(sessionID).append(divider);
            sbServerQuery.append("name=").append(name).append(divider);
            sbServerQuery.append("surname=").append(surname).append(divider);
            sbServerQuery.append("session_id2=").append(sessionSecurityID);

            int status = appValidated(getApplicationContext(), sbServerQuery.toString());

            if (status == 0)
                return "success";
            else if (status == -1)
                return "failure";
            else if (status == -2)
                return "connection_problem";
            else if (status == 1)
                return "processing";
            else if (status == 2)
                return "attestation_missing";
            else return "unknown_problem";
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

                appValidationSuccessful = true;
                configurationTask = new appConfigurationTask();
                configurationTask.execute();

            } else if (result.equals("failure")) {

                appValidationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Twoja aplikacja nie przeszła walidacji!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            } else if (result.equals("processed")) {

                appValidationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Twoja aplikacja w trakcie walidacji! Spróbuj ponownie w celu potwierdzenia statusu!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            } else if (result.equals("attestation_missing")) {

                responseUploadSuccessful = false;
                appValidationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Informacje o weryfikacji nie dotarły na serwer! Spróbuj ponownie!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            } else if (result.equals("connection_problem")) {

                responseUploadSuccessful = false;
                appValidationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Problem z połączeniem z serwerem! Spróbuj ponownie!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            } else if (result.equals("unknown_problem")) {

                responseUploadSuccessful = false;
                appValidationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Nieznany błąd! Zakończ aplikację i pisz na kartce!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            }

        }
    }

    /**
     * Being used to download app configuration.
     */
    private class appConfigurationTask extends AsyncTask<String, String, String> {

        /**
         * Downloads configuration file from server
         *
         * @param context               of application
         * @param serverDocumentAddress address of configuration file
         * @return true if downloaded configuration, false if not
         */
        private boolean configurationDownloaded(Context context, String serverDocumentAddress) {
            // First, check we have any sort of connectivity
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            boolean downloadedConfiguration = false;

            if (netInfo != null && netInfo.isConnected()) {
                // Some sort of connection is open, check if server is reachable
                try {
                    URL url = new URL(serverDocumentAddress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setSSLSocketFactory(SSLContext.getSocketFactory());

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
            String sessionSecurityID = databaseSecurityCheck.getSetting("setting_sessionSecurityID");
            String sessionID = databaseSecurityCheck.getSetting("setting_sessionID");
            String question_no = "";
            String answer = "";

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

            if (configurationDownloaded(getApplicationContext(), sbServerQuery.toString()))
                return "success";
            else
                return "failure";
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

                if (!isConfigurationProper(minAppVersion, maxAppVersion, cipheringKey)) {
                    spinnerUnsuccessful(progressButtonSecurityCheck_appConfiguration);
                    buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                    appConfigurationSuccessful = false;
                    return;
                }

                if (!isAppVersionAcceptable(minAppVersion, maxAppVersion)) {
                    spinnerUnsuccessful(progressButtonSecurityCheck_appConfiguration);
                    buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                    appConfigurationSuccessful = false;

                    String announcePattern = getResources().getString(R.string.message_invalid_app_version);
                    String announcement = String.format(announcePattern, getResources().getString(R.string.version_value), minAppVersion, maxAppVersion);

                    alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(getResources().getString(R.string.label_attention))
                            .setMessage(announcement)
                            .setPositiveButton(getResources().getString(R.string.button_ok), null)
                            .show();

                    return;
                }

                spinnerSuccessful(progressButtonSecurityCheck_appConfiguration);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_continue));
                appConfigurationSuccessful = true;

            } else if (result.equals("failure")) {
                spinnerUnsuccessful(progressButtonSecurityCheck_appConfiguration);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                appConfigurationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Błąd połączenia internetowego lub niewłaściwe ID testu!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            }
        }
    }

    //TODO ten fragment jeszcze nie działa
    private class settingsConfirmationTask extends AsyncTask<String, String, String> {

        private boolean settingsConfirmed(Context context, String serverDocumentAddress) {
            // First, check we have any sort of connectivity
            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            boolean confirmed = false;

            if (netInfo != null && netInfo.isConnected()) {
                // Some sort of connection is open, check if server is reachable
                try {
                    URL url = new URL(serverDocumentAddress);
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setSSLSocketFactory(SSLContext.getSocketFactory());

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
                                if (name.equals("validation")) {
                                    confirmationStatus = configurationParser.getAttributeValue(null, "status");
                                }
                                break;
                        }
                        event = configurationParser.next();
                    }

                    if (confirmationStatus.equals("Confirmed")) confirmed = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return confirmed;
        }

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

            stringServerUrl = stringServerUrl
                    .concat("fcgi-bin")
                    .concat("/")
                    .concat("hasz_serwer")
                    .concat("/")
                    .concat("verify.fcgi").toLowerCase();

            stringServerUrl = stringServerUrl.replace(" ", "");

            sbServerQuery.append(stringServerUrl).append("?");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS", Locale.getDefault());
            Date now = new Date();
            String timestamp = formatter.format(now);

            String studentNo = databaseSecurityCheck.getSetting("setting_studentNo");
            String course = databaseSecurityCheck.getSetting("setting_course");
            String testId = databaseSecurityCheck.getSetting("setting_test_id");
            String hall_row = databaseSecurityCheck.getSetting("setting_hall_row");
            String hall_seat = databaseSecurityCheck.getSetting("setting_hall_seat");
            String name = databaseSecurityCheck.getSetting("setting_name");
            String surname = databaseSecurityCheck.getSetting("setting_surname");
            String vector = databaseSecurityCheck.getSetting("setting_vector");
            String group = databaseSecurityCheck.getSetting("setting_group");
            String sessionSecurityID = databaseSecurityCheck.getSetting("setting_sessionSecurityID");
            String sessionID = databaseSecurityCheck.getSetting("setting_sessionID");
            String question_no = "";
            String answer = "";

            sbServerQuery.append("student_no=").append(studentNo).append(divider);
            sbServerQuery.append("course=").append(course).append(divider);
            sbServerQuery.append("test_id=").append(testId).append(divider);
            sbServerQuery.append("hall_row=").append(hall_row).append(divider);
            sbServerQuery.append("hall_seat=").append(hall_seat).append(divider);
            sbServerQuery.append("group=").append(group).append(divider);
            sbServerQuery.append("timestamp=").append(timestamp).append(divider);
            sbServerQuery.append("question_no=").append(question_no).append(divider);
            sbServerQuery.append("answer=").append(answer).append(divider);
            sbServerQuery.append("vector=").append(vector).append(divider);
            sbServerQuery.append("version=").append(getResources().getString(R.string.version_value)).append(divider);
            sbServerQuery.append("session_id=").append(sessionID).append(divider);
            sbServerQuery.append("name=").append(name).append(divider);
            sbServerQuery.append("surname=").append(surname).append(divider);
            sbServerQuery.append("session_id2=").append(sessionSecurityID);

            if (settingsConfirmed(getApplicationContext(), sbServerQuery.toString()))
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
                confirmationSuccessful = true;
            } else if (result.equals("failure")) {

                spinnerUnsuccessful(progressButtonSecurityCheck_appConfiguration);
                buttonSecurityCheck_continue.setText(getResources().getString(R.string.button_try_again));
                confirmationSuccessful = false;

                alertDialog = new AlertDialog.Builder(SecurityCheckActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(getResources().getString(R.string.label_attention))
                        .setMessage("Podane miejsce zostało juz zajęte! Zajmij inne miejsce!")
                        .setPositiveButton(getResources().getString(R.string.button_ok), null)
                        .show();
            }
        }
    }

}
