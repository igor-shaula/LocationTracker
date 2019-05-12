package com.igor_shaula.location_tracker.utilities;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.igor_shaula.location_tracker.service.MainService;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.LOCATION_SERVICE;

public final class U {

    // PERMISSIONS ---------------------------------------------------------------------------------

    public static boolean isAnyPermissionMissed(@NonNull Context context) {
        return troubleWithPermission(context ,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                || troubleWithPermission(context ,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private static boolean troubleWithPermission(@NonNull Context context ,
                                                 @NonNull String permission) {
        return ActivityCompat.checkSelfPermission(context , permission)
                != PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings("unused")
    @RequiresApi(api = Build.VERSION_CODES.M)
    private static boolean TroubleWithPermission23plus(@NonNull Context context ,
                                                       @NonNull String permission) {
        return context.checkSelfPermission(permission)
                != PackageManager.PERMISSION_GRANTED;
    }

    // FROM MainActivity ---------------------------------------------------------------------------

    // crazy simple magic method - it finds my service among others \
    public static boolean isMyServiceRunning(@NonNull Context context) {
        final ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
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
    public static boolean isGpsEnabled(@NonNull Context context) {
        // checking the state of GPS - inform user and later ask him to enable GPS if needed \
        final LocationManager locationManager =
                (LocationManager) context.getSystemService(LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    // totally independent checking \
    public static boolean isInetEnabled(@NonNull Context context) {
        final ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = null;
        if (connectivityManager != null)
            networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}