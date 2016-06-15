package com.igor_shaula.location_tracker.activity;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.igor_shaula.location_tracker.R;
import com.igor_shaula.location_tracker.events.RadioStateChangeEvent;
import com.igor_shaula.location_tracker.receivers.GpsStateReceiver;
import com.igor_shaula.location_tracker.receivers.InetStateReceiver;
import com.igor_shaula.location_tracker.service.MainService;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;
import com.igor_shaula.location_tracker.utilities.MyLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private SwitchCompat scTrackingStatus;
    private AppCompatTextView actvGpsStatus;
    private AppCompatTextView actvInetStatus;
    private AppCompatTextView actvGpsData;
    private AppCompatTextView actvGpsTime;
    private AppCompatTextView actvDistance;

    private int mWhiteColor;
    private int mPrimaryDarkColor;
    private int mPrimaryTextColor;
    private int mAccentColor;
    private boolean mTrackingIsOn;

    private Vibrator mVibrator;

// LIFECYCLE =======================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // registering a receiver for GPS state updates - it's needed only in this activtity \
        getApplicationContext().registerReceiver(new GpsStateReceiver(),
                new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        // registering a receiver for inet state updates - needed only when this activity lives \
        getApplicationContext().registerReceiver(new InetStateReceiver(),
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        setContentView(R.layout.activity_main);

      /* now comes the time of getting all views and setting their listeners and properties */

        scTrackingStatus = (SwitchCompat) findViewById(R.id.sc_TrackingStatus);
        scTrackingStatus.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // getting some touch feedback about start/stop action \
                        if (isChecked) {
                            if (isGpsEnabled()) {
                                startTracking();
                                actvGpsData.setText(getString(R.string.gpsDataOn));
                                actvGpsTime.setText(getString(R.string.gpsTimeOn));
                            } else {
                                showSystemScreenForGps();
                                setTrackingSwitchStatus(false);
                            }
                        } else {
                            stopTracking();
                            actvGpsData.setText(getString(R.string.gpsDataOff));
                            actvGpsTime.setText(getString(R.string.gpsTimeOff));
                        }
                        updateGpsData(null);
                    } // end of onCheckedChanged-method \\
                } // end of OnCheckedChangeListener-instance defenition \\
        );

        actvGpsStatus = (AppCompatTextView) findViewById(R.id.actv_GpsStatus);
        actvInetStatus = (AppCompatTextView) findViewById(R.id.actv_InetStatus);
        actvGpsData = (AppCompatTextView) findViewById(R.id.actv_GpsData);
        actvGpsTime = (AppCompatTextView) findViewById(R.id.actv_GpsTime);
        actvDistance = (AppCompatTextView) findViewById(R.id.actvDistance);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // 0 = beautiful technique to get colors values in non-deprecated way \
        mWhiteColor = ContextCompat.getColor(this, android.R.color.white);
        mPrimaryDarkColor = ContextCompat.getColor(this, R.color.primary_dark);
        mPrimaryTextColor = ContextCompat.getColor(this, R.color.primary_text);
        mAccentColor = ContextCompat.getColor(this, R.color.accent);

        // 1 = checking if the service has already being running at the start of this activity \
        if (isMyServiceRunning(MainService.class)) mTrackingIsOn = true;
        setTrackingSwitchStatus(mTrackingIsOn);

        // 2 = checking the state of internet - only to inform user \
        isGpsEnabled();
        isInetEnabled();

        // 3 = this check is necessary for correct application relaunch \
        updateGpsData(null);

    } // end of onCreate-method \

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
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) return true;
        }
        return false;
    }

    // totally independent checking \
    private boolean isGpsEnabled() { // also changes appearance of GPS text view \
        // checking the state of GPS - inform user and later ask him to enable GPS if needed \
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        setActvGpsStatus(isGpsEnabled);
        return isGpsEnabled;
    }

    // totally independent checking \
    private boolean isInetEnabled() { // also changes appearance of inet info view \
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null)
            networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isInetEnabled = networkInfo != null && networkInfo.isConnected();
        setActvInetStatus(isInetEnabled);
        return isInetEnabled;
    }

    // providing user with info about GPS sensor availability \
    private void setActvGpsStatus(boolean isGpsEnabled) {
        if (isGpsEnabled) {
            actvGpsStatus.setText(getString(R.string.gpsEnabled));
            actvGpsStatus.setTextColor(mPrimaryDarkColor);
        } else {
            actvGpsStatus.setText(getString(R.string.gpsDisabled));
            actvGpsStatus.setTextColor(mPrimaryTextColor);
        }
    }

    // giving user info about internet connection availability \
    private void setActvInetStatus(boolean isInetEnabled) {
        if (isInetEnabled) {
            actvInetStatus.setText(getString(R.string.inetConnected));
            actvInetStatus.setTextColor(mPrimaryDarkColor);
        } else {
            actvInetStatus.setText(getString(R.string.inetDisconnected));
            actvInetStatus.setTextColor(mPrimaryTextColor);
        }
    }

    private void setTrackingSwitchStatus(boolean statusOn) {
        if (statusOn) {
            scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOn));
            scTrackingStatus.setTextColor(mPrimaryDarkColor);
            scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape);
            // informing user about switching on action \
            mVibrator.vibrate(100);
        } else {
            scTrackingStatus.setText(getString(R.string.textForTrackingSwitchedOff));
            scTrackingStatus.setTextColor(mWhiteColor);
            scTrackingStatus.setBackgroundResource(R.drawable.my_rounded_button_shape_dark);
        }
        scTrackingStatus.setChecked(statusOn);
        mTrackingIsOn = statusOn;
    }

// MAIN SET OF METHODS =============================================================================

    private void showSystemScreenForGps() {
        Toast.makeText(MainActivity.this, getString(R.string.toastSwitchGpsOn), Toast.LENGTH_SHORT).show();
        // opening window with system settings for GPS \
        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    private void startTracking() {
        // preparing intent for qr-code sending service \
        PendingIntent pendingIntent = createPendingResult(GlobalKeys.REQUEST_CODE_MAIN_SERVICE, new Intent(), 0);
        Intent intentServiceGps = new Intent(this, MainService.class);
        intentServiceGps.putExtra(GlobalKeys.P_I_KEY, pendingIntent);

        startService(intentServiceGps);
        setTrackingSwitchStatus(true);
    }

    private void stopTracking() {
        // here we have to switch service off completely \
        stopService(new Intent(this, MainService.class));
        setTrackingSwitchStatus(false);
    }

    // returning point to this activity \
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // remnants of the big statement before refactoring \
        if (requestCode == GlobalKeys.REQUEST_CODE_MAIN_SERVICE) {
            // recognizing what has come from the service by contents of resultCode \
            switch (resultCode) {
                // result about GPS from service - incoming intent available \
                case GlobalKeys.P_I_CODE_DATA_FROM_GPS: {
                    MyLog.v("receiving new location point from the service");
                    if (mTrackingIsOn)
                        updateGpsData(data); // data from GPS is obtained and the service is running \
                    break;
                }
            } // end of switch-statement \\
        } // end of if-statement \\
    } // end of onActivityResult-method \\

    private void updateGpsData(Intent data) {
        if (data != null) {

            // preparing fields for location arguments \
            double latitude = data.getDoubleExtra(GlobalKeys.GPS_LATITUDE, 0.0);
            double longitude = data.getDoubleExtra(GlobalKeys.GPS_LONGITUDE, 0.0);
            String coordinates = String.valueOf("Lat. " + latitude + " / Long. " + longitude);
            actvGpsData.setText(coordinates);

            // preparing field for time data \
            long timeOfTakingCoordinates = data.getLongExtra(GlobalKeys.GPS_TAKING_TIME, 0);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timeOfTakingCoordinates);
            String stringTime = calendar.getTime().toString();
            actvGpsTime.setText(String.valueOf("Time: " + stringTime));

            int distance = data.getIntExtra(GlobalKeys.DISTANCE, 0);
            actvDistance.setText(String.valueOf("Passed distance: " + distance + " m"));

            actvGpsData.setTextColor(mAccentColor);
            actvGpsTime.setTextColor(mAccentColor);
        } else {
            // data is absent - we have nothing to show \
            actvGpsData.setTextColor(mPrimaryTextColor);
            actvGpsTime.setTextColor(mPrimaryTextColor);

            if (isMyServiceRunning(MainService.class)) {
                actvGpsData.setText(getString(R.string.gpsDataOn));
                actvGpsTime.setText(getString(R.string.gpsTimeOn));
            } else {
                actvGpsData.setText(getString(R.string.gpsDataOff));
                actvGpsTime.setText(getString(R.string.gpsTimeOff));
            }
        }
    } // end of updateGpsData-method \\

// EVENTBUS ========================================================================================

    @SuppressWarnings("unused")
    @Subscribe
    public void onEvent(RadioStateChangeEvent event) {

        MyLog.i("onEvent = " + event.getWhatIsChanged());

        switch (event.getWhatIsChanged()) {
            case GlobalKeys.EVENT_INET_ON:
                setActvInetStatus(true);
                break;
            case GlobalKeys.EVENT_INET_OFF:
                setActvInetStatus(false);
                break;
            case GlobalKeys.EVENT_GPS_ON:
                setActvGpsStatus(true);
                break;
            case GlobalKeys.EVENT_GPS_OFF:
                setActvGpsStatus(false);
                break;
        }
    }
}