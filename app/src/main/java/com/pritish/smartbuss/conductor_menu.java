package com.pritish.smartbuss;

import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import nl.joery.animatedbottombar.AnimatedBottomBar;

public class conductor_menu extends AppCompatActivity {

    DrawerLayout drawerLayout;
    AnimatedBottomBar animatedBottomBar;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conductor_menu);

        String mainuserPhone = getIntent().getStringExtra("mainuserPhone");
        String userName = getIntent().getStringExtra("userName");

        initViews();
        setupBottomNavigation(mainuserPhone, userName);
        setupDrawer();

        if (savedInstanceState == null) {
            loadHomeFragment(mainuserPhone, userName);
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        animatedBottomBar = findViewById(R.id.animatedBottomBar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupBottomNavigation(String mainuserPhone, String userName) {
        animatedBottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, AnimatedBottomBar.Tab lastTab,
                                      int newIndex, AnimatedBottomBar.Tab newTab) {
                switch (newIndex) {
                    case 0:
                        loadHomeFragment(mainuserPhone, userName);
                        break;
                    case 1:
                        loadUserProfileFragment(mainuserPhone, userName);
                        break;
                    case 2:
                        loadConductorIncentiveFragment(mainuserPhone);
                        break;
                    case 3:
                        loadNotificationFragment(mainuserPhone);
                        break;
                }
            }

            @Override
            public void onTabReselected(int index, AnimatedBottomBar.Tab tab) {
                // Optional: Handle tab reselection
            }
        });
    }

    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_nav, R.string.close_nav
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void loadHomeFragment(String mainuserPhone, String userName) {
        conductor_home fragment = new conductor_home();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", mainuserPhone);
        bundle.putString("userName", userName);
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void loadUserProfileFragment(String mainuserPhone, String userName) {
        conductor_profile fragment = new conductor_profile();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", mainuserPhone);
        bundle.putString("userName", userName);
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void loadConductorIncentiveFragment(String mainuserPhone) {
        conductor_incentive fragment = new conductor_incentive();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", mainuserPhone);
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
    }

    private void loadNotificationFragment(String mainuserPhone) {
        user_notification fragment = new user_notification();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", mainuserPhone);
        fragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .commit();
    }
}