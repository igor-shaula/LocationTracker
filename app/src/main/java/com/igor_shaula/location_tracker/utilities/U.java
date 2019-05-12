package com.igor_shaula.location_tracker.utilities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

public final class U {

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
}