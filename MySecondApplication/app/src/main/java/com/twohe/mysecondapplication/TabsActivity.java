package com.twohe.mysecondapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
            }
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
                    setTab(rootView, 1);
                }
            });
            rootView.findViewById(R.id.button_no).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setTab(rootView, 2);

                }
            });
            rootView.findViewById(R.id.button_dunno).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                    ((TabsActivity) getActivity()).amount_of_questions++;
                }
            });


            final SettingsDataSource db = new SettingsDataSource(getActivity());
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
