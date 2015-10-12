package ru.dyatel.tsuschedule;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import ru.dyatel.tsuschedule.data.DataFragment;

public class MainActivity extends AppCompatActivity {

    public static final String PREFERENCES_FILE = "prefs";
    private static final String GROUP_INDEX_KEY = "group_index";
    private static final String SUBGROUP_KEY = "subgroup";

    private DataFragment dataFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load saved preferences
        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        String groupIndex = preferences.getString(GROUP_INDEX_KEY, "");
        int subgroup = preferences.getInt(SUBGROUP_KEY, 1);

        FragmentManager fragmentManager = getFragmentManager();

        // Get the data fragment
        dataFragment = (DataFragment) fragmentManager.findFragmentByTag(DataFragment.TAG);
        if (dataFragment == null) {
            dataFragment = new DataFragment();
            fragmentManager.beginTransaction().add(dataFragment, DataFragment.TAG).commit();
        }

        // Replace ActionBar with Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the navigation drawer
        NavigationDrawerFragment navigationDrawer =
                (NavigationDrawerFragment) fragmentManager.findFragmentById(R.id.navigation_drawer);
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawer.initialize(drawerLayout, toolbar, dataFragment, groupIndex, subgroup);

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new WeekFragmentPagerAdapter(fragmentManager, dataFragment, this));

        // Set up the TabLayout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences preferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
        preferences.edit()
                .putString(GROUP_INDEX_KEY, dataFragment.getGroup())
                .putInt(SUBGROUP_KEY, dataFragment.getSubgroup())
                .apply();

        dataFragment.saveData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataFragment.clearListeners();
    }

}
