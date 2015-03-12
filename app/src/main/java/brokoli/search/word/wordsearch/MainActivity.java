package brokoli.search.word.wordsearch;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    final static String PREFS_NAME = "MyPrefsFile";

    ActionBar actionBar;
    private DrawerLayout drawerLayout;
    private ListView drawerList;
    ArrayAdapter<String> arrayAdapter;
    ActionBarDrawerToggle drawerToggle;
    private static long back_pressed;
    Fragment puzzleFragment;
    ArrayList<String> allWords = new ArrayList<String>();
    String [] selectedWords;
    boolean isNewGame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findIsNewGame();
        getAllWords();
        selectWords();
        initializeActionBar();
        initializeFragment();
    }

    public void findIsNewGame() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String restoredLetters = prefs.getString("letters", null);
        if(restoredLetters != null) {
            isNewGame = false;
        } else {
            isNewGame = true;
        }
    }

    private void initializeFragment() {
        puzzleFragment = new WordGridFragment();
        FragmentManager fragmentManager = getFragmentManager();
        Bundle bundle = new Bundle();
        bundle.putStringArray("selectedWords", selectedWords);
        bundle.putBoolean("isNewGame", isNewGame);
        isNewGame = false;
        puzzleFragment.setArguments(bundle);
        fragmentManager.beginTransaction().replace(R.id.content_frame, puzzleFragment).commit();
    }

    private void initializeActionBar() {
        actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawers();
        }else {
            if (back_pressed + 2000 > System.currentTimeMillis())
                super.onBackPressed();
            else
                Toast.makeText(getBaseContext(), R.string.close_warning, Toast.LENGTH_SHORT).show();
            back_pressed = System.currentTimeMillis();
        }
    }

    public String[] selectWords() {
        int size = allWords.size();
        selectedWords = new String[5012];
        Random r = new Random();
        for (int i = 0; i < 5012; i++) {
            selectedWords[i] = allWords.get(r.nextInt(size)).toUpperCase(Locale.ENGLISH);
        }
        return selectedWords;
    }

    public void getAllWords() {
        String str = "";
        try {
            InputStream is = getResources().openRawResource(R.raw.words);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            if (is!=null) {
                while ((str = reader.readLine()) != null) {
                    if(str.length() > 3 && str.length() < 11 && str.matches("[a-zA-Z]+"))
                        allWords.add(str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeNavigationDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        drawerList = (ListView) findViewById(R.id.left_drawer);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.custom_navigation_item_list, selectedWords);
        drawerList.setAdapter(arrayAdapter);
        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                drawerLayout.closeDrawers();
            }
        });
        drawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                drawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);
    }

    public void updateNavigationDrawer(String[] words, int howManyPlaced) {
        selectedWords = new String[howManyPlaced];
        for (int i = 0; i < howManyPlaced; i++) {
            selectedWords[i] = words[i];
        }
        initializeNavigationDrawer();
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
        if(drawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.settingButton:
                return true;
            case R.id.refreshButton:
                selectWords();
                isNewGame = true;
                initializeFragment();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
