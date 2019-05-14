package com.igor_shaula.location_tracker.location;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.location_tracker.R;
import com.igor_shaula.location_tracker.entity.LocationPoint;
import com.igor_shaula.location_tracker.service.MainService;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;
import com.igor_shaula.location_tracker.utilities.MyLog;

import static android.content.Context.LOCATION_SERVICE;

/*
this class has to keep all system-level location dependencies inside,
allowing only app-dependent data to be visible out from it ..
*/
public final class LocationConnector {

    @NonNull
    private MainService mainService; // beware as this field holds Context object inside

    @Nullable
    private LocationManager locationManager;

    @NonNull
    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onProviderDisabled(@Nullable String provider) {
            MyLog.i("onProviderDisabled: " + provider);
            Toast.makeText(mainService ,
                    mainService.getString(R.string.toastGpsProviderOff) , Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(@Nullable String provider) {
            MyLog.i("onProviderEnabled: " + provider);
            Toast.makeText(mainService ,
                    mainService.getString(R.string.toastGpsProviderOn) , Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStatusChanged(@Nullable String provider , int status , @Nullable Bundle extras) {
            MyLog.i("onStatusChanged: provider: " + provider + " & status = " + status);
            if (extras != null) {
                MyLog.i("onStatusChanged: extras: " + extras.getString("satellites"));
            }
        }

        @Override
        public void onLocationChanged(@Nullable Location newLocation) {
            MyLog.i("onLocationChanged = started");
            // the only place where all interesting operations are done \
            if (newLocation != null) {
                // only one (current) location point - for only one line of network requests \

                MyLog.i("provider = " + newLocation.getProvider() + " - location accuracy = " + newLocation.getAccuracy());

                // extracting needed fields - we cannot take the whole object because of Realm restrictions \
                dataLatitude = newLocation.getLatitude();
                dataLongitude = newLocation.getLongitude();
                dataTime = newLocation.getTime();
                if (newLocation.hasSpeed()) dataSpeed = newLocation.getSpeed();
                else dataSpeed = 0; // explicitly clearing value from previous possible point \
                if (newLocation.hasAccuracy()) dataAccuracy = newLocation.getAccuracy();
                else dataAccuracy = 0; // explicitly clearing value from previous possible point \

                mainService.processLocationUpdate(new LocationPoint(
                        dataLatitude , dataLongitude , dataTime , dataSpeed , dataAccuracy));
            }
        }
    }; // end of LocationListener-description \\

    // service ought not keep data in self - so these variables are crutches for multithreading usage \
    private double dataLatitude, dataLongitude;
    private long dataTime;
    private float dataSpeed, dataAccuracy;

    public LocationConnector(@NonNull MainService mainService) {
        this.mainService = mainService;
    }

    // PAYLOAD -------------------------------------------------------------------------------------

    // launched from onStartCommand \
    public void gpsTrackingStart() {

        // this is the global data source of all location information \
        locationManager = (LocationManager) mainService.getSystemService(LOCATION_SERVICE);

        // if all permissions are given - time to launch listening to location updates \
        try {
            if (locationManager != null) {
                locationManager.requestLocationUpdates( // TODO: 14.05.2019 RuntimeException here !!!
                        LocationManager.GPS_PROVIDER , // for GPS - fine precision
                        GlobalKeys.MIN_PERIOD_MILLISECONDS ,
                        GlobalKeys.MIN_DISTANCE_IN_METERS ,
                        locationListener);

                final LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
                if (locationProvider != null) {
                    MyLog.i("accuracy of GPS_PROVIDER = " + locationProvider.getAccuracy());
                }
            } else {
                MyLog.e("");
            }
        } catch (SecurityException se) {
            MyLog.e(se.getLocalizedMessage());
        }
        // TODO: 13.05.2019 handle RuntimeException which is about multithreading here
    } // end of gpsTrackingStart-method \\

    public void clearAllResources() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @NonNull
    public LocationPoint getCurrentLocationPoint() {
        return new LocationPoint(dataLatitude , dataLongitude , dataTime , dataSpeed , dataAccuracy);
    }

    public static float[] getDistanceBetween(double startLat , double startLong , double endLat , double endLong) {
        final float[] resultArray = new float[3]; // 3 is taken after looking into source code
        // result of calculations is stored inside the resultArray \
        Location.distanceBetween(startLat , startLong , endLat , endLong , resultArray);
        return resultArray;
    }
}