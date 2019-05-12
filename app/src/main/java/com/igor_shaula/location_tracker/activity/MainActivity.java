package com.igor_shaula.location_tracker.activity;

import android.Manifest;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.igor_shaula.location_tracker.R;
import com.igor_shaula.location_tracker.events.RadioStateChangeEvent;
import com.igor_shaula.location_tracker.receivers.GpsStateReceiver;
import com.igor_shaula.location_tracker.receivers.InetStateReceiver;
import com.igor_shaula.location_tracker.service.MainService;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;
import com.igor_shaula.location_tracker.utilities.MyLog;
import com.igor_shaula.location_tracker.utilities.U;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    public static final String CN = "MainActivity";

    private Switch sTrackingStatus;
    private TextView tvGpsStatus;
    private TextView tvInetStatus;
    private TextView tvGpsData;
    private TextView tvGpsTime;
    private TextView tvDistance;

    private static int whiteColor;
    private static int primaryDarkColor;
    private static int primaryTextColor;
    private static int accentColor;
    private boolean trackingIsOn;

    private Vibrator vibrator;

// LIFECYCLE =======================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // registering a receiver for GPS state updates - it's needed only in this activtity \
        getApplicationContext().registerReceiver(new GpsStateReceiver() ,
                new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        // registering a receiver for inet state updates - needed only when this activity lives \
        getApplicationContext().registerReceiver(new InetStateReceiver() ,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        setContentView(R.layout.activity_main);

        /* now comes the time of getting all views and setting their listeners and properties */

        sTrackingStatus = findViewById(R.id.sc_TrackingStatus);
        sTrackingStatus.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView , boolean isChecked) {
                        // getting some touch feedback about start/stop action \
                        if (isChecked) {
                            if (isGpsEnabled()) {
                                startTracking();
                                tvGpsData.setText(getString(R.string.gpsDataOn));
                                tvGpsTime.setText(getString(R.string.gpsTimeOn));
                            } else {
                                showSystemScreenForGps();
                                setTrackingSwitchStatus(false);
                            }
                        } else {
                            stopTracking();
                            tvGpsData.setText(getString(R.string.gpsDataOff));
                            tvGpsTime.setText(getString(R.string.gpsTimeOff));
                        }
                        updateGpsData(null);
                    }
                }
        );

        tvGpsStatus = findViewById(R.id.actv_GpsStatus);
        tvInetStatus = findViewById(R.id.actv_InetStatus);
        tvGpsData = findViewById(R.id.actv_GpsData);
        tvGpsTime = findViewById(R.id.actv_GpsTime);
        tvDistance = findViewById(R.id.actvDistance);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 0 = beautiful technique to get colors values in non-deprecated way \
        whiteColor = ContextCompat.getColor(this , android.R.color.white);
        primaryDarkColor = ContextCompat.getColor(this , R.color.primary_dark);
        primaryTextColor = ContextCompat.getColor(this , R.color.primary_text);
        accentColor = ContextCompat.getColor(this , R.color.accent);

        // 1 = checking if the service has already being running at the start of this activity \
        if (isMyServiceRunning()) trackingIsOn = true;
        setTrackingSwitchStatus(trackingIsOn);

        // 2 = checking the state of internet - only to inform user \
        isGpsEnabled();
        isInetEnabled();

        // 3 = this check is necessary for correct application relaunch \
        updateGpsData(null);

        if (U.isAnyPermissionMissed(this)) if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final String[] permissions = new String[2];
            permissions[0] = Manifest.permission.ACCESS_COARSE_LOCATION;
            permissions[1] = Manifest.permission.ACCESS_FINE_LOCATION;
            requestPermissions(permissions , 123);
        } // answer is awaited in onRequestPermissionsResult(..)

    } // end of onCreate-method \

    // TODO: 12.05.2019 get rid of Eventbus ASAP

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

// CHECKERS & VIEW STATE SWITCHERS =================================================================

    // crazy simple magic method - it finds my service among others \
    private boolean isMyServiceRunning() {
        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            MyLog.e("activityManager is null - we must not ever see this");
            return false;
        }
        for (ActivityManager.RunningServiceInfo service :
                activityManager.getRunningServices(Integer.MAX_VALUE)) { // this may take a while
            if (MainService.class.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    // totally independent checking \
    private boolean isGpsEnabled() { // also changes appearance of GPS text view \
        // checking the state of GPS - inform user and later ask him to enable GPS if needed \
        final LocationManager locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled =
                locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        setTvGpsStatus(isGpsEnabled);
        return isGpsEnabled;
    }

    // totally independent checking \
    private boolean isInetEnabled() { // also changes appearance of inet info view \
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null)
            networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isInetEnabled = networkInfo != null && networkInfo.isConnected();
        setTvInetStatus(isInetEnabled);
        return isInetEnabled;
    }

    // providing user with info about GPS sensor availability \
    private void setTvGpsStatus(boolean isGpsEnabled) {
        if (isGpsEnabled) {
            tvGpsStatus.setText(getString(R.string.gpsEnabled));
            tvGpsStatus.setTextColor(primaryDarkColor);
        } else {
            tvGpsStatus.setText(getString(R.string.gpsDisabled));
            tvGpsStatus.setTextColor(primaryTextColor);
        }
    }

    // giving user info about internet connection availability \
    private void setTvInetStatus(boolean isInetEnabled) {
        if (isInetEnabled) {
            tvInetStatus.setText(getString(R.string.inetConnected));
            tvInetStatus.setTextColor(primaryDarkColor);
        } else {
            tvInetStatus.setText(getString(R.string.inetDisconnected));
            tvInetStatus.setTextColor(primaryTextColor);
        }
    }

    private void setTrackingSwitchStatus(boolean statusOn) {
        if (statusOn) {
            sTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOn));
            sTrackingStatus.setTextColor(primaryDarkColor);
            sTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
            // informing user about switching on action \
            vibrator.vibrate(100);
        } else {
            sTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOff));
            sTrackingStatus.setTextColor(whiteColor);
            sTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
        }
        sTrackingStatus.setChecked(statusOn);
        trackingIsOn = statusOn;
    }

// MAIN SET OF METHODS =============================================================================

    private void showSystemScreenForGps() {
        Toast.makeText(MainActivity.this , getString(R.string.toastSwitchGpsOn) , Toast.LENGTH_SHORT).show();
        // opening window with system settings for GPS \
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void startTracking() {
        // preparing intent for qr-code sending service \
        final PendingIntent pendingIntent =
                createPendingResult(GlobalKeys.REQUEST_CODE_MAIN_SERVICE , new Intent() , 0);
        final Intent intentServiceGps = new Intent(this , MainService.class);
        intentServiceGps.putExtra(GlobalKeys.P_I_KEY , pendingIntent);

        startService(intentServiceGps);
        setTrackingSwitchStatus(true);
    }

    private void stopTracking() {
        // here we have to switch service off completely \
        stopService(new Intent(this , MainService.class));
        setTrackingSwitchStatus(false);
    }

    // returning point to this activity \
    @Override
    protected void onActivityResult(int requestCode , int resultCode , Intent data) {
        super.onActivityResult(requestCode , resultCode , data);

        if (requestCode == GlobalKeys.REQUEST_CODE_MAIN_SERVICE) {
            if (resultCode == GlobalKeys.P_I_CODE_DATA_FROM_GPS) {
                MyLog.v("receiving new location point from the service");
                if (trackingIsOn)
                    updateGpsData(data); // data from GPS is obtained and the service is running \
            }
        }
    }

    private void updateGpsData(@Nullable Intent data) {
        if (data != null) {

            // preparing fields for location arguments \
            double latitude = data.getDoubleExtra(GlobalKeys.GPS_LATITUDE , 0.0);
            double longitude = data.getDoubleExtra(GlobalKeys.GPS_LONGITUDE , 0.0);
            final String coordinates = "Lat. " + latitude + " / Long. " + longitude;
            tvGpsData.setText(coordinates);

            // preparing field for time data \
            long timeOfTakingCoordinates = data.getLongExtra(GlobalKeys.GPS_TAKING_TIME , 0);
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeOfTakingCoordinates);
            final String stringTime = "Time: " + calendar.getTime().toString();
            tvGpsTime.setText(stringTime);

            int distance = data.getIntExtra(GlobalKeys.DISTANCE , 0);
            final String distanceText = "Passed distance: " + distance + " m";
            tvDistance.setText(distanceText);

            tvGpsData.setTextColor(accentColor);
            tvGpsTime.setTextColor(accentColor);
        } else {
            // data is absent - we have nothing to show \
            tvGpsData.setTextColor(primaryTextColor);
            tvGpsTime.setTextColor(primaryTextColor);

            if (isMyServiceRunning()) {
                tvGpsData.setText(getString(R.string.gpsDataOn));
                tvGpsTime.setText(getString(R.string.gpsTimeOn));
            } else {
                tvGpsData.setText(getString(R.string.gpsDataOff));
                tvGpsTime.setText(getString(R.string.gpsTimeOff));
            }
        }
    } // end of updateGpsData-method \\

    @Override
    public void onRequestPermissionsResult(int requestCode , @NonNull String[] permissions , @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults);
        Log.d(CN , "onRequestPermissionsResult: requestCode: " + requestCode);
        Log.d(CN , "onRequestPermissionsResult: grantResults[0]: " + grantResults[0]);
    }

    // EVENTBUS ====================================================================================

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(RadioStateChangeEvent event) {

        MyLog.i("onEvent = " + event.getWhatIsChanged());

        switch (event.getWhatIsChanged()) {
            case GlobalKeys.EVENT_INET_ON:
                setTvInetStatus(true);
                break;
            case GlobalKeys.EVENT_INET_OFF:
                setTvInetStatus(false);
                break;
            case GlobalKeys.EVENT_GPS_ON:
                setTvGpsStatus(true);
                break;
            case GlobalKeys.EVENT_GPS_OFF:
                setTvGpsStatus(false);
                break;
        }
    }
}