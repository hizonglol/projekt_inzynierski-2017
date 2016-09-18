package com.twohe.mysecondapplication;

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by TwoHe on 10.07.2016.
 */
public class TabsActivity extends AppCompatActivity {


    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    int amount_of_questions = 1;
    int amount_of_yes_answers = 0;
    int amount_of_no_answers = 0;
    int amount_of_dunno_answers = 0;

    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        Log.v("Ilosc instancji: ", String.valueOf(list.size()));
        return list.size() < 2;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs);

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

    public void addTab() {
        mSectionsPagerAdapter.addFragment();
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent intentSettings = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intentSettings);

            Log.i("Menu", "Settings");

            return true;
        }

        if (id == R.id.action_info) {

            Intent intentInfo = new Intent(getApplicationContext(), InfoActivity.class);
            startActivity(intentInfo);

            Log.i("Menu", "Info");

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

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
                return contentAsString;

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
                Log.d("Wynik", result);
            }
        }

        public QuestionFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static QuestionFragment newInstance(int sectionNumber) {
            QuestionFragment fragment = new QuestionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            args.putInt("choosen_answer", 0);
            fragment.setArguments(args);
            return fragment;
        }

        private void setTab(View rootView, int arg) {

            Button buttonYes = (Button) rootView.findViewById(R.id.button_yes);
            Button buttonNo = (Button) rootView.findViewById(R.id.button_no);
            Button buttonDunno = (Button) rootView.findViewById(R.id.button_dunno);

            int last_answer = getArguments().getInt("choosen_answer");

            if (last_answer == 1) ((TabsActivity) getActivity()).amount_of_yes_answers--;
            else if (last_answer == 2) ((TabsActivity) getActivity()).amount_of_no_answers--;
            else if (last_answer == 3) ((TabsActivity) getActivity()).amount_of_dunno_answers--;

            if (arg == 1) {
                buttonYes.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                buttonNo.getBackground().clearColorFilter();
                buttonDunno.getBackground().clearColorFilter();
                getArguments().putInt("choosen_answer", 1);
                ((TabsActivity) getActivity()).amount_of_yes_answers++;
            } else if (arg == 2) {
                buttonYes.getBackground().clearColorFilter();
                buttonNo.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                buttonDunno.getBackground().clearColorFilter();
                getArguments().putInt("choosen_answer", 2);
                ((TabsActivity) getActivity()).amount_of_no_answers++;
            } else if (arg == 3) {
                buttonYes.getBackground().clearColorFilter();
                buttonNo.getBackground().clearColorFilter();
                buttonDunno.getBackground().setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
                getArguments().putInt("choosen_answer", 3);
                ((TabsActivity) getActivity()).amount_of_dunno_answers++;
            } else if (arg == 0) {
                buttonYes.getBackground().clearColorFilter();
                buttonNo.getBackground().clearColorFilter();
                buttonDunno.getBackground().clearColorFilter();
                getArguments().putInt("choosen_answer", 0);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 final Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_tabs, container, false);

            final SettingsDataSource db = new SettingsDataSource(getActivity());

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
                    db.open();
                    String stringServerUrl = "http://www.zpcir.ict.pwr.wroc.pl/~witold/empty.html";
                    StringBuilder sbServerQuery = new StringBuilder();
                    sbServerQuery.append(stringServerUrl).append("?");
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

                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        new DownloadWebpageTask().execute(sentUrl);
                    } else {
                        Log.d("Warning", "No network connection available.");
                        Toast.makeText(getActivity().getBaseContext(), "Brak połączenia z internetem!", Toast.LENGTH_SHORT).show();
                        setTab(rootView, 0);
                        return;
                    }
                    db.close();

                    setTab(rootView, 1);
                }
            });
            rootView.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    db.open();
                    String stringServerUrl = "http://www.zpcir.ict.pwr.wroc.pl/~witold/empty.html";
                    StringBuilder sbServerQuery = new StringBuilder();
                    sbServerQuery.append(stringServerUrl).append("?");
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
                    String answer = "no";
                    sbServerQuery.append("answer=").append(answer);

                    String sentUrl = sbServerQuery.toString();
                    sentUrl = sentUrl.replace(" ", "");

                    Log.d("Sent uri", sentUrl);

                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        new DownloadWebpageTask().execute(sentUrl);
                    } else {
                        Log.d("Warning", "No network connection available.");
                        Toast.makeText(getActivity().getBaseContext(), "Brak połączenia z internetem!", Toast.LENGTH_SHORT).show();
                        setTab(rootView, 0);
                        return;
                    }
                    db.close();

                    setTab(rootView, 2);

                }
            });
            rootView.findViewById(R.id.button_dunno).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    db.open();
                    String stringServerUrl = "http://www.zpcir.ict.pwr.wroc.pl/~witold/empty.html";
                    StringBuilder sbServerQuery = new StringBuilder();
                    sbServerQuery.append(stringServerUrl).append("?");
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
                    String answer = "dunno";
                    sbServerQuery.append("answer=").append(answer);

                    String sentUrl = sbServerQuery.toString();
                    sentUrl = sentUrl.replace(" ", "");

                    Log.d("Sent uri", sentUrl);

                    ConnectivityManager connMgr = (ConnectivityManager)
                            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        new DownloadWebpageTask().execute(sentUrl);
                    } else {
                        Log.d("Warning", "No network connection available.");
                        Toast.makeText(getActivity().getBaseContext(), "Brak połączenia z internetem!", Toast.LENGTH_SHORT).show();
                        setTab(rootView, 0);
                        return;
                    }
                    db.close();

                    setTab(rootView, 3);
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


            db.open();

            TextView viewGroup = (TextView) rootView.findViewById(R.id.group_number);
            TextView viewQuestionNumber = (TextView) rootView.findViewById(R.id.question_number);

            if (viewGroup != null)
                viewGroup.setText(db.getSetting("setting_group"));

            if (viewQuestionNumber != null)
                viewQuestionNumber.setText(String.valueOf(getArguments().getInt(ARG_SECTION_NUMBER)));

            db.close();

            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();

            View rootView = getView();
            int odpowiedz = getArguments().getInt("choosen_answer");

            Log.v("Tabs Activity", "Restoring tab");
            setTab(rootView, odpowiedz);
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
