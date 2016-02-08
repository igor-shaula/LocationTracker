package com.mol.drivergps.zzz_not_used.db;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;

public class HelpFactory {

    private static DatabaseHelper databaseHelper;

    public static DatabaseHelper getDatabaseHelper(){
        return databaseHelper;
    }


    public static void setHelper(Context context){
        databaseHelper = OpenHelperManager.getHelper(context, DatabaseHelper.class);
    }

    public static void releaseHelper(){
        OpenHelperManager.releaseHelper();
        databaseHelper = null;
    }
}