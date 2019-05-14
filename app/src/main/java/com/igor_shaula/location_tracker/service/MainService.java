package com.igor_shaula.location_tracker.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.location_tracker.entity.LocationPoint;
import com.igor_shaula.location_tracker.location.LocationConnector;
import com.igor_shaula.location_tracker.location.LocationMath;
import com.igor_shaula.location_tracker.storage.StorageActions;
import com.igor_shaula.location_tracker.storage.in_memory.InMemory;
import com.igor_shaula.location_tracker.utilities.GlobalKeys;
import com.igor_shaula.location_tracker.utilities.MyLog;

import java.lang.ref.WeakReference;
import java.util.List;

// TODO: 14.05.2019 describe the purpose of this service existence
public class MainService extends Service {

    private static final String STORAGE_THREAD = "my-storage-thread";

    private int runnableState;

    @Nullable
    private PendingIntent pendingIntent;

    @NonNull
    private StorageActions storageActions = InMemory.getSingleton();
    @NonNull
    private Handler mainHandler = new MainHandler(this);
    // it will be used to send messages from inside worker threads and catch them inside UI thread \
    @NonNull
    private LocationConnector locationConnector = new LocationConnector(this);
    @NonNull
    private Runnable rStorageTask = new Runnable() {
        @Override
        public void run() {
            // avoiding potential concurrency for resources with main thread \
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            switch (runnableState) {
                // thread works this way by default but only once from the start of the service \
                case InMemory.STORAGE_INIT_CLEAR:
                    storageActions.clearAll();
                    mainHandler.sendEmptyMessage(InMemory.STORAGE_INIT_CLEAR);
                    break;
                // saving new point \
                case InMemory.STORAGE_SAVE_NEW:
                    // taking arguments in such way looks like a crutch - but it's needed \
                    final LocationPoint currentLocationPoint = locationConnector.getCurrentLocationPoint();
                    if (storageActions.write(currentLocationPoint))
                        mainHandler.sendEmptyMessage(InMemory.STORAGE_SAVE_NEW);
                    else MyLog.e("writing to storage failed - that should not ever happen");
                    break;
                // reading all \
                case InMemory.STORAGE_READ_ALL:
                    mainHandler.sendEmptyMessage(InMemory.STORAGE_READ_ALL);
                    break;
            } // end of switch-statement \\
        } // end of run-method \\
    };
    @NonNull
    private Thread storageThread = new Thread(rStorageTask , STORAGE_THREAD);
    @NonNull
    private List <LocationPoint> locationPointList = storageActions.readAll();

// LIFECYCLE =======================================================================================

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent , int flags , int startId) {
        MyLog.v("onStartCommand = MainService started");

        // now starting service as from zero \
        pendingIntent = intent.getParcelableExtra(GlobalKeys.P_I_KEY);
        // when service is restarted after reboot - intent is empty \
        if (pendingIntent == null)
            pendingIntent = PendingIntent.getActivity(getApplicationContext() ,
                    GlobalKeys.REQUEST_CODE_MAIN_SERVICE , new Intent() , 0);

        // all even potentially hard work is kept in other threads \
        storageThread.setDaemon(true);
        storageThread.start();

        // finally launching main sequence to get the location data \
        locationConnector.gpsTrackingStart();

        return Service.START_REDELIVER_INTENT;

        // TODO: 14.06.2016 use WakeLock to prevent processor from sleeping \
    } // end of onStartCommand-method \\

    @Override
    public void onDestroy() {
        super.onDestroy();
        // time to clean all resources \
        locationConnector.clearAllResources();
        mainHandler.removeCallbacks(rStorageTask);
        mainHandler.removeCallbacksAndMessages(null);
    }

// ACTIONS FROM LISTENER ===========================================================================

    // this method is called only from inside location listener - works in main thread \
    public void processLocationUpdate(@NonNull final LocationPoint locationPoint) {

        // the only place of saving current point into database \
        runnableState = 1;
        storageThread.start(); // java.lang.IllegalThreadStateException: Thread already started

        final int[] distance = new int[1];
        // my way to launch one action after another accounting worker threads completion \
        new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    // i have to launch reading data thread only when writing new point is done \
                    case InMemory.STORAGE_SAVE_NEW:
                        MyLog.i("handleMessage: saving thread finished - can begin reading");
                        // now it is possible to safely read all data from the storage \
                        runnableState = 2;
                        storageThread.start();
                        return true;
                    // begin calculations only when reading thread is completed \
                    case InMemory.STORAGE_READ_ALL:
                        // the next step has to be busy with saving new point of data \
                        runnableState = 1;
                        MyLog.i("handleMessage: reading thread finished - can begin calculations");
                        distance[0] = (int) LocationMath.getTotalDistance(locationPointList);
                        // preparing and sending data to MainActivity to update its UI \
                        final Intent intentToReturn = new Intent()
                                .putExtra(GlobalKeys.GPS_LATITUDE , locationPoint.getLatitude())
                                .putExtra(GlobalKeys.GPS_LONGITUDE , locationPoint.getLongitude())
                                .putExtra(GlobalKeys.GPS_TAKING_TIME , locationPoint.getTime())
                                .putExtra(GlobalKeys.DISTANCE , distance[0]);
                        sendIntentToActivity(intentToReturn); // 100
                        return true;
                } // end of switch-statement \\
                return false;
            } // end of handleMessage-method \\
        }); // end of Handler instance definition \\
    } // end of processLocationUpdate-method \\

// UTILS ===========================================================================================

    // universal point to send info to MainActivity \
    private void sendIntentToActivity(@NonNull Intent intent) {
        try {
            if (pendingIntent != null) {
                pendingIntent.send(this , GlobalKeys.P_I_CODE_DATA_FROM_GPS , intent);
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
        }
    }

// MULTITHREADING ==================================================================================

    // created to avoid memory leaks if class not static when using default Handler-class \
    private static class MainHandler extends Handler {

        @NonNull
        WeakReference <MainService> wrMainService;

        private MainHandler(@NonNull MainService mainService) {
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
    } // end of MainHandler-inner-class \\
}