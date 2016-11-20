package com.twohe.morri.haszowki;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.twohe.morri.tools.SettingsDataSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


/**
 * Created by TwoHe on 10.07.2016.
 * <p>
 * This file contains class Tabs Activity.
 */
@SuppressWarnings("FieldCanBeLocal")
public class TabsActivity extends AppCompatActivity {

    private static String cryptoPass;
    threadKiller threadKillerTabs;
    int guestionsAmount = 1;
    int tabs_fileYesAnswers = 0;
    int tabs_fileNoAnswers = 0;
    int tabs_fileDunnoAnswers = 0;
    int tabs_serverYesAnswers = 0;
    int tabs_serverNoAnswers = 0;
    int tabs_serverDunnoAnswers = 0;
    //variables and classes
    private boolean wasInBackgroundTabs = false;
    private SharedPreferences sharedPrefTabs;
    private SectionsPagerAdapter SectionsPagerAdapterTabs;
    private ViewPager ViewPagerTabs;
    private AlertDialog alertDialog;
    private File fileTabs_toWrite;

    /**
     * Creates layout.
     * Initializes variables.
     * Launches proper methods to instantiate activity.
     *
     * @param savedInstanceState instance state of created activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("On create", "TabsActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);

        sharedPrefTabs = PreferenceManager.getDefaultSharedPreferences(this);
        cryptoPass = sharedPrefTabs.getString("Key", "");

        threadKillerTabs = new threadKiller();

        MemoryBoss mMemoryBoss;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mMemoryBoss = new MemoryBoss();
            registerComponentCallbacks(mMemoryBoss);
        }

        if (createTestFile()) endTestWhenTestFileCreationFailure();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapterTabs = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPagerTabs = (ViewPager) findViewById(R.id.container);
        if (ViewPagerTabs != null)
            ViewPagerTabs.setAdapter(SectionsPagerAdapterTabs);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null)
            tabLayout.setupWithViewPager(ViewPagerTabs);

        addTab();

        /*
        To cos pozwala na unikniecie bledu gdy polaczenie przychodzace na androidzie Lollipop
        zostanie odrzucone przez broadcast receiver. Odpowiedzi do serwera nie dochodza,
        przyciski sa zakolorowane na szaro a aplikacja zachowuje sie tak jakby
        nie miala przyznanych uprawnien do internetu, a socket bylby zajety.
         */
        enableIncomingCallReceiver();
    }

    /**
     * Method used to terminate Tabs Activity in case of failure with
     * test file creation.
     */
    private void endTestWhenTestFileCreationFailure() {
        alertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Błąd")
                .setMessage("Plik testu nie mógł być poprawnie utworzony.")
                .setCancelable(false)
                .show();

        threadKillerTabs.start();
    }

    /**
     * Used to enable IncomingCallReceiver that rejects any incoming calls
     */
    private void enableIncomingCallReceiver() {


        SharedPreferences.Editor editor = sharedPrefTabs.edit();
        editor.putBoolean("Rejecting enabled", true);
        editor.apply();
    }

    /**
     * Used to disable IncomingCallReceiver that rejects any incoming calls
     */
    private void disableIncomingCallReceiver() {

        SharedPreferences.Editor editor = sharedPrefTabs.edit();
        editor.putBoolean("Rejecting enabled", false);
        editor.apply();
    }

    /**
     * Called when activity is being resumed.
     * <p>
     * Enables incoming call receiver.
     * Checks if test should be ended by not authorised app usage.
     */
    @Override
    protected void onResume() {
        super.onResume();

        enableIncomingCallReceiver();

        Boolean endTest = sharedPrefTabs.getBoolean("End test", false);

        if (endTest)
            TabsActivity.this.finish();
        else if (wasInBackgroundTabs) {

            if (sharedPrefTabs.getBoolean("Call handled", false) || sharedPrefTabs.getBoolean("Call handle changed", false)) {
                wasInBackgroundTabs = false;

                SharedPreferences.Editor editor = sharedPrefTabs.edit();
                editor.putBoolean("Call handled", false);
                editor.putBoolean("Call handle changed", false);
                editor.apply();
                return;
            }

            alertDialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getResources().getString(R.string.message_you_quit_test))
                    .setMessage(getResources().getString(R.string.message_test_ended))
                    .setCancelable(false)
                    .show();

            threadKillerTabs.start();
        }

    }

    /**
     * Called when user presses back button.
     * Asks him if he really wants to quit test.
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.message_i_quit_test))
                .setMessage(getResources().getString(R.string.message_do_you_want_to_quit_test))
                .setPositiveButton(getResources().getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton(getResources().getString(R.string.button_no), null)
                .show();
    }

    /**
     * Called when Tabs Activity is being destroyed.
     * Disables Incoming Call Receiver.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        disableIncomingCallReceiver();
    }

    /**
     * Adds fragment to fragment adapter.
     * Fragment represents one tab with one question on test.
     */
    public void addTab() {
        SectionsPagerAdapterTabs.addFragment();
    }

    /**
     * Generates random string containing characters safe for URL requests.
     * String is shortened to 4 characters and all characters are changed to lower case characters.
     *
     * @return shortened random string
     */
    private String generateSessionID() {
        SecureRandom randomizer = new SecureRandom();
        byte bytes[] = new byte[6];
        randomizer.nextBytes(bytes);
        String base64 = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP);

        String shortenedBase64 = base64.substring(0, 4).toLowerCase();

        return shortenedBase64;
    }

    /**
     * Creates test file with a name in format: YYYY_MM_DD-HH_MM_SS-TTTT.txt where:
     * Y - year
     * M - month
     * D - day
     * H - hour
     * M - minute
     * S - second
     * T - token
     * Heading of file contains:
     * name_of_course test_id session_id app_version time_stamp
     * <p>
     * Before creating file, checks if directory for test files exists. If not then creates it.
     *
     * @return true if something went bad with creating test file, false if everything was okay
     */
    private boolean createTestFile() {

        SettingsDataSource databaseTabsTestFile = new SettingsDataSource(this);
        databaseTabsTestFile.open();

        String sessionID = generateSessionID();
        databaseTabsTestFile.createSetting("setting_sessionID", sessionID);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.getDefault());
        Date now = new Date();
        String fileName = formatter.format(now) + "-" + sessionID + ".txt";//like 2016_01_12-12_30_00-ABCdef12.txt

        try {
            File root = new File(Environment.getExternalStorageDirectory() + "/Haszowki");
            if (!root.exists()) {
                root.mkdir();
            }
            fileTabs_toWrite = new File(root, fileName);

            fileTabs_toWrite.setReadable(true);
            fileTabs_toWrite.setWritable(true);

            MediaScannerConnection.scanFile(this, new String[]{fileTabs_toWrite.getAbsolutePath()}, null, null);

            FileWriter writer = new FileWriter(fileTabs_toWrite, true);
            //naglowek pliku testu to: nazwa przedmiotu, kod testu, kod sesji, wersja aplikacji, stempel czasowy
            writer.append(databaseTabsTestFile.getSetting("setting_course"));
            writer.append("\t");
            writer.append(databaseTabsTestFile.getSetting("setting_test_id"));
            writer.append("\t");
            writer.append(sessionID);
            writer.append("\t");
            writer.append(getResources().getString(R.string.version_value));
            writer.append("\t");
            writer.append(formatter.format(now));
            writer.append("\n====\n");
            writer.flush();
            writer.close();
            MediaScannerConnection.scanFile(this, new String[]{fileTabs_toWrite.getAbsolutePath()}, null, null);
            Toast.makeText(this, getResources().getString(R.string.message_your_test_file_was_created), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            finish();
            return true;
        }

        databaseTabsTestFile.close();

        return false;
    }

    /**
     * Method that ciphers given text and appends it to test file.
     * Informs about the success of operation by returning proper bool value.
     *
     * @param text String that will be ciphered and appended to test file
     * @return false if IO operation failed, true if everything went fine
     */
    public boolean appendToFileCiphered(String text) {

        text = encryptItRSA(text);

        if (text.equals("")) return false;

        try {
            scanFile(fileTabs_toWrite.getAbsolutePath());
            FileWriter writer = new FileWriter(fileTabs_toWrite, true);
            writer.append(text);
            writer.append("\n");
            writer.append("====");
            writer.append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Scanning method used to scan file after text appending
     * so it's content is visible after connecting to Windows PC.
     *
     * @param path of file to be scanned
     */
    private void scanFile(String path) {

        MediaScannerConnection.scanFile(TabsActivity.this,
                new String[]{path}, null,
                new MediaScannerConnection.OnScanCompletedListener() {

                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("TAG", "Finished scanning " + path);
                    }
                });
    }

    /**
     * Ciphers given text using RSA algorythm and password stored in
     * keyString variable.
     *
     * @param text encrypted string containing test answer
     * @return ciphered text if everything fine, "" if something went wrong
     */
    private String encryptItRSA(String text) {

        String cipheredText;

        try {
            byte[] keyBytes = Base64.decode(cryptoPass.getBytes(), Base64.DEFAULT);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            PublicKey key = keyFactory.generatePublic(spec);
            Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            cipheredText = new String(cipher.doFinal(text.getBytes("ISO-8859-1")), "ISO-8859-1");

        } catch (InvalidKeyException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        } catch (BadPaddingException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
            endTestEmergency();
            return "";
        }

        return cipheredText;
    }

    /**
     * Used to end test in emergency situations.
     * Shows to user an alert that something went wrong and launches terminator.
     */
    private void endTestEmergency() {
        alertDialog = new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getResources().getString(R.string.label_attention))
                .setMessage(getResources().getString(R.string.message_something_went_wrong_end_test))
                .setCancelable(false)
                .show();

        threadKillerTabs.start();
    }

    /**
     * Prepares bundle for Summary Activity.
     * Starts Summary Activity after bundle has been prepared.
     */
    protected void summariseTest() {

        Intent intentSummary = new Intent(getApplicationContext(), SummaryActivity.class);
        Bundle b = new Bundle();
        b.putInt("tabs_fileYesAnswers", tabs_fileYesAnswers);
        b.putInt("tabs_fileNoAnswers", tabs_fileNoAnswers);
        b.putInt("tabs_fileDunnoAnswers", tabs_fileDunnoAnswers);
        b.putInt("tabs_serverYesAnswers", tabs_serverYesAnswers);
        b.putInt("tabs_serverNoAnswers", tabs_serverNoAnswers);
        b.putInt("tabs_serverDunnoAnswers", tabs_serverDunnoAnswers);
        intentSummary.putExtras(b);
        startActivity(intentSummary);
    }

    /**
     * A placeholder fragment containing one question tab.
     */
    public static class QuestionFragment extends Fragment {

        //variables and classes
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_SECTION_ANSWER_FILE = "section_answer_file";
        private static final String ARG_SECTION_ANSWER_SERVER = "section_answer_server";
        private static final String DEBUG_TAG = "HttpExample";
        private String serverResponse = "";
        private Integer serverResponseCode = 404;
        //used to check whether any answer has been choosen or not
        private boolean anyAnswerSent;

        /**
         * Instantiates fragment. Creates bundle with arguments that carry fragment
         * number, current choosen answer for file and current choosen answer
         * that has been sent successfully to server.
         * <p>
         * ARG_SECTION_ANSWER_FILE and ARG_SECTION_ANSWER_SERVER parameters explanation:
         * 0 - no answer
         * 1 - yes answer
         * 2 - no answer
         * 3 - dunno answer
         *
         * @param sectionNumber Number of corresponding tab
         * @return New instance of fragment to the given section number
         */
        public static QuestionFragment newInstance(int sectionNumber) {
            QuestionFragment fragment = new QuestionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putInt(ARG_SECTION_ANSWER_FILE, 0);
            args.putInt(ARG_SECTION_ANSWER_SERVER, 0);
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * Initializes corresponding tab. Sets up button handlers.
         *
         * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment
         * @param container          If non-null, this is the parent view that the fragment's UI should be attached to. The fragment should not add the view itself, but this can be used to generate the LayoutParams of the view.
         * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
         * @return Return the View for the fragment's UI, or null.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_tabs, container, false);

            initialize(rootView);

            rootView.findViewById(R.id.button_summary).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ((TabsActivity) getActivity()).summariseTest();
                    if ((getArguments().getInt(ARG_SECTION_NUMBER) % 16) == 0 && !anyAnswerSent) {
                        String timestamp = makeTimeStamp();
                        if (saveToFile(rootView, 0, "no_answer", timestamp)) return;
                        sendToServer(rootView, 0, "no_answer", timestamp);
                        anyAnswerSent = true;
                    }
                }
            });

            rootView.findViewById(R.id.button_yes).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String timestamp = makeTimeStamp();
                    if (saveToFile(rootView, 1, "yes", timestamp)) return;
                    sendToServer(rootView, 1, "yes", timestamp);
                    anyAnswerSent = true;
                }
            });

            rootView.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String timestamp = makeTimeStamp();
                    if (saveToFile(rootView, 2, "no", timestamp)) return;
                    sendToServer(rootView, 2, "no", timestamp);
                    anyAnswerSent = true;
                }
            });

            rootView.findViewById(R.id.button_dunno).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String timestamp = makeTimeStamp();
                    if (saveToFile(rootView, 3, "dunno", timestamp)) return;
                    sendToServer(rootView, 3, "dunno", timestamp);
                    anyAnswerSent = true;
                }
            });

            rootView.findViewById(R.id.button_add_question).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    ((TabsActivity) getActivity()).addTab();
                    int nextTab = ((TabsActivity) getActivity()).guestionsAmount++;
                    if (getArguments().getInt(ARG_SECTION_ANSWER_FILE) == 0 && !anyAnswerSent) {
                        String timestamp = makeTimeStamp();
                        if (saveToFile(rootView, 0, "no_answer", timestamp)) return;
                        sendToServer(rootView, 0, "no_answer", timestamp);
                        anyAnswerSent = true;
                    }

                    ((TabsActivity) getActivity()).ViewPagerTabs.setCurrentItem(nextTab);
                }
            });

            return rootView;
        }

        /**
         * Checks tab arguments and according to them sets proper color of answer.
         * <p>
         * It also checks if answerServer is not equal to 0. If it is then answer has been
         * successfully sent to server and last answer in the file is the same as last answer
         * in server, and because of that button can be coloured to blue.
         */
        @Override
        public void onResume() {
            super.onResume();

            View rootView = getView();
            int answerFile = getArguments().getInt(ARG_SECTION_ANSWER_FILE);
            int answerServer = getArguments().getInt(ARG_SECTION_ANSWER_SERVER);

            if (answerServer != 0)
                setGivenAnswerBlue(rootView, answerServer);
            else
                setGivenAnswerGray(rootView, answerFile);
        }

        /**
         * Initializes viewGroup and viewSessionID with data stored in app's database.
         * Initializes viewQuestionNumber with number of corresponding tab.
         *
         * @param rootView Root view of corresponding tab
         */
        private void initialize(View rootView) {
            SettingsDataSource database = new SettingsDataSource(getActivity());

            database.open();

            TextView viewGroup = (TextView) rootView.findViewById(R.id.view_group_number);
            TextView viewQuestionNumber = (TextView) rootView.findViewById(R.id.view_question_number);
            TextView viewSessionID = (TextView) rootView.findViewById(R.id.view_token);

            if (viewGroup != null)
                viewGroup.setText(database.getSetting("setting_group"));

            if (viewQuestionNumber != null)
                viewQuestionNumber.setText(String.valueOf(getArguments().getInt(ARG_SECTION_NUMBER)));

            if (viewSessionID != null)
                viewSessionID.setText(database.getSetting("setting_sessionID"));

            database.close();
        }

        /**
         * Used to save an answer of corresponding tab into test file.
         * <p>
         * It also sets up ARG_SECTION_ANSWER_SERVER to 0 before sendToServer() is terminated.
         * If GET in sendToServer() will be terminated successfully then ARG_SECTION_ANSWER_SERVER
         * will be set to proper number.
         *
         * @param rootView Root view of corresponding tab
         * @param question Number of corresponding tab
         * @param answerNo Type of given answer
         * @return false if everything went well, true if something bad happened
         */
        private boolean saveToFile(View rootView, int question, String answerNo, String timestamp) {

            if (((TabsActivity) getActivity()).appendToFileCiphered(createDataURL("to_file", answerNo, timestamp))) {
                setGivenAnswerGray(rootView, question);

                //Gets last answer to decrease corresponding variable
                int last_answer = getArguments().getInt(ARG_SECTION_ANSWER_SERVER);
                if (last_answer == 1) ((TabsActivity) getActivity()).tabs_serverYesAnswers--;
                else if (last_answer == 2) ((TabsActivity) getActivity()).tabs_serverNoAnswers--;
                else if (last_answer == 3) ((TabsActivity) getActivity()).tabs_serverDunnoAnswers--;
                //Sets it to 0. If sending will be successful then it will be set to proper answer number.
                //I use it as an information if question has been sent to server or not.
                getArguments().putInt(ARG_SECTION_ANSWER_SERVER, 0);
                return false;
            }
            return true;
        }

        /**
         * Used to make a GET request on server for given answer on corresponding tab.
         *
         * @param rootView Root view of corresponding tab
         * @param question Number of corresponding tab
         * @param answerNo Type of given answer
         */
        private void sendToServer(View rootView, int question, String answerNo, String timestamp) {

            if (isOnline()) {
                DownloadWebpageTask task = new DownloadWebpageTask();
                task.configure(rootView, question);
                task.execute(createDataURL("to_server", answerNo, timestamp));
            } else {
                Toast.makeText(getActivity().getBaseContext(), getResources().getString(R.string.message_no_internet_connection), Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Generates timestamp
         *
         * @return timestamp of given format
         */
        private String makeTimeStamp() {

            //Get current timestamp
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss_SSS", Locale.getDefault());
            Date timestamp = new Date();
            return formatter.format(timestamp);
        }

        /**
         * Creates URL for corresponding tab and answer. Gives it in format for server
         * or for file.
         *
         * @param mode   Determines if string has to be built for server or for file
         * @param answer Type of answer
         * @return Built URL with all significant data
         */
        private String createDataURL(String mode, String answer, String timestamp) {

            SettingsDataSource databaseCreateDataURL = new SettingsDataSource(getActivity());
            databaseCreateDataURL.open();

            StringBuilder sbServerQuery = new StringBuilder();
            String divider = "&";

            if (mode.equals("to_server")) {
                String stringDbServerUrl = databaseCreateDataURL.getSetting("setting_serverAddress");
                String stringServerUrl = getResources().getString(R.string.server_address);
                if (stringDbServerUrl.length() > 1) {
                    stringServerUrl = stringDbServerUrl;
                }
                stringServerUrl = stringServerUrl
                        .concat(databaseCreateDataURL.getSetting("setting_course"))
                        .concat("/")
                        .concat(databaseCreateDataURL.getSetting("setting_test_id"))
                        .concat(".xml").toLowerCase();
                stringServerUrl = stringServerUrl.replace(" ", "");
                sbServerQuery.append(stringServerUrl).append("?");
            }

            String studentNo = databaseCreateDataURL.getSetting("setting_studentNo");
            String course = databaseCreateDataURL.getSetting("setting_course");
            String testId = databaseCreateDataURL.getSetting("setting_test_id");
            String hall_row = databaseCreateDataURL.getSetting("setting_hall_row");
            String hall_seat = databaseCreateDataURL.getSetting("setting_hall_seat");
            String name = databaseCreateDataURL.getSetting("setting_name");
            String surname = databaseCreateDataURL.getSetting("setting_surname");
            String vector = databaseCreateDataURL.getSetting("setting_vector");
            String group = databaseCreateDataURL.getSetting("setting_group");
            String question_no = String.valueOf(getArguments().getInt(ARG_SECTION_NUMBER));
            String sessionID = databaseCreateDataURL.getSetting("setting_sessionID");

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
            sbServerQuery.append("surname=").append(surname);

            String sentUrl = sbServerQuery.toString();
            sentUrl = sentUrl.replace(" ", "");

            databaseCreateDataURL.close();

            return sentUrl;
        }

        /**
         * Sets choosen button's background from corresponding view to gray
         * and makes letters on gray button white.
         * Resets other button's texts to black.
         *
         * @param rootView Root view of corresponding tab.
         * @param arg      Choosen answer
         */
        private void setGivenAnswerGray(View rootView, int arg) {
            Button buttonYes = (Button) rootView.findViewById(R.id.button_yes);
            Button buttonNo = (Button) rootView.findViewById(R.id.button_no);
            Button buttonDunno = (Button) rootView.findViewById(R.id.button_dunno);

            int last_answer = getArguments().getInt(ARG_SECTION_ANSWER_FILE);

            if (last_answer == 1) ((TabsActivity) getActivity()).tabs_fileYesAnswers--;
            else if (last_answer == 2) ((TabsActivity) getActivity()).tabs_fileNoAnswers--;
            else if (last_answer == 3) ((TabsActivity) getActivity()).tabs_fileDunnoAnswers--;

            if (arg == 1) {
                buttonYes.setBackgroundColor(Color.DKGRAY);
                buttonYes.setTextColor(Color.WHITE);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setTextColor(Color.BLACK);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setTextColor(Color.BLACK);
                getArguments().putInt(ARG_SECTION_ANSWER_FILE, 1);
                ((TabsActivity) getActivity()).tabs_fileYesAnswers++;
            } else if (arg == 2) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonYes.setTextColor(Color.BLACK);
                buttonNo.setBackgroundColor(Color.DKGRAY);
                buttonNo.setTextColor(Color.WHITE);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setTextColor(Color.BLACK);
                getArguments().putInt(ARG_SECTION_ANSWER_FILE, 2);
                ((TabsActivity) getActivity()).tabs_fileNoAnswers++;
            } else if (arg == 3) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonYes.setTextColor(Color.BLACK);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setTextColor(Color.BLACK);
                buttonDunno.setBackgroundColor(Color.DKGRAY);
                buttonDunno.setTextColor(Color.WHITE);
                getArguments().putInt(ARG_SECTION_ANSWER_FILE, 3);
                ((TabsActivity) getActivity()).tabs_fileDunnoAnswers++;
            } else if (arg == 0) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER_FILE, 0);
            }
        }

        /**
         * Sets choosen button's background from corresponding view to blue
         * and sets button's text to black.
         *
         * @param rootView Root view of corresponding tab.
         * @param arg      Choosen answer
         */
        private void setGivenAnswerBlue(View rootView, int arg) {
            Button buttonYes = (Button) rootView.findViewById(R.id.button_yes);
            Button buttonNo = (Button) rootView.findViewById(R.id.button_no);
            Button buttonDunno = (Button) rootView.findViewById(R.id.button_dunno);

            int last_answer = getArguments().getInt(ARG_SECTION_ANSWER_SERVER);
            if (last_answer == 1) ((TabsActivity) getActivity()).tabs_serverYesAnswers--;
            else if (last_answer == 2) ((TabsActivity) getActivity()).tabs_serverNoAnswers--;
            else if (last_answer == 3) ((TabsActivity) getActivity()).tabs_serverDunnoAnswers--;

            if (arg == 1) {
                buttonYes.setBackgroundColor(Color.parseColor("#819FF7"));
                buttonYes.setTextColor(Color.BLACK);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER_SERVER, 1);
                ((TabsActivity) getActivity()).tabs_serverYesAnswers++;
            } else if (arg == 2) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundColor(Color.parseColor("#819FF7"));
                buttonNo.setTextColor(Color.BLACK);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER_SERVER, 2);
                ((TabsActivity) getActivity()).tabs_serverNoAnswers++;
            } else if (arg == 3) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundColor(Color.parseColor("#819FF7"));
                buttonDunno.setTextColor(Color.BLACK);
                getArguments().putInt(ARG_SECTION_ANSWER_SERVER, 3);
                ((TabsActivity) getActivity()).tabs_serverDunnoAnswers++;
            } else if (arg == 0) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER_SERVER, 0);
            }
        }

        /**
         * Given a URL, establishes an HttpUrlConnection and retrieves
         * the server response code, which it returns as string.
         *
         * @param myurl URL of server to which it connects.
         * @return Server response code
         * @throws IOException
         */
        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 1;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("HEAD");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                serverResponseCode = response;
                Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                serverResponse = convertInputStreamToString(is, len);
                //return contentAsString;
                return String.valueOf(response);

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                try {
                    if (is != null) {
                        is.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Reads an InputStream and converts it to a String.
         *
         * @param stream Input stream taken from server.
         * @param len    Length of input stream
         * @return Input stream converted into string.
         * @throws IOException
         * @throws UnsupportedEncodingException
         */
        public String convertInputStreamToString(InputStream stream, int len) throws IOException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }

        /**
         * Checks whether app has network access.
         *
         * @return true if online, false if not
         */
        public boolean isOnline() {
            ConnectivityManager connMgr =
                    (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connMgr.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnectedOrConnecting();
        }

        /**
         * Uses AsyncTask to create a task away from the main UI threadTabs_killer. This task takes a
         * URL string and uses it to create an HttpUrlConnection. Once the connection
         * has been established, the AsyncTask downloads the contents of the webpage.
         * Finally it sets up corresponding tab if server response code is 200.
         */
        private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
            View rootView;
            int answerNo;

            @Override
            protected String doInBackground(String... urls) {

                try {
                    return downloadUrl(urls[0]);
                } catch (IOException e) {
                    return "Unable to retrieve web page. URL may be invalid.";
                }
            }

            /**
             * Sets color of button according to successfully sent answer.
             * Updates tab's info about current sent answer - look into
             * sendToServer() to understand what is going on.
             *
             * @param result Response code got from server.
             */
            @Override
            protected void onPostExecute(String result) {
                Log.d(serverResponseCode.toString(), serverResponse);
                if (result.equals("200")) {
                    setGivenAnswerBlue(rootView, answerNo);
                    getArguments().putInt(ARG_SECTION_ANSWER_SERVER, answerNo);
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), getActivity().getApplicationContext().getResources().getString(R.string.message_unable_to_send_answer), Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * @param passedView     Root view of corresponding tab.
             * @param passedAnswerNo Number of corresponding tab.
             */
            void configure(View passedView, int passedAnswerNo) {
                rootView = passedView;
                answerNo = passedAnswerNo;
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * given position of it's tab. Holds an array of instantiated fragments.
     * Instantiates new fragment when getItem is called.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragments = new ArrayList<>();

        /**
         * @param fragmentManagerSections Interface for interacting with Fragment objects inside of an Activity
         */
        public SectionsPagerAdapter(FragmentManager fragmentManagerSections) {
            super(fragmentManagerSections);
        }

        /**
         * Is called to instantiate the fragment for the given page.
         *
         * @param position Position of current fragment to be viewed
         * @return Instantiated fragment corresponding to current view
         */
        @Override
        public Fragment getItem(int position) {
            return fragments.get(position);
        }

        /**
         * @return Amount of instantiated fragments.
         */
        @Override
        public int getCount() {
            return fragments.size();
        }

        /**
         * Used to title fragments. Titles are then viewed on viewPagerTabs.
         *
         * @param position Position of corresponding fragment to be titled
         * @return Title of fragment.
         */
        @Override
        public CharSequence getPageTitle(int position) {

            String titlePattern = getResources().getString(R.string.label_section);
            String title = String.format(titlePattern, position + 1);

            return title;
        }

        /**
         * Used to add new fragment and notify SectionsPagerAdapter that amount of fragments
         * has been changed.
         */
        public void addFragment() {
            fragments.add(QuestionFragment.newInstance(fragments.size() + 1));
            notifyDataSetChanged();
        }

    }

    /**
     * A {@link Thread} that is used to finish TabsActivity intent.
     */
    public class threadKiller extends Thread {
        @Override
        public void run() {
            try {
                Thread.sleep(3500);
                alertDialog.dismiss();
                TabsActivity.this.finish();
            } catch (Exception e) {
                alertDialog.dismiss();
                e.printStackTrace();
            }
        }
    }

    /**
     * A {@link ComponentCallbacks2} which is used to determine if app has been in
     * background. If it was then wasInBackgroundTabs is changed to true.
     */
    public class MemoryBoss implements ComponentCallbacks2 {
        @Override
        public void onConfigurationChanged(final Configuration newConfig) {
        }

        @Override
        public void onLowMemory() {
        }

        @Override
        public void onTrimMemory(final int level) {
            if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {

                wasInBackgroundTabs = true;

                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disableIncomingCallReceiver();
                    }
                }, 1500);
            }
            // you might as well implement some memory cleanup here and be a nice Android dev.
        }
    }
}
