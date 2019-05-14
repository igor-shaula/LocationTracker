package com.igor_shaula.location_tracker.service;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.igor_shaula.location_tracker.storage.in_memory.InMemory;
import com.igor_shaula.location_tracker.utilities.MyLog;

import java.lang.ref.WeakReference;

public final class MainHandler extends Handler {

    @NonNull
    private WeakReference <MainService> wrMainService;

    MainHandler(@NonNull MainService mainService) {
        wrMainService = new WeakReference <>(mainService);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);

        final MainService mainService = wrMainService.get();
        if (mainService == null) {
            MyLog.e("handleMessage: mainService == null");
            return;
        }

        String whatMeaning = "";
        switch (msg.what) {
            case InMemory.STORAGE_INIT_CLEAR:
                whatMeaning = "storage is prepared and cleaned";
                break;
            case InMemory.STORAGE_SAVE_NEW:
                whatMeaning = "new data is written to the storage";
                break;
            case InMemory.STORAGE_READ_ALL:
                whatMeaning = "all data is read from the storage";
                break;
        }
        // what is need to be updated in UI thread - is here \
        Toast.makeText(mainService , "handleMessage: " + whatMeaning , Toast.LENGTH_SHORT).show();
        MyLog.i("handleMessage: " + whatMeaning);
    }
}