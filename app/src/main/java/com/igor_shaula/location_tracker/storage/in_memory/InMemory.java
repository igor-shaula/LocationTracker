package com.igor_shaula.location_tracker.storage.in_memory;

import com.igor_shaula.location_tracker.entity.LocationPoint;
import com.igor_shaula.location_tracker.storage.StorageActions;
import com.igor_shaula.location_tracker.utilities.MyLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor shaula
 * <p>
 * acts as a storage implementation for case of long-living service \
 */
public class InMemory implements StorageActions {

    private List<LocationPoint> mLocationPointList;

    private static volatile InMemory sInMemorySingleton;

    private InMemory() {
        // no need to keep it single - because of real singleton above this list \
        mLocationPointList = new ArrayList<>();
    }

    // i prefer the way of Double Checked Locking & volatile \
    public static InMemory getSingleton() {

        InMemory localInstance = sInMemorySingleton;
        // first check \
        if (localInstance == null)
            synchronized (InMemory.class) {
                localInstance = sInMemorySingleton;
                // second check \
                if (localInstance == null)
                    sInMemorySingleton = localInstance = new InMemory();
            }
        return localInstance;
    }

    @Override
    public boolean write(LocationPoint locationPoint) {
        // simple check - if the argument is valid \
        if (locationPoint == null) {
            MyLog.e("InMemory - write: locationPoint == null !!!");
            return false;
        }
        mLocationPointList.add(locationPoint);
        return true;
    }

    @Override
    public List<LocationPoint> readAll() {
        return mLocationPointList;
    }

    @Override
    public void clearAll() {
        for (LocationPoint locationPoint : mLocationPointList) {
            mLocationPointList.remove(locationPoint);
        }
    }
}