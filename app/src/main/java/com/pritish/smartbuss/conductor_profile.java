package com.pritish.smartbuss;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class conductor_profile extends Fragment implements OnMapReadyCallback, BusRouteData.OnDataLoadedListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private static final String TAG_CONDUCTOR_PROFILE = "ConductorProfile";
    private static final String SHARED_PREF_NAME = "smartbus_pref";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_PHONE_CONDUCTOR = "mainuserPhone";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_CITY = "user_city";
    private static final String KEY_USER_ROUTE = "user_route";
    private static final String KEY_USER_BUS = "user_bus";
    private static final String KEY_BUS_DIRECTION = "bus_direction";
    private static final String KEY_CONDUCTOR_PROFILE_PIC_URI = "conductorProfilePicUri";
    private static final String KEY_IS_SERVICE_RUNNING = "isServiceRunning";

    // Direction constants
    private static final String DIRECTION_FORWARD = "forward";
    private static final String DIRECTION_RETURN = "return";

    private BluetoothAdapter bluetoothAdapter;
    private Handler uiHandler;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private boolean isMapFullScreen = false;
    private ViewGroup nonMapViews;
    private FrameLayout mapContainer;
    private Marker currentLocationMarker;

    private Button logoutButton, startStopServiceButton;
    private SharedPreferences sharedPreferences;
    private ImageView profilePictureImageView;
    private ImageButton iconEditConductorDetailsImageButton;
    private EditText phoneNumberEditText, cityEditText, busEditText;
    private AutoCompleteTextView routeEditText; // Changed from EditText to AutoCompleteTextView
    private TextView userNameTextView, directionStatusTextView;
    private RadioGroup directionRadioGroup;
    private RadioButton forwardRadioButton, returnRadioButton;

    private boolean isEditingMode = false;
    private boolean isPermissionRequested = false;
    private int originalPhoneTextColor, originalCityTextColor, originalRouteTextColor, originalBusTextColor;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<CropImageContractOptions> cropImageLauncher;
    private ActivityResultLauncher<String[]> requestBluetoothPermissionsLauncher;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;

    // Broadcast Receiver
    private BroadcastReceiver bluetoothServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (context == null) {
                Log.e(TAG_CONDUCTOR_PROFILE, "Context is null in onReceive");
                return;
            }
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case BluetoothService.ACTION_GPS_DATA_RECEIVED:
                    double latitude = intent.getDoubleExtra(BluetoothService.EXTRA_LATITUDE, 0.0);
                    double longitude = intent.getDoubleExtra(BluetoothService.EXTRA_LONGITUDE, 0.0);
                    float speed = intent.getFloatExtra(BluetoothService.EXTRA_SPEED, 0.0f);
                    float heading = intent.getFloatExtra("EXTRA_HEADING", 0.0f);
                    updateCurrentLocationOnMap(latitude, longitude, speed, heading);
                    break;
                case BluetoothService.ACTION_BLUETOOTH_CONNECTED:
                    uiHandler.obtainMessage(0, "Bluetooth Connected: " + intent.getStringExtra(BluetoothService.EXTRA_DATA)).sendToTarget();
                    updateServiceButtonState(true);
                    break;
                case BluetoothService.ACTION_BLUETOOTH_DISCONNECTED:
                    uiHandler.obtainMessage(0, "Bluetooth Disconnected: " + intent.getStringExtra(BluetoothService.EXTRA_DATA)).sendToTarget();
                    updateServiceButtonState(false);
                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                        currentLocationMarker = null;
                    }
                    break;
                case BluetoothService.ACTION_BLUETOOTH_CONNECTION_FAILED:
                    uiHandler.obtainMessage(0, "Bluetooth Connection Failed: " + intent.getStringExtra(BluetoothService.EXTRA_DATA)).sendToTarget();
                    updateServiceButtonState(false);
                    break;
                case BluetoothService.ACTION_SERVICE_STATUS_UPDATE:
                    boolean isRunning = intent.getBooleanExtra(BluetoothService.EXTRA_STATUS, false);
                    updateServiceButtonState(isRunning);
                    break;
                case BluetoothService.ACTION_VALIDATION_SUCCESS:
                    uiHandler.obtainMessage(0, "Validation Success: " + intent.getStringExtra(BluetoothService.EXTRA_MESSAGE)).sendToTarget();
                    break;
                case BluetoothService.ACTION_VALIDATION_FAILURE:
                    uiHandler.obtainMessage(0, "Validation Failed: " + intent.getStringExtra(BluetoothService.EXTRA_MESSAGE)).sendToTarget();
                    break;
            }
        }
    };

    public conductor_profile() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = requireActivity().getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        uiHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what == 0 && getContext() != null) {
                    Toast.makeText(getContext(), (String) msg.obj, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        // Initialize BusRouteData
        BusRouteData.initializeDataFromFirebase(this);

        // Initialize image cropping launcher
        cropImageLauncher = registerForActivityResult(new CropImageContract(), result -> {
            if (result.isSuccessful()) {
                Uri croppedImageUri = result.getUriContent();
                if (croppedImageUri != null && profilePictureImageView != null) {
                    profilePictureImageView.setImageURI(croppedImageUri);
                    sharedPreferences.edit().putString(KEY_CONDUCTOR_PROFILE_PIC_URI, croppedImageUri.toString()).apply();
                    if (getContext() != null) Toast.makeText(getContext(), "Profile picture updated!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Exception error = result.getError();
                Log.e(TAG_CONDUCTOR_PROFILE, "Image cropping failed: " + (error != null ? error.getMessage() : "Cancelled"));
            }
        });

        // Initialize permission launchers
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startImageCrop();
                    } else {
                        if (getContext() != null) Toast.makeText(getContext(), "Permission denied to access gallery.", Toast.LENGTH_LONG).show();
                    }
                });

        requestBluetoothPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (String perm : permissions.keySet()) {
                        if (Boolean.FALSE.equals(permissions.get(perm))) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        toggleLocationServiceInternal(true);
                    } else {
                        if (getContext() != null) Toast.makeText(getContext(), "Bluetooth permissions are required.", Toast.LENGTH_LONG).show();
                    }
                });

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        toggleLocationServiceInternal(true);
                    } else {
                        if (getContext() != null) Toast.makeText(getContext(), "Bluetooth must be enabled.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conductor_profile, container, false);
        initializeViews(view);

        Bundle args = getArguments();
        String mainuserPhone = args != null ? args.getString(KEY_USER_PHONE_CONDUCTOR) : null;
        String userName = args != null ? args.getString("userName") : null;

        if (mainuserPhone == null) {
            mainuserPhone = sharedPreferences.getString(KEY_USER_PHONE_CONDUCTOR, null);
        }
        if (mainuserPhone != null && sharedPreferences.getString(KEY_USER_PHONE_CONDUCTOR, null) == null) {
            sharedPreferences.edit().putString(KEY_USER_PHONE_CONDUCTOR, mainuserPhone).apply();
        }

        setUserInfo(mainuserPhone, userName);
        initializeMap();
        loadDataAndSetupUI();
        setupClickListeners(mainuserPhone);

        if (!isPermissionRequested) {
            checkLocationPermission();
            isPermissionRequested = true;
        }

        updateServiceButtonState(sharedPreferences.getBoolean(KEY_IS_SERVICE_RUNNING, false));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothService.ACTION_GPS_DATA_RECEIVED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECTED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_DISCONNECTED);
        filter.addAction(BluetoothService.ACTION_BLUETOOTH_CONNECTION_FAILED);
        filter.addAction(BluetoothService.ACTION_VALIDATION_SUCCESS);
        filter.addAction(BluetoothService.ACTION_VALIDATION_FAILURE);
        filter.addAction(BluetoothService.ACTION_SERVICE_STATUS_UPDATE);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(bluetoothServiceReceiver, filter);
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
                new Intent(BluetoothService.ACTION_SERVICE_STATUS_UPDATE));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(bluetoothServiceReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMap != null) {
            mMap.clear();
        }
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
    }

    private void initializeViews(View view) {
        profilePictureImageView = view.findViewById(R.id.profile_picture);
        iconEditConductorDetailsImageButton = view.findViewById(R.id.icon_edit_conductor_details);
        phoneNumberEditText = view.findViewById(R.id.phone_number_text_view);
        cityEditText = view.findViewById(R.id.citydetail);
        routeEditText = view.findViewById(R.id.routedetail); // This should be AutoCompleteTextView in your layout
        busEditText = view.findViewById(R.id.busdetail);
        userNameTextView = view.findViewById(R.id.user_name);
        logoutButton = view.findViewById(R.id.logout_button);
        startStopServiceButton = view.findViewById(R.id.startStopServiceButton);
        nonMapViews = view.findViewById(R.id.non_map_views);
        mapContainer = view.findViewById(R.id.map_container);

        directionRadioGroup = view.findViewById(R.id.direction_radio_group);
        forwardRadioButton = view.findViewById(R.id.forward_radio_button);
        returnRadioButton = view.findViewById(R.id.return_radio_button);
        directionStatusTextView = view.findViewById(R.id.direction_status);
    }

    // Implement BusRouteData.OnDataLoadedListener methods
    @Override
    public void onDataLoaded() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Setup AutoCompleteTextView with route numbers
                List<String> routeNumbers = BusRouteData.getAllRoutes();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        R.layout.dropdown_item,  // Custom layout
                        routeNumbers
                );
                routeEditText.setAdapter(adapter);
                routeEditText.setThreshold(1); // Start showing suggestions after 1 character

                // Load existing route if available
                String savedRoute = sharedPreferences.getString(KEY_USER_ROUTE, "");
                if (!savedRoute.isEmpty()) {
                    routeEditText.setText(savedRoute.replace("_", "/"));
                }
            });
        }
    }

    @Override
    public void onDataLoadFailed(String errorMessage) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Failed to load route data: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private void setUserInfo(String phone, String name) {
        if (phoneNumberEditText != null) {
            phoneNumberEditText.setText(phone != null ? phone : "Phone: Not available");
            phoneNumberEditText.setEnabled(false);
        }

        if (userNameTextView != null) {
            userNameTextView.setText(name != null ? name : "Conductor Name");
        }
    }

    private void initializeMap() {
        mapFragment = SupportMapFragment.newInstance();
        getChildFragmentManager().beginTransaction().replace(R.id.map_container, mapFragment).commit();
        mapFragment.getMapAsync(this);

        if (mapContainer != null) {
            mapContainer.setOnClickListener(v -> toggleMapFullScreen());
        }
    }

    private void loadDataAndSetupUI() {
        loadConductorProfilePicture();
        loadConductorCardData();
        loadDirectionData();
        storeOriginalTextColors();
        updateConductorFieldsAndIconUI(false);
        setupDirectionRadioGroup();
    }

    private void setupClickListeners(final String mainuserPhoneArgument) {
        final String currentPhone = sharedPreferences.getString(KEY_USER_PHONE_CONDUCTOR, mainuserPhoneArgument);

        if (profilePictureImageView != null) {
            profilePictureImageView.setOnClickListener(v -> checkPermissionAndStartCrop());
        }

        if (iconEditConductorDetailsImageButton != null) {
            iconEditConductorDetailsImageButton.setOnClickListener(v -> {
                isEditingMode = !isEditingMode;
                if (!isEditingMode) {
                    saveConductorCardDetails(currentPhone);
                }
                updateConductorFieldsAndIconUI(isEditingMode);
            });
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> performLogout());
        }

        if (startStopServiceButton != null) {
            startStopServiceButton.setOnClickListener(v -> toggleLocationService());
        }
    }

    private void setupDirectionRadioGroup() {
        if (directionRadioGroup != null) {
            directionRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                String direction;
                if (checkedId == R.id.forward_radio_button) {
                    direction = DIRECTION_FORWARD;
                } else if (checkedId == R.id.return_radio_button) {
                    direction = DIRECTION_RETURN;
                } else {
                    return;
                }
                saveDirection(direction);
                updateDirectionStatus(direction);
                updateMapWithRoute();
                if (sharedPreferences.getBoolean(KEY_IS_SERVICE_RUNNING, false)) {
                    startBluetoothService(true);
                }
                if (getContext() != null) Toast.makeText(getContext(), "Direction updated to: " +
                                (DIRECTION_FORWARD.equals(direction) ? "A → B" : "B → A"),
                        Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateCurrentLocationOnMap(double latitude, double longitude, float speed, float heading) {
        if (mMap == null || getContext() == null) return;

        LatLng currentLatLng = new LatLng(latitude, longitude);

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }

        currentLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title("Current Bus Location")
                .rotation(heading)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.buslocationpicbg))
                .snippet("Lat: " + String.format(Locale.US, "%.6f", latitude) +
                        ", Lng: " + String.format(Locale.US, "%.6f", longitude) +
                        ", Speed: " + String.format(Locale.US, "%.1f", speed) + " km/h" +
                        ", Heading: " + String.format(Locale.US, "%.1f", heading) + "°"));
    }

    private void toggleLocationService() {
        boolean isServiceRunning = sharedPreferences.getBoolean(KEY_IS_SERVICE_RUNNING, false);

        if (!isServiceRunning) {
            if (checkAndRequestBluetoothPermissions()) {
                toggleLocationServiceInternal(true);
            }
        } else {
            toggleLocationServiceInternal(false);
        }
    }

    private void toggleLocationServiceInternal(boolean startService) {
        if (getContext() == null || getActivity() == null) {
            Log.e(TAG_CONDUCTOR_PROFILE, "Context or Activity is null");
            return;
        }

        String routeNumber = sharedPreferences.getString(KEY_USER_ROUTE, "");
        String busNumber = sharedPreferences.getString(KEY_USER_BUS, "");
        String conductorName = sharedPreferences.getString(KEY_USER_NAME, "");
        String conductorPhone = sharedPreferences.getString(KEY_USER_PHONE_CONDUCTOR, "");
        String direction = sharedPreferences.getString(KEY_BUS_DIRECTION, DIRECTION_FORWARD);

        if (startService) {
            if (routeNumber.isEmpty() || routeNumber.equals("N/A") || busNumber.isEmpty() || busNumber.equals("N/A") ||
                    conductorName.isEmpty() || conductorName.equals("N/A") || conductorPhone.isEmpty() || conductorPhone.equals("N/A")) {
                Toast.makeText(getActivity(), "Please ensure all details are set before starting service.", Toast.LENGTH_LONG).show();
                updateServiceButtonState(false);
                return;
            }
            startBluetoothService(true);
        } else {
            stopBluetoothService();
        }
    }

    private boolean checkAndRequestBluetoothPermissions() {
        if (getContext() == null) {
            Log.e(TAG_CONDUCTOR_PROFILE, "Context is null");
            return false;
        }
        String[] permissionsToRequest;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissionsToRequest = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        boolean allGranted = true;
        for (String perm : permissionsToRequest) {
            if (ContextCompat.checkSelfPermission(getContext(), perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            requestBluetoothPermissionsLauncher.launch(permissionsToRequest);
            return false;
        }
        return true;
    }

    private void startBluetoothService(boolean allowBluetoothEnable) {
        if (getContext() == null || getActivity() == null) {
            Log.e(TAG_CONDUCTOR_PROFILE, "Context or Activity is null");
            return;
        }
        if (bluetoothAdapter == null) {
            uiHandler.obtainMessage(0, "Bluetooth not supported").sendToTarget();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            if (allowBluetoothEnable) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        uiHandler.obtainMessage(0, "Bluetooth permission needed").sendToTarget();
                        checkAndRequestBluetoothPermissions();
                        return;
                    }
                }
                enableBluetoothLauncher.launch(enableBtIntent);
            } else {
                uiHandler.obtainMessage(0, "Bluetooth is disabled").sendToTarget();
            }
            return;
        }

        String conductorPhone = sharedPreferences.getString(KEY_USER_PHONE_CONDUCTOR, "");
        String conductorName = sharedPreferences.getString(KEY_USER_NAME, "");
        String busNumber = sharedPreferences.getString(KEY_USER_BUS, "");
        String routeNumber = sharedPreferences.getString(KEY_USER_ROUTE, "");
        String direction = sharedPreferences.getString(KEY_BUS_DIRECTION, DIRECTION_FORWARD);

        Intent serviceIntent = new Intent(getActivity(), BluetoothService.class);
        serviceIntent.putExtra("CONDUCTOR_PHONE", conductorPhone);
        serviceIntent.putExtra("CONDUCTOR_NAME", conductorName);
        serviceIntent.putExtra("BUS_NUMBER", busNumber);
        serviceIntent.putExtra("ROUTE_NUMBER", routeNumber);
        serviceIntent.putExtra("DIRECTION", direction);

        ContextCompat.startForegroundService(requireContext(), serviceIntent);
        sharedPreferences.edit().putBoolean(KEY_IS_SERVICE_RUNNING, true).apply();
        updateServiceButtonState(true);
        uiHandler.obtainMessage(0, "Starting service...").sendToTarget();
    }

    private void stopBluetoothService() {
        if (getContext() == null) return;
        Intent serviceIntent = new Intent(getActivity(), BluetoothService.class);
        getActivity().stopService(serviceIntent);
        sharedPreferences.edit().putBoolean(KEY_IS_SERVICE_RUNNING, false).apply();
        updateServiceButtonState(false);
        uiHandler.obtainMessage(0, "Service stopped").sendToTarget();

        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
            currentLocationMarker = null;
        }
    }

    private void updateServiceButtonState(boolean isRunning) {
        if (startStopServiceButton != null) {
            startStopServiceButton.setText(isRunning ? "Stop Service" : "Start Service");
            int color = isRunning ? ContextCompat.getColor(requireContext(), R.color.red) :
                    ContextCompat.getColor(requireContext(), R.color.green);
            startStopServiceButton.setBackgroundColor(color);
        }
        sharedPreferences.edit().putBoolean(KEY_IS_SERVICE_RUNNING, isRunning).apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_CONDUCTOR_PROFILE, "Location permission granted");
            } else {
                if (getContext() != null) Toast.makeText(getContext(), "Location permission needed", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadConductorProfilePicture() {
        if (sharedPreferences == null || profilePictureImageView == null || getContext() == null) return;
        String imageUriString = sharedPreferences.getString(KEY_CONDUCTOR_PROFILE_PIC_URI, null);
        if (imageUriString != null) {
            try {
                profilePictureImageView.setImageURI(Uri.parse(imageUriString));
            } catch (Exception e) {
                profilePictureImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
        } else {
            profilePictureImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void loadConductorCardData() {
        if (sharedPreferences == null || getContext() == null) return;
        if (cityEditText != null) {
            cityEditText.setText(sharedPreferences.getString(KEY_USER_CITY, "N/A"));
        }
        if (routeEditText != null) {
            routeEditText.setText(sharedPreferences.getString(KEY_USER_ROUTE, "N/A").replace("_", "/"));
        }
        if (busEditText != null) {
            busEditText.setText(sharedPreferences.getString(KEY_USER_BUS, "N/A").replace("_", "/"));
        }
    }

    private void loadDirectionData() {
        if (sharedPreferences == null || getContext() == null) return;
        String savedDirection = sharedPreferences.getString(KEY_BUS_DIRECTION, DIRECTION_FORWARD);

        if (forwardRadioButton != null && returnRadioButton != null && directionRadioGroup != null) {
            if (DIRECTION_FORWARD.equals(savedDirection)) {
                forwardRadioButton.setChecked(true);
            } else {
                returnRadioButton.setChecked(true);
            }
        }
        updateDirectionStatus(savedDirection);
    }

    private void storeOriginalTextColors() {
        if (phoneNumberEditText != null) originalPhoneTextColor = phoneNumberEditText.getCurrentTextColor();
        if (cityEditText != null) originalCityTextColor = cityEditText.getCurrentTextColor();
        if (routeEditText != null) originalRouteTextColor = routeEditText.getCurrentTextColor();
        if (busEditText != null) originalBusTextColor = busEditText.getCurrentTextColor();
    }

    private void updateConductorFieldsAndIconUI(boolean enableEditing) {
        if (getContext() == null) return;
        int editableColor = ContextCompat.getColor(getContext(), R.color.text_color_editable);

        if (cityEditText != null) {
            cityEditText.setEnabled(enableEditing);
            cityEditText.setTextColor(enableEditing ? editableColor : originalCityTextColor);
        }
        if (routeEditText != null) {
            routeEditText.setEnabled(enableEditing);
            routeEditText.setTextColor(enableEditing ? editableColor : originalRouteTextColor);
        }
        if (busEditText != null) {
            busEditText.setEnabled(enableEditing);
            busEditText.setTextColor(enableEditing ? editableColor : originalBusTextColor);
        }
        if (directionRadioGroup != null) {
            for (int i = 0; i < directionRadioGroup.getChildCount(); i++) {
                directionRadioGroup.getChildAt(i).setEnabled(true);
            }
        }
        if (iconEditConductorDetailsImageButton != null) {
            iconEditConductorDetailsImageButton.setImageResource(enableEditing ? R.drawable.ic_save : R.drawable.ic_pencil);
            iconEditConductorDetailsImageButton.setContentDescription(enableEditing ? "Save Details" : "Edit Details");
        }
    }

    private void saveConductorCardDetails(String phone) {
        if (sharedPreferences == null || getContext() == null) {
            Toast.makeText(getContext(), "Error saving details", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(getContext(), "Phone number missing", Toast.LENGTH_LONG).show();
            isEditingMode = true;
            updateConductorFieldsAndIconUI(true);
            return;
        }

        String city = cityEditText.getText().toString().trim();
        String routeForValidation = routeEditText.getText().toString().trim();
        String bus = busEditText.getText().toString().trim();
        String conductorName = userNameTextView.getText().toString().trim();

        if (city.isEmpty() || routeForValidation.isEmpty() || bus.isEmpty() ||
                city.equals("N/A") || routeForValidation.equals("N/A") || bus.equals("N/A") ||
                conductorName.isEmpty() || conductorName.equals("Conductor Name")) {
            Toast.makeText(getContext(), "All fields must be filled", Toast.LENGTH_LONG).show();
            isEditingMode = true;
            updateConductorFieldsAndIconUI(true);
            return;
        }

        List<LatLng> routeStops = BusRouteData.getStopsForRoute(routeForValidation);
        if (routeStops == null || routeStops.isEmpty()) {
            Toast.makeText(getContext(), "Invalid route number", Toast.LENGTH_LONG).show();
            isEditingMode = true;
            updateConductorFieldsAndIconUI(true);
        } else {
            String routeForFirebase = routeForValidation.replace("/", "_");
            String busForFirebase = bus.replace("/", "_");
            saveDetailsToFirebaseAndPrefs(phone, conductorName, city, routeForFirebase, busForFirebase);
        }
    }

    private void saveDetailsToFirebaseAndPrefs(String phone, String name, String city, String route, String bus) {
        String direction = (forwardRadioButton != null && forwardRadioButton.isChecked()) ? DIRECTION_FORWARD : DIRECTION_RETURN;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_CITY, city);
        editor.putString(KEY_USER_ROUTE, route);
        editor.putString(KEY_USER_BUS, bus);
        editor.putString(KEY_BUS_DIRECTION, direction);
        editor.apply();

        DatabaseReference conductorRef = FirebaseDatabase.getInstance().getReference("Conductor").child(phone);
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("city", city);
        updates.put("route", route);
        updates.put("bus", bus);
        updates.put("direction", direction);

        conductorRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Details saved", Toast.LENGTH_SHORT).show();
                        updateDirectionStatus(direction);
                        updateMapWithRoute();
                        isEditingMode = false;
                        updateConductorFieldsAndIconUI(false);

                        if (sharedPreferences.getBoolean(KEY_IS_SERVICE_RUNNING, false)) {
                            startBluetoothService(false);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        isEditingMode = true;
                        updateConductorFieldsAndIconUI(true);
                    }
                });
    }

    private void performLogout() {
        stopBluetoothService();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();

        if (getActivity() != null) {
            Toast.makeText(getActivity(), "Logged out", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getActivity(), AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finishAffinity();
        }
    }

    private void checkPermissionAndStartCrop() {
        if (getContext() == null) return;
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            startImageCrop();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void startImageCrop() {
        if (getContext() == null) return;
        CropImageOptions cropOptions = new CropImageOptions();
        cropOptions.cropShape = CropImageView.CropShape.OVAL;
        cropOptions.aspectRatioX = 1;
        cropOptions.aspectRatioY = 1;
        cropOptions.fixAspectRatio = true;
        CropImageContractOptions contractOptions = new CropImageContractOptions(null, cropOptions);
        cropImageLauncher.launch(contractOptions);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        if (getContext() != null) {
            updateMapWithRoute();
        }
    }

    private void updateMapWithRoute() {
        if (mMap == null || sharedPreferences == null || getContext() == null) return;

        String routeNumberKey = sharedPreferences.getString(KEY_USER_ROUTE, "");
        String direction = sharedPreferences.getString(KEY_BUS_DIRECTION, DIRECTION_FORWARD);

        if (routeNumberKey.isEmpty() || routeNumberKey.equals("N/A")) {
            mMap.clear();
            return;
        }

        String routeNumberForDisplay = routeNumberKey.replace("_", "/");
        List<LatLng> stops = BusRouteData.getStopsForRoute(routeNumberForDisplay);

        if (stops == null || stops.isEmpty()) {
            if (getContext() != null) Toast.makeText(getContext(), "Route data not found", Toast.LENGTH_SHORT).show();
            mMap.clear();
            return;
        }

        mMap.clear();
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(stops)
                .width(12f)
                .color(DIRECTION_FORWARD.equals(direction) ? Color.GREEN : Color.BLUE)
                .clickable(false);
        mMap.addPolyline(polylineOptions);

        if (stops.size() >= 1) {
            LatLng firstStop = stops.get(0);
            LatLng lastStop = stops.get(stops.size() - 1);

            mMap.addMarker(new MarkerOptions().position(firstStop).title("Start of Route " + routeNumberForDisplay));
            if (stops.size() > 1) {
                mMap.addMarker(new MarkerOptions().position(lastStop).title("End of Route " + routeNumberForDisplay));
            }
        }

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (LatLng point : stops) {
            boundsBuilder.include(point);
        }

        if (!stops.isEmpty()){
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
            } catch (IllegalStateException e) {
                mMap.setOnMapLoadedCallback(() -> {
                    try {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
                    } catch (IllegalStateException ex) {
                        Log.e(TAG_CONDUCTOR_PROFILE, "Camera move error: " + ex.getMessage());
                    }
                });
            }
        }
    }

    private void toggleMapFullScreen() {
        if (mapContainer == null || nonMapViews == null || getResources() == null) return;
        isMapFullScreen = !isMapFullScreen;
        if (isMapFullScreen) {
            mapContainer.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            nonMapViews.setVisibility(View.GONE);
        } else {
            mapContainer.getLayoutParams().height = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
            nonMapViews.setVisibility(View.VISIBLE);
        }
        mapContainer.requestLayout();
    }

    private void checkLocationPermission() {
        if (getActivity() == null || getContext() == null) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateDirectionStatus(String direction) {
        if (directionStatusTextView != null && sharedPreferences != null && getContext() != null) {
            String routeNumberForDisplay = sharedPreferences.getString(KEY_USER_ROUTE, "N/A").replace("_", "/");
            String displayText = (routeNumberForDisplay.equals("N/A") || routeNumberForDisplay.isEmpty()) ? "" : " on Route " + routeNumberForDisplay;

            if (DIRECTION_FORWARD.equals(direction)) {
                directionStatusTextView.setText("Direction: A → B" + displayText);
                directionStatusTextView.setTextColor(Color.GREEN);
            } else {
                directionStatusTextView.setText("Direction: B → A" + displayText);
                directionStatusTextView.setTextColor(Color.BLUE);
            }
        }
    }

    private void saveDirection(String direction) {
        if (sharedPreferences != null && getContext() != null) {
            sharedPreferences.edit().putString(KEY_BUS_DIRECTION, direction).apply();
        }
    }
}