package com.mol.drivergps.application;

import android.app.Application;

import com.mol.drivergps.db.HelpFactory;

// is announced only to hold all aou components singular \
public class MyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // here we're making sure of singuarity of Helper object \
        HelpFactory.setHelper(getApplicationContext());
    }

    @Override
    public void onTerminate() {
        HelpFactory.releaseHelper();
        super.onTerminate();
    }
}