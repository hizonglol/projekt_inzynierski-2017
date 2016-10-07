package com.twohe.mysecondapplication;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.SecretKeySpec;


/**
 * Created by TwoHe on 10.07.2016.
 */
public class TabsActivity extends AppCompatActivity {

    // Create object of SharedPreferences.
    SharedPreferences sharedPref;

    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    int amount_of_questions = 1;
    int amount_of_yes_answers = 0;
    int amount_of_no_answers = 0;
    int amount_of_dunno_answers = 0;

    String token;
    File fileToWrite;

    boolean wasInBackground = false;

    /*
    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        Log.i("isCallable", String.valueOf(list.size()));
        return list.size() < 2;
    }
    */

    Thread thread = new Thread() {
        @Override
        public void run() {
            try {
                Thread.sleep(3500); // As I am using LENGTH_LONG in Toast
                TabsActivity.this.finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private String generateToken() {
        SecureRandom randomizer = new SecureRandom();
        byte bytes[] = new byte[6];
        randomizer.nextBytes(bytes);
        String base64 = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP);
        Log.i("generateCode", base64);

        String shortenedBase64 = base64.substring(0, 4).toLowerCase();

        return shortenedBase64;
    }

    private boolean createTestFile(String token) {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss", Locale.getDefault());
        //SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd", Locale.getDefault());
        Date now = new Date();
        String fileName = formatter.format(now) + "-" + token + ".txt";//like 2016_01_12-12_30_00-ABCdef12.txt

        try {
            File root = new File(Environment.getExternalStorageDirectory() + "/Haszowki");
            if (!root.exists()) {
                root.mkdir();
            }
            fileToWrite = new File(root, fileName);


            FileWriter writer = new FileWriter(fileToWrite, true);
            writer.append("Twoj kod testu to: ");
            writer.append(token);
            writer.append("\n\n");
            writer.flush();
            writer.close();
            Toast.makeText(this, "Twoj plik z testem zostal utworzony", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    public boolean appendToFileCiphered(String text) {

        text = encryptIt(text);

        decryptIt(text);

        try {
            FileWriter writer = new FileWriter(fileToWrite, true);
            writer.append(text);
            writer.append("\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private static String cryptoPass = "Moje haslo to brak hasla";

    String encryptIt(String text) {

        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] clearText = text.getBytes("UTF8");
            // Cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            text = Base64.encodeToString(cipher.doFinal(clearText), Base64.DEFAULT);
            Log.d("appendToFileCiphered", "Encrypted: " + text + " -> " + text);

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }

        return text;
    }

    String decryptIt(String text) {

        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encrypedPwdBytes = Base64.decode(text, Base64.DEFAULT);
            // cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));

            String decrypedValue = new String(decrypedValueBytes);
            Log.d("decryptIt", "Decrypted: " + text + " -> " + decrypedValue);
            return decrypedValue;

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return text;

    }

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
                // We're in the Background
                wasInBackground = true;
            }
            // you might as well implement some memory cleanup here and be a nice Android dev.
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        MemoryBoss mMemoryBoss;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mMemoryBoss = new MemoryBoss();
            registerComponentCallbacks(mMemoryBoss);
        }

        SettingsDataSource db = new SettingsDataSource(this);
        db.open();
        token = generateToken();
        db.createSetting("setting_token", token);
        db.close();

        createTestFile(token);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        if (mViewPager != null)
            mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        if (tabLayout != null)
            tabLayout.setupWithViewPager(mViewPager);


        addTab();

    }

    @Override
    protected void onResume() {
        super.onResume();

        Boolean endTest = sharedPref.getBoolean("End test", false);

        if (endTest)
            TabsActivity.this.finish();


        if (wasInBackground) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Wyszedłeś z aplikacji")
                    .setMessage("Test zakończony")
                    .setCancelable(false)
                    .show();

            thread.start();
        }

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Wychodzę z testu")
                .setMessage("Czy na pewno chcesz wyjść z testu?")
                .setPositiveButton("Tak", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("Nie", null)
                .show();
    }


    public void addTab() {
        mSectionsPagerAdapter.addFragment();
    }


    //*********************************************************************************************

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class QuestionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String ARG_SECTION_ANSWER = "section_answer";
        private static final String ARG_SECTION_SENT = "section_sent";
        private static final String DEBUG_TAG = "HttpExample";

        // Reads an InputStream and converts it to a String.
        public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
            Reader reader = null;
            reader = new InputStreamReader(stream, "UTF-8");
            char[] buffer = new char[len];
            reader.read(buffer);
            return new String(buffer);
        }

        // Given a URL, establishes an HttpUrlConnection and retrieves
        // the web page content as a InputStream, which it returns as
        // a string.
        private String downloadUrl(String myurl) throws IOException {
            InputStream is = null;
            // Only display the first 500 characters of the retrieved
            // web page content.
            int len = 500;

            try {
                URL url = new URL(myurl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();
                Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                // Convert the InputStream into a string
                String contentAsString = readIt(is, len);
                //return contentAsString;
                return String.valueOf(response);

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }

        // Uses AsyncTask to create a task away from the main UI thread. This task takes a
        // URL string and uses it to create an HttpUrlConnection. Once the connection
        // has been established, the AsyncTask downloads the contents of the webpage as
        // an InputStream. Finally, the InputStream is converted into a string, which is
        // displayed in the UI by the AsyncTask's onPostExecute method.
        private class DownloadWebpageTask extends AsyncTask<String, Void, String> {
            View rootView;
            int tab;

            @Override
            protected String doInBackground(String... urls) {

                // params comes from the execute() call: params[0] is the url.
                try {
                    return downloadUrl(urls[0]);
                } catch (IOException e) {
                    return "Unable to retrieve web page. URL may be invalid.";
                }
            }

            // onPostExecute displays the results of the AsyncTask.
            @Override
            protected void onPostExecute(String result) {
                if (result.equals("200")) {
                    setTab(rootView, tab);
                    getArguments().putBoolean(ARG_SECTION_SENT, true);
                }

                Log.d("Wynik", result);
            }

            void configure(View passedView, int passedTab) {
                rootView = passedView;
                tab = passedTab;
            }
        }

        public QuestionFragment() {
        }

        /**
         * @param sectionNumber - number of section
         * @return New instance of fragment to the given section number
         */
        public static QuestionFragment newInstance(int sectionNumber) {
            QuestionFragment fragment = new QuestionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putInt(ARG_SECTION_ANSWER, 0);
            args.putBoolean(ARG_SECTION_SENT, false);
            fragment.setArguments(args);
            return fragment;
        }

        /**
         * @param rootView - root view of tab
         * @param arg      - which answer has been chosen
         */

        private void setTab(View rootView, int arg) {
            Button buttonYes = (Button) rootView.findViewById(R.id.button_yes);
            Button buttonNo = (Button) rootView.findViewById(R.id.button_no);
            Button buttonDunno = (Button) rootView.findViewById(R.id.button_dunno);

            int last_answer = getArguments().getInt(ARG_SECTION_ANSWER);

            if (last_answer == 1) ((TabsActivity) getActivity()).amount_of_yes_answers--;
            else if (last_answer == 2) ((TabsActivity) getActivity()).amount_of_no_answers--;
            else if (last_answer == 3) ((TabsActivity) getActivity()).amount_of_dunno_answers--;

            if (arg == 1) {
                buttonYes.setBackgroundColor(Color.GREEN);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER, 1);
                ((TabsActivity) getActivity()).amount_of_yes_answers++;
            } else if (arg == 2) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundColor(Color.RED);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER, 2);
                ((TabsActivity) getActivity()).amount_of_no_answers++;
            } else if (arg == 3) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundColor(Color.YELLOW);
                getArguments().putInt(ARG_SECTION_ANSWER, 3);
                ((TabsActivity) getActivity()).amount_of_dunno_answers++;
            } else if (arg == 0) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER, 0);
            }
        }

        private void setTabGray(View rootView, int arg) {
            Button buttonYes = (Button) rootView.findViewById(R.id.button_yes);
            Button buttonNo = (Button) rootView.findViewById(R.id.button_no);
            Button buttonDunno = (Button) rootView.findViewById(R.id.button_dunno);

            int last_answer = getArguments().getInt(ARG_SECTION_ANSWER);

            if (last_answer == 1) ((TabsActivity) getActivity()).amount_of_yes_answers--;
            else if (last_answer == 2) ((TabsActivity) getActivity()).amount_of_no_answers--;
            else if (last_answer == 3) ((TabsActivity) getActivity()).amount_of_dunno_answers--;

            if (arg == 1) {
                buttonYes.setBackgroundColor(Color.DKGRAY);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER, 1);
                ((TabsActivity) getActivity()).amount_of_yes_answers++;
            } else if (arg == 2) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundColor(Color.DKGRAY);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER, 2);
                ((TabsActivity) getActivity()).amount_of_no_answers++;
            } else if (arg == 3) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundColor(Color.DKGRAY);
                getArguments().putInt(ARG_SECTION_ANSWER, 3);
                ((TabsActivity) getActivity()).amount_of_dunno_answers++;
            } else if (arg == 0) {
                buttonYes.setBackgroundResource(android.R.drawable.btn_default);
                buttonNo.setBackgroundResource(android.R.drawable.btn_default);
                buttonDunno.setBackgroundResource(android.R.drawable.btn_default);
                getArguments().putInt(ARG_SECTION_ANSWER, 0);
            }
        }

        private String createUriFromThisPage() {
            SettingsDataSource db = new SettingsDataSource(getActivity());
            db.open();
            String stringServerUrl = "http://www.zpcir.ict.pwr.wroc.pl/~witold/empty.html";
            StringBuilder sbServerQuery = new StringBuilder();
            sbServerQuery.append(stringServerUrl).append("?");
            sbServerQuery.append("token=").append(((TabsActivity) getActivity()).token).append("&");
            String subject = db.getSetting("setting_subject");
            sbServerQuery.append("subject=").append(subject).append("&");
            String testId = db.getSetting("setting_test_id");
            sbServerQuery.append("test_id=").append(testId).append("&");
            String name = db.getSetting("setting_name");
            sbServerQuery.append("name=").append(name).append("&");
            String surname = db.getSetting("setting_surname");
            sbServerQuery.append("surname=").append(surname).append("&");
            String index = db.getSetting("setting_index");
            sbServerQuery.append("index=").append(index).append("&");
            String weights = db.getSetting("setting_weights");
            sbServerQuery.append("weights=").append(weights).append("&");
            String group = db.getSetting("setting_group");
            sbServerQuery.append("group=").append(group).append("&");
            String hall_row = db.getSetting("setting_hall_row");
            sbServerQuery.append("hall_row=").append(hall_row).append("&");
            String hall_place = db.getSetting("setting_hall_place");
            sbServerQuery.append("hall_place=").append(hall_place).append("&");
            String question_no = String.valueOf(getArguments().getInt(ARG_SECTION_NUMBER));
            sbServerQuery.append("question_no=").append(question_no).append("&");
            String answer = "yes";
            sbServerQuery.append("answer=").append(answer);

            String sentUrl = sbServerQuery.toString();
            sentUrl = sentUrl.replace(" ", "");

            Log.d("Sent uri", sentUrl);

            db.close();

            return sentUrl;
        }

        private boolean initialize(View rootView) {
            SettingsDataSource db = new SettingsDataSource(getActivity());

            db.open();

            TextView viewGroup = (TextView) rootView.findViewById(R.id.view_group_number);
            TextView viewQuestionNumber = (TextView) rootView.findViewById(R.id.view_question_number);
            TextView viewSessionToken = (TextView) rootView.findViewById(R.id.view_token);

            if (viewGroup != null)
                viewGroup.setText(db.getSetting("setting_group"));

            if (viewQuestionNumber != null)
                viewQuestionNumber.setText(String.valueOf(getArguments().getInt(ARG_SECTION_NUMBER)));

            if (viewSessionToken != null)
                viewSessionToken.setText(db.getSetting("setting_token"));

            db.close();

            return true;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_tabs, container, false);

            rootView.findViewById(R.id.button_end_test).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intentSummary = new Intent(getActivity().getApplication(), SummaryActivity.class);
                    Bundle b = new Bundle();
                    b.putInt("amount_of_questions", ((TabsActivity) getActivity()).amount_of_questions);
                    b.putInt("amount_of_yes_answers", ((TabsActivity) getActivity()).amount_of_yes_answers);
                    b.putInt("amount_of_no_answers", ((TabsActivity) getActivity()).amount_of_no_answers);
                    b.putInt("amount_of_dunno_answers", ((TabsActivity) getActivity()).amount_of_dunno_answers);
                    intentSummary.putExtras(b); //Put your id to your next Intent
                    startActivity(intentSummary);
                }
            });

            rootView.findViewById(R.id.button_yes).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String sentUrl = createUriFromThisPage();

                    if (((TabsActivity) getActivity()).appendToFileCiphered(sentUrl)) {
                        setTabGray(rootView, 1);
                        getArguments().putBoolean(ARG_SECTION_SENT, false);
                    }


                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        DownloadWebpageTask task = new DownloadWebpageTask();
                        task.configure(rootView, 1);
                        task.execute(sentUrl);
                    } else {
                        Log.d("Warning", "No network connection available.");
                        Toast.makeText(getActivity().getBaseContext(), "Brak połączenia z internetem!", Toast.LENGTH_SHORT).show();
                        //setTab(rootView, 0);
                        return;
                    }

                    //setTab(rootView, 1);
                }
            });

            rootView.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String sentUrl = createUriFromThisPage();

                    if (((TabsActivity) getActivity()).appendToFileCiphered(sentUrl)) {
                        setTabGray(rootView, 2);
                        getArguments().putBoolean(ARG_SECTION_SENT, false);
                    }

                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        DownloadWebpageTask task = new DownloadWebpageTask();
                        task.configure(rootView, 2);
                        task.execute(sentUrl);
                    } else {
                        Log.d("Warning", "No network connection available.");
                        Toast.makeText(getActivity().getBaseContext(), "Brak połączenia z internetem!", Toast.LENGTH_SHORT).show();
                        //setTab(rootView, 0);
                        return;
                    }

                    //setTab(rootView, 2);

                }
            });

            rootView.findViewById(R.id.button_dunno).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String sentUrl = createUriFromThisPage();

                    if (((TabsActivity) getActivity()).appendToFileCiphered(sentUrl)) {
                        setTabGray(rootView, 3);
                        getArguments().putBoolean(ARG_SECTION_SENT, false);
                    }

                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        DownloadWebpageTask task = new DownloadWebpageTask();
                        task.configure(rootView, 3);
                        task.execute(sentUrl);
                    } else {
                        Log.d("Warning", "No network connection available.");
                        Toast.makeText(getActivity().getBaseContext(), "Brak połączenia z internetem!", Toast.LENGTH_SHORT).show();
                        //setTab(rootView, 0);
                        return;
                    }

                    //setTab(rootView, 3);
                }
            });

            rootView.findViewById(R.id.button_add_question).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int liczba = getArguments().getInt(ARG_SECTION_NUMBER);
                    CharSequence toast = "Dodano pytanie nr: " + String.valueOf(liczba + 1);
                    Toast wyswietl = Toast.makeText(getActivity(), toast, Toast.LENGTH_SHORT);
                    wyswietl.show();

                    ((TabsActivity) getActivity()).addTab();
                    int nextTab = ((TabsActivity) getActivity()).amount_of_questions++;
                    ((TabsActivity) getActivity()).mViewPager.setCurrentItem(nextTab);
                }
            });

            initialize(rootView);

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();

            View rootView = getView();
            int answer = getArguments().getInt(ARG_SECTION_ANSWER);
            boolean sent = getArguments().getBoolean(ARG_SECTION_SENT);

            Log.v("onResume", "Restoring tab");
            Log.w("onResume", String.valueOf(answer));

            if (sent)
                setTab(rootView, answer);
            else
                setTabGray(rootView, answer);
        }
    }

    //*********************************************************************************************

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> fragments = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {

            String titlePattern = getResources().getString(R.string.label_section);
            String title = String.format(titlePattern, position + 1);

            return title;
        }

        public void addFragment() {
            fragments.add(QuestionFragment.newInstance(fragments.size() + 1));
            notifyDataSetChanged();
        }

    }

}
