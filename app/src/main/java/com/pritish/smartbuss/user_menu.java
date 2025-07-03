package com.pritish.smartbuss;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import nl.joery.animatedbottombar.AnimatedBottomBar;

public class user_menu extends AppCompatActivity {

    private static final String TAG = "UserMenuActivity";

    DrawerLayout drawerLayout;
    Toolbar toolbar;
    AnimatedBottomBar animatedBottomBar;
    private ActionBarDrawerToggle mDrawerToggle;

    private static final String SHARED_PREF_NAME = "smartbus_pref";
    private static final String KEY_LOGGED_IN_PHONE = "loggedInPhone";
    private static final String KEY_USERNAME = "userName";

    private String currentMainUserPhone;
    private String currentUserName;
    private String initialSelectedStart, initialSelectedStop, initialRoutes, initialMessage, initialEnteredNumber;
    private boolean isHandlingNotificationClick = false;
    private boolean showTicketConfirmation = false;
    private String transactionId;
    private float ticketPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_menu);
        Log.d(TAG, "onCreate called. Intent: " + getIntent());

        initViews();
        fetchInitialUserDataFromPrefs();
        processIntentExtras(getIntent());

        Log.d(TAG, "Initial UserPhone: " + currentMainUserPhone + ", UserName: " + currentUserName);
        Log.d(TAG, "Initial Selected Start: " + initialSelectedStart);
        Log.d(TAG, "Initial Selected Stop: " + initialSelectedStop);
        Log.d(TAG, "Initial Selected routes: " + initialRoutes);

        if (initialMessage != null) {
            Log.d(TAG, "Initial Received message: " + initialMessage);
        }

        setupBottomNavigation();

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called. Intent: " + intent);
        setIntent(intent);
        processIntentExtras(intent);
        fetchInitialUserDataFromPrefs();
        handleIntent(intent);
    }

    private void fetchInitialUserDataFromPrefs() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        if (this.currentMainUserPhone == null) {
            this.currentMainUserPhone = sharedPreferences.getString(KEY_LOGGED_IN_PHONE, null);
        }
        if (this.currentUserName == null) {
            this.currentUserName = sharedPreferences.getString(KEY_USERNAME, null);
        }
        Log.d(TAG, "After fetchInitialUserDataFromPrefs - Phone: " + currentMainUserPhone + ", Name: " + currentUserName);
    }

    private void processIntentExtras(Intent intent) {
        if (intent == null) return;

        String phoneFromIntent = intent.getStringExtra("mainuserPhone");
        if (phoneFromIntent != null) this.currentMainUserPhone = phoneFromIntent;

        String nameFromIntent = intent.getStringExtra("userName");
        if (nameFromIntent != null) this.currentUserName = nameFromIntent;

        // Handle booking confirmation extras
        if (intent.getBooleanExtra("showTicket", false)) {
            this.showTicketConfirmation = true;
            this.initialSelectedStart = intent.getStringExtra("startStop");
            this.initialSelectedStop = intent.getStringExtra("destinationStop");
            this.initialRoutes = intent.getStringExtra("routeName");
            this.transactionId = intent.getStringExtra("transactionId");
            this.ticketPrice = intent.getFloatExtra("ticketPrice", 0.0f);
        } else {
            // Regular extras processing
            this.showTicketConfirmation = false;
            this.initialSelectedStart = intent.getStringExtra("selectedStartStop");
            this.initialSelectedStop = intent.getStringExtra("selectedDestinationStop");
            this.initialRoutes = intent.getStringExtra("routes");
        }

        this.initialMessage = intent.getStringExtra("message");
        this.initialEnteredNumber = intent.getStringExtra("enteredNumber");

        if (intent.getExtras() != null) {
            Log.d(TAG, "Processing Intent Extras from intent: " + intent.hashCode());
            for (String key : intent.getExtras().keySet()) {
                Log.d(TAG, "Extra: " + key + " = " + intent.getExtras().get(key));
            }
        } else {
            Log.d(TAG, "No extras in intent: " + intent.hashCode());
        }
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawer_layout);
        animatedBottomBar = findViewById(R.id.animatedBottomBar);
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            setupDrawer();
        } else {
            Log.w(TAG, "Toolbar not found. Drawer toggle will not be functional.");
        }
    }

    private void handleIntent(Intent intent) {
        Log.d(TAG, "handleIntent called. currentMainUserPhone: " + currentMainUserPhone);

        if (intent != null && showTicketConfirmation) {
            Log.d(TAG, "Booking confirmation flow - showing ticket");
            showFragment(createUserHomeFragment(), false, "user_home_booking_confirmation");
            animatedBottomBar.selectTabAt(0, true); // Ensure home tab is selected
        }
        else if (intent != null && intent.getBooleanExtra("OPEN_NOTIFICATIONS_FRAGMENT", false)) {
            Log.d(TAG, "OPEN_NOTIFICATIONS_FRAGMENT extra is true.");
            isHandlingNotificationClick = true;

            Fragment currentVisibleFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (!(currentVisibleFragment instanceof user_home)) {
                Log.d(TAG, "Notification flow: Current fragment is not user_home. Loading user_home as base.");
                showFragment(createUserHomeFragment(), false, "user_home_base_notif_flow");
            }

            Log.d(TAG, "Programmatically selecting Notification tab (index 3).");
            animatedBottomBar.selectTabAt(3, true);
            intent.removeExtra("OPEN_NOTIFICATIONS_FRAGMENT");
        }
        else if (getSupportFragmentManager().findFragmentById(R.id.frame_layout) == null) {
            Log.d(TAG, "No specific extra or no fragment loaded. Loading initial user_home fragment.");
            showFragment(createUserHomeFragment(), false, "user_home_initial");
            animatedBottomBar.selectTabAt(0, true);
        }
        else {
            Log.d(TAG, "handleIntent: No action, fragment likely already exists or no relevant extra.");
        }
    }

    private void setupBottomNavigation() {
        animatedBottomBar.setOnTabSelectListener(new AnimatedBottomBar.OnTabSelectListener() {
            @Override
            public void onTabSelected(int lastIndex, AnimatedBottomBar.Tab lastTab, int newIndex, AnimatedBottomBar.Tab newTab) {
                Log.d(TAG, "BottomNav Tab selected: " + (newTab != null ? newTab.getTitle() : "Unknown") + " at index " + newIndex + ". isHandlingNotificationClick: " + isHandlingNotificationClick);
                boolean addNotificationFragmentToBackStack = false;

                if (newIndex == 3 && isHandlingNotificationClick) {
                    addNotificationFragmentToBackStack = true;
                    isHandlingNotificationClick = false;
                }

                switch (newIndex) {
                    case 0:
                        showFragment(createUserHomeFragment(), false, "user_home_tab");
                        break;
                    case 1:
                        showFragment(createUserProfileFragment(), false, "user_profile_tab");
                        break;
                    case 2:
                        showFragment(createUserWalletFragment(), false, "user_wallet_tab");
                        break;
                    case 3:
                        showFragment(createUserNotificationFragment(
                                        addNotificationFragmentToBackStack ? initialMessage : null,
                                        addNotificationFragmentToBackStack ? initialEnteredNumber : null),
                                addNotificationFragmentToBackStack,
                                "user_notification_tab_or_flow");
                        break;
                }
            }
            @Override
            public void onTabReselected(int index, AnimatedBottomBar.Tab tab) { /* Optional */ }
        });
    }

    private void setupDrawer() {
        if (toolbar != null && drawerLayout != null) {
            mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.open_nav, R.string.close_nav);
            drawerLayout.addDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();
        } else {
            Log.w(TAG, "Toolbar or DrawerLayout is null, cannot setup drawer toggle properly.");
        }
    }

    public void setNavigationDrawerEnabled(boolean enabled) {
        if (drawerLayout == null || mDrawerToggle == null || getSupportActionBar() == null) {
            Log.w(TAG, "Cannot set navigation drawer state: drawerLayout, mDrawerToggle, or SupportActionBar is null.");
            return;
        }
        if (enabled) {
            Log.d(TAG, "Enabling navigation drawer.");
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            mDrawerToggle.setDrawerIndicatorEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            Log.d(TAG, "Disabling navigation drawer.");
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mDrawerToggle.setDrawerIndicatorEnabled(false);
        }
        mDrawerToggle.syncState();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.isDrawerIndicatorEnabled() && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Fragment createUserHomeFragment() {
        user_home fragment = new user_home();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", this.currentMainUserPhone);

        if (this.initialSelectedStart != null) {
            bundle.putString("selectedStart", this.initialSelectedStart);
        }
        if (this.initialSelectedStop != null) {
            bundle.putString("selectedStop", this.initialSelectedStop);
        }
        if (this.initialRoutes != null) {
            bundle.putString("routes", this.initialRoutes);
        }

        if (showTicketConfirmation) {
            bundle.putBoolean("showTicket", true);
            bundle.putString("transactionId", this.transactionId);
            bundle.putFloat("ticketPrice", this.ticketPrice);
        }

        fragment.setArguments(bundle);
        return fragment;
    }

    private Fragment createUserProfileFragment() {
        user_profile fragment = new user_profile();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", this.currentMainUserPhone);
        bundle.putString("userName", this.currentUserName);
        fragment.setArguments(bundle);
        return fragment;
    }

    private Fragment createUserWalletFragment() {
        user_wallet fragment = new user_wallet();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", this.currentMainUserPhone);
        fragment.setArguments(bundle);
        return fragment;
    }

    private Fragment createUserNotificationFragment(@Nullable String messageFromIntent, @Nullable String enteredNumberFromIntent) {
        user_notification fragment = new user_notification();
        Bundle bundle = new Bundle();
        bundle.putString("mainuserPhone", this.currentMainUserPhone);
        if (messageFromIntent != null) bundle.putString("message", messageFromIntent);
        if (enteredNumberFromIntent != null) bundle.putString("enteredNumber", enteredNumberFromIntent);
        fragment.setArguments(bundle);
        return fragment;
    }

    private void showFragment(Fragment fragment, boolean addToBackStack, String tag) {
        Log.d(TAG, "Showing fragment: " + tag + ", addToBackStack: " + addToBackStack);
        if (isFinishing() || isDestroyed()) {
            Log.w(TAG, "Activity is finishing or destroyed. Cannot commit fragment transaction for " + tag);
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment, tag);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }
        try {
            fragmentTransaction.commit();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error committing fragment transaction for " + tag + ": " + e.getMessage(), e);
        }
    }
}