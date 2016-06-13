package com.igor_shaula.location_tracker.utilities;

import android.util.Log;

public class MyLog {

    private static final String TAG = "LOG";

    @SuppressWarnings("unused")
    public static void v(String message) {
        Log.v(TAG, message);
    }

    @SuppressWarnings("unused")
    public static void d(String message) {
        Log.d(TAG, message);
    }

    @SuppressWarnings("unused")
    public static void i(String message) {
        Log.i(TAG, message);
    }

    @SuppressWarnings("unused")
    public static void w(String message) {
        Log.w(TAG, message);
    }

    @SuppressWarnings("unused")
    public static void e(String message) {
        Log.e(TAG, message);
    }
}